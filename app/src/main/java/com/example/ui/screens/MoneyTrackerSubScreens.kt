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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ExitToApp
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.CloudUpload
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.GppBad
import androidx.compose.material.icons.rounded.GppMaybe
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.Sms
import androidx.compose.material.icons.rounded.TrackChanges
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.VerifiedUser
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.MoneyTransaction
import com.example.data.UserProfile
import com.example.money.BankTemplateRegistry
import com.example.money.TransactionStatus
import com.example.money.TrustClassification
import com.example.money.formatInr
import com.example.money.formatTransactionDateLabel
import com.example.ui.viewmodel.MoneySummaryState
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.abs

// --- PANEL 3: FULL-SCREEN ADD TRANSACTION ---

@Composable
fun MoneyAddTransactionScreen(
    onBack: () -> Unit,
    onSave: (title: String, amount: Double, type: String, category: String, paymentMode: String, timestampMs: Long) -> Unit
) {
    var txType by rememberSaveable { mutableStateOf("EXPENSE") }
    var amountText by rememberSaveable { mutableStateOf("") }
    var selectedCategory by rememberSaveable { mutableStateOf("Food") }
    var selectedMode by rememberSaveable { mutableStateOf("UPI") }
    var noteText by rememberSaveable { mutableStateOf("") }
    var dateOffsetDays by rememberSaveable { mutableStateOf(0) } // 0 = Today, -1 = Yesterday, -2 = 2 days ago

    val categories = if (txType == "INCOME") INCOME_CATEGORY_VISUALS else EXPENSE_CATEGORY_VISUALS
    val effectiveCategory = if (categories.any { it.name == selectedCategory }) {
        selectedCategory
    } else {
        categories.first().name
    }

    val selectedTimestamp = remember(dateOffsetDays) {
        Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, dateOffsetDays)
        }.timeInMillis
    }
    val dateDisplay = remember(selectedTimestamp) {
        SimpleDateFormat("MMM d, yyyy", Locale.US).format(Date(selectedTimestamp))
    }

    val parsedAmount = amountText.replace(",", "").trim().toDoubleOrNull()
    val canSave = parsedAmount != null && parsedAmount > 0.0

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MoneyBg),
        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Top Bar
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
                        .testTag("add_tx_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = "Back",
                        tint = MoneyInk
                    )
                }
                Text(
                    text = "Add Transaction",
                    color = MoneyInk,
                    fontSize = 23.sp,
                    fontWeight = FontWeight.Black
                )
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(MoneyCardWhite)
                        .border(1.dp, MoneyBorder, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Tune,
                        contentDescription = null,
                        tint = MoneyInk,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }

        // Expense / Income Toggle Pills (exact match to Panel 3)
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                val isExpense = txType == "EXPENSE"
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .height(54.dp)
                        .clickable {
                            txType = "EXPENSE"
                            selectedCategory = "Food"
                        }
                        .testTag("add_tx_type_expense"),
                    shape = RoundedCornerShape(18.dp),
                    color = if (isExpense) MoneySoftRed else MoneyCardWhite,
                    border = BorderStroke(
                        if (isExpense) 1.8.dp else 1.dp,
                        if (isExpense) MoneyRed.copy(alpha = 0.55f) else MoneyBorder
                    )
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(MoneyRed),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Rounded.Remove, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Expense",
                            color = if (isExpense) MoneyRed else MoneyInk,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                }

                val isIncome = txType == "INCOME"
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .height(54.dp)
                        .clickable {
                            txType = "INCOME"
                            selectedCategory = "Salary"
                        }
                        .testTag("add_tx_type_income"),
                    shape = RoundedCornerShape(18.dp),
                    color = if (isIncome) MoneySoftGreen else MoneyCardWhite,
                    border = BorderStroke(
                        if (isIncome) 1.8.dp else 1.dp,
                        if (isIncome) MoneyGreenMain.copy(alpha = 0.55f) else MoneyBorder
                    )
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(MoneyGreenMain),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Rounded.Add, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Income",
                            color = if (isIncome) MoneyGreenDeep else MoneyInk,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                }
            }
        }

        // Giant Centered ₹ Amount Input Box (matching Panel 3)
        item {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                color = Color(0xFFEEF2F8),
                border = BorderStroke(1.2.dp, MoneyBorder)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 20.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "₹",
                        color = MoneyInk,
                        fontSize = 38.sp,
                        fontWeight = FontWeight.Black
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    BasicTextField(
                        value = amountText,
                        onValueChange = { input ->
                            val filtered = input.filter { it.isDigit() || it == '.' }
                            if (filtered.count { it == '.' } <= 1 && filtered.length <= 10) {
                                amountText = filtered
                            }
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        cursorBrush = SolidColor(MoneyGreenDeep),
                        textStyle = TextStyle(
                            color = MoneyInk,
                            fontSize = 38.sp,
                            fontWeight = FontWeight.Black,
                            textAlign = TextAlign.Start
                        ),
                        modifier = Modifier
                            .width(180.dp)
                            .testTag("add_tx_amount_input"),
                        decorationBox = { inner ->
                            if (amountText.isEmpty()) {
                                Text(
                                    text = "0",
                                    color = MoneySecondary.copy(alpha = 0.65f),
                                    fontSize = 38.sp,
                                    fontWeight = FontWeight.Black
                                )
                            }
                            inner()
                        }
                    )
                }
            }
        }

        // 4x2 Category Grid (matching Panel 3)
        item {
            val rows = categories.chunked(4)
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                rows.forEach { rowItems ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        rowItems.forEach { cat ->
                            val selected = effectiveCategory == cat.name
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(18.dp))
                                    .clickable { selectedCategory = cat.name }
                                    .padding(vertical = 4.dp)
                                    .testTag("add_tx_cat_${cat.name.lowercase()}"),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(64.dp)
                                        .clip(RoundedCornerShape(20.dp))
                                        .background(if (selected) cat.tint else cat.bg)
                                        .border(
                                            width = if (selected) 2.5.dp else 1.dp,
                                            color = if (selected) MoneyInk else cat.tint.copy(alpha = 0.18f),
                                            shape = RoundedCornerShape(20.dp)
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = cat.icon,
                                        contentDescription = cat.name,
                                        tint = if (selected) Color.White else cat.tint,
                                        modifier = Modifier.size(30.dp)
                                    )
                                }
                                Text(
                                    text = cat.name,
                                    color = if (selected) MoneyInk else MoneySecondary,
                                    fontSize = 13.sp,
                                    fontWeight = if (selected) FontWeight.Black else FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                        repeat(4 - rowItems.size) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }

        // Date Selector Card
        item {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                color = MoneyCardWhite,
                border = BorderStroke(1.dp, MoneyBorder)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Rounded.CalendarMonth,
                                contentDescription = null,
                                tint = MoneyInk,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text("Date", color = MoneyInk, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                        }
                        Text(dateDisplay, color = MoneyInk, fontSize = 17.sp, fontWeight = FontWeight.Black)
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(0 to "Today", -1 to "Yesterday", -2 to "2 Days Ago").forEach { (offset, label) ->
                            val sel = dateOffsetDays == offset
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(50))
                                    .background(if (sel) MoneyGreenDeep else MoneyChipBg)
                                    .clickable { dateOffsetDays = offset }
                                    .padding(horizontal = 14.dp, vertical = 7.dp)
                            ) {
                                Text(
                                    text = label,
                                    color = if (sel) Color.White else MoneyInk,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }
                        }
                    }
                }
            }
        }

        // Payment Mode Row
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("UPI", "Bank", "Cash", "Card").forEach { mode ->
                    val sel = selectedMode == mode
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(14.dp))
                            .background(if (sel) MoneyGreenDeep else MoneyCardWhite)
                            .border(1.dp, if (sel) MoneyGreenDeep else MoneyBorder, RoundedCornerShape(14.dp))
                            .clickable { selectedMode = mode }
                            .padding(vertical = 11.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = mode,
                            color = if (sel) Color.White else MoneyInk,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                }
            }
        }

        // Note (Optional) Card
        item {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                color = MoneyCardWhite,
                border = BorderStroke(1.dp, MoneyBorder)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Edit,
                        contentDescription = null,
                        tint = MoneySecondary,
                        modifier = Modifier
                            .padding(top = 12.dp)
                            .size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    OutlinedTextField(
                        value = noteText,
                        onValueChange = { noteText = it },
                        label = { Text("Note (Optional)", fontWeight = FontWeight.Bold) },
                        placeholder = { Text("Add a note or merchant name...") },
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("add_tx_note_input"),
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = MoneyChipBg.copy(alpha = 0.45f),
                            unfocusedContainerColor = MoneyChipBg.copy(alpha = 0.45f),
                            focusedBorderColor = MoneyGreenMain,
                            unfocusedBorderColor = Color.Transparent,
                            focusedTextColor = MoneyInk,
                            unfocusedTextColor = MoneyInk
                        )
                    )
                }
            }
        }

        // Deep Emerald Submit Button
        item {
            Button(
                onClick = {
                    if (canSave && parsedAmount != null) {
                        val finalTitle = noteText.trim().ifEmpty { effectiveCategory }
                        onSave(finalTitle, parsedAmount, txType, effectiveCategory, selectedMode, selectedTimestamp)
                        onBack()
                    }
                },
                enabled = canSave,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(58.dp)
                    .testTag("add_tx_submit_button"),
                shape = RoundedCornerShape(50),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MoneyGreenDeep,
                    contentColor = Color.White,
                    disabledContainerColor = MoneyGreenDeep.copy(alpha = 0.4f)
                )
            ) {
                Text(
                    text = "Add Transaction",
                    fontSize = 19.sp,
                    fontWeight = FontWeight.Black
                )
            }
        }
    }
}

// --- PANEL 5: ANALYTICS SCREEN ---

@Composable
fun MoneyAnalyticsScreen(
    transactions: List<MoneyTransaction>,
    onBack: () -> Unit,
    bottomPadding: PaddingValues
) {
    var selectedPeriod by rememberSaveable { mutableStateOf("Month") }

    val confirmedExpenses = remember(transactions) {
        transactions.filter { it.status == TransactionStatus.CONFIRMED.key && it.type == "EXPENSE" }
    }

    val filteredExpenses = remember(confirmedExpenses, selectedPeriod) {
        val now = System.currentTimeMillis()
        val nowCal = Calendar.getInstance()
        val curMonth = nowCal.get(Calendar.MONTH)
        val curYear = nowCal.get(Calendar.YEAR)
        val txCal = Calendar.getInstance()

        confirmedExpenses.filter { tx ->
            txCal.timeInMillis = tx.timestampMs
            when (selectedPeriod) {
                "Week" -> tx.timestampMs >= now - 7L * 24L * 60L * 60L * 1000L
                "Month" -> txCal.get(Calendar.MONTH) == curMonth && txCal.get(Calendar.YEAR) == curYear
                else -> txCal.get(Calendar.YEAR) == curYear
            }
        }
    }

    val totalSpent = filteredExpenses.sumOf { it.amount }

    val (trendPoints, xLabels) = remember(filteredExpenses, selectedPeriod) {
        val cal = Calendar.getInstance()
        when (selectedPeriod) {
            "Week" -> {
                val buckets = DoubleArray(7) { 0.0 }
                val todayCal = Calendar.getInstance()
                val todayDay = todayCal.get(Calendar.DAY_OF_YEAR)
                for (tx in filteredExpenses) {
                    cal.timeInMillis = tx.timestampMs
                    val diff = (todayDay - cal.get(Calendar.DAY_OF_YEAR)).coerceIn(0, 6)
                    buckets[6 - diff] += tx.amount
                }
                buckets.toList() to listOf("D1", "D2", "D3", "D4", "D5", "D6", "Today")
            }
            "Year" -> {
                val buckets = DoubleArray(4) { 0.0 }
                for (tx in filteredExpenses) {
                    cal.timeInMillis = tx.timestampMs
                    val q = (cal.get(Calendar.MONTH) / 3).coerceIn(0, 3)
                    buckets[q] += tx.amount
                }
                buckets.toList() to listOf("Q1", "Q2", "Q3", "Q4")
            }
            else -> {
                val buckets = DoubleArray(4) { 0.0 }
                for (tx in filteredExpenses) {
                    cal.timeInMillis = tx.timestampMs
                    val day = cal.get(Calendar.DAY_OF_MONTH)
                    val idx = when {
                        day <= 7 -> 0
                        day <= 14 -> 1
                        day <= 21 -> 2
                        else -> 3
                    }
                    buckets[idx] += tx.amount
                }
                buckets.toList() to listOf("1-7", "8-14", "15-21", "22-30")
            }
        }
    }

    val topCategories = remember(filteredExpenses, totalSpent) {
        val grouped = filteredExpenses.groupBy { it.category }
            .mapValues { (_, list) -> list.sumOf { it.amount } }
        val baseKeys = (grouped.keys + listOf("Food", "Study", "Travel", "Others")).distinct()
        baseKeys.map { cat ->
            val amt = grouped[cat] ?: 0.0
            val pct = if (totalSpent > 0.0) ((amt / totalSpent) * 100.0).toInt() else 0
            CategorySlice(cat, amt, pct, categoryVisualFor(cat).tint)
        }.sortedByDescending { it.amount }.take(6)
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
        // Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(MoneyCardWhite)
                        .border(1.dp, MoneyBorder, CircleShape)
                        .testTag("analytics_back_button")
                ) {
                    Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back", tint = MoneyInk)
                }
                Icon(
                    imageVector = Icons.Rounded.BarChart,
                    contentDescription = null,
                    tint = MoneyGreenMain,
                    modifier = Modifier.size(30.dp)
                )
                Text(
                    text = "Analytics",
                    color = MoneyInk,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Black
                )
            }
        }

        // Week | Month | Year Segmented Pill Bar
        item {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(50),
                color = MoneyChipBg
            ) {
                Row(modifier = Modifier.padding(4.dp)) {
                    listOf("Week", "Month", "Year").forEach { period ->
                        val selected = selectedPeriod == period
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(50))
                                .background(if (selected) MoneyGreenDeep else Color.Transparent)
                                .clickable { selectedPeriod = period }
                                .padding(vertical = 12.dp)
                                .testTag("analytics_period_${period.lowercase()}"),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = period,
                                color = if (selected) Color.White else MoneyInk,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Black
                            )
                        }
                    }
                }
            }
        }

        // Spending Trend Card (Panel 5)
        item {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                color = MoneyCardWhite,
                shadowElevation = 2.dp,
                border = BorderStroke(1.dp, MoneyBorder)
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Spending Trend",
                        color = MoneyInk,
                        fontSize = 21.sp,
                        fontWeight = FontWeight.Black
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = formatInr(totalSpent),
                            color = MoneyInk,
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Black
                        )
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(MoneySoftGreen)
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "Confirmed Only",
                                color = MoneyGreenDeep,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Black
                            )
                        }
                    }
                    SpendingTrendLineChart(
                        points = trendPoints,
                        xLabels = xLabels
                    )
                }
            }
        }

        // Top Categories Horizontal Progress Bars (Panel 5)
        item {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                color = MoneyCardWhite,
                shadowElevation = 2.dp,
                border = BorderStroke(1.dp, MoneyBorder)
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        text = "Top Categories",
                        color = MoneyInk,
                        fontSize = 21.sp,
                        fontWeight = FontWeight.Black
                    )
                    topCategories.forEach { slice ->
                        val vis = categoryVisualFor(slice.name)
                        val barFraction = (slice.percent / 100f).coerceIn(0.03f, 1f)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(vis.bg),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(vis.icon, contentDescription = null, tint = vis.tint, modifier = Modifier.size(20.dp))
                            }
                            Text(
                                text = slice.name,
                                color = MoneyInk,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.ExtraBold,
                                modifier = Modifier.width(74.dp),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(10.dp)
                                    .clip(RoundedCornerShape(50))
                                    .background(MoneyChipBg)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(if (slice.amount > 0.0) barFraction else 0.02f)
                                        .height(10.dp)
                                        .clip(RoundedCornerShape(50))
                                        .background(vis.tint)
                                )
                            }
                            Text(
                                text = "${slice.percent}%",
                                color = MoneyInk,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Black,
                                modifier = Modifier.width(42.dp),
                                textAlign = TextAlign.End
                            )
                        }
                    }
                }
            }
        }
    }
}

// --- PANEL 6: MY GOALS + QUICK TOOLS + SETTINGS ---

@Composable
fun MoneyGoalsAndSettingsScreen(
    showSettingsFocus: Boolean,
    profile: UserProfile,
    summary: MoneySummaryState,
    transactions: List<MoneyTransaction>,
    onOpenAnalytics: () -> Unit,
    onOpenTrustCenter: (TrustCenterTab) -> Unit,
    onUpdateBudget: (Double) -> Unit,
    onUpdateOpeningBalance: (Double) -> Unit,
    onUpdateSavingsGoal: (String, Double, Double, String) -> Unit,
    onDeleteImportedSmsData: () -> Unit,
    onClearAllTransactions: () -> Unit,
    onExitToLifeOs: () -> Unit,
    bottomPadding: PaddingValues
) {
    val context = LocalContext.current
    var showBudgetDialog by remember { mutableStateOf(false) }
    var showOpeningBalanceDialog by remember { mutableStateOf(false) }
    var showGoalDialog by remember { mutableStateOf(false) }
    var showDeleteSmsConfirm by remember { mutableStateOf(false) }
    var showClearAllConfirm by remember { mutableStateOf(false) }

    val goalTarget = profile.savingsGoalTarget.coerceAtLeast(100.0)
    val effectiveSaved = if (profile.savingsGoalSaved > 0.0) {
        profile.savingsGoalSaved
    } else {
        summary.calculatedAppBalance.coerceAtLeast(0.0)
    }
    val goalPct = ((effectiveSaved / goalTarget) * 100.0).toInt().coerceIn(0, 100)
    val flaggedTotalCount = summary.pendingCount + summary.quarantinedCount

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
        // Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    imageVector = if (showSettingsFocus) Icons.Rounded.VerifiedUser else Icons.Rounded.TrackChanges,
                    contentDescription = null,
                    tint = if (showSettingsFocus) MoneyGreenDeep else MoneyRed,
                    modifier = Modifier.size(30.dp)
                )
                Text(
                    text = if (showSettingsFocus) "Settings & Trust" else "My Goals",
                    color = MoneyInk,
                    fontSize = 27.sp,
                    fontWeight = FontWeight.Black
                )
            }
        }

        // Emerald Savings Goal Card (matching Panel 6 Hero)
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(
                        Brush.linearGradient(
                            listOf(MoneyGreenDeep, MoneyGreenMain, Color(0xFF065F46))
                        )
                    )
                    .clickable { showGoalDialog = true }
                    .padding(20.dp)
                    .testTag("money_savings_goal_card")
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = profile.savingsGoalName.ifBlank { "Save ${formatInr(goalTarget)}" },
                            color = Color.White,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Black
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(10.dp)
                                    .clip(RoundedCornerShape(50))
                                    .background(Color.White.copy(alpha = 0.25f))
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth((goalPct / 100f).coerceAtLeast(0.05f))
                                        .height(10.dp)
                                        .clip(RoundedCornerShape(50))
                                        .background(Color(0xFF86EFAC))
                                )
                            }
                            Text(
                                text = "$goalPct%",
                                color = Color.White,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Black
                            )
                        }
                        Text(
                            text = "Saved: ${formatInr(effectiveSaved)} / ${formatInr(goalTarget)}",
                            color = Color(0xFFD1FAE5),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Text(
                            text = "Target Date: ${profile.savingsGoalTargetDate.ifBlank { "Dec 31, 2026" }}  •  Tap to edit",
                            color = Color.White.copy(alpha = 0.85f),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Box(
                        modifier = Modifier
                            .size(66.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFFDE047).copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.EmojiEvents,
                            contentDescription = "Goal Trophy",
                            tint = Color(0xFFFDE047),
                            modifier = Modifier.size(44.dp)
                        )
                    }
                }
            }
        }

        // Quick Tools 4-tile row (matching Panel 6)
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Quick Tools",
                    color = MoneyInk,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Black
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    QuickToolTile(
                        label = "Set Budget",
                        icon = Icons.Rounded.AccountBalanceWallet,
                        tint = Color(0xFF2563EB),
                        bg = Color(0xFFEAF3FF),
                        onClick = { showBudgetDialog = true },
                        modifier = Modifier.weight(1f).testTag("quick_tool_budget")
                    )
                    QuickToolTile(
                        label = "Edit Goal",
                        icon = Icons.Rounded.TrackChanges,
                        tint = MoneyRed,
                        bg = MoneySoftRed,
                        onClick = { showGoalDialog = true },
                        modifier = Modifier.weight(1f).testTag("quick_tool_goal")
                    )
                    QuickToolTile(
                        label = "Reports",
                        icon = Icons.Rounded.BarChart,
                        tint = MoneyGreenMain,
                        bg = MoneySoftGreen,
                        onClick = onOpenAnalytics,
                        modifier = Modifier.weight(1f).testTag("quick_tool_reports")
                    )
                    QuickToolTile(
                        label = "SMS Shield",
                        icon = Icons.Rounded.Shield,
                        tint = MoneyAmber,
                        bg = MoneySoftAmber,
                        onClick = { onOpenTrustCenter(TrustCenterTab.OVERVIEW) },
                        modifier = Modifier.weight(1f).testTag("quick_tool_sms_shield")
                    )
                }
            }
        }

        // Settings & SMS Security Card
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Settings & Privacy",
                    color = MoneyInk,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Black
                )
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(22.dp),
                    color = MoneyCardWhite,
                    border = BorderStroke(1.dp, MoneyBorder),
                    shadowElevation = 2.dp
                ) {
                    Column(modifier = Modifier.padding(vertical = 6.dp)) {
                        SettingsMenuRow(
                            title = "SMS Trust Center",
                            subtitle = if (flaggedTotalCount > 0) {
                                "${summary.quarantinedCount} fraud blocked • ${summary.pendingCount} pending"
                            } else {
                                "Fraud shield, live tester & reconciliation"
                            },
                            icon = Icons.Rounded.VerifiedUser,
                            iconBg = MoneySoftGreen,
                            iconTint = MoneyGreenDeep,
                            badgeText = if (flaggedTotalCount > 0) "$flaggedTotalCount" else null,
                            onClick = { onOpenTrustCenter(TrustCenterTab.OVERVIEW) }
                        )
                        SettingsMenuRow(
                            title = "Trusted Senders & Templates",
                            subtitle = "${BankTemplateRegistry.DEFAULT_TEMPLATES.size} banks + custom senders",
                            icon = Icons.Rounded.Sms,
                            iconBg = Color(0xFFEAF3FF),
                            iconTint = MoneyBlueAvatar,
                            onClick = { onOpenTrustCenter(TrustCenterTab.SENDERS) }
                        )
                        SettingsMenuRow(
                            title = "Opening Balance",
                            subtitle = "Current: ${formatInr(summary.openingBalance)} • Monthly Budget: ${formatInr(summary.monthlyBudget)}",
                            icon = Icons.Rounded.AccountBalanceWallet,
                            iconBg = MoneySoftAmber,
                            iconTint = MoneyAmber,
                            onClick = { showOpeningBalanceDialog = true }
                        )
                        SettingsMenuRow(
                            title = "Export Confirmed CSV",
                            subtitle = "Share confirmed transactions only",
                            icon = Icons.Rounded.CloudUpload,
                            iconBg = Color(0xFFEDE9FE),
                            iconTint = CatShoppingColor,
                            onClick = {
                                val confirmed = transactions.filter { it.status == TransactionStatus.CONFIRMED.key }
                                val csv = buildString {
                                    appendLine("Date,Title,Type,Category,Mode,Status,Amount(INR)")
                                    confirmed.forEach { tx ->
                                        appendLine(
                                            "${formatTransactionDateLabel(tx.timestampMs)},\"${tx.title}\",${tx.type},${tx.category},${tx.paymentMode},${tx.status},${tx.amount}"
                                        )
                                    }
                                }
                                val share = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_SUBJECT, "LifeOS Money Tracker Report")
                                    putExtra(Intent.EXTRA_TEXT, csv)
                                }
                                context.startActivity(Intent.createChooser(share, "Export Confirmed Report"))
                            }
                        )
                        SettingsMenuRow(
                            title = "Delete Imported SMS Data",
                            subtitle = "Removes auto-imported SMS only; keeps manual records",
                            icon = Icons.Rounded.Delete,
                            iconBg = MoneySoftRed,
                            iconTint = MoneyRed,
                            onClick = { showDeleteSmsConfirm = true }
                        )
                        SettingsMenuRow(
                            title = "Clear All Transactions",
                            subtitle = "Reset all money records",
                            icon = Icons.Rounded.Info,
                            iconBg = MoneyChipBg,
                            iconTint = MoneySecondary,
                            onClick = { showClearAllConfirm = true }
                        )
                    }
                }
            }
        }

        // Back to LifeOS Button
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
                Icon(Icons.AutoMirrored.Rounded.ExitToApp, contentDescription = null, tint = MoneyRed, modifier = Modifier.size(22.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Back to LifeOS Home", color = MoneyRed, fontSize = 17.sp, fontWeight = FontWeight.Black)
            }
        }
    }

    if (showBudgetDialog) {
        NumericAmountDialog(
            title = "Set Monthly Budget",
            label = "Monthly Budget (₹)",
            initialValue = summary.monthlyBudget.toLong().toString(),
            onDismiss = { showBudgetDialog = false },
            onSave = onUpdateBudget
        )
    }

    if (showOpeningBalanceDialog) {
        NumericAmountDialog(
            title = "Set Opening Balance",
            label = "Starting Balance before tracked SMS (₹)",
            initialValue = summary.openingBalance.toLong().toString(),
            onDismiss = { showOpeningBalanceDialog = false },
            onSave = onUpdateOpeningBalance
        )
    }

    if (showGoalDialog) {
        EditSavingsGoalDialog(
            initialName = profile.savingsGoalName,
            initialTarget = profile.savingsGoalTarget,
            initialSaved = profile.savingsGoalSaved,
            initialDate = profile.savingsGoalTargetDate,
            onDismiss = { showGoalDialog = false },
            onSave = { name, target, saved, date ->
                onUpdateSavingsGoal(name, target, saved, date)
                showGoalDialog = false
            }
        )
    }

    if (showDeleteSmsConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteSmsConfirm = false },
            containerColor = MoneyCardWhite,
            title = { Text("Delete All Imported SMS Data?", fontSize = 20.sp, fontWeight = FontWeight.Black, color = MoneyInk) },
            text = {
                Text(
                    "All transactions imported from SMS (including pending & quarantined) will be permanently deleted from this device. Manual transactions will NOT be affected.",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MoneySecondary
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteImportedSmsData()
                        showDeleteSmsConfirm = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MoneyRed)
                ) {
                    Text("Delete SMS Data", fontWeight = FontWeight.Black)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteSmsConfirm = false }) {
                    Text("Cancel", color = MoneySecondary, fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    if (showClearAllConfirm) {
        AlertDialog(
            onDismissRequest = { showClearAllConfirm = false },
            containerColor = MoneyCardWhite,
            title = { Text("Clear All Transactions?", fontSize = 20.sp, fontWeight = FontWeight.Black, color = MoneyInk) },
            text = {
                Text(
                    "This deletes every saved transaction (manual and SMS). Opening balance stays unchanged.",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MoneySecondary
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onClearAllTransactions()
                        showClearAllConfirm = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MoneyRed)
                ) {
                    Text("Clear All", fontWeight = FontWeight.Black)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearAllConfirm = false }) {
                    Text("Cancel", color = MoneySecondary, fontWeight = FontWeight.Bold)
                }
            }
        )
    }
}

@Composable
private fun QuickToolTile(
    label: String,
    icon: ImageVector,
    tint: Color,
    bg: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        color = MoneyCardWhite,
        border = BorderStroke(1.dp, MoneyBorder),
        shadowElevation = 2.dp
    ) {
        Column(
            modifier = Modifier.padding(vertical = 14.dp, horizontal = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(bg),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = label, tint = tint, modifier = Modifier.size(24.dp))
            }
            Text(
                text = label,
                color = MoneyInk,
                fontSize = 12.sp,
                fontWeight = FontWeight.Black,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun SettingsMenuRow(
    title: String,
    subtitle: String,
    icon: ImageVector,
    iconBg: Color,
    iconTint: Color,
    badgeText: String? = null,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 13.dp),
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
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = MoneyInk, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold)
            Text(subtitle, color = MoneySecondary, fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        if (badgeText != null) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(MoneyRed)
                    .padding(horizontal = 8.dp, vertical = 2.dp)
            ) {
                Text(badgeText, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Black)
            }
            Spacer(modifier = Modifier.width(6.dp))
        }
        Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, contentDescription = null, tint = MoneySecondary)
    }
}

// --- TRANSACTION VERIFICATION DETAILS & MANUAL CORRECTION DIALOG ---

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TransactionVerificationDetailDialog(
    tx: MoneyTransaction,
    onDismiss: () -> Unit,
    onUpdateTx: (MoneyTransaction) -> Unit,
    onApproveTx: (MoneyTransaction, Boolean) -> Unit,
    onQuarantineTx: (MoneyTransaction) -> Unit,
    onDeleteTx: (String) -> Unit
) {
    var editMode by remember { mutableStateOf(false) }
    var editTitle by remember(tx.id) { mutableStateOf(tx.title) }
    var editAmount by remember(tx.id) { mutableStateOf(tx.amount.toString()) }
    var editType by remember(tx.id) { mutableStateOf(tx.type) }
    var editCategory by remember(tx.id) { mutableStateOf(tx.category) }

    val statusEnum = TransactionStatus.fromKey(tx.status)
    val trustEnum = TrustClassification.fromKey(tx.trustClassification)
    val isInc = tx.type == "INCOME"
    val statusColor = when (statusEnum) {
        TransactionStatus.CONFIRMED -> MoneyGreenMain
        TransactionStatus.PENDING_REVIEW -> MoneyAmber
        TransactionStatus.QUARANTINED -> MoneyRed
    }
    val statusBg = when (statusEnum) {
        TransactionStatus.CONFIRMED -> MoneySoftGreen
        TransactionStatus.PENDING_REVIEW -> MoneySoftAmber
        TransactionStatus.QUARANTINED -> MoneySoftRed
    }
    val reasons = tx.flagReasonsList()

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MoneyCardWhite,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (editMode) "Edit Transaction" else tx.title,
                    color = MoneyInk,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Black,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                IconButton(onClick = { editMode = !editMode }) {
                    Icon(Icons.Rounded.Edit, contentDescription = "Manual Correction", tint = MoneyBlueAvatar)
                }
            }
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (editMode) {
                    OutlinedTextField(
                        value = editTitle,
                        onValueChange = { editTitle = it },
                        label = { Text("Title / Merchant", fontWeight = FontWeight.Bold) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = editAmount,
                        onValueChange = { editAmount = it.filter { c -> c.isDigit() || c == '.' } },
                        label = { Text("Amount (₹)", fontWeight = FontWeight.Bold) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("EXPENSE", "INCOME").forEach { t ->
                            val sel = editType == t
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (sel) MoneyGreenDeep else MoneyChipBg)
                                    .clickable { editType = t }
                                    .padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(t, color = if (sel) Color.White else MoneyInk, fontWeight = FontWeight.Black, fontSize = 13.sp)
                            }
                        }
                    }
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        val catChoices = if (editType == "INCOME") {
                            listOf("Salary", "Gift", "Income", "Others")
                        } else {
                            listOf("Food", "Travel", "Shopping", "Bills", "Study", "Health", "Entertainment", "Others")
                        }
                        catChoices.forEach { cat ->
                            val sel = editCategory == cat
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(50))
                                    .background(if (sel) MoneyGreenMain else MoneyChipBg)
                                    .clickable { editCategory = cat }
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text(cat, color = if (sel) Color.White else MoneyInk, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                } else {
                    Text(
                        text = "${if (isInc) "+" else "-"}${formatInr(tx.amount)}",
                        color = if (statusEnum == TransactionStatus.QUARANTINED) MoneyRed else if (isInc) MoneyGreenMain else MoneyRed,
                        fontSize = 30.sp,
                        fontWeight = FontWeight.Black
                    )

                    // Trust & Status Pill
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = statusBg,
                        border = BorderStroke(1.dp, statusColor.copy(alpha = 0.45f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = when (statusEnum) {
                                    TransactionStatus.CONFIRMED -> Icons.Rounded.VerifiedUser
                                    TransactionStatus.PENDING_REVIEW -> Icons.Rounded.GppMaybe
                                    TransactionStatus.QUARANTINED -> Icons.Rounded.GppBad
                                },
                                contentDescription = null,
                                tint = statusColor,
                                modifier = Modifier.size(20.dp)
                            )
                            Column {
                                Text(
                                    text = "${statusEnum.label} • ${trustEnum.label}",
                                    color = statusColor,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Black
                                )
                                Text(
                                    text = if (statusEnum == TransactionStatus.CONFIRMED) {
                                        "Included in Calculated App Balance"
                                    } else {
                                        "EXCLUDED from Confirmed Income & App Balance"
                                    },
                                    color = MoneyInk,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    Text(
                        text = "${tx.paymentMode} • ${tx.category} • ${formatTransactionDateLabel(tx.timestampMs)}",
                        color = MoneySecondary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )

                    if (tx.source == "SMS") {
                        Text(
                            text = "Sender: ${tx.smsSender.ifBlank { "Unknown" }} • Risk Score: ${tx.riskScore}/100",
                            color = MoneyInk,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                        if (tx.matchedTemplateId.isNotBlank()) {
                            Text(
                                text = "Template: ${tx.matchedTemplateId}",
                                color = MoneyGreenDeep,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        if (tx.accountSuffix.isNotBlank() || tx.referenceId.isNotBlank()) {
                            Text(
                                text = buildString {
                                    if (tx.accountSuffix.isNotBlank()) append("A/c XX${tx.accountSuffix}  ")
                                    if (tx.referenceId.isNotBlank()) append("Ref: ${tx.referenceId}")
                                },
                                color = MoneySecondary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    tx.detectedBankBalance?.let { bal ->
                        val verifiedTag = if (statusEnum == TransactionStatus.CONFIRMED) "Verified SMS" else "Unverified (Ignored)"
                        Text(
                            text = "SMS Bank Balance ($verifiedTag): ${formatInr(bal)}",
                            color = if (statusEnum == TransactionStatus.CONFIRMED) MoneyGreenMain else MoneyAmber,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }

                    if (reasons.isNotEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(statusBg)
                                .padding(10.dp)
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text("Why was this SMS flagged?", color = MoneyInk, fontSize = 13.sp, fontWeight = FontWeight.Black)
                                reasons.forEach { reason ->
                                    Text("• $reason", color = MoneyInk, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    if (tx.smsBody.isNotBlank()) {
                        // Raw SMS body strictly non-clickable so phishing links cannot be opened
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(MoneyChipBg)
                                .padding(10.dp)
                        ) {
                            Text(
                                text = tx.smsBody,
                                color = MoneyInk,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    // Quick Verification Actions
                    if (statusEnum != TransactionStatus.CONFIRMED) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = {
                                    onApproveTx(tx, false)
                                    onDismiss()
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = MoneyGreenDeep)
                            ) {
                                Text("Confirm", fontSize = 13.sp, fontWeight = FontWeight.ExtraBold)
                            }
                            if (statusEnum == TransactionStatus.PENDING_REVIEW) {
                                OutlinedButton(
                                    onClick = {
                                        onQuarantineTx(tx)
                                        onDismiss()
                                    },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(12.dp),
                                    border = BorderStroke(1.2.dp, MoneyRed)
                                ) {
                                    Text("Mark Fraud", color = MoneyRed, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold)
                                }
                            }
                        }
                    } else if (tx.source == "SMS") {
                        OutlinedButton(
                            onClick = {
                                onQuarantineTx(tx)
                                onDismiss()
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.2.dp, MoneyRed)
                        ) {
                            Text("Report as Suspicious / Quarantine", color = MoneyRed, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold)
                        }
                    }
                }
            }
        },
        confirmButton = {
            if (editMode) {
                Button(
                    onClick = {
                        val amt = editAmount.toDoubleOrNull()
                        if (amt != null && amt > 0.0) {
                            onUpdateTx(
                                tx.copy(
                                    title = editTitle.trim().ifEmpty { tx.title },
                                    amount = amt,
                                    type = editType,
                                    category = editCategory
                                )
                            )
                            onDismiss()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MoneyGreenDeep)
                ) {
                    Text("Save Changes", fontWeight = FontWeight.Black)
                }
            } else {
                TextButton(onClick = onDismiss) {
                    Text("Close", color = MoneyGreenDeep, fontSize = 16.sp, fontWeight = FontWeight.Black)
                }
            }
        },
        dismissButton = {
            TextButton(
                onClick = {
                    onDeleteTx(tx.id)
                    onDismiss()
                }
            ) {
                Text("Delete", color = MoneyRed, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold)
            }
        }
    )
}

@Composable
private fun EditSavingsGoalDialog(
    initialName: String,
    initialTarget: Double,
    initialSaved: Double,
    initialDate: String,
    onDismiss: () -> Unit,
    onSave: (String, Double, Double, String) -> Unit
) {
    var name by remember { mutableStateOf(initialName) }
    var target by remember { mutableStateOf(initialTarget.toLong().toString()) }
    var saved by remember { mutableStateOf(initialSaved.toLong().toString()) }
    var date by remember { mutableStateOf(initialDate) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MoneyCardWhite,
        title = { Text("Edit Savings Goal", fontSize = 20.sp, fontWeight = FontWeight.Black, color = MoneyInk) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Goal Name", fontWeight = FontWeight.Bold) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = target,
                    onValueChange = { target = it.filter { c -> c.isDigit() || c == '.' } },
                    label = { Text("Target Amount (₹)", fontWeight = FontWeight.Bold) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = saved,
                    onValueChange = { saved = it.filter { c -> c.isDigit() || c == '.' } },
                    label = { Text("Saved So Far (₹, 0 = auto balance)", fontWeight = FontWeight.Bold) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = date,
                    onValueChange = { date = it },
                    label = { Text("Target Date", fontWeight = FontWeight.Bold) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val t = target.toDoubleOrNull() ?: initialTarget
                    val s = saved.toDoubleOrNull() ?: 0.0
                    onSave(name, t, s, date)
                },
                colors = ButtonDefaults.buttonColors(containerColor = MoneyGreenDeep)
            ) {
                Text("Save Goal", fontWeight = FontWeight.Black)
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
private fun NumericAmountDialog(
    title: String,
    label: String,
    initialValue: String,
    onDismiss: () -> Unit,
    onSave: (Double) -> Unit
) {
    var text by remember { mutableStateOf(initialValue) }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MoneyCardWhite,
        title = { Text(title, fontSize = 20.sp, fontWeight = FontWeight.Black, color = MoneyInk) },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it.filter { c -> c.isDigit() || c == '.' || c == '-' } },
                label = { Text(label, fontWeight = FontWeight.Bold) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp)
            )
        },
        confirmButton = {
            Button(
                onClick = {
                    text.toDoubleOrNull()?.let { onSave(it) }
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(containerColor = MoneyGreenDeep)
            ) {
                Text("Save", fontWeight = FontWeight.Black)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = MoneySecondary, fontWeight = FontWeight.Bold)
            }
        }
    )
}
