package com.example.ui.screens

import android.content.Intent
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.automirrored.rounded.ExitToApp
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.AccountBalance
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Category
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CloudUpload
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material.icons.rounded.PhoneAndroid
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Sms
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.MoneyTransaction
import com.example.data.UserProfile
import com.example.money.ParsedSmsMoney
import com.example.money.SmsMoneyParser
import com.example.money.formatInr
import com.example.money.formatTransactionDateLabel
import com.example.ui.viewmodel.MoneySummaryState
import java.util.Calendar
import java.util.Locale

// --- PANEL 4: MESSAGE TRACKING SCREEN ---

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MoneyMessageTrackingScreen(
    trackingEnabled: Boolean,
    hasSmsPermission: Boolean,
    smsSyncing: Boolean,
    smsTrackedCount: Int,
    onBack: () -> Unit,
    onRequestSmsPermission: () -> Unit,
    onSyncSmsNow: () -> Unit,
    onAddParsedSms: (ParsedSmsMoney) -> Unit
) {
    var sampleInput by remember {
        mutableStateOf("Credited Rs.1,000 to your account XXXXX1234 via UPI from Rahul")
    }
    var lastTestResult by remember { mutableStateOf<ParsedSmsMoney?>(null) }
    var testFailedMessage by remember { mutableStateOf<String?>(null) }

    val quickSamples = remember {
        listOf(
            "UPI Credit" to "Credited Rs.1,000 to your account XXXXX1234 via UPI from Rahul. Avl Bal: Rs.12,850",
            "Amazon Spend" to "Rs.1,299.00 debited from your A/c XX4321 to Amazon Payment via UPI. Avl Bal Rs.11,551",
            "Salary" to "INR 15,000.00 Salary credited to your Bank A/c XX4321 via NEFT.",
            "Electricity" to "Paid Rs.870 towards Electricity Bill via PhonePe UPI Ref 501294."
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MoneyBg),
        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
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
                        .testTag("money_msg_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = "Back",
                        tint = MoneyInk,
                        modifier = Modifier.size(26.dp)
                    )
                }
                Text(
                    text = "Message Tracking",
                    color = MoneyInk,
                    fontSize = 23.sp,
                    fontWeight = FontWeight.Black
                )
                IconButton(
                    onClick = { if (hasSmsPermission) onSyncSmsNow() else onRequestSmsPermission() },
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("money_msg_sync_icon")
                ) {
                    if (smsSyncing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(22.dp),
                            color = MoneyGreenMain,
                            strokeWidth = 2.5.dp
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Rounded.Refresh,
                            contentDescription = "Sync Bank SMS",
                            tint = MoneyInk,
                            modifier = Modifier.size(25.dp)
                        )
                    }
                }
            }
        }

        // Status Card (Green Tracking Active matching Panel 4)
        item {
            val activeAndReady = trackingEnabled && hasSmsPermission
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                color = if (activeAndReady) MoneySoftGreen else Color(0xFFFFF7E6),
                border = BorderStroke(
                    1.2.dp,
                    if (activeAndReady) MoneyGreenMain.copy(alpha = 0.35f) else CatFoodColor.copy(alpha = 0.5f)
                )
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(54.dp)
                                .clip(CircleShape)
                                .background(if (activeAndReady) MoneyGreenMain else CatFoodColor),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (activeAndReady) Icons.Rounded.Check else Icons.Rounded.Warning,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(30.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (activeAndReady) "Tracking Active" else "Allow SMS Access",
                                color = MoneyInk,
                                fontSize = 21.sp,
                                fontWeight = FontWeight.Black
                            )
                            Text(
                                text = if (activeAndReady) {
                                    "All bank & UPI SMS will be auto-tracked ($smsTrackedCount SMS synced)"
                                } else {
                                    "Grant SMS permission to read bank credits & debits automatically"
                                },
                                color = MoneySecondary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        if (!hasSmsPermission) {
                            Button(
                                onClick = onRequestSmsPermission,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(50.dp)
                                    .testTag("grant_sms_permission_button"),
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = MoneyGreenMain)
                            ) {
                                Text("Grant SMS Permission", fontSize = 15.sp, fontWeight = FontWeight.ExtraBold)
                            }
                        } else {
                            Button(
                                onClick = onSyncSmsNow,
                                enabled = !smsSyncing,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(50.dp)
                                    .testTag("sync_sms_inbox_button"),
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = MoneyGreenMain)
                            ) {
                                Text(
                                    text = if (smsSyncing) "Scanning SMS Inbox..." else "Sync Bank & UPI SMS Now",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }
                        }
                    }
                }
            }
        }

        // Supported Messages (4-tile grid matching Panel 4)
        item {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "Supported Messages",
                    color = MoneyInk,
                    fontSize = 19.sp,
                    fontWeight = FontWeight.Black
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    SupportedMessageTile(
                        title = "UPI\nPayments",
                        icon = Icons.Rounded.Payments,
                        iconTint = Color(0xFF6366F1),
                        bgColor = Color(0xFFEEF0FF),
                        modifier = Modifier.weight(1f)
                    )
                    SupportedMessageTile(
                        title = "Bank\nSMS",
                        icon = Icons.Rounded.AccountBalance,
                        iconTint = Color(0xFF2563EB),
                        bgColor = Color(0xFFEAF3FF),
                        modifier = Modifier.weight(1f)
                    )
                    SupportedMessageTile(
                        title = "Wallet\nMessages",
                        icon = Icons.Rounded.AccountBalanceWallet,
                        iconTint = MoneyGreenMain,
                        bgColor = MoneySoftGreen,
                        modifier = Modifier.weight(1f)
                    )
                    SupportedMessageTile(
                        title = "Recharge\n& Bills",
                        icon = Icons.Rounded.PhoneAndroid,
                        iconTint = Color(0xFF4F46E5),
                        bgColor = Color(0xFFEFF0FD),
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // Sample Messages & Live Parser Tester
        item {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "Sample Messages & Live Test",
                    color = MoneyInk,
                    fontSize = 19.sp,
                    fontWeight = FontWeight.Black
                )

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    quickSamples.forEach { (label, body) ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(MoneyCardWhite)
                                .border(1.2.dp, MoneyBorder, RoundedCornerShape(50))
                                .clickable {
                                    sampleInput = body
                                    lastTestResult = null
                                    testFailedMessage = null
                                }
                                .padding(horizontal = 13.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = label,
                                color = MoneyInk,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = sampleInput,
                    onValueChange = {
                        sampleInput = it
                        testFailedMessage = null
                    },
                    label = { Text("Paste any Bank / UPI SMS to test", fontWeight = FontWeight.Bold) },
                    minLines = 3,
                    maxLines = 4,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("sms_test_input"),
                    shape = RoundedCornerShape(18.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MoneyCardWhite,
                        unfocusedContainerColor = MoneyCardWhite,
                        focusedBorderColor = MoneyGreenMain,
                        unfocusedBorderColor = MoneyBorder,
                        focusedTextColor = MoneyInk,
                        unfocusedTextColor = MoneyInk
                    )
                )

                Button(
                    onClick = {
                        val parsed = SmsMoneyParser.parse(sampleInput, sender = "SMS-TEST")
                        if (parsed != null) {
                            lastTestResult = parsed
                            testFailedMessage = null
                        } else {
                            lastTestResult = null
                            testFailedMessage = "Ignored safely (OTP, offer, failed payment, or no valid credit/debit amount detected)."
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .testTag("sms_test_now_button"),
                    shape = RoundedCornerShape(18.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MoneyGreenMain)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Bolt,
                        contentDescription = null,
                        tint = Color(0xFFFDE047),
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Test Now",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White
                    )
                }

                testFailedMessage?.let { msg ->
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        color = MoneySoftRed,
                        border = BorderStroke(1.dp, MoneyRed.copy(alpha = 0.4f))
                    ) {
                        Text(
                            text = msg,
                            color = MoneyRed,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(14.dp)
                        )
                    }
                }

                lastTestResult?.let { parsed ->
                    val isInc = parsed.type == "INCOME"
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp),
                        color = MoneyCardWhite,
                        border = BorderStroke(1.5.dp, if (isInc) MoneyGreenMain else MoneyRed)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (isInc) "DETECTED INCOME" else "DETECTED EXPENSE",
                                    color = if (isInc) MoneyGreenMain else MoneyRed,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Black
                                )
                                Text(
                                    text = "${if (isInc) "+" else "-"}${formatInr(parsed.amount)}",
                                    color = if (isInc) MoneyGreenMain else MoneyRed,
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.Black
                                )
                            }
                            Text(
                                text = "${parsed.title} • ${parsed.paymentMode} • ${parsed.category}",
                                color = MoneyInk,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                            parsed.detectedBankBalance?.let { bal ->
                                Text(
                                    text = "Bank Avl Bal in SMS: ${formatInr(bal)}",
                                    color = MoneySecondary,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Button(
                                onClick = {
                                    onAddParsedSms(parsed)
                                    lastTestResult = null
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .testTag("sms_test_add_balance_button"),
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = MoneyInk)
                            ) {
                                Text(
                                    text = "Add to Real Balance",
                                    fontSize = 15.sp,
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

@Composable
private fun SupportedMessageTile(
    title: String,
    icon: ImageVector,
    iconTint: Color,
    bgColor: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .background(bgColor)
            .padding(vertical = 14.dp, horizontal = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = title,
            tint = iconTint,
            modifier = Modifier.size(30.dp)
        )
        Text(
            text = title,
            color = MoneyInk,
            fontSize = 12.sp,
            lineHeight = 15.sp,
            fontWeight = FontWeight.ExtraBold,
            maxLines = 2
        )
    }
}

// --- PANEL 5: ANALYTICS SCREEN ---

@Composable
fun MoneyAnalyticsScreen(
    transactions: List<MoneyTransaction>,
    bottomPadding: PaddingValues
) {
    var selectedPeriod by remember { mutableStateOf("This Month") }

    val filteredExpenses = remember(transactions, selectedPeriod) {
        val nowCal = Calendar.getInstance()
        val curMonth = nowCal.get(Calendar.MONTH)
        val curYear = nowCal.get(Calendar.YEAR)
        val prevCal = Calendar.getInstance().apply { add(Calendar.MONTH, -1) }
        val prevMonth = prevCal.get(Calendar.MONTH)
        val prevYear = prevCal.get(Calendar.YEAR)

        val txCal = Calendar.getInstance()
        transactions.filter { tx ->
            if (tx.type != "EXPENSE") return@filter false
            txCal.timeInMillis = tx.timestampMs
            when (selectedPeriod) {
                "This Month" -> txCal.get(Calendar.MONTH) == curMonth && txCal.get(Calendar.YEAR) == curYear
                "Last Month" -> txCal.get(Calendar.MONTH) == prevMonth && txCal.get(Calendar.YEAR) == prevYear
                else -> true
            }
        }
    }

    val totalExp = filteredExpenses.sumOf { it.amount }
    val categoryOrder = listOf("Food", "Shopping", "Bills", "Travel", "Others")
    val slices = remember(filteredExpenses, totalExp) {
        categoryOrder.map { cat ->
            val sum = filteredExpenses.filter {
                if (cat == "Others") it.category !in listOf("Food", "Shopping", "Bills", "Travel")
                else it.category == cat
            }.sumOf { it.amount }
            val pct = if (totalExp > 0.0) ((sum / totalExp) * 100.0).toInt() else 0
            CategorySlice(cat, sum, pct, categoryColorFor(cat))
        }
    }

    val trendPoints = remember(filteredExpenses) {
        val buckets = DoubleArray(10) { 0.0 }
        val cal = Calendar.getInstance()
        for (tx in filteredExpenses) {
            cal.timeInMillis = tx.timestampMs
            val day = cal.get(Calendar.DAY_OF_MONTH)
            val idx = ((day - 1) / 3).coerceIn(0, 9)
            buckets[idx] += tx.amount
        }
        buckets.toList()
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
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "Analytics",
                color = MoneyInk,
                fontSize = 28.sp,
                fontWeight = FontWeight.Black
            )
        }

        // Period Filter Pills (This Month | Last Month | All Time)
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                listOf("This Month", "Last Month", "All Time").forEach { period ->
                    val selected = selectedPeriod == period
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(50))
                            .background(if (selected) MoneyGreenMain else MoneyChipBg)
                            .clickable { selectedPeriod = period }
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = period,
                            color = if (selected) Color.White else MoneyInk,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }
            }
        }

        // Donut Chart + Category Breakdown Card
        item {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                color = MoneyCardWhite,
                shadowElevation = 3.dp,
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
                        slices = slices,
                        totalExpense = totalExp
                    )
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        slices.forEach { slice ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
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
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Text(
                                    text = "${slice.percent}%",
                                    color = MoneyInk,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Black
                                )
                            }
                        }
                    }
                }
            }
        }

        // Spending Trend Card
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
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Spending Trend",
                        color = MoneyInk,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Black
                    )
                    SpendingTrendLineChart(points = trendPoints)
                }
            }
        }
    }
}

// --- PANEL 6: MORE / PROFILE & BUDGET SCREEN ---

@Composable
fun MoneyMoreScreen(
    profile: UserProfile,
    summary: MoneySummaryState,
    transactions: List<MoneyTransaction>,
    onOpenMessageTracking: () -> Unit,
    onSyncSmsNow: () -> Unit,
    onUpdateBudget: (Double) -> Unit,
    onUpdateOpeningBalance: (Double) -> Unit,
    onClearAllTransactions: () -> Unit,
    onExitToLifeOs: () -> Unit,
    bottomPadding: PaddingValues
) {
    val context = LocalContext.current
    var showBudgetDialog by remember { mutableStateOf(false) }
    var showOpeningBalanceDialog by remember { mutableStateOf(false) }
    var showCategoriesDialog by remember { mutableStateOf(false) }
    var showClearConfirm by remember { mutableStateOf(false) }

    val initial = profile.displayName.trim().firstOrNull()?.uppercaseChar()?.toString() ?: "S"
    val budgetRatio = (summary.thisMonthExpense / summary.monthlyBudget.coerceAtLeast(1.0)).toFloat().coerceIn(0f, 1f)

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
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Profile Row
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(62.dp)
                        .clip(CircleShape)
                        .background(MoneyBlueAvatar),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = initial,
                        color = Color.White,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Black
                    )
                }
                Spacer(modifier = Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = profile.displayName.ifBlank { "Shekhar" },
                        color = MoneyInk,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Black
                    )
                    Text(
                        text = "Real Balance: ${formatInr(summary.realBalance)}",
                        color = MoneyGreenMain,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }
        }

        // Set Monthly Budget Mint Card (Panel 6 Hero)
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(
                        Brush.horizontalGradient(
                            listOf(Color(0xFFD6F7E4), Color(0xFFBCEFD3))
                        )
                    )
                    .padding(18.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "Set Monthly Budget",
                            color = MoneyInk,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Black
                        )
                        Text(
                            text = "${formatInr(summary.thisMonthExpense)} / ${formatInr(summary.monthlyBudget)} used",
                            color = MoneySecondary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(50))
                                .background(Color.White.copy(alpha = 0.7f))
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(budgetRatio.coerceAtLeast(0.04f))
                                    .height(8.dp)
                                    .clip(RoundedCornerShape(50))
                                    .background(if (budgetRatio > 0.85f) MoneyRed else MoneyGreenMain)
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Button(
                            onClick = { showBudgetDialog = true },
                            shape = RoundedCornerShape(50),
                            colors = ButtonDefaults.buttonColors(containerColor = MoneyGreenMain),
                            contentPadding = PaddingValues(horizontal = 18.dp, vertical = 10.dp),
                            modifier = Modifier.testTag("set_monthly_budget_button")
                        ) {
                            Text("Set Now", fontSize = 15.sp, fontWeight = FontWeight.ExtraBold)
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(Icons.AutoMirrored.Rounded.ArrowForward, contentDescription = null, modifier = Modifier.size(18.dp))
                        }
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    BudgetTargetArtwork()
                }
            }
        }

        // Menu List Card
        item {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                color = MoneyCardWhite,
                shadowElevation = 2.dp,
                border = BorderStroke(1.dp, MoneyBorder)
            ) {
                Column(modifier = Modifier.padding(vertical = 6.dp)) {
                    MoreMenuRow(
                        title = "Categories Breakdown",
                        icon = Icons.Rounded.Category,
                        iconBg = Color(0xFFFEF3C7),
                        iconTint = Color(0xFFD97706),
                        onClick = { showCategoriesDialog = true }
                    )
                    MoreMenuRow(
                        title = "Opening Balance (${formatInr(summary.openingBalance)})",
                        icon = Icons.Rounded.AccountBalanceWallet,
                        iconBg = MoneySoftGreen,
                        iconTint = MoneyGreenMain,
                        onClick = { showOpeningBalanceDialog = true }
                    )
                    MoreMenuRow(
                        title = "Message Tracking & SMS Sync",
                        icon = Icons.Rounded.Sms,
                        iconBg = Color(0xFFE0E7FF),
                        iconTint = Color(0xFF4F46E5),
                        onClick = onOpenMessageTracking
                    )
                    MoreMenuRow(
                        title = "Sync Bank SMS Now",
                        icon = Icons.Rounded.Refresh,
                        iconBg = Color(0xFFE0F2FE),
                        iconTint = Color(0xFF0284C7),
                        onClick = onSyncSmsNow
                    )
                    MoreMenuRow(
                        title = "Export CSV Report",
                        icon = Icons.Rounded.CloudUpload,
                        iconBg = Color(0xFFDBEAFE),
                        iconTint = Color(0xFF2563EB),
                        onClick = {
                            val csv = buildString {
                                appendLine("Date,Title,Type,Category,Mode,Amount(INR)")
                                transactions.forEach { tx ->
                                    appendLine(
                                        "${formatTransactionDateLabel(tx.timestampMs)},\"${tx.title}\",${tx.type},${tx.category},${tx.paymentMode},${tx.amount}"
                                    )
                                }
                            }
                            val share = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_SUBJECT, "LifeOS Money Tracker Report")
                                putExtra(Intent.EXTRA_TEXT, csv)
                            }
                            context.startActivity(Intent.createChooser(share, "Export Money Report"))
                        }
                    )
                    MoreMenuRow(
                        title = "Clear All Transactions",
                        icon = Icons.Rounded.Delete,
                        iconBg = MoneySoftRed,
                        iconTint = MoneyRed,
                        onClick = { showClearConfirm = true }
                    )
                }
            }
        }

        // Bottom Return to LifeOS pill (mirrors red Logout button slot cleanly!)
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(MoneySoftRed)
                    .clickable(onClick = onExitToLifeOs)
                    .padding(vertical = 16.dp)
                    .testTag("money_back_to_lifeos_button"),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.ExitToApp,
                    contentDescription = null,
                    tint = MoneyRed,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Back to LifeOS Home",
                    color = MoneyRed,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Black
                )
            }
        }
    }

    if (showBudgetDialog) {
        NumericAmountDialog(
            title = "Set Monthly Budget",
            label = "Monthly Budget Amount (₹)",
            initialValue = summary.monthlyBudget.toLong().toString(),
            onDismiss = { showBudgetDialog = false },
            onSave = { onUpdateBudget(it) }
        )
    }

    if (showOpeningBalanceDialog) {
        NumericAmountDialog(
            title = "Set Opening Balance",
            label = "Starting Bank / Cash Balance (₹)",
            initialValue = summary.openingBalance.toLong().toString(),
            onDismiss = { showOpeningBalanceDialog = false },
            onSave = { onUpdateOpeningBalance(it) }
        )
    }

    if (showCategoriesDialog) {
        val grouped = transactions.filter { it.type == "EXPENSE" }
            .groupBy { it.category }
            .mapValues { (_, list) -> list.sumOf { it.amount } }
        AlertDialog(
            onDismissRequest = { showCategoriesDialog = false },
            containerColor = MoneyCardWhite,
            titleContentColor = MoneyInk,
            textContentColor = MoneyInk,
            title = { Text("Expense Categories", fontSize = 20.sp, fontWeight = FontWeight.Black) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    listOf("Food", "Shopping", "Bills", "Travel", "Others").forEach { cat ->
                        val amt = grouped[cat] ?: 0.0
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(cat, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MoneyInk)
                            Text(formatInr(amt), fontSize = 16.sp, fontWeight = FontWeight.Black, color = categoryColorFor(cat))
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showCategoriesDialog = false }) {
                    Text("Done", color = MoneyGreenMain, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold)
                }
            }
        )
    }

    if (showClearConfirm) {
        AlertDialog(
            onDismissRequest = { showClearConfirm = false },
            containerColor = MoneyCardWhite,
            titleContentColor = MoneyInk,
            textContentColor = MoneySecondary,
            title = { Text("Clear All Transactions?", fontSize = 20.sp, fontWeight = FontWeight.Black) },
            text = {
                Text(
                    "This deletes all saved transactions. You can always re-sync bank SMS later.",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onClearAllTransactions()
                        showClearConfirm = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MoneyRed)
                ) {
                    Text("Clear All", fontWeight = FontWeight.ExtraBold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirm = false }) {
                    Text("Cancel", color = MoneySecondary, fontWeight = FontWeight.Bold)
                }
            }
        )
    }
}

@Composable
private fun MoreMenuRow(
    title: String,
    icon: ImageVector,
    iconBg: Color,
    iconTint: Color,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(RoundedCornerShape(13.dp))
                .background(iconBg),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(22.dp))
        }
        Spacer(modifier = Modifier.width(14.dp))
        Text(
            text = title,
            color = MoneyInk,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Icon(
            imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
            contentDescription = null,
            tint = MoneySecondary,
            modifier = Modifier.size(22.dp)
        )
    }
}

// --- DIALOGS ---

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AddMoneyTransactionDialog(
    onDismiss: () -> Unit,
    onSave: (title: String, amount: Double, type: String, category: String, mode: String) -> Unit
) {
    var type by remember { mutableStateOf("EXPENSE") }
    var amountText by remember { mutableStateOf("") }
    var title by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("Food") }
    var mode by remember { mutableStateOf("UPI") }
    var errorText by remember { mutableStateOf<String?>(null) }

    val categories = if (type == "INCOME") {
        listOf("Salary", "Income", "Others")
    } else {
        listOf("Food", "Shopping", "Bills", "Travel", "Others")
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MoneyCardWhite,
        titleContentColor = MoneyInk,
        textContentColor = MoneyInk,
        title = {
            Text("Add Transaction", fontSize = 22.sp, fontWeight = FontWeight.Black)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // Expense vs Income Pill Toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    listOf("EXPENSE" to "Expense (-)", "INCOME" to "Income (+)").forEach { (key, label) ->
                        val selected = type == key
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(14.dp))
                                .background(
                                    when {
                                        selected && key == "INCOME" -> MoneyGreenMain
                                        selected -> MoneyRed
                                        else -> MoneyChipBg
                                    }
                                )
                                .clickable {
                                    type = key
                                    category = if (key == "INCOME") "Income" else "Food"
                                }
                                .padding(vertical = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                color = if (selected) Color.White else MoneyInk,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Black
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = amountText,
                    onValueChange = {
                        amountText = it.filter { ch -> ch.isDigit() || ch == '.' }
                        errorText = null
                    },
                    label = { Text("Amount (₹)", fontWeight = FontWeight.Bold) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("add_tx_amount_input"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MoneyGreenMain,
                        unfocusedBorderColor = MoneyBorder,
                        focusedTextColor = MoneyInk,
                        unfocusedTextColor = MoneyInk
                    )
                )

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Description (e.g. Food Order, Rahul)", fontWeight = FontWeight.Bold) },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("add_tx_title_input"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MoneyGreenMain,
                        unfocusedBorderColor = MoneyBorder,
                        focusedTextColor = MoneyInk,
                        unfocusedTextColor = MoneyInk
                    )
                )

                Text("Category", fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, color = MoneySecondary)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    categories.forEach { cat ->
                        val sel = category == cat
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(if (sel) MoneyInk else MoneyChipBg)
                                .clickable { category = cat }
                                .padding(horizontal = 14.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = cat,
                                color = if (sel) Color.White else MoneyInk,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    }
                }

                Text("Payment Mode", fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, color = MoneySecondary)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("UPI", "Bank", "Wallet", "Card", "Cash").forEach { m ->
                        val sel = mode == m
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(if (sel) MoneyGreenMain else MoneyChipBg)
                                .clickable { mode = m }
                                .padding(horizontal = 14.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = m,
                                color = if (sel) Color.White else MoneyInk,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    }
                }

                errorText?.let {
                    Text(it, color = MoneyRed, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amt = amountText.toDoubleOrNull()
                    if (amt == null || amt <= 0.0) {
                        errorText = "Please enter a valid amount greater than ₹0."
                    } else {
                        val finalTitle = title.trim().ifEmpty {
                            if (type == "INCOME") "$mode Credit" else "$category Expense"
                        }
                        onSave(finalTitle, amt, type, category, mode)
                        onDismiss()
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = MoneyGreenMain),
                modifier = Modifier.testTag("save_tx_button")
            ) {
                Text("Save", fontSize = 16.sp, fontWeight = FontWeight.Black)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = MoneySecondary, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            }
        }
    )
}

@Composable
private fun NumericAmountDialog(
    title: String,
    label: String,
    initialValue: String,
    onDismiss: () -> Unit,
    onSave: (Double) -> Unit
) {
    var valueText by remember { mutableStateOf(initialValue) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MoneyCardWhite,
        titleContentColor = MoneyInk,
        textContentColor = MoneyInk,
        title = { Text(title, fontSize = 20.sp, fontWeight = FontWeight.Black) },
        text = {
            OutlinedTextField(
                value = valueText,
                onValueChange = { valueText = it.filter { ch -> ch.isDigit() || ch == '.' || ch == '-' } },
                label = { Text(label, fontWeight = FontWeight.Bold) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MoneyGreenMain,
                    unfocusedBorderColor = MoneyBorder,
                    focusedTextColor = MoneyInk,
                    unfocusedTextColor = MoneyInk
                )
            )
        },
        confirmButton = {
            Button(
                onClick = {
                    val num = valueText.toDoubleOrNull() ?: 0.0
                    onSave(num)
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(containerColor = MoneyGreenMain)
            ) {
                Text("Save", fontSize = 16.sp, fontWeight = FontWeight.ExtraBold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = MoneySecondary, fontWeight = FontWeight.Bold)
            }
        }
    )
}

@Composable
fun TransactionDetailDialog(
    tx: MoneyTransaction,
    onDismiss: () -> Unit,
    onDelete: () -> Unit
) {
    val isInc = tx.type == "INCOME"
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MoneyCardWhite,
        titleContentColor = MoneyInk,
        textContentColor = MoneyInk,
        title = {
            Text(tx.title, fontSize = 20.sp, fontWeight = FontWeight.Black)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "${if (isInc) "+" else "-"}${formatInr(tx.amount)}",
                    color = if (isInc) MoneyGreenMain else MoneyRed,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Black
                )
                Text(
                    text = "${tx.paymentMode} • ${tx.category} • ${formatTransactionDateLabel(tx.timestampMs)}",
                    color = MoneySecondary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Source: ${if (tx.source == "SMS") "Auto-tracked SMS (${tx.smsSender})" else "Manual Entry"}",
                    color = MoneySecondary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
                tx.detectedBankBalance?.let { bal ->
                    Text(
                        text = "SMS Bank Balance: ${formatInr(bal)}",
                        color = MoneyGreenMain,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
                if (tx.smsBody.isNotBlank()) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MoneyChipBg
                    ) {
                        Text(
                            text = tx.smsBody,
                            color = MoneyInk,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", color = MoneyGreenMain, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold)
            }
        },
        dismissButton = {
            TextButton(
                onClick = {
                    onDelete()
                    onDismiss()
                }
            ) {
                Text("Delete", color = MoneyRed, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold)
            }
        }
    )
}
