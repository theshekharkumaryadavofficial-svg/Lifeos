package com.example.ui.screens

import android.Manifest
import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.automirrored.rounded.ReceiptLong
import androidx.compose.material.icons.automirrored.rounded.TrendingUp
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.GppBad
import androidx.compose.material.icons.rounded.GppMaybe
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.TrackChanges
import androidx.compose.material.icons.rounded.VerifiedUser
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material.icons.rounded.WorkspacePremium
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.MoneyTransaction
import com.example.data.UiState
import com.example.data.UserProfile
import com.example.money.SmsInboxScanner
import com.example.money.TransactionStatus
import com.example.money.formatInr
import com.example.money.formatTransactionDateLabel
import com.example.ui.viewmodel.LifeViewModel
import com.example.ui.viewmodel.MoneySummaryState
import java.util.Calendar
import kotlin.math.abs

private enum class MoneyRoute {
    HOME,
    HISTORY,
    GOALS,
    SETTINGS,
    ADD_TRANSACTION,
    ANALYTICS,
    TRUST_CENTER
}

@Composable
fun MoneyTrackerScreen(
    viewModel: LifeViewModel,
    onExitToLifeOs: () -> Unit
) {
    val context = LocalContext.current
    val view = LocalView.current
    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(view) {
        val window = (context as? Activity)?.window
        val controller = window?.let { WindowCompat.getInsetsController(it, view) }
        val prevLightStatus = controller?.isAppearanceLightStatusBars ?: false
        val prevLightNav = controller?.isAppearanceLightNavigationBars ?: false
        controller?.isAppearanceLightStatusBars = true
        controller?.isAppearanceLightNavigationBars = true
        onDispose {
            controller?.isAppearanceLightStatusBars = prevLightStatus
            controller?.isAppearanceLightNavigationBars = prevLightNav
        }
    }

    val profileState by viewModel.profileState.collectAsStateWithLifecycle()
    val transactionsState by viewModel.moneyTransactionsState.collectAsStateWithLifecycle()
    val summary by viewModel.moneySummary.collectAsStateWithLifecycle()
    val smsSyncing by viewModel.smsSyncing.collectAsStateWithLifecycle()
    val snackbarMessage by viewModel.snackbarMessage.collectAsStateWithLifecycle()

    val profile = (profileState as? UiState.Success)?.data ?: UserProfile()
    val transactions = (transactionsState as? UiState.Success)?.data.orEmpty()

    var hasSmsPermission by remember { mutableStateOf(SmsInboxScanner.hasSmsPermissions(context)) }
    var activeRoute by rememberSaveable { mutableStateOf(MoneyRoute.HOME) }
    var trustCenterInitialTab by rememberSaveable { mutableStateOf(TrustCenterTab.OVERVIEW) }
    var inspectingTx by remember { mutableStateOf<MoneyTransaction?>(null) }
    var localWelcomeDismissed by rememberSaveable { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(snackbarMessage) {
        snackbarMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearSnackbar()
        }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                hasSmsPermission = SmsInboxScanner.hasSmsPermissions(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // Quiet background SMS inbox sync on entry when permission is granted & tracking is enabled
    LaunchedEffect(hasSmsPermission, profile.moneySmsTrackingEnabled) {
        if (hasSmsPermission && profile.moneySmsTrackingEnabled) {
            viewModel.syncBankSmsInbox(silentIfZero = true)
        }
    }

    val smsPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        val granted = result[Manifest.permission.READ_SMS] == true
        hasSmsPermission = SmsInboxScanner.hasSmsPermissions(context)
        if (granted) {
            viewModel.setMoneySmsTrackingEnabled(true)
        }
    }

    fun requestSmsPermissions() {
        smsPermissionLauncher.launch(
            arrayOf(Manifest.permission.READ_SMS, Manifest.permission.RECEIVE_SMS)
        )
    }

    fun openTrustCenter(tab: TrustCenterTab = TrustCenterTab.OVERVIEW) {
        trustCenterInitialTab = tab
        activeRoute = MoneyRoute.TRUST_CENTER
    }

    BackHandler {
        when (activeRoute) {
            MoneyRoute.ADD_TRANSACTION,
            MoneyRoute.ANALYTICS,
            MoneyRoute.TRUST_CENTER -> activeRoute = MoneyRoute.HOME
            MoneyRoute.HOME -> onExitToLifeOs()
            else -> activeRoute = MoneyRoute.HOME
        }
    }

    // Panel 1: Welcome Splash (Glass Jar of Gold ₹ Coins + Sprout Leaf)
    val showWelcome = !profile.moneyOnboardingSeen && !localWelcomeDismissed
    if (showWelcome) {
        MoneyWelcomeSplash(
            onGetStarted = {
                localWelcomeDismissed = true
                viewModel.markMoneyOnboardingSeen()
            },
            onBack = onExitToLifeOs
        )
        return
    }

    val showBottomBar = activeRoute in setOf(
        MoneyRoute.HOME,
        MoneyRoute.HISTORY,
        MoneyRoute.GOALS,
        MoneyRoute.SETTINGS,
        MoneyRoute.ANALYTICS
    )

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .background(MoneyBg)
            .statusBarsPadding(),
        containerColor = MoneyBg,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            if (showBottomBar) {
                MoneyBottomNavBar(
                    currentRoute = activeRoute,
                    onSelectRoute = { activeRoute = it },
                    onAddClick = { activeRoute = MoneyRoute.ADD_TRANSACTION }
                )
            }
        }
    ) { innerPadding ->
        AnimatedContent(
            targetState = activeRoute,
            transitionSpec = { fadeIn(tween(220)) togetherWith fadeOut(tween(150)) },
            label = "money_route_anim"
        ) { route ->
            when (route) {
                MoneyRoute.HOME -> MoneyHomeDashboard(
                    profile = profile,
                    summary = summary,
                    transactions = transactions,
                    hasSmsPermission = hasSmsPermission,
                    smsSyncing = smsSyncing,
                    onBackToLifeOs = onExitToLifeOs,
                    onOpenTrustCenter = { tab -> openTrustCenter(tab) },
                    onOpenAnalytics = { activeRoute = MoneyRoute.ANALYTICS },
                    onOpenGoals = { activeRoute = MoneyRoute.GOALS },
                    onSyncSmsNow = {
                        if (hasSmsPermission) viewModel.syncBankSmsInbox(silentIfZero = false)
                        else requestSmsPermissions()
                    },
                    bottomPadding = innerPadding
                )

                MoneyRoute.HISTORY -> MoneyTransactionHistoryScreen(
                    transactions = transactions,
                    summary = summary,
                    smsSyncing = smsSyncing,
                    onBack = { activeRoute = MoneyRoute.HOME },
                    onSyncSms = {
                        if (hasSmsPermission) viewModel.syncBankSmsInbox(silentIfZero = false)
                        else requestSmsPermissions()
                    },
                    onOpenTrustCenter = { tab -> openTrustCenter(tab) },
                    onSelectTransaction = { inspectingTx = it },
                    bottomPadding = innerPadding
                )

                MoneyRoute.ADD_TRANSACTION -> MoneyAddTransactionScreen(
                    onBack = { activeRoute = MoneyRoute.HOME },
                    onSave = { title, amount, type, category, mode, ts ->
                        viewModel.addManualMoneyTransaction(title, amount, type, category, mode, ts)
                    }
                )

                MoneyRoute.ANALYTICS -> MoneyAnalyticsScreen(
                    transactions = transactions,
                    onBack = { activeRoute = MoneyRoute.HOME },
                    bottomPadding = innerPadding
                )

                MoneyRoute.GOALS -> MoneyGoalsAndSettingsScreen(
                    showSettingsFocus = false,
                    profile = profile,
                    summary = summary,
                    transactions = transactions,
                    onOpenAnalytics = { activeRoute = MoneyRoute.ANALYTICS },
                    onOpenTrustCenter = { tab -> openTrustCenter(tab) },
                    onUpdateBudget = { viewModel.updateMoneyMonthlyBudget(it) },
                    onUpdateOpeningBalance = { viewModel.updateMoneyOpeningBalance(it) },
                    onUpdateSavingsGoal = { n, t, s, d -> viewModel.updateSavingsGoal(n, t, s, d) },
                    onDeleteImportedSmsData = { viewModel.deleteAllImportedSmsTransactions() },
                    onClearAllTransactions = { viewModel.clearAllMoneyTransactions() },
                    onExitToLifeOs = onExitToLifeOs,
                    bottomPadding = innerPadding
                )

                MoneyRoute.SETTINGS -> MoneyGoalsAndSettingsScreen(
                    showSettingsFocus = true,
                    profile = profile,
                    summary = summary,
                    transactions = transactions,
                    onOpenAnalytics = { activeRoute = MoneyRoute.ANALYTICS },
                    onOpenTrustCenter = { tab -> openTrustCenter(tab) },
                    onUpdateBudget = { viewModel.updateMoneyMonthlyBudget(it) },
                    onUpdateOpeningBalance = { viewModel.updateMoneyOpeningBalance(it) },
                    onUpdateSavingsGoal = { n, t, s, d -> viewModel.updateSavingsGoal(n, t, s, d) },
                    onDeleteImportedSmsData = { viewModel.deleteAllImportedSmsTransactions() },
                    onClearAllTransactions = { viewModel.clearAllMoneyTransactions() },
                    onExitToLifeOs = onExitToLifeOs,
                    bottomPadding = innerPadding
                )

                MoneyRoute.TRUST_CENTER -> MoneyTrustCenterScreen(
                    profile = profile,
                    summary = summary,
                    transactions = transactions,
                    hasSmsPermission = hasSmsPermission,
                    smsSyncing = smsSyncing,
                    initialTab = trustCenterInitialTab,
                    onBack = { activeRoute = MoneyRoute.HOME },
                    onToggleSmsTracking = { viewModel.setMoneySmsTrackingEnabled(it) },
                    onRequestSmsPermission = { requestSmsPermissions() },
                    onSyncSmsNow = { viewModel.syncBankSmsInbox(silentIfZero = false) },
                    onReAuditSmsNow = { viewModel.reAuditImportedSmsNow() },
                    onAddParsedSms = { viewModel.addParsedSmsTransaction(it) },
                    onApprovePending = { tx, trust -> viewModel.approvePendingTransaction(tx, trust) },
                    onQuarantineTx = { viewModel.quarantineTransaction(it) },
                    onDeleteTx = { viewModel.deleteMoneyTransaction(it) },
                    onClearQuarantined = { viewModel.deleteAllQuarantinedTransactions() },
                    onAddTrustedSender = { viewModel.addCustomTrustedSender(it) },
                    onRemoveTrustedSender = { viewModel.removeCustomTrustedSender(it) },
                    onInspectTransaction = { inspectingTx = it }
                )
            }
        }
    }

    inspectingTx?.let { tx ->
        TransactionVerificationDetailDialog(
            tx = tx,
            onDismiss = { inspectingTx = null },
            onUpdateTx = { viewModel.updateManualTransaction(it) },
            onApproveTx = { target, trust -> viewModel.approvePendingTransaction(target, trust) },
            onQuarantineTx = { viewModel.quarantineTransaction(it) },
            onDeleteTx = { viewModel.deleteMoneyTransaction(it) }
        )
    }
}

// --- PANEL 1: WELCOME SPLASH ---

@Composable
private fun MoneyWelcomeSplash(
    onGetStarted: () -> Unit,
    onBack: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color(0xFF43B5F4),
                        Color(0xFFD9F3FF),
                        Color(0xFFFFFFFF),
                        Color(0xFFD5FAE6)
                    )
                )
            )
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 24.dp, vertical = 18.dp)
    ) {
        IconButton(
            onClick = onBack,
            modifier = Modifier
                .align(Alignment.TopStart)
                .size(48.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.88f))
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                contentDescription = "Back to LifeOS",
                tint = MoneyInk
            )
        }

        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            MoneyWelcomeHeroArtwork(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(255.dp)
            )

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "Money\nTracker",
                color = MoneyGreenDeep,
                fontSize = 46.sp,
                lineHeight = 50.sp,
                fontWeight = FontWeight.Black,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Plan  •  Track  •  Grow",
                color = MoneyInk,
                fontSize = 19.sp,
                fontWeight = FontWeight.Black
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "Take control of your money\nand build a better future!",
                color = MoneySecondary,
                fontSize = 16.sp,
                lineHeight = 23.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(modifier = Modifier.width(34.dp).height(2.dp).background(MoneyGold.copy(alpha = 0.65f)))
                Icon(Icons.Rounded.WorkspacePremium, contentDescription = null, tint = MoneyGold, modifier = Modifier.size(22.dp))
                Box(modifier = Modifier.width(34.dp).height(2.dp).background(MoneyGold.copy(alpha = 0.65f)))
            }

            Spacer(modifier = Modifier.height(28.dp))

            Button(
                onClick = onGetStarted,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(58.dp)
                    .testTag("money_get_started_button"),
                shape = RoundedCornerShape(50),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MoneyGreenDeep,
                    contentColor = Color.White
                )
            ) {
                Text(
                    text = "Get Started",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Black
                )
                Spacer(modifier = Modifier.width(10.dp))
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                    contentDescription = null,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    }
}

// --- PANEL 2: MONEY TRACKER HOME DASHBOARD ---

@Composable
private fun MoneyHomeDashboard(
    profile: UserProfile,
    summary: MoneySummaryState,
    transactions: List<MoneyTransaction>,
    hasSmsPermission: Boolean,
    smsSyncing: Boolean,
    onBackToLifeOs: () -> Unit,
    onOpenTrustCenter: (TrustCenterTab) -> Unit,
    onOpenAnalytics: () -> Unit,
    onOpenGoals: () -> Unit,
    onSyncSmsNow: () -> Unit,
    bottomPadding: PaddingValues
) {
    var hideBalance by rememberSaveable { mutableStateOf(false) }

    val firstName = profile.displayName.trim().substringBefore(" ").ifEmpty { "Shekhar" }
    val initial = firstName.firstOrNull()?.uppercaseChar()?.toString() ?: "S"
    val flaggedAlerts = summary.pendingCount + summary.quarantinedCount

    // Compute current month confirmed category slices for "This Month Overview" donut card
    val confirmedMonthExpenses = remember(transactions) {
        val nowCal = Calendar.getInstance()
        val curMonth = nowCal.get(Calendar.MONTH)
        val curYear = nowCal.get(Calendar.YEAR)
        val cal = Calendar.getInstance()
        transactions.filter { tx ->
            if (tx.status != TransactionStatus.CONFIRMED.key || tx.type != "EXPENSE") return@filter false
            cal.timeInMillis = tx.timestampMs
            cal.get(Calendar.MONTH) == curMonth && cal.get(Calendar.YEAR) == curYear
        }
    }
    val monthSpent = confirmedMonthExpenses.sumOf { it.amount }
    val overviewSlices = remember(confirmedMonthExpenses, monthSpent) {
        val grouped = confirmedMonthExpenses.groupBy { it.category }
            .mapValues { (_, list) -> list.sumOf { it.amount } }
        val food = grouped["Food"] ?: 0.0
        val study = grouped["Study"] ?: 0.0
        val travel = grouped["Travel"] ?: 0.0
        val others = (monthSpent - food - study - travel).coerceAtLeast(0.0)
        listOf(
            CategorySlice("Food", food, if (monthSpent > 0) ((food / monthSpent) * 100).toInt() else 0, Color(0xFF15803D)),
            CategorySlice("Study", study, if (monthSpent > 0) ((study / monthSpent) * 100).toInt() else 0, Color(0xFFF59E0B)),
            CategorySlice("Travel", travel, if (monthSpent > 0) ((travel / monthSpent) * 100).toInt() else 0, Color(0xFF2563EB)),
            CategorySlice("Others", others, if (monthSpent > 0) ((others / monthSpent) * 100).toInt() else 0, Color(0xFF7C3AED))
        )
    }

    val netPctLabel = remember(summary) {
        if (summary.confirmedIncome > 0.0) {
            val savedPct = (((summary.confirmedIncome - summary.confirmedExpense) / summary.confirmedIncome) * 100.0).toInt()
            if (savedPct >= 0) "↑ +$savedPct%" else "↓ $savedPct%"
        } else {
            "Verified"
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MoneyBg),
        contentPadding = PaddingValues(
            start = 18.dp,
            end = 18.dp,
            top = 12.dp,
            bottom = bottomPadding.calculateBottomPadding() + 96.dp
        ),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Header: Back + Avatar + "Hello Shekhar 👋" + "Keep going! 💪" + Bell (SMS Trust Center)
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBackToLifeOs,
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(MoneyCardWhite)
                        .border(1.dp, MoneyBorder, CircleShape)
                        .testTag("money_home_back_button")
                ) {
                    Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back to LifeOS", tint = MoneyInk)
                }

                Spacer(modifier = Modifier.width(10.dp))

                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFCBD5E1)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = initial,
                        color = MoneyInk,
                        fontSize = 21.sp,
                        fontWeight = FontWeight.Black
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Hello $firstName 👋",
                        color = MoneyInk,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Black,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "Keep going! 💪",
                        color = MoneySecondary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                IconButton(
                    onClick = {
                        val targetTab = when {
                            summary.quarantinedCount > 0 -> TrustCenterTab.QUARANTINE
                            summary.pendingCount > 0 -> TrustCenterTab.PENDING
                            else -> TrustCenterTab.OVERVIEW
                        }
                        onOpenTrustCenter(targetTab)
                    },
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(MoneyCardWhite)
                        .border(1.dp, MoneyBorder, CircleShape)
                        .testTag("money_bell_trust_center_button")
                ) {
                    Box {
                        Icon(
                            imageVector = Icons.Rounded.Notifications,
                            contentDescription = "SMS Trust Center",
                            tint = MoneyInk,
                            modifier = Modifier.size(25.dp)
                        )
                        Box(
                            modifier = Modifier
                                .size(11.dp)
                                .align(Alignment.TopEnd)
                                .clip(CircleShape)
                                .background(if (flaggedAlerts > 0 || !hasSmsPermission) MoneyRed else MoneyGreenMain)
                        )
                    }
                }
            }
        }

        // 2. Deep Emerald Total Balance Card (Panel 2 Hero)
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(10.dp, RoundedCornerShape(26.dp), spotColor = MoneyGreenDeep.copy(alpha = 0.35f))
                    .clip(RoundedCornerShape(26.dp))
                    .background(
                        Brush.linearGradient(
                            listOf(Color(0xFF044E36), MoneyGreenDeep, Color(0xFF0B8550))
                        )
                    )
                    .testTag("money_total_balance_card")
            ) {
                BalanceRisingBarsOverlay(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .width(116.dp)
                        .height(114.dp)
                        .padding(end = 10.dp, bottom = 10.dp)
                )

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 22.dp, vertical = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Total Balance",
                            color = Color.White.copy(alpha = 0.92f),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Icon(
                            imageVector = if (hideBalance) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                            contentDescription = if (hideBalance) "Show Balance" else "Hide Balance",
                            tint = Color.White.copy(alpha = 0.9f),
                            modifier = Modifier
                                .size(22.dp)
                                .clickable { hideBalance = !hideBalance }
                                .testTag("money_toggle_balance_visibility")
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = if (hideBalance) "₹ ••••••" else formatInr(summary.calculatedAppBalance),
                            color = Color.White,
                            fontSize = 38.sp,
                            fontWeight = FontWeight.Black,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(Color.White.copy(alpha = 0.18f))
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = netPctLabel,
                                color = Color(0xFF86EFAC),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Black
                            )
                        }
                    }

                    // Subtle Verified SMS Bank Balance pill if present
                    summary.smsReportedBankBalance?.let { verifiedBal ->
                        Text(
                            text = "Verified SMS Bank Bal: ${formatInr(verifiedBal)}",
                            color = Color(0xFFD1FAE5),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }
            }
        }

        // 3. Soft-Mint Total Income & Soft-Rose Total Expense Side-by-Side Cards (Panel 2)
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                IncomeExpensePillCard(
                    label = "Total Income",
                    value = formatInr(summary.confirmedIncome),
                    isIncome = true,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("money_total_income_card")
                )
                IncomeExpensePillCard(
                    label = "Total Expense",
                    value = formatInr(summary.confirmedExpense),
                    isIncome = false,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("money_total_expense_card")
                )
            }
        }

        // 4. Compact SMS Fraud Shield / Pending Review / Reconciliation Banner
        item {
            val hasFraud = summary.quarantinedCount > 0
            val hasPending = summary.pendingCount > 0
            val hasReconcileAlert = summary.reconciliationDiff?.let { abs(it) >= 1.0 } == true

            val bannerBg = when {
                hasFraud -> MoneySoftRed
                hasPending || hasReconcileAlert -> MoneySoftAmber
                else -> MoneySoftGreen
            }
            val bannerAccent = when {
                hasFraud -> MoneyRed
                hasPending || hasReconcileAlert -> MoneyAmber
                else -> MoneyGreenDeep
            }
            val bannerTitle = when {
                hasFraud -> "${summary.quarantinedCount} Fraud SMS Blocked"
                hasPending -> "${summary.pendingCount} SMS Pending Review"
                hasReconcileAlert -> "Balance Reconciliation Alert"
                else -> "SMS Fraud Shield Active"
            }
            val reconcileDiffAmt = summary.reconciliationDiff
            val bannerSubtitle = when {
                hasFraud -> "Excluded ${formatInr(summary.quarantinedTotal)} from balance • Tap to inspect"
                hasPending -> "Unverified SMS held safely out of balance • Tap to review"
                hasReconcileAlert && reconcileDiffAmt != null -> "Verified SMS bank balance differs by ${formatInr(abs(reconcileDiffAmt))}"
                else -> "${summary.smsTrackedCount} SMS checked • Tap for Trust Center & Tester"
            }

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        val tab = when {
                            hasFraud -> TrustCenterTab.QUARANTINE
                            hasPending -> TrustCenterTab.PENDING
                            else -> TrustCenterTab.OVERVIEW
                        }
                        onOpenTrustCenter(tab)
                    }
                    .testTag("money_sms_trust_banner"),
                shape = RoundedCornerShape(20.dp),
                color = bannerBg,
                border = BorderStroke(1.2.dp, bannerAccent.copy(alpha = 0.45f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(bannerAccent),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = when {
                                hasFraud -> Icons.Rounded.GppBad
                                hasPending || hasReconcileAlert -> Icons.Rounded.Warning
                                else -> Icons.Rounded.VerifiedUser
                            },
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(bannerTitle, color = MoneyInk, fontSize = 15.sp, fontWeight = FontWeight.Black)
                        Text(bannerSubtitle, color = MoneySecondary, fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 2)
                    }
                    if (smsSyncing) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), color = bannerAccent, strokeWidth = 2.dp)
                    } else {
                        IconButton(
                            onClick = onSyncSmsNow,
                            modifier = Modifier.size(38.dp)
                        ) {
                            Icon(Icons.Rounded.Refresh, contentDescription = "Sync SMS", tint = bannerAccent)
                        }
                    }
                }
            }
        }

        // 5. "This Month Overview" Donut Ring + Category Legend Card (exact match to Panel 2)
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "This Month Overview",
                        color = MoneyInk,
                        fontSize = 21.sp,
                        fontWeight = FontWeight.Black
                    )
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .clickable(onClick = onOpenAnalytics)
                            .padding(horizontal = 6.dp, vertical = 4.dp)
                            .testTag("money_view_all_analytics_button"),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("View All", color = MoneyGreenDeep, fontSize = 15.sp, fontWeight = FontWeight.Black)
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(Icons.AutoMirrored.Rounded.ArrowForward, contentDescription = null, tint = MoneyGreenDeep, modifier = Modifier.size(18.dp))
                    }
                }

                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = onOpenAnalytics),
                    shape = RoundedCornerShape(24.dp),
                    color = MoneyCardWhite,
                    shadowElevation = 2.dp,
                    border = BorderStroke(1.dp, MoneyBorder)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        AnalyticsDonutChart(
                            slices = overviewSlices,
                            totalExpense = monthSpent,
                            centerLabel = "Spent"
                        )
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(11.dp)
                        ) {
                            overviewSlices.forEach { slice ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(11.dp)
                                                .clip(CircleShape)
                                                .background(slice.color)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = slice.name,
                                            color = MoneyInk,
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.ExtraBold
                                        )
                                    }
                                    Text(
                                        text = "${slice.percent}%",
                                        color = MoneyInk,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Black
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // 6. Warm Gold "You're on track!" Trophy Card (exact match to Panel 2)
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(22.dp))
                    .background(
                        Brush.horizontalGradient(
                            listOf(Color(0xFFFFF8DB), Color(0xFFFEF3C7), Color(0xFFE7F9EE))
                        )
                    )
                    .border(1.dp, Color(0xFFFDE68A), RoundedCornerShape(22.dp))
                    .clickable(onClick = onOpenGoals)
                    .padding(horizontal = 18.dp, vertical = 16.dp)
                    .testTag("money_on_track_card")
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFFDE68A).copy(alpha = 0.7f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.EmojiEvents,
                            contentDescription = null,
                            tint = Color(0xFFB45309),
                            modifier = Modifier.size(34.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "You're on track!",
                            color = MoneyInk,
                            fontSize = 19.sp,
                            fontWeight = FontWeight.Black
                        )
                        Text(
                            text = "Save more, dream bigger.",
                            color = MoneySecondary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.TrendingUp,
                        contentDescription = null,
                        tint = MoneyGreenMain,
                        modifier = Modifier.size(34.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun IncomeExpensePillCard(
    label: String,
    value: String,
    isIncome: Boolean,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(22.dp),
        color = if (isIncome) Color(0xFFECFDF3) else Color(0xFFFFF1F2),
        border = BorderStroke(
            1.dp,
            if (isIncome) MoneyGreenMain.copy(alpha = 0.25f) else MoneyRed.copy(alpha = 0.22f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 13.dp, vertical = 15.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(if (isIncome) MoneyGreenBright else Color(0xFFF43F5E)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isIncome) Icons.Rounded.ArrowDownward else Icons.Rounded.ArrowUpward,
                    contentDescription = label,
                    tint = Color.White,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = label,
                    color = MoneySecondary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.ExtraBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = value,
                    color = MoneyInk,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Black,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

// --- PANEL 4: TRANSACTION HISTORY SCREEN ---

@Composable
private fun MoneyTransactionHistoryScreen(
    transactions: List<MoneyTransaction>,
    summary: MoneySummaryState,
    smsSyncing: Boolean,
    onBack: () -> Unit,
    onSyncSms: () -> Unit,
    onOpenTrustCenter: (TrustCenterTab) -> Unit,
    onSelectTransaction: (MoneyTransaction) -> Unit,
    bottomPadding: PaddingValues
) {
    var selectedFilter by rememberSaveable { mutableStateOf("All") }

    // History shows CONFIRMED transactions by default so quarantined fraud never pollutes main history
    val confirmedTransactions = remember(transactions) {
        transactions.filter { it.status == TransactionStatus.CONFIRMED.key }
    }
    val filtered = remember(confirmedTransactions, selectedFilter) {
        when (selectedFilter) {
            "Income" -> confirmedTransactions.filter { it.type == "INCOME" }
            "Expense" -> confirmedTransactions.filter { it.type == "EXPENSE" }
            else -> confirmedTransactions
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MoneyBg),
        contentPadding = PaddingValues(
            start = 18.dp,
            end = 18.dp,
            top = 14.dp,
            bottom = bottomPadding.calculateBottomPadding() + 96.dp
        ),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Header (exact match to Panel 4)
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(MoneyCardWhite)
                        .border(1.dp, MoneyBorder, CircleShape)
                        .testTag("history_back_button")
                ) {
                    Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back", tint = MoneyInk)
                }
                Text(
                    text = "Transaction History",
                    color = MoneyInk,
                    fontSize = 23.sp,
                    fontWeight = FontWeight.Black
                )
                IconButton(
                    onClick = onSyncSms,
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(MoneyCardWhite)
                        .border(1.dp, MoneyBorder, CircleShape)
                        .testTag("history_sync_button")
                ) {
                    if (smsSyncing) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), color = MoneyGreenMain, strokeWidth = 2.4.dp)
                    } else {
                        Icon(Icons.Rounded.CalendarMonth, contentDescription = "Sync SMS", tint = MoneyInk)
                    }
                }
            }
        }

        // Filter Pills: All | Income | Expense (matching Panel 4)
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                listOf("All", "Income", "Expense").forEach { filter ->
                    val selected = selectedFilter == filter
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(50))
                            .background(if (selected) MoneyGreenDeep else MoneyChipBg)
                            .clickable { selectedFilter = filter }
                            .padding(vertical = 12.dp)
                            .testTag("money_filter_$filter"),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = filter,
                            color = if (selected) Color.White else MoneyInk,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                }
            }
        }

        // Quick jump chip if Pending or Quarantined SMS exist
        if (summary.pendingCount > 0 || summary.quarantinedCount > 0) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (summary.pendingCount > 0) {
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clickable { onOpenTrustCenter(TrustCenterTab.PENDING) },
                            shape = RoundedCornerShape(14.dp),
                            color = MoneySoftAmber,
                            border = BorderStroke(1.dp, MoneyAmber.copy(alpha = 0.5f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(Icons.Rounded.GppMaybe, contentDescription = null, tint = MoneyAmber, modifier = Modifier.size(18.dp))
                                Text(
                                    text = "${summary.pendingCount} Pending Review",
                                    color = MoneyInk,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Black
                                )
                            }
                        }
                    }
                    if (summary.quarantinedCount > 0) {
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clickable { onOpenTrustCenter(TrustCenterTab.QUARANTINE) },
                            shape = RoundedCornerShape(14.dp),
                            color = MoneySoftRed,
                            border = BorderStroke(1.dp, MoneyRed.copy(alpha = 0.5f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(Icons.Rounded.GppBad, contentDescription = null, tint = MoneyRed, modifier = Modifier.size(18.dp))
                                Text(
                                    text = "${summary.quarantinedCount} Fraud Blocked",
                                    color = MoneyRed,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Black
                                )
                            }
                        }
                    }
                }
            }
        }

        if (filtered.isEmpty()) {
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(22.dp),
                    color = MoneyCardWhite,
                    border = BorderStroke(1.dp, MoneyBorder)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.AutoMirrored.Rounded.ReceiptLong, contentDescription = null, tint = MoneyGreenMain, modifier = Modifier.size(44.dp))
                        Text("No confirmed transactions yet", color = MoneyInk, fontSize = 19.sp, fontWeight = FontWeight.Black)
                        Text(
                            "Tap + to log income/expense or sync verified bank SMS in Trust Center.",
                            color = MoneySecondary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        } else {
            items(filtered, key = { it.id }) { tx ->
                HistoryTransactionRow(
                    tx = tx,
                    onClick = { onSelectTransaction(tx) }
                )
            }
        }
    }
}

@Composable
private fun HistoryTransactionRow(
    tx: MoneyTransaction,
    onClick: () -> Unit
) {
    val vis = categoryVisualFor(tx.category)
    val isInc = tx.type == "INCOME"

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("money_tx_row_${tx.id}"),
        shape = RoundedCornerShape(20.dp),
        color = MoneyCardWhite,
        border = BorderStroke(1.dp, MoneyBorder)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(vis.bg),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = vis.icon,
                    contentDescription = tx.category,
                    tint = vis.tint,
                    modifier = Modifier.size(26.dp)
                )
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = tx.title,
                    color = MoneyInk,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = formatTransactionDateLabel(tx.timestampMs),
                        color = MoneySecondary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                    if (tx.source == "SMS") {
                        Text(
                            text = "• Verified SMS",
                            color = MoneyGreenMain,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "${if (isInc) "+ " else "- "}${formatInr(tx.amount)}",
                color = if (isInc) MoneyGreenMain else MoneyRed,
                fontSize = 19.sp,
                fontWeight = FontWeight.Black
            )
        }
    }
}

// --- BOTTOM NAVIGATION BAR (Home | History | + FAB | Goals | Settings) ---

@Composable
private fun MoneyBottomNavBar(
    currentRoute: MoneyRoute,
    onSelectRoute: (MoneyRoute) -> Unit,
    onAddClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding(),
        contentAlignment = Alignment.BottomCenter
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MoneyCardWhite,
            shadowElevation = 14.dp,
            border = BorderStroke(1.dp, MoneyBorder)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(72.dp)
                    .padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                BottomNavItem(
                    label = "Home",
                    icon = Icons.Rounded.Home,
                    selected = currentRoute == MoneyRoute.HOME || currentRoute == MoneyRoute.ANALYTICS,
                    onClick = { onSelectRoute(MoneyRoute.HOME) },
                    modifier = Modifier.weight(1f).testTag("money_nav_home")
                )
                BottomNavItem(
                    label = "History",
                    icon = Icons.AutoMirrored.Rounded.ReceiptLong,
                    selected = currentRoute == MoneyRoute.HISTORY,
                    onClick = { onSelectRoute(MoneyRoute.HISTORY) },
                    modifier = Modifier.weight(1f).testTag("money_nav_history")
                )
                Spacer(modifier = Modifier.width(66.dp))
                BottomNavItem(
                    label = "Goals",
                    icon = Icons.Rounded.TrackChanges,
                    selected = currentRoute == MoneyRoute.GOALS,
                    onClick = { onSelectRoute(MoneyRoute.GOALS) },
                    modifier = Modifier.weight(1f).testTag("money_nav_goals")
                )
                BottomNavItem(
                    label = "Settings",
                    icon = Icons.Rounded.Settings,
                    selected = currentRoute == MoneyRoute.SETTINGS,
                    onClick = { onSelectRoute(MoneyRoute.SETTINGS) },
                    modifier = Modifier.weight(1f).testTag("money_nav_settings")
                )
            }
        }

        // Center Floating Circular Green + Button
        Box(
            modifier = Modifier
                .offset(y = (-18).dp)
                .size(64.dp)
                .shadow(10.dp, CircleShape, spotColor = MoneyGreenDeep.copy(alpha = 0.45f))
                .clip(CircleShape)
                .background(Brush.linearGradient(listOf(MoneyGreenMain, MoneyGreenDeep)))
                .border(4.dp, Color.White, CircleShape)
                .clickable(onClick = onAddClick)
                .testTag("money_nav_add_fab"),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Rounded.Add,
                contentDescription = "Add Transaction",
                tint = Color.White,
                modifier = Modifier.size(34.dp)
            )
        }
    }
}

@Composable
private fun BottomNavItem(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val tint = if (selected) MoneyGreenDeep else MoneySecondary
    Column(
        modifier = modifier
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = tint,
            modifier = Modifier.size(26.dp)
        )
        Text(
            text = label,
            color = tint,
            fontSize = 12.sp,
            fontWeight = if (selected) FontWeight.Black else FontWeight.Bold,
            maxLines = 1
        )
    }
}
