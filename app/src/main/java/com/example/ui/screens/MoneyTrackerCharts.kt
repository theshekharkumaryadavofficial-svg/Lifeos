package com.example.ui.screens

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.money.formatInr
import com.example.money.formatInrCompact

// Money Tracker Light Palette (faithfully reproducing the reference screens)
val MoneyBg = Color(0xFFF5F7FB)
val MoneyCardWhite = Color(0xFFFFFFFF)
val MoneyGreenDeep = Color(0xFF047843)
val MoneyGreenMain = Color(0xFF0EA255)
val MoneyGreenBright = Color(0xFF1CD172)
val MoneySoftGreen = Color(0xFFE5F9ED)
val MoneySoftRed = Color(0xFFFFEDED)
val MoneyRed = Color(0xFFE53935)
val MoneyInk = Color(0xFF0F172A)
val MoneySecondary = Color(0xFF475569)
val MoneyBorder = Color(0xFFE5ECF5)
val MoneyChipBg = Color(0xFFEEF2F8)
val MoneyBlueAvatar = Color(0xFF2B6CEE)

val CatFoodColor = Color(0xFFF59E0B)
val CatShoppingColor = Color(0xFF3B82F6)
val CatBillsColor = Color(0xFF8B5CF6)
val CatTravelColor = Color(0xFF10B981)
val CatOthersColor = Color(0xFF94A3B8)

fun categoryColorFor(category: String): Color = when (category) {
    "Food" -> CatFoodColor
    "Shopping" -> CatShoppingColor
    "Bills" -> CatBillsColor
    "Travel" -> CatTravelColor
    "Salary", "Income" -> MoneyGreenMain
    else -> CatOthersColor
}

@Composable
fun BalanceWaveOverlay(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val wavePath = Path().apply {
            moveTo(0f, h * 0.88f)
            cubicTo(w * 0.18f, h * 0.62f, w * 0.30f, h * 0.82f, w * 0.48f, h * 0.48f)
            cubicTo(w * 0.64f, h * 0.22f, w * 0.78f, h * 0.60f, w, h * 0.28f)
        }
        val fillPath = Path().apply {
            addPath(wavePath)
            lineTo(w, h)
            lineTo(0f, h)
            close()
        }
        drawPath(
            path = fillPath,
            brush = Brush.verticalGradient(
                listOf(Color.White.copy(alpha = 0.22f), Color.Transparent)
            )
        )
        drawPath(
            path = wavePath,
            color = Color(0xFF6EF5B1).copy(alpha = 0.72f),
            style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
        )
    }
}

@Composable
fun MonthlyDualBarChart(
    incomeBuckets: List<Double>,
    expenseBuckets: List<Double>,
    modifier: Modifier = Modifier
) {
    var entered by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { entered = true }
    val anim by animateFloatAsState(
        targetValue = if (entered) 1f else 0f,
        animationSpec = tween(750, easing = FastOutSlowInEasing),
        label = "monthly_bars_anim"
    )

    val count = maxOf(incomeBuckets.size, expenseBuckets.size, 10)
    val maxVal = (incomeBuckets + expenseBuckets).maxOrNull()?.coerceAtLeast(1.0) ?: 1.0
    val hasAnyData = (incomeBuckets + expenseBuckets).any { it > 0.0 }

    Column(modifier = modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(118.dp)
                .padding(top = 6.dp)
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height
                val slotW = w / count
                val barW = (slotW * 0.34f).coerceIn(5.dp.toPx(), 13.dp.toPx())
                val gap = 2.dp.toPx()

                // Soft baseline
                drawLine(
                    color = MoneyBorder,
                    start = Offset(0f, h),
                    end = Offset(w, h),
                    strokeWidth = 1.dp.toPx()
                )

                for (i in 0 until count) {
                    val inc = incomeBuckets.getOrElse(i) { 0.0 }
                    val exp = expenseBuckets.getOrElse(i) { 0.0 }
                    val incRatio = if (hasAnyData) (inc / maxVal).toFloat().coerceIn(0f, 1f) else 0.08f
                    val expRatio = if (hasAnyData) (exp / maxVal).toFloat().coerceIn(0f, 1f) else 0.06f

                    val incH = (h * 0.90f * incRatio * anim).coerceAtLeast(if (inc > 0) 10.dp.toPx() else 5.dp.toPx())
                    val expH = (h * 0.90f * expRatio * anim).coerceAtLeast(if (exp > 0) 10.dp.toPx() else 5.dp.toPx())

                    val groupCenter = slotW * i + slotW / 2f
                    val leftX = groupCenter - barW - gap / 2f
                    val rightX = groupCenter + gap / 2f

                    drawRoundRect(
                        brush = Brush.verticalGradient(
                            listOf(
                                if (inc > 0) MoneyGreenBright else Color(0xFFD5E9DE),
                                if (inc > 0) MoneyGreenMain.copy(alpha = 0.72f) else Color(0xFFE8F3ED)
                            )
                        ),
                        topLeft = Offset(leftX, h - incH),
                        size = Size(barW, incH),
                        cornerRadius = CornerRadius(barW / 2f, barW / 2f)
                    )
                    drawRoundRect(
                        brush = Brush.verticalGradient(
                            listOf(
                                if (exp > 0) Color(0xFFFCA5A5) else Color(0xFFF3E2E4),
                                if (exp > 0) Color(0xFFF87171).copy(alpha = 0.78f) else Color(0xFFF9ECEE)
                            )
                        ),
                        topLeft = Offset(rightX, h - expH),
                        size = Size(barW, expH),
                        cornerRadius = CornerRadius(barW / 2f, barW / 2f)
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            listOf("1", "7", "14", "21", "30").forEach { day ->
                Text(
                    text = day,
                    color = MoneySecondary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

data class CategorySlice(
    val name: String,
    val amount: Double,
    val percent: Int,
    val color: Color
)

@Composable
fun AnalyticsDonutChart(
    slices: List<CategorySlice>,
    totalExpense: Double,
    modifier: Modifier = Modifier
) {
    var entered by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { entered = true }
    val sweepAnim by animateFloatAsState(
        targetValue = if (entered) 1f else 0f,
        animationSpec = tween(800, easing = FastOutSlowInEasing),
        label = "donut_anim"
    )

    Box(
        modifier = modifier.size(168.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeW = 24.dp.toPx()
            val pad = strokeW / 2f + 4.dp.toPx()
            val arcSize = Size(size.width - pad * 2f, size.height - pad * 2f)

            if (totalExpense <= 0.0 || slices.isEmpty()) {
                drawArc(
                    color = MoneyChipBg,
                    startAngle = 0f,
                    sweepAngle = 360f,
                    useCenter = false,
                    topLeft = Offset(pad, pad),
                    size = arcSize,
                    style = Stroke(width = strokeW)
                )
            } else {
                var startAngle = -90f
                val nonZero = slices.filter { it.amount > 0.0 }
                val gapAngle = if (nonZero.size > 1) 3.5f else 0f
                for (slice in nonZero) {
                    val rawSweep = ((slice.amount / totalExpense) * 360.0).toFloat()
                    val drawnSweep = ((rawSweep - gapAngle).coerceAtLeast(4f)) * sweepAnim
                    drawArc(
                        color = slice.color,
                        startAngle = startAngle,
                        sweepAngle = drawnSweep,
                        useCenter = false,
                        topLeft = Offset(pad, pad),
                        size = arcSize,
                        style = Stroke(width = strokeW, cap = StrokeCap.Butt)
                    )
                    startAngle += rawSweep
                }
            }
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = formatInrCompact(totalExpense),
                color = MoneyInk,
                fontSize = 22.sp,
                fontWeight = FontWeight.Black
            )
            Text(
                text = "Expenses",
                color = MoneySecondary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun SpendingTrendLineChart(
    points: List<Double>,
    modifier: Modifier = Modifier
) {
    var entered by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { entered = true }
    val anim by animateFloatAsState(
        targetValue = if (entered) 1f else 0f,
        animationSpec = tween(750, easing = FastOutSlowInEasing),
        label = "trend_anim"
    )

    val safePoints = if (points.size >= 2) points else listOf(0.0, 0.0, 0.0, 0.0, 0.0, 0.0)
    val maxVal = (safePoints.maxOrNull() ?: 0.0).coerceAtLeast(100.0)
    val peakIndex = safePoints.indices.maxByOrNull { safePoints[it] } ?: 0
    val peakAmount = safePoints.getOrElse(peakIndex) { 0.0 }

    Column(modifier = modifier.fillMaxWidth()) {
        if (peakAmount > 0.0) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(MoneyInk)
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "Peak: ${formatInr(peakAmount)}",
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(132.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Y-Axis Labels
            Column(
                modifier = Modifier
                    .width(44.dp)
                    .height(126.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = formatInrCompact(maxVal),
                    color = MoneySecondary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = formatInrCompact(maxVal / 2.0),
                    color = MoneySecondary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "₹0",
                    color = MoneySecondary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Canvas(
                modifier = Modifier
                    .weight(1f)
                    .height(126.dp)
            ) {
                val w = size.width
                val h = size.height
                val topPad = 12.dp.toPx()
                val bottomPad = 8.dp.toPx()
                val usableH = h - topPad - bottomPad

                // Horizontal subtle grid lines
                for (g in 0..2) {
                    val y = topPad + (usableH * g / 2f)
                    drawLine(
                        color = MoneyBorder,
                        start = Offset(0f, y),
                        end = Offset(w, y),
                        strokeWidth = 1.dp.toPx()
                    )
                }

                val stepX = w / (safePoints.size - 1).coerceAtLeast(1)
                val coords = safePoints.mapIndexed { idx, value ->
                    val ratio = ((value / maxVal).toFloat().coerceIn(0f, 1f)) * anim
                    Offset(
                        x = idx * stepX,
                        y = topPad + usableH * (1f - ratio)
                    )
                }

                val linePath = Path().apply {
                    moveTo(coords.first().x, coords.first().y)
                    for (i in 1 until coords.size) {
                        val prev = coords[i - 1]
                        val curr = coords[i]
                        val midX = (prev.x + curr.x) / 2f
                        cubicTo(midX, prev.y, midX, curr.y, curr.x, curr.y)
                    }
                }
                val areaPath = Path().apply {
                    addPath(linePath)
                    lineTo(coords.last().x, h - bottomPad)
                    lineTo(coords.first().x, h - bottomPad)
                    close()
                }

                drawPath(
                    path = areaPath,
                    brush = Brush.verticalGradient(
                        listOf(MoneyGreenBright.copy(alpha = 0.28f), Color.Transparent)
                    )
                )
                drawPath(
                    path = linePath,
                    color = MoneyGreenMain,
                    style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                )

                coords.forEachIndexed { idx, pt ->
                    drawCircle(
                        color = Color.White,
                        radius = if (idx == peakIndex && peakAmount > 0) 6.dp.toPx() else 4.dp.toPx(),
                        center = pt
                    )
                    drawCircle(
                        color = MoneyGreenMain,
                        radius = if (idx == peakIndex && peakAmount > 0) 4.dp.toPx() else 2.6.dp.toPx(),
                        center = pt
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 44.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            listOf("1", "7", "14", "21", "30").forEach { label ->
                Text(
                    text = label,
                    color = MoneySecondary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun MoneyWelcomeHeroArtwork(modifier: Modifier = Modifier) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

            // Soft mint ambient glow
            drawCircle(
                brush = Brush.radialGradient(
                    listOf(MoneyGreenBright.copy(alpha = 0.28f), Color.Transparent)
                ),
                radius = size.minDimension * 0.52f,
                center = Offset(w * 0.52f, h * 0.52f)
            )

            // Green rising bars on right
            val barBase = h * 0.80f
            val barWidths = 16.dp.toPx()
            val barHeights = listOf(h * 0.18f, h * 0.28f, h * 0.38f)
            barHeights.forEachIndexed { idx, bh ->
                drawRoundRect(
                    brush = Brush.verticalGradient(listOf(MoneyGreenBright, MoneyGreenDeep)),
                    topLeft = Offset(w * 0.64f + idx * 20.dp.toPx(), barBase - bh),
                    size = Size(barWidths, bh),
                    cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx())
                )
            }

            // Rising green growth arrow
            val arrowPath = Path().apply {
                moveTo(w * 0.50f, h * 0.42f)
                lineTo(w * 0.82f, h * 0.16f)
            }
            drawPath(
                path = arrowPath,
                color = MoneyGreenBright,
                style = Stroke(width = 10.dp.toPx(), cap = StrokeCap.Round)
            )
            val headPath = Path().apply {
                moveTo(w * 0.85f, h * 0.14f)
                lineTo(w * 0.70f, h * 0.14f)
                lineTo(w * 0.83f, h * 0.29f)
                close()
            }
            drawPath(path = headPath, color = MoneyGreenMain)

            // Deep navy leather wallet
            val walletLeft = w * 0.18f
            val walletTop = h * 0.28f
            val walletW = w * 0.46f
            val walletH = h * 0.46f
            drawRoundRect(
                brush = Brush.linearGradient(listOf(Color(0xFF283E6B), Color(0xFF14213D))),
                topLeft = Offset(walletLeft, walletTop),
                size = Size(walletW, walletH),
                cornerRadius = CornerRadius(20.dp.toPx(), 20.dp.toPx())
            )
            // Green cash bill peeking from wallet
            drawRoundRect(
                color = MoneyGreenBright,
                topLeft = Offset(walletLeft + 10.dp.toPx(), walletTop - 10.dp.toPx()),
                size = Size(walletW * 0.72f, 18.dp.toPx()),
                cornerRadius = CornerRadius(8.dp.toPx(), 8.dp.toPx())
            )
            // Wallet strap clasp
            drawRoundRect(
                color = Color(0xFF334E85),
                topLeft = Offset(walletLeft + walletW - 22.dp.toPx(), walletTop + walletH * 0.36f),
                size = Size(28.dp.toPx(), 22.dp.toPx()),
                cornerRadius = CornerRadius(8.dp.toPx(), 8.dp.toPx())
            )
            drawCircle(
                color = Color(0xFFFBBF24),
                radius = 5.dp.toPx(),
                center = Offset(walletLeft + walletW - 6.dp.toPx(), walletTop + walletH * 0.36f + 11.dp.toPx())
            )

            // Left small coin stack
            for (c in 0..2) {
                drawRoundRect(
                    brush = Brush.horizontalGradient(listOf(Color(0xFFF59E0B), Color(0xFFFDE047), Color(0xFFD97706))),
                    topLeft = Offset(w * 0.08f, h * 0.74f - c * 10.dp.toPx()),
                    size = Size(44.dp.toPx(), 10.dp.toPx()),
                    cornerRadius = CornerRadius(5.dp.toPx(), 5.dp.toPx())
                )
            }
            // Right taller coin stack
            for (c in 0..4) {
                drawRoundRect(
                    brush = Brush.horizontalGradient(listOf(Color(0xFFF59E0B), Color(0xFFFDE047), Color(0xFFD97706))),
                    topLeft = Offset(w * 0.62f, h * 0.76f - c * 10.dp.toPx()),
                    size = Size(50.dp.toPx(), 10.dp.toPx()),
                    cornerRadius = CornerRadius(5.dp.toPx(), 5.dp.toPx())
                )
            }

            // Big shiny gold Rupee coin in foreground
            val coinCenter = Offset(w * 0.42f, h * 0.68f)
            drawCircle(
                brush = Brush.radialGradient(
                    listOf(Color(0xFFFEF08A), Color(0xFFF59E0B), Color(0xFFB45309))
                ),
                radius = 36.dp.toPx(),
                center = coinCenter
            )
            drawCircle(
                color = Color(0xFFFFFBEB).copy(alpha = 0.7f),
                radius = 29.dp.toPx(),
                center = coinCenter,
                style = Stroke(width = 2.5.dp.toPx())
            )
        }
        // Bold ₹ symbol centered on gold coin
        Text(
            text = "₹",
            color = Color(0xFF78350F),
            fontSize = 36.sp,
            fontWeight = FontWeight.Black,
            modifier = Modifier.padding(top = 80.dp, end = 34.dp)
        )
    }
}

@Composable
fun BudgetTargetArtwork(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(74.dp)) {
        val c = Offset(size.width * 0.5f, size.height * 0.54f)
        val r = size.minDimension * 0.44f
        drawCircle(color = Color(0xFFEF4444), radius = r, center = c)
        drawCircle(color = Color.White, radius = r * 0.74f, center = c)
        drawCircle(color = Color(0xFFEF4444), radius = r * 0.50f, center = c)
        drawCircle(color = Color.White, radius = r * 0.28f, center = c)
        drawCircle(color = Color(0xFFDC2626), radius = r * 0.14f, center = c)

        // Dart arrow hitting bullseye
        drawLine(
            color = Color(0xFF1E293B),
            start = c,
            end = Offset(size.width * 0.88f, size.height * 0.14f),
            strokeWidth = 3.dp.toPx(),
            cap = StrokeCap.Round
        )
        drawCircle(
            color = MoneyGreenMain,
            radius = 7.dp.toPx(),
            center = Offset(size.width * 0.86f, size.height * 0.16f)
        )
    }
}
