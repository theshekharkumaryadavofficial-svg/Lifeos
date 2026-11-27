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
import androidx.compose.material.icons.rounded.AccountBalance
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.Assessment
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.DirectionsCar
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material.icons.rounded.PhoneAndroid
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Restaurant
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.ShoppingCart
import androidx.compose.material.icons.rounded.Sms
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material.icons.rounded.WorkspacePremium
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.core.view.WindowCompat
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.MoneyTransaction
import com.example.data.UiState
import com.example.data.UserProfile
import com.example.money.SmsInboxScanner
import com.example.money.formatInr
import com.example.money.formatTransactionDateLabel
import com.example.ui.viewmodel.LifeViewModel
import com.example.ui.viewmodel.MoneySummaryState
import java.util.Calendar

private enum class MoneyTab {
    HOME, ANALYTICS, REPORTS, MORE, MESSAGE_TRACKING
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
    var activeTab by rememberSaveable { mutableStateOf(MoneyTab.HOME) }
    var showAddTxDialog by remember { mutableStateOf(false) }
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

    BackHandler {
        when (activeTab) {
            MoneyTab.MESSAGE_TRACKING -> activeTab = MoneyTab.HOME
            MoneyTab.HOME -> onExitToLifeOs()
            else -> activeTab = MoneyTab.HOME
        }
    }

    // Panel 1: Welcome Intro (shown on first visit until "Get Started →" is tapped)
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

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .background(MoneyBg)
            .statusBarsPadding(),
        containerColor = MoneyBg,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            if (activeTab != MoneyTab.MESSAGE_TRACKING) {
                MoneyBottomNavBar(
                    currentTab = activeTab,
                    onSelectTab = { activeTab = it },
                    onAddClick = { showAddTxDialog = true }
                )
            }
        }
    ) { innerPadding ->
        AnimatedContent(
            targetState = activeTab,
            transitionSpec = { fadeIn(tween(220)) togetherWith fadeOut(tween(160)) },
            label = "money_tab_anim"
        ) { tab ->
            when (tab) {
                MoneyTab.HOME -> MoneyHomeDashboard(
                    profile = profile,
                    summary = summary,
                    transactions = transactions,
                    hasSmsPermission = hasSmsPermission,
                    smsSyncing = smsSyncing,
                    onBackToLifeOs = onExitToLifeOs,
                    onOpenMessageTracking = { activeTab = MoneyTab.MESSAGE_TRACKING },
                    onViewAllTransactions = { activeTab = MoneyTab.REPORTS },
                    onToggleMessageTracking = { nextOn ->
                        if (nextOn && !hasSmsPermission) {
                            requestSmsPermissions()
                        } else {
                            viewModel.setMoneySmsTrackingEnabled(nextOn)
                        }
                    },
                    onSyncSmsNow = {
                        if (hasSmsPermission) viewModel.syncBankSmsInbox(silentIfZero = false)
                        else requestSmsPermissions()
                    },
                    bottomPadding = innerPadding
                )

                MoneyTab.ANALYTICS -> MoneyAnalyticsScreen(
                    transactions = transactions,
                    bottomPadding = innerPadding
                )

                MoneyTab.REPORTS -> MoneyTransactionsTab(
                    transactions = transactions,
                    hasSmsPermission = hasSmsPermission,
                    smsSyncing = smsSyncing,
                    onSyncSms = {
                        if (hasSmsPermission) viewModel.syncBankSmsInbox(silentIfZero = false)
                        else requestSmsPermissions()
                    },
                    onAddManual = { showAddTxDialog = true },
                    onSelectTransaction = { inspectingTx = it },
                    bottomPadding = innerPadding
                )

                MoneyTab.MORE -> MoneyMoreScreen(
                    profile = profile,
                    summary = summary,
                    transactions = transactions,
                    onOpenMessageTracking = { activeTab = MoneyTab.MESSAGE_TRACKING },
                    onSyncSmsNow = {
                        if (hasSmsPermission) viewModel.syncBankSmsInbox(silentIfZero = false)
                        else requestSmsPermissions()
                    },
                    onUpdateBudget = { viewModel.updateMoneyMonthlyBudget(it) },
                    onUpdateOpeningBalance = { viewModel.updateMoneyOpeningBalance(it) },
                    onClearAllTransactions = { viewModel.clearAllMoneyTransactions() },
                    onExitToLifeOs = onExitToLifeOs,
                    bottomPadding = innerPadding
                )

                MoneyTab.MESSAGE_TRACKING -> MoneyMessageTrackingScreen(
                    trackingEnabled = profile.moneySmsTrackingEnabled,
                    hasSmsPermission = hasSmsPermission,
                    smsSyncing = smsSyncing,
                    smsTrackedCount = summary.smsTrackedCount,
                    onBack = { activeTab = MoneyTab.HOME },
                    onRequestSmsPermission = { requestSmsPermissions() },
                    onSyncSmsNow = { viewModel.syncBankSmsInbox(silentIfZero = false) },
                    onAddParsedSms = { parsed -> viewModel.addParsedSmsTransaction(parsed) }
                )
            }
        }
    }

    if (showAddTxDialog) {
        AddMoneyTransactionDialog(
            onDismiss = { showAddTxDialog = false },
            onSave = { title, amount, type, category, mode ->
                viewModel.addManualMoneyTransaction(title, amount, type, category, mode)
            }
        )
    }

    inspectingTx?.let { tx ->
        TransactionDetailDialog(
            tx = tx,
            onDismiss = { inspectingTx = null },
            onDelete = { viewModel.deleteMoneyTransaction(tx.id) }
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
                        Color(0xFFDBFCE9),
                        Color(0xFFFFFFFF),
                        Color(0xFFF3F6FF),
                        Color(0xFFD1FAE5)
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
                .background(Color.White.copy(alpha = 0.85f))
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
            Icon(
                imageVector = Icons.Rounded.WorkspacePremium,
                contentDescription = null,
                tint = Color(0xFFF59E0B),
                modifier = Modifier.size(56.dp)
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Money\nTracker",
                color = MoneyInk,
                fontSize = 44.sp,
                lineHeight = 48.sp,
                fontWeight = FontWeight.Black,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Track  •  Analyze  •  Save",
                color = MoneySecondary,
                fontSize = 18.sp,
                fontWeight = FontWeight.ExtraBold
            )

            Spacer(modifier = Modifier.height(22.dp))

            MoneyWelcomeHeroArtwork(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(236.dp)
            )

            Spacer(modifier = Modifier.height(22.dp))

            Text(
                text = "Your Personal Finance Buddy",
                color = MoneyInk,
                fontSize = 20.sp,
                fontWeight = FontWeight.ExtraBold,
                textAlign = TextAlign.Center
            )
            Text(
                text = "Auto-tracks Bank & UPI SMS into Real Balance",
                color = MoneySecondary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(26.dp))

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
                    fontSize = 19.sp,
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
    onOpenMessageTracking: () -> Unit,
    onViewAllTransactions: () -> Unit,
    onToggleMessageTracking: (Boolean) -> Unit,
    onSyncSmsNow: () -> Unit,
    bottomPadding: PaddingValues
) {
    var hideBalance by rememberSaveable { mutableStateOf(false) }

    val firstName = profile.displayName.trim().substringBefore(" ").ifEmpty { "Shekhar" }
    val initial = firstName.firstOrNull()?.uppercaseChar()?.toString() ?: "S"

    // Compute 10-bucket current month income & expense bars from real transactions
    val (incomeBuckets, expenseBuckets) = remember(transactions) {
        val inc = DoubleArray(10) { 0.0 }
        val exp = DoubleArray(10) { 0.0 }
        val nowCal = Calendar.getInstance()
        val curMonth = nowCal.get(Calendar.MONTH)
        val curYear = nowCal.get(Calendar.YEAR)
        val cal = Calendar.getInstance()
        for (tx in transactions) {
            cal.timeInMillis = tx.timestampMs
            if (cal.get(Calendar.MONTH) == curMonth && cal.get(Calendar.YEAR) == curYear) {
                val day = cal.get(Calendar.DAY_OF_MONTH)
                val bucket = ((day - 1) / 3).coerceIn(0, 9)
                if (tx.type == "INCOME") inc[bucket] += tx.amount else exp[bucket] += tx.amount
            }
        }
        inc.toList() to exp.toList()
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
        // 1. Top Header ("S" Avatar + "Hello, Shekhar 👋" + Bell/SMS Sync + Back to LifeOS)
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
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = "Back to LifeOS",
                        tint = MoneyInk
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(MoneyGreenMain),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = initial,
                        color = Color.White,
                        fontSize = 21.sp,
                        fontWeight = FontWeight.Black
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Hello, $firstName 👋",
                        color = MoneyInk,
                        fontSize = 21.sp,
                        fontWeight = FontWeight.Black,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "Keep going! You're on track!",
                        color = MoneySecondary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                IconButton(
                    onClick = onOpenMessageTracking,
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(MoneyCardWhite)
                        .border(1.dp, MoneyBorder, CircleShape)
                        .testTag("money_bell_message_tracking_button")
                ) {
                    Box {
                        Icon(
                            imageVector = Icons.Rounded.Notifications,
                            contentDescription = "Message Tracking",
                            tint = MoneyInk,
                            modifier = Modifier.size(24.dp)
                        )
                        Box(
                            modifier = Modifier
                                .size(9.dp)
                                .align(Alignment.TopEnd)
                                .clip(CircleShape)
                                .background(if (hasSmsPermission) MoneyGreenMain else MoneyRed)
                        )
                    }
                }
            }
        }

        // 2. Emerald Gradient Total Balance Hero Card (matching Panel 2)
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(10.dp, RoundedCornerShape(24.dp), spotColor = MoneyGreenDeep.copy(alpha = 0.35f))
                    .clip(RoundedCornerShape(24.dp))
                    .background(
                        Brush.linearGradient(
                            listOf(MoneyGreenDeep, MoneyGreenMain, Color(0xFF06924E))
                        )
                    )
                    .testTag("money_total_balance_card")
            ) {
                BalanceWaveOverlay(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .width(190.dp)
                        .height(110.dp)
                )

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 22.dp, vertical = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Total Balance",
                            color = Color.White.copy(alpha = 0.92f),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                        IconButton(
                            onClick = { hideBalance = !hideBalance },
                            modifier = Modifier
                                .size(40.dp)
                                .testTag("money_toggle_balance_visibility")
                        ) {
                            Icon(
                                imageVector = if (hideBalance) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                                contentDescription = if (hideBalance) "Show Balance" else "Hide Balance",
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }

                    Text(
                        text = if (hideBalance) "₹ ••••••" else formatInr(summary.realBalance),
                        color = Color.White,
                        fontSize = 40.sp,
                        fontWeight = FontWeight.Black,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    val weekSign = if (summary.thisWeekNet >= 0.0) "↑ +" else "↓ "
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "$weekSign${formatInr(summary.thisWeekNet)} this week",
                            color = Color(0xFFCFFCE2),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }
            }
        }

        // 3. Total Income & Total Expenses Side-by-Side Cards
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                IncomeExpenseStatCard(
                    label = "Total Income",
                    value = formatInr(summary.totalIncome),
                    isIncome = true,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("money_total_income_card")
                )
                IncomeExpenseStatCard(
                    label = "Total Expenses",
                    value = formatInr(summary.totalExpense),
                    isIncome = false,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("money_total_expense_card")
                )
            }
        }

        // 4. This Month Dual Bar Chart Card + View All →
        item {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                color = MoneyCardWhite,
                shadowElevation = 3.dp,
                border = BorderStroke(1.dp, MoneyBorder)
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "This Month",
                            color = MoneyInk,
                            fontSize = 21.sp,
                            fontWeight = FontWeight.Black
                        )
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .clickable(onClick = onViewAllTransactions)
                                .padding(horizontal = 6.dp, vertical = 4.dp)
                                .testTag("money_view_all_button"),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "View All",
                                color = MoneyGreenMain,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                                contentDescription = null,
                                tint = MoneyGreenMain,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    MonthlyDualBarChart(
                        incomeBuckets = incomeBuckets,
                        expenseBuckets = expenseBuckets
                    )
                }
            }
        }

        // 5. Message Tracking Active Card (Matches Panel 2 bottom card)
        item {
            val isLive = profile.moneySmsTrackingEnabled && hasSmsPermission
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onOpenMessageTracking)
                    .testTag("money_message_tracking_card"),
                shape = RoundedCornerShape(22.dp),
                color = MoneyCardWhite,
                shadowElevation = 3.dp,
                border = BorderStroke(1.2.dp, if (isLive) MoneyGreenMain.copy(alpha = 0.4f) else MoneyBorder)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(46.dp)
                                .clip(CircleShape)
                                .background(if (isLive) MoneyGreenMain else CatFoodColor),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (isLive) Icons.Rounded.Check else Icons.Rounded.Sms,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(25.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (isLive) "Message Tracking Active" else "Enable SMS Tracking",
                                color = MoneyInk,
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Black
                            )
                            Text(
                                text = if (isLive) {
                                    "Auto track from SMS & UPI messages"
                                } else {
                                    "Tap switch to auto-read Bank & UPI SMS"
                                },
                                color = MoneySecondary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Switch(
                            checked = isLive,
                            onCheckedChange = onToggleMessageTracking,
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = MoneyGreenMain,
                                uncheckedThumbColor = Color.White,
                                uncheckedTrackColor = Color(0xFFCBD5E1)
                            ),
                            modifier = Modifier.testTag("money_sms_tracking_switch")
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${summary.smsTrackedCount} SMS tracked • Tap for Tester & Sync",
                            color = MoneyGreenMain,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                        if (smsSyncing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                color = MoneyGreenMain,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(50))
                                    .background(MoneySoftGreen)
                                    .clickable(onClick = onSyncSmsNow)
                                    .padding(horizontal = 10.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Refresh,
                                    contentDescription = "Sync SMS",
                                    tint = MoneyGreenDeep,
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Sync SMS",
                                    color = MoneyGreenDeep,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Black
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun IncomeExpenseStatCard(
    label: String,
    value: String,
    isIncome: Boolean,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(22.dp),
        color = MoneyCardWhite,
        shadowElevation = 3.dp,
        border = BorderStroke(1.dp, MoneyBorder)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(if (isIncome) MoneySoftGreen else MoneySoftRed),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isIncome) Icons.Rounded.ArrowDownward else Icons.Rounded.ArrowUpward,
                    contentDescription = label,
                    tint = if (isIncome) MoneyGreenMain else MoneyRed,
                    modifier = Modifier.size(23.dp)
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = label,
                    color = MoneySecondary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
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

// --- PANEL 3: TRANSACTIONS / REPORTS SCREEN ---

@Composable
private fun MoneyTransactionsTab(
    transactions: List<MoneyTransaction>,
    hasSmsPermission: Boolean,
    smsSyncing: Boolean,
    onSyncSms: () -> Unit,
    onAddManual: () -> Unit,
    onSelectTransaction: (MoneyTransaction) -> Unit,
    bottomPadding: PaddingValues
) {
    var selectedFilter by rememberSaveable { mutableStateOf("All") }
    var searchVisible by rememberSaveable { mutableStateOf(false) }
    var searchQuery by rememberSaveable { mutableStateOf("") }

    val filtered = remember(transactions, selectedFilter, searchQuery) {
        val q = searchQuery.trim().lowercase()
        transactions.filter { tx ->
            val matchesFilter = when (selectedFilter) {
                "Income" -> tx.type == "INCOME"
                "Expense" -> tx.type == "EXPENSE"
                "UPI" -> tx.paymentMode.equals("UPI", ignoreCase = true)
                else -> true
            }
            val matchesSearch = q.isEmpty() ||
                tx.title.lowercase().contains(q) ||
                tx.category.lowercase().contains(q) ||
                tx.paymentMode.lowercase().contains(q)
            matchesFilter && matchesSearch
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
        // Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Transactions",
                    color = MoneyInk,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Black
                )
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    IconButton(
                        onClick = { searchVisible = !searchVisible },
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(MoneyCardWhite)
                            .border(1.dp, MoneyBorder, CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Search,
                            contentDescription = "Search Transactions",
                            tint = MoneyInk
                        )
                    }
                    IconButton(
                        onClick = onSyncSms,
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(MoneyCardWhite)
                            .border(1.dp, MoneyBorder, CircleShape)
                    ) {
                        if (smsSyncing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = MoneyGreenMain,
                                strokeWidth = 2.4.dp
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Rounded.Refresh,
                                contentDescription = "Sync SMS",
                                tint = MoneyGreenMain
                            )
                        }
                    }
                }
            }
        }

        if (searchVisible) {
            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search merchant, UPI, category...", fontWeight = FontWeight.SemiBold) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MoneyCardWhite,
                        unfocusedContainerColor = MoneyCardWhite,
                        focusedBorderColor = MoneyGreenMain,
                        unfocusedBorderColor = MoneyBorder,
                        focusedTextColor = MoneyInk,
                        unfocusedTextColor = MoneyInk
                    )
                )
            }
        }

        // 4 Filter Pills (All | Income | Expense | UPI)
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("All", "Income", "Expense", "UPI").forEach { filter ->
                    val selected = selectedFilter == filter
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(50))
                            .background(if (selected) MoneyGreenMain else MoneyChipBg)
                            .clickable { selectedFilter = filter }
                            .padding(vertical = 11.dp)
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
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.AccountBalanceWallet,
                            contentDescription = null,
                            tint = MoneyGreenMain,
                            modifier = Modifier.size(44.dp)
                        )
                        Text(
                            text = "No Transactions Yet",
                            color = MoneyInk,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Black
                        )
                        Text(
                            text = "Sync your Bank & UPI SMS inbox or tap + to add an income or expense entry.",
                            color = MoneySecondary,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            textAlign = TextAlign.Center
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Button(
                                onClick = onSyncSms,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp),
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = MoneyGreenMain)
                            ) {
                                Text(
                                    text = if (hasSmsPermission) "Sync SMS" else "Allow SMS",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }
                            Button(
                                onClick = onAddManual,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp),
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = MoneyInk)
                            ) {
                                Text("+ Add Entry", fontSize = 15.sp, fontWeight = FontWeight.ExtraBold)
                            }
                        }
                    }
                }
            }
        } else {
            items(filtered, key = { it.id }) { tx ->
                MoneyTransactionRowCard(
                    tx = tx,
                    onClick = { onSelectTransaction(tx) }
                )
            }
        }
    }
}

@Composable
private fun MoneyTransactionRowCard(
    tx: MoneyTransaction,
    onClick: () -> Unit
) {
    val isInc = tx.type == "INCOME"
    val (icon, iconTint, iconBg) = transactionVisualStyle(tx)

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("money_tx_row_${tx.id}"),
        shape = RoundedCornerShape(20.dp),
        color = MoneyCardWhite,
        shadowElevation = 2.dp,
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
                    .size(50.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(iconBg),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = tx.category,
                    tint = iconTint,
                    modifier = Modifier.size(26.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = tx.title,
                    color = MoneyInk,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Black,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${tx.paymentMode} • ${formatTransactionDateLabel(tx.timestampMs)}" +
                        if (tx.source == "SMS") " • SMS" else "",
                    color = MoneySecondary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "${if (isInc) "+" else "-"}${formatInr(tx.amount)}",
                color = if (isInc) MoneyGreenMain else MoneyRed,
                fontSize = 18.sp,
                fontWeight = FontWeight.Black
            )
        }
    }
}

private fun transactionVisualStyle(tx: MoneyTransaction): Triple<ImageVector, Color, Color> {
    return when {
        tx.category == "Food" -> Triple(Icons.Rounded.Restaurant, Color(0xFFEA580C), Color(0xFFFFEDD5))
        tx.category == "Shopping" -> Triple(Icons.Rounded.ShoppingCart, Color(0xFFF97316), Color(0xFFFFF1E6))
        tx.category == "Bills" && tx.title.contains("Recharge", ignoreCase = true) ->
            Triple(Icons.Rounded.PhoneAndroid, Color(0xFF2563EB), Color(0xFFDBEAFE))
        tx.category == "Bills" -> Triple(Icons.Rounded.Bolt, Color(0xFFD97706), Color(0xFFFEF3C7))
        tx.category == "Travel" -> Triple(Icons.Rounded.DirectionsCar, MoneyGreenMain, MoneySoftGreen)
        tx.category == "Salary" || tx.paymentMode == "Bank" ->
            Triple(Icons.Rounded.AccountBalance, Color(0xFF2563EB), Color(0xFFE0ECFF))
        tx.paymentMode == "UPI" -> Triple(Icons.Rounded.Payments, Color(0xFF6366F1), Color(0xFFEDE9FE))
        else -> Triple(Icons.Rounded.AccountBalanceWallet, MoneyGreenMain, MoneySoftGreen)
    }
}

// --- BOTTOM NAVIGATION BAR WITH RAISED GREEN "+" FAB (Matches Reference Panels 2, 3, 5, 6) ---

@Composable
private fun MoneyBottomNavBar(
    currentTab: MoneyTab,
    onSelectTab: (MoneyTab) -> Unit,
    onAddClick: () -> Unit
) {
    Surface(
        color = MoneyCardWhite,
        shadowElevation = 14.dp,
        border = BorderStroke(1.dp, MoneyBorder)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            MoneyNavItem(
                label = "Home",
                icon = Icons.Rounded.Home,
                selected = currentTab == MoneyTab.HOME,
                onClick = { onSelectTab(MoneyTab.HOME) }
            )
            MoneyNavItem(
                label = "Analytics",
                icon = Icons.Rounded.BarChart,
                selected = currentTab == MoneyTab.ANALYTICS,
                onClick = { onSelectTab(MoneyTab.ANALYTICS) }
            )

            // Center Big Circular Green "+" Add Button
            Box(
                modifier = Modifier
                    .offset(y = (-6).dp)
                    .size(58.dp)
                    .shadow(10.dp, CircleShape, spotColor = MoneyGreenMain)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(listOf(MoneyGreenBright, MoneyGreenMain))
                    )
                    .clickable(onClick = onAddClick)
                    .testTag("money_center_add_fab"),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.Add,
                    contentDescription = "Add Transaction",
                    tint = Color.White,
                    modifier = Modifier.size(32.dp)
                )
            }

            MoneyNavItem(
                label = "Reports",
                icon = Icons.Rounded.Assessment,
                selected = currentTab == MoneyTab.REPORTS,
                onClick = { onSelectTab(MoneyTab.REPORTS) }
            )
            MoneyNavItem(
                label = "More",
                icon = Icons.Rounded.MoreHoriz,
                selected = currentTab == MoneyTab.MORE,
                onClick = { onSelectTab(MoneyTab.MORE) }
            )
        }
    }
}

@Composable
private fun MoneyNavItem(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 6.dp)
            .testTag("money_nav_${label.lowercase()}"),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (selected) MoneyGreenMain else MoneySecondary,
            modifier = Modifier.size(25.dp)
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = label,
            color = if (selected) MoneyGreenMain else MoneySecondary,
            fontSize = 12.sp,
            fontWeight = FontWeight.ExtraBold
        )
    }
}
