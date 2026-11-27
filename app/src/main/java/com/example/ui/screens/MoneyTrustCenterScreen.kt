package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.GppBad
import androidx.compose.material.icons.rounded.GppMaybe
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.VerifiedUser
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.MoneyTransaction
import com.example.data.UserProfile
import com.example.money.BankTemplateRegistry
import com.example.money.ParsedSmsMoney
import com.example.money.SmsMoneyParser
import com.example.money.TransactionStatus
import com.example.money.TrustClassification
import com.example.money.formatInr
import com.example.money.formatTransactionDateLabel
import com.example.ui.viewmodel.MoneySummaryState
import kotlin.math.abs

enum class TrustCenterTab(val label: String) {
    OVERVIEW("Overview"),
    PENDING("Pending"),
    QUARANTINE("Fraud"),
    SENDERS("Senders")
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MoneyTrustCenterScreen(
    profile: UserProfile,
    summary: MoneySummaryState,
    transactions: List<MoneyTransaction>,
    hasSmsPermission: Boolean,
    smsSyncing: Boolean,
    initialTab: TrustCenterTab = TrustCenterTab.OVERVIEW,
    onBack: () -> Unit,
    onToggleSmsTracking: (Boolean) -> Unit,
    onRequestSmsPermission: () -> Unit,
    onSyncSmsNow: () -> Unit,
    onReAuditSmsNow: () -> Unit,
    onAddParsedSms: (ParsedSmsMoney) -> Unit,
    onApprovePending: (MoneyTransaction, Boolean) -> Unit,
    onQuarantineTx: (MoneyTransaction) -> Unit,
    onDeleteTx: (String) -> Unit,
    onClearQuarantined: () -> Unit,
    onAddTrustedSender: (String) -> Unit,
    onRemoveTrustedSender: (String) -> Unit,
    onInspectTransaction: (MoneyTransaction) -> Unit
) {
    var activeTab by rememberSaveable { mutableStateOf(initialTab) }

    val pendingList = remember(transactions) {
        transactions.filter { it.status == TransactionStatus.PENDING_REVIEW.key }
    }
    val quarantinedList = remember(transactions) {
        transactions.filter { it.status == TransactionStatus.QUARANTINED.key }
    }
    val customSenders = remember(profile.trustedSmsSendersCsv) {
        BankTemplateRegistry.parseCustomSendersCsv(profile.trustedSmsSendersCsv)
    }

    var testSender by remember { mutableStateOf("AX-TBTACC-S") }
    var testBody by remember {
        mutableStateOf(
            "A/C829****404, Welcometoezooperstoreurl- Rs.5912 has been credited, Bal Rs.8935 UserName-829****404 Password- check here: bit.ly/4jDeQvh - TBTSMS"
        )
    }
    var lastTestResult by remember { mutableStateOf<ParsedSmsMoney?>(null) }
    var testIgnoredNote by remember { mutableStateOf<String?>(null) }
    var newTrustedSenderInput by remember { mutableStateOf("") }

    val presetTests = remember {
        listOf(
            Triple(
                "Scam SMS (₹5,912)",
                "AX-TBTACC-S",
                "A/C829****404, Welcometoezooperstoreurl- Rs.5912 has been credited, Bal Rs.8935 UserName-829****404 Password- check here: bit.ly/4jDeQvh - TBTSMS"
            ),
            Triple(
                "Genuine SBI Credit",
                "VK-SBIUPI-T",
                "Dear UPI user A/C X4321 credited by Rs 1500.00 on date 27Nov26 trf from RAHUL Refno 623489012345. Avl Bal Rs 14350.00 -SBI"
            ),
            Triple(
                "Genuine HDFC Debit",
                "AD-HDFCBK-T",
                "Amt Sent Rs.249.00 From HDFC Bank A/C *9012 To SWIGGY via UPI Ref 512390812345. Avl Bal: Rs.11,200.00"
            ),
            Triple(
                "Unknown Sender",
                "VM-MYSHOP-S",
                "Rs.750 credited to your A/c XX4012 on 27-Nov Ref 99182736"
            )
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MoneyBg),
        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(MoneyCardWhite)
                        .border(1.dp, MoneyBorder, CircleShape)
                        .testTag("trust_center_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = "Back",
                        tint = MoneyInk
                    )
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "SMS Trust Center",
                        color = MoneyInk,
                        fontSize = 23.sp,
                        fontWeight = FontWeight.Black
                    )
                    Text(
                        text = "Fraud Shield & Balance Audit",
                        color = MoneyGreenMain,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
                IconButton(
                    onClick = { if (hasSmsPermission) onSyncSmsNow() else onRequestSmsPermission() },
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(MoneyCardWhite)
                        .border(1.dp, MoneyBorder, CircleShape)
                        .testTag("trust_center_sync_button")
                ) {
                    if (smsSyncing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = MoneyGreenMain,
                            strokeWidth = 2.5.dp
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

        // 4 Sub-tabs: Overview | Pending (N) | Fraud (N) | Senders
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TrustCenterTab.entries.forEach { tab ->
                    val selected = activeTab == tab
                    val badgeCount = when (tab) {
                        TrustCenterTab.PENDING -> pendingList.size
                        TrustCenterTab.QUARANTINE -> quarantinedList.size
                        else -> 0
                    }
                    val activeBg = when {
                        !selected -> MoneyChipBg
                        tab == TrustCenterTab.QUARANTINE -> MoneyRed
                        tab == TrustCenterTab.PENDING -> MoneyAmber
                        else -> MoneyGreenDeep
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(50))
                            .background(activeBg)
                            .clickable { activeTab = tab }
                            .padding(vertical = 11.dp, horizontal = 4.dp)
                            .testTag("trust_tab_${tab.name.lowercase()}"),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (badgeCount > 0) "${tab.label} ($badgeCount)" else tab.label,
                            color = if (selected) Color.White else MoneyInk,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.ExtraBold,
                            maxLines = 1
                        )
                    }
                }
            }
        }

        when (activeTab) {
            TrustCenterTab.OVERVIEW -> {
                // Privacy & Fraud Protection Banner
                item {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(22.dp),
                        color = MoneySoftGreen,
                        border = BorderStroke(1.2.dp, MoneyGreenMain.copy(alpha = 0.35f))
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
                                        .background(MoneyGreenDeep),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.VerifiedUser,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(26.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "SMS Fraud Shield Active",
                                        color = MoneyInk,
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Black
                                    )
                                    Text(
                                        text = "Only verified bank templates affect balance. SMS stays 100% local on device.",
                                        color = MoneySecondary,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Switch(
                                    checked = profile.moneySmsTrackingEnabled && hasSmsPermission,
                                    onCheckedChange = { on ->
                                        if (on && !hasSmsPermission) onRequestSmsPermission()
                                        else onToggleSmsTracking(on)
                                    },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = Color.White,
                                        checkedTrackColor = MoneyGreenMain
                                    ),
                                    modifier = Modifier.testTag("trust_sms_switch")
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Button(
                                    onClick = { if (hasSmsPermission) onSyncSmsNow() else onRequestSmsPermission() },
                                    enabled = !smsSyncing,
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(48.dp)
                                        .testTag("trust_sync_now_button"),
                                    shape = RoundedCornerShape(14.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = MoneyGreenDeep)
                                ) {
                                    Text(
                                        text = if (!hasSmsPermission) "Allow SMS" else if (smsSyncing) "Syncing..." else "Sync Inbox",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                }
                                OutlinedButton(
                                    onClick = onReAuditSmsNow,
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(48.dp)
                                        .testTag("trust_reaudit_button"),
                                    shape = RoundedCornerShape(14.dp),
                                    border = BorderStroke(1.5.dp, MoneyGreenDeep)
                                ) {
                                    Text(
                                        text = "Re-Audit SMS",
                                        color = MoneyGreenDeep,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                }
                            }
                        }
                    }
                }

                // Separated Balance Architecture Breakdown Card
                item {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(22.dp),
                        color = MoneyCardWhite,
                        border = BorderStroke(1.dp, MoneyBorder),
                        shadowElevation = 2.dp
                    ) {
                        Column(
                            modifier = Modifier.padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = "Balance Architecture",
                                color = MoneyInk,
                                fontSize = 19.sp,
                                fontWeight = FontWeight.Black
                            )
                            Text(
                                text = "Only Confirmed transactions affect your Calculated App Balance.",
                                color = MoneySecondary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                            HorizontalDivider(color = MoneyBorder)
                            BalanceRow("Opening Balance", formatInr(summary.openingBalance), MoneyInk)
                            BalanceRow("Confirmed Income", "+${formatInr(summary.confirmedIncome)}", MoneyGreenMain)
                            BalanceRow("Confirmed Expenses", "-${formatInr(summary.confirmedExpense)}", MoneyRed)
                            HorizontalDivider(color = MoneyBorder)
                            BalanceRow("Calculated App Balance", formatInr(summary.calculatedAppBalance), MoneyGreenDeep, bold = true)
                            BalanceRow(
                                "Verified SMS Bank Balance",
                                summary.smsReportedBankBalance?.let { formatInr(it) } ?: "Not detected yet",
                                MoneyBlueAvatar
                            )
                            summary.unverifiedSmsBankBalance?.let { unverifiedBal ->
                                BalanceRow(
                                    "Unverified SMS Balance (Ignored)",
                                    formatInr(unverifiedBal),
                                    MoneyAmber
                                )
                            }
                            BalanceRow(
                                "Pending Review (${summary.pendingCount})",
                                formatInr(summary.pendingTotal),
                                MoneyAmber
                            )
                            BalanceRow(
                                "Quarantined Fraud (${summary.quarantinedCount})",
                                "${formatInr(summary.quarantinedTotal)} excluded",
                                MoneyRed
                            )

                            val diff = summary.reconciliationDiff
                            if (diff != null && abs(diff) >= 1.0) {
                                Surface(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(14.dp),
                                    color = MoneySoftAmber,
                                    border = BorderStroke(1.dp, MoneyAmber.copy(alpha = 0.5f))
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Icon(Icons.Rounded.Warning, contentDescription = null, tint = MoneyAmber)
                                        Text(
                                            text = "Reconciliation Alert: Verified SMS bank balance differs by ${formatInr(abs(diff))}. Update Opening Balance or review pending transactions.",
                                            color = MoneyInk,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Live SMS Fraud & Trust Tester (with 1-tap Screenshot Scam Test)
                item {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(22.dp),
                        color = MoneyCardWhite,
                        border = BorderStroke(1.dp, MoneyBorder),
                        shadowElevation = 2.dp
                    ) {
                        Column(
                            modifier = Modifier.padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = "Live Fraud & Trust Tester",
                                color = MoneyInk,
                                fontSize = 19.sp,
                                fontWeight = FontWeight.Black
                            )
                            Text(
                                text = "Test any bank or suspicious SMS (links are never opened automatically).",
                                color = MoneySecondary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )

                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                presetTests.forEach { (label, sender, body) ->
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(50))
                                            .background(if (label.contains("Scam")) MoneySoftRed else MoneyChipBg)
                                            .border(
                                                1.dp,
                                                if (label.contains("Scam")) MoneyRed.copy(alpha = 0.45f) else MoneyBorder,
                                                RoundedCornerShape(50)
                                            )
                                            .clickable {
                                                testSender = sender
                                                testBody = body
                                                lastTestResult = null
                                                testIgnoredNote = null
                                            }
                                            .padding(horizontal = 12.dp, vertical = 8.dp)
                                    ) {
                                        Text(
                                            text = label,
                                            color = if (label.contains("Scam")) MoneyRed else MoneyInk,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.ExtraBold
                                        )
                                    }
                                }
                            }

                            OutlinedTextField(
                                value = testSender,
                                onValueChange = { testSender = it },
                                label = { Text("SMS Sender Header (e.g. AX-TBTACC-S or VK-SBIUPI-T)", fontWeight = FontWeight.Bold) },
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("trust_tester_sender_input"),
                                shape = RoundedCornerShape(14.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = MoneyGreenMain,
                                    unfocusedBorderColor = MoneyBorder,
                                    focusedTextColor = MoneyInk,
                                    unfocusedTextColor = MoneyInk
                                )
                            )

                            OutlinedTextField(
                                value = testBody,
                                onValueChange = { testBody = it },
                                label = { Text("SMS Message Body", fontWeight = FontWeight.Bold) },
                                minLines = 3,
                                maxLines = 5,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("trust_tester_body_input"),
                                shape = RoundedCornerShape(14.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = MoneyGreenMain,
                                    unfocusedBorderColor = MoneyBorder,
                                    focusedTextColor = MoneyInk,
                                    unfocusedTextColor = MoneyInk
                                )
                            )

                            Button(
                                onClick = {
                                    val parsed = SmsMoneyParser.parse(
                                        body = testBody,
                                        sender = testSender,
                                        customTrustedSenders = customSenders
                                    )
                                    if (parsed != null) {
                                        lastTestResult = parsed
                                        testIgnoredNote = null
                                    } else {
                                        lastTestResult = null
                                        testIgnoredNote = "Safely ignored (OTP, failed payment, collect request, balance-only, or malformed non-transaction SMS)."
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(54.dp)
                                    .testTag("trust_tester_analyze_button"),
                                shape = RoundedCornerShape(16.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = MoneyGreenDeep)
                            ) {
                                Icon(Icons.Rounded.Bolt, contentDescription = null, tint = Color(0xFFFDE047))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Analyze SMS Trust & Fraud", fontSize = 16.sp, fontWeight = FontWeight.Black)
                            }

                            testIgnoredNote?.let { note ->
                                Surface(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(14.dp),
                                    color = MoneyChipBg
                                ) {
                                    Text(
                                        text = note,
                                        color = MoneySecondary,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(14.dp)
                                    )
                                }
                            }

                            lastTestResult?.let { res ->
                                val cardBorder = when (res.trustClassification) {
                                    TrustClassification.TRUSTED_PATTERN -> MoneyGreenMain
                                    TrustClassification.REVIEW_REQUIRED -> MoneyAmber
                                    TrustClassification.HIGH_RISK -> MoneyRed
                                }
                                val cardBg = when (res.trustClassification) {
                                    TrustClassification.TRUSTED_PATTERN -> MoneySoftGreen
                                    TrustClassification.REVIEW_REQUIRED -> MoneySoftAmber
                                    TrustClassification.HIGH_RISK -> MoneySoftRed
                                }
                                Surface(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(16.dp),
                                    color = cardBg,
                                    border = BorderStroke(1.5.dp, cardBorder)
                                ) {
                                    Column(
                                        modifier = Modifier.padding(14.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "${res.trustClassification.label} (${res.status.label})",
                                                color = cardBorder,
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.Black
                                            )
                                            Text(
                                                text = "Risk ${res.riskScore}/100",
                                                color = cardBorder,
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Black
                                            )
                                        }
                                        Text(
                                            text = "${if (res.type == "INCOME") "+" else "-"}${formatInr(res.amount)} • ${res.title}",
                                            color = MoneyInk,
                                            fontSize = 18.sp,
                                            fontWeight = FontWeight.Black
                                        )
                                        if (res.status != TransactionStatus.CONFIRMED) {
                                            Text(
                                                text = "Confirmed Balance Impact: NO CHANGE (Excluded)",
                                                color = MoneyRed,
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.ExtraBold
                                            )
                                        }
                                        res.detectedBankBalance?.let { bal ->
                                            val tag = if (res.status == TransactionStatus.CONFIRMED) "Verified" else "Unverified"
                                            Text(
                                                text = "SMS Bank Balance ($tag): ${formatInr(bal)}",
                                                color = MoneySecondary,
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                        if (res.flagReasons.isNotEmpty()) {
                                            Text(
                                                text = "Why was this SMS flagged?",
                                                color = MoneyInk,
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.Black
                                            )
                                            res.flagReasons.forEach { reason ->
                                                Text(
                                                    text = "• $reason",
                                                    color = MoneyInk,
                                                    fontSize = 13.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                        Button(
                                            onClick = {
                                                onAddParsedSms(res)
                                                lastTestResult = null
                                            },
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(48.dp)
                                                .testTag("trust_tester_save_result_button"),
                                            shape = RoundedCornerShape(14.dp),
                                            colors = ButtonDefaults.buttonColors(containerColor = MoneyInk)
                                        ) {
                                            Text(
                                                text = when (res.status) {
                                                    TransactionStatus.CONFIRMED -> "Save Confirmed Transaction"
                                                    TransactionStatus.PENDING_REVIEW -> "Save to Pending Review"
                                                    TransactionStatus.QUARANTINED -> "Save to Fraud Quarantine"
                                                },
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.ExtraBold
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            TrustCenterTab.PENDING -> {
                if (pendingList.isEmpty()) {
                    item {
                        EmptyTrustCard(
                            icon = Icons.Rounded.CheckCircle,
                            tint = MoneyGreenMain,
                            title = "No Pending SMS Reviews",
                            subtitle = "All unverified SMS transactions have been reviewed."
                        )
                    }
                } else {
                    items(pendingList, key = { it.id }) { tx ->
                        FlaggedSmsCard(
                            tx = tx,
                            isQuarantine = false,
                            onApproveOnce = { onApprovePending(tx, false) },
                            onApproveAndTrust = { onApprovePending(tx, true) },
                            onQuarantine = { onQuarantineTx(tx) },
                            onDelete = { onDeleteTx(tx.id) },
                            onInspect = { onInspectTransaction(tx) }
                        )
                    }
                }
            }

            TrustCenterTab.QUARANTINE -> {
                item {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        color = MoneySoftRed,
                        border = BorderStroke(1.2.dp, MoneyRed.copy(alpha = 0.45f))
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(Icons.Rounded.GppBad, contentDescription = null, tint = MoneyRed, modifier = Modifier.size(28.dp))
                                Text(
                                    text = "Fraud Quarantine Protection",
                                    color = MoneyRed,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Black
                                )
                            }
                            Text(
                                text = "Suspicious SMS below are blocked from your Confirmed Income and App Balance. Links are disabled. Never share passwords, UPI PINs, or OTPs.",
                                color = MoneyInk,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                            if (quarantinedList.isNotEmpty()) {
                                OutlinedButton(
                                    onClick = onClearQuarantined,
                                    shape = RoundedCornerShape(12.dp),
                                    border = BorderStroke(1.2.dp, MoneyRed)
                                ) {
                                    Icon(Icons.Rounded.Delete, contentDescription = null, tint = MoneyRed, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Delete All Quarantined (${quarantinedList.size})", color = MoneyRed, fontWeight = FontWeight.ExtraBold)
                                }
                            }
                        }
                    }
                }

                if (quarantinedList.isEmpty()) {
                    item {
                        EmptyTrustCard(
                            icon = Icons.Rounded.Shield,
                            tint = MoneyGreenMain,
                            title = "Quarantine is Clean",
                            subtitle = "No high-risk scam or phishing SMS detected right now."
                        )
                    }
                } else {
                    items(quarantinedList, key = { it.id }) { tx ->
                        FlaggedSmsCard(
                            tx = tx,
                            isQuarantine = true,
                            onApproveOnce = { onApprovePending(tx, false) },
                            onApproveAndTrust = { onApprovePending(tx, true) },
                            onQuarantine = {},
                            onDelete = { onDeleteTx(tx.id) },
                            onInspect = { onInspectTransaction(tx) }
                        )
                    }
                }
            }

            TrustCenterTab.SENDERS -> {
                // Custom Trusted Senders Manager
                item {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(22.dp),
                        color = MoneyCardWhite,
                        border = BorderStroke(1.dp, MoneyBorder)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = "Your Custom Trusted Senders",
                                color = MoneyInk,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Black
                            )
                            Text(
                                text = "Add official regional bank headers (e.g. MAHBNK, KVBANK). Fraud checks still run on every SMS even for trusted senders.",
                                color = MoneySecondary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedTextField(
                                    value = newTrustedSenderInput,
                                    onValueChange = { newTrustedSenderInput = it },
                                    placeholder = { Text("e.g. AX-MAHBNK-T", fontWeight = FontWeight.SemiBold) },
                                    singleLine = true,
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("add_trusted_sender_input"),
                                    shape = RoundedCornerShape(14.dp)
                                )
                                Button(
                                    onClick = {
                                        onAddTrustedSender(newTrustedSenderInput)
                                        newTrustedSenderInput = ""
                                    },
                                    modifier = Modifier
                                        .height(54.dp)
                                        .testTag("add_trusted_sender_button"),
                                    shape = RoundedCornerShape(14.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = MoneyGreenDeep)
                                ) {
                                    Icon(Icons.Rounded.Add, contentDescription = "Add")
                                    Text("Add", fontWeight = FontWeight.ExtraBold)
                                }
                            }

                            if (customSenders.isEmpty()) {
                                Text(
                                    text = "No custom senders added yet.",
                                    color = MoneySecondary,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            } else {
                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    customSenders.forEach { header ->
                                        Row(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(50))
                                                .background(MoneySoftGreen)
                                                .border(1.dp, MoneyGreenMain.copy(alpha = 0.4f), RoundedCornerShape(50))
                                                .padding(horizontal = 12.dp, vertical = 6.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = header,
                                                color = MoneyGreenDeep,
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Black
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Icon(
                                                imageVector = Icons.Rounded.Close,
                                                contentDescription = "Remove $header",
                                                tint = MoneyRed,
                                                modifier = Modifier
                                                    .size(18.dp)
                                                    .clickable { onRemoveTrustedSender(header) }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Built-in Bank Templates Registry
                item {
                    Text(
                        text = "Built-in Bank Templates (${BankTemplateRegistry.DEFAULT_TEMPLATES.size})",
                        color = MoneyInk,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Black
                    )
                }

                items(BankTemplateRegistry.DEFAULT_TEMPLATES, key = { it.id }) { tpl ->
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp),
                        color = MoneyCardWhite,
                        border = BorderStroke(1.dp, MoneyBorder)
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = tpl.bankName,
                                    color = MoneyInk,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Black
                                )
                                Icon(
                                    imageVector = Icons.Rounded.Lock,
                                    contentDescription = "Verified Template",
                                    tint = MoneyGreenMain,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Text(
                                text = tpl.description,
                                color = MoneySecondary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Headers: ${tpl.senderHeaders.joinToString(", ")}",
                                color = MoneyGreenDeep,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BalanceRow(label: String, value: String, valueColor: Color, bold: Boolean = false) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            color = if (bold) MoneyInk else MoneySecondary,
            fontSize = if (bold) 16.sp else 14.sp,
            fontWeight = if (bold) FontWeight.Black else FontWeight.Bold
        )
        Text(
            text = value,
            color = valueColor,
            fontSize = if (bold) 18.sp else 15.sp,
            fontWeight = FontWeight.Black
        )
    }
}

@Composable
private fun EmptyTrustCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    tint: Color,
    title: String,
    subtitle: String
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        color = MoneyCardWhite,
        border = BorderStroke(1.dp, MoneyBorder)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(26.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(46.dp))
            Text(title, color = MoneyInk, fontSize = 19.sp, fontWeight = FontWeight.Black)
            Text(subtitle, color = MoneySecondary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun FlaggedSmsCard(
    tx: MoneyTransaction,
    isQuarantine: Boolean,
    onApproveOnce: () -> Unit,
    onApproveAndTrust: () -> Unit,
    onQuarantine: () -> Unit,
    onDelete: () -> Unit,
    onInspect: () -> Unit
) {
    val accent = if (isQuarantine) MoneyRed else MoneyAmber
    val bgTint = if (isQuarantine) MoneySoftRed else MoneySoftAmber
    val reasons = tx.flagReasonsList()

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onInspect),
        shape = RoundedCornerShape(20.dp),
        color = MoneyCardWhite,
        border = BorderStroke(1.4.dp, accent.copy(alpha = 0.55f)),
        shadowElevation = 2.dp
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = if (isQuarantine) Icons.Rounded.GppBad else Icons.Rounded.GppMaybe,
                        contentDescription = null,
                        tint = accent,
                        modifier = Modifier.size(22.dp)
                    )
                    Text(
                        text = if (isQuarantine) "HIGH RISK • QUARANTINED" else "REVIEW REQUIRED",
                        color = accent,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Black
                    )
                }
                Text(
                    text = "Risk ${tx.riskScore}/100",
                    color = accent,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Black
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = tx.title,
                        color = MoneyInk,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Black
                    )
                    Text(
                        text = "Sender: ${tx.smsSender.ifBlank { "Unknown" }} • ${formatTransactionDateLabel(tx.timestampMs)}",
                        color = MoneySecondary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Text(
                    text = "${if (tx.type == "INCOME") "+" else "-"}${formatInr(tx.amount)}",
                    color = accent,
                    fontSize = 21.sp,
                    fontWeight = FontWeight.Black
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(bgTint)
                    .padding(10.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "Why was this SMS flagged?",
                        color = MoneyInk,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Black
                    )
                    if (reasons.isEmpty()) {
                        Text(
                            text = "• Sender or format could not be automatically verified.",
                            color = MoneyInk,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    } else {
                        reasons.forEach { reason ->
                            Text(
                                text = "• $reason",
                                color = MoneyInk,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            tx.detectedBankBalance?.let { bal ->
                Text(
                    text = "Claimed SMS Balance (Unverified, Excluded): ${formatInr(bal)}",
                    color = MoneySecondary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }

            if (tx.smsBody.isNotBlank()) {
                // Plain non-clickable text only (suspicious links are NEVER auto-opened or clickable)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MoneyChipBg)
                        .padding(10.dp)
                ) {
                    Text(
                        text = tx.smsBody,
                        color = MoneySecondary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (!isQuarantine) {
                    Button(
                        onClick = onApproveOnce,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MoneyGreenDeep)
                    ) {
                        Text("Confirm", fontSize = 13.sp, fontWeight = FontWeight.ExtraBold)
                    }
                    OutlinedButton(
                        onClick = onApproveAndTrust,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.2.dp, MoneyGreenMain)
                    ) {
                        Text("Confirm & Trust", color = MoneyGreenDeep, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold)
                    }
                } else {
                    OutlinedButton(
                        onClick = onApproveOnce,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, MoneySecondary)
                    ) {
                        Text("Not Fraud", color = MoneyInk, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold)
                    }
                }

                if (!isQuarantine) {
                    OutlinedButton(
                        onClick = onQuarantine,
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.2.dp, MoneyRed)
                    ) {
                        Text("Fraud", color = MoneyRed, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold)
                    }
                }
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MoneySoftRed)
                ) {
                    Icon(Icons.Rounded.Delete, contentDescription = "Delete", tint = MoneyRed)
                }
            }
        }
    }
}
