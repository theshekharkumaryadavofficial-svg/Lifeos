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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.rounded.AccountBalance
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.CardGiftcard
import androidx.compose.material.icons.rounded.DirectionsCar
import androidx.compose.material.icons.rounded.LocalHospital
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material.icons.rounded.Restaurant
import androidx.compose.material.icons.rounded.ShoppingBag
import androidx.compose.material.icons.rounded.SportsEsports
import androidx.compose.material.icons.rounded.Work
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.money.formatInr
import com.example.money.formatInrCompact

// Money Tracker Light Emerald Palette (matching redesigned reference screens)
val MoneyBg = Color(0xFFF6F8FC)
val MoneyCardWhite = Color(0xFFFFFFFF)
val MoneyGreenDeep = Color(0xFF035E3F)
val MoneyGreenMain = Color(0xFF0B8F56)
val MoneyGreenBright = Color(0xFF22C55E)
val MoneySoftGreen = Color(0xFFE7F9EE)
val MoneySoftRed = Color(0xFFFFEFEF)
val MoneySoftAmber = Color(0xFFFFF8E1)
val MoneyAmber = Color(0xFFD97706)
val MoneyGold = Color(0xFFF59E0B)
val MoneyRed = Color(0xFFE53935)
val MoneyInk = Color(0xFF0F172A)
val MoneySecondary = Color(0xFF475569)
val MoneyBorder = Color(0xFFE5ECF5)
val MoneyChipBg = Color(0xFFEEF2F8)
val MoneyBlueAvatar = Color(0xFF2563EB)

// Category Visual Palette (8 Expense + Income categories)
val CatFoodColor = Color(0xFFEF4444)
val CatFoodBg = Color(0xFFFFECEE)
val CatTravelColor = Color(0xFF2563EB)
val CatTravelBg = Color(0xFFEAF3FF)
val CatShoppingColor = Color(0xFF8B5CF6)
val CatShoppingBg = Color(0xFFF3ECFF)
val CatBillsColor = Color(0xFFF59E0B)
val CatBillsBg = Color(0xFFFFF5E0)
val CatStudyColor = Color(0xFF10B981)
val CatStudyBg = Color(0xFFE6F9F0)
val CatHealthColor = Color(0xFFEC4899)
val CatHealthBg = Color(0xFFFDECF4)
val CatEntertainmentColor = Color(0xFF6366F1)
val CatEntertainmentBg = Color(0xFFEEF0FF)
val CatOthersColor = Color(0xFF64748B)
val CatOthersBg = Color(0xFFF1F5F9)
val CatSalaryColor = Color(0xFF059669)
val CatSalaryBg = Color(0xFFE3F8EE)
val CatGiftColor = Color(0xFFEA580C)
val CatGiftBg = Color(0xFFFFEFE5)

data class CategoryVisual(
    val name: String,
    val icon: ImageVector,
    val tint: Color,
    val bg: Color
)

val EXPENSE_CATEGORY_VISUALS: List<CategoryVisual> = listOf(
    CategoryVisual("Food", Icons.Rounded.Restaurant, CatFoodColor, CatFoodBg),
    CategoryVisual("Travel", Icons.Rounded.DirectionsCar, CatTravelColor, CatTravelBg),
    CategoryVisual("Shopping", Icons.Rounded.ShoppingBag, CatShoppingColor, CatShoppingBg),
    CategoryVisual("Bills", Icons.Rounded.Bolt, CatBillsColor, CatBillsBg),
    CategoryVisual("Study", Icons.AutoMirrored.Rounded.MenuBook, CatStudyColor, CatStudyBg),
    CategoryVisual("Health", Icons.Rounded.LocalHospital, CatHealthColor, CatHealthBg),
    CategoryVisual("Entertainment", Icons.Rounded.SportsEsports, CatEntertainmentColor, CatEntertainmentBg),
    CategoryVisual("Others", Icons.Rounded.MoreHoriz, CatOthersColor, CatOthersBg)
)

val INCOME_CATEGORY_VISUALS: List<CategoryVisual> = listOf(
    CategoryVisual("Salary", Icons.Rounded.Work, CatSalaryColor, CatSalaryBg),
    CategoryVisual("Gift", Icons.Rounded.CardGiftcard, CatGiftColor, CatGiftBg),
    CategoryVisual("Income", Icons.Rounded.AccountBalance, MoneyGreenMain, MoneySoftGreen),
    CategoryVisual("Others", Icons.Rounded.Payments, CatTravelColor, CatTravelBg)
)

fun categoryVisualFor(category: String): CategoryVisual =
    (EXPENSE_CATEGORY_VISUALS + INCOME_CATEGORY_VISUALS)
        .firstOrNull { it.name.equals(category, ignoreCase = true) }
        ?: CategoryVisual(category.ifBlank { "Others" }, Icons.Rounded.MoreHoriz, CatOthersColor, CatOthersBg)

fun categoryColorFor(category: String): Color = when (category) {
    "Food" -> Color(0xFF15803D)
    "Study" -> Color(0xFFF59E0B)
    "Travel" -> Color(0xFF2563EB)
    "Shopping" -> Color(0xFF8B5CF6)
    "Bills" -> Color(0xFFEA580C)
    "Health" -> Color(0xFFEC4899)
    "Entertainment" -> Color(0xFF6366F1)
    "Salary", "Income" -> MoneyGreenMain
    "Gift" -> CatGiftColor
    else -> Color(0xFF7C3AED)
}

@Composable
fun BalanceRisingBarsOverlay(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        // 4 rising green emerald bars matching Panel 2 Total Balance card
        val barWidth = 10.dp.toPx()
        val gap = 7.dp.toPx()
        val heights = listOf(h * 0.28f, h * 0.42f, h * 0.56f, h * 0.72f)
        val totalBarsW = barWidth * heights.size + gap * (heights.size - 1)
        val startX = (w - totalBarsW - 6.dp.toPx()).coerceAtLeast(4.dp.toPx())
        val baseY = h * 0.92f

        heights.forEachIndexed { idx, bh ->
            val x = startX + idx * (barWidth + gap)
            drawRoundRect(
                brush = Brush.verticalGradient(
                    listOf(Color(0xFF4ADE80).copy(alpha = 0.78f), Color(0xFF15803D).copy(alpha = 0.35f))
                ),
                topLeft = Offset(x, baseY - bh),
                size = Size(barWidth, bh),
                cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
            )
        }

        // Upward zig-zag growth arrow floating above bars
        val arrowPath = Path().apply {
            moveTo(startX - 4.dp.toPx(), h * 0.56f)
            lineTo(startX + totalBarsW * 0.36f, h * 0.34f)
            lineTo(startX + totalBarsW * 0.54f, h * 0.44f)
            lineTo(startX + totalBarsW, h * 0.14f)
        }
        drawPath(
            path = arrowPath,
            color = Color(0xFF6EE7B7),
            style = Stroke(width = 3.5.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
        )
        // Arrow head
        val tipX = startX + totalBarsW
        val tipY = h * 0.14f
        val head = Path().apply {
            moveTo(tipX - 11.dp.toPx(), tipY + 1.dp.toPx())
            lineTo(tipX + 1.dp.toPx(), tipY - 1.dp.toPx())
            lineTo(tipX - 1.dp.toPx(), tipY + 11.dp.toPx())
        }
        drawPath(
            path = head,
            color = Color(0xFF6EE7B7),
            style = Stroke(width = 3.5.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
        )
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
    centerLabel: String = "Spent",
    modifier: Modifier = Modifier
) {
    var entered by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { entered = true }
    val sweepAnim by animateFloatAsState(
        targetValue = if (entered) 1f else 0f,
        animationSpec = tween(750, easing = FastOutSlowInEasing),
        label = "donut_anim"
    )

    Box(
        modifier = modifier.size(162.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeW = 22.dp.toPx()
            val pad = strokeW / 2f + 4.dp.toPx()
            val arcSize = Size(size.width - pad * 2f, size.height - pad * 2f)

            if (totalExpense <= 0.0 || slices.none { it.amount > 0.0 }) {
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
                val gapAngle = if (nonZero.size > 1) 3.2f else 0f
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
                fontSize = 23.sp,
                fontWeight = FontWeight.Black
            )
            Text(
                text = centerLabel,
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
    xLabels: List<String> = listOf("1-7", "8-14", "15-21", "22-30"),
    modifier: Modifier = Modifier
) {
    var entered by remember { mutableStateOf(false) }
    LaunchedEffect(points) { entered = true }
    val anim by animateFloatAsState(
        targetValue = if (entered) 1f else 0f,
        animationSpec = tween(700, easing = FastOutSlowInEasing),
        label = "trend_anim"
    )

    val safePoints = if (points.size >= 2) points else listOf(0.0, 0.0, 0.0, 0.0)
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
                        .background(Color(0xFF0A2E25))
                        .padding(horizontal = 12.dp, vertical = 5.dp)
                ) {
                    Text(
                        text = formatInr(peakAmount),
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Black
                    )
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(142.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier
                    .width(46.dp)
                    .height(134.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = formatInrCompact(maxVal),
                    color = MoneySecondary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = formatInrCompact(maxVal * 0.5),
                    color = MoneySecondary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "0",
                    color = MoneySecondary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Canvas(
                modifier = Modifier
                    .weight(1f)
                    .height(134.dp)
            ) {
                val w = size.width
                val h = size.height
                val topPad = 14.dp.toPx()
                val bottomPad = 8.dp.toPx()
                val usableH = h - topPad - bottomPad

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
                    style = Stroke(width = 3.2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                )

                val peakPt = coords.getOrNull(peakIndex)
                if (peakPt != null && peakAmount > 0.0) {
                    drawCircle(
                        color = MoneyGreenBright.copy(alpha = 0.25f),
                        radius = 11.dp.toPx(),
                        center = peakPt
                    )
                    drawCircle(
                        color = Color.White,
                        radius = 6.dp.toPx(),
                        center = peakPt
                    )
                    drawCircle(
                        color = MoneyGreenDeep,
                        radius = 4.dp.toPx(),
                        center = peakPt
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 46.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            xLabels.forEach { label ->
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

/**
 * Panel 1 Hero Artwork: Glass Savings Jar filled with Golden ₹ Coins,
 * Twin Sprouting Green Leaves, and Rising Glowing Green Growth Arrow.
 */
@Composable
fun MoneyWelcomeHeroArtwork(modifier: Modifier = Modifier) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val cx = w * 0.48f

            // Radiant mint-gold halo behind jar
            drawCircle(
                brush = Brush.radialGradient(
                    listOf(Color(0xFFFDE68A).copy(alpha = 0.55f), Color(0xFF86EFAC).copy(alpha = 0.22f), Color.Transparent)
                ),
                radius = size.minDimension * 0.52f,
                center = Offset(cx, h * 0.56f)
            )

            // Glowing upward green arrow behind/right of jar
            val arrowShaft = Path().apply {
                moveTo(w * 0.54f, h * 0.64f)
                lineTo(w * 0.80f, h * 0.28f)
            }
            drawPath(
                path = arrowShaft,
                brush = Brush.linearGradient(listOf(MoneyGreenMain, Color(0xFF4ADE80))),
                style = Stroke(width = 18.dp.toPx(), cap = StrokeCap.Round)
            )
            val arrowHead = Path().apply {
                moveTo(w * 0.87f, h * 0.19f)
                lineTo(w * 0.66f, h * 0.24f)
                lineTo(w * 0.84f, h * 0.41f)
                close()
            }
            drawPath(
                path = arrowHead,
                brush = Brush.linearGradient(listOf(Color(0xFF86EFAC), MoneyGreenMain))
            )

            // Twin sprouting green leaves above the jar lid
            val stemBottom = Offset(cx, h * 0.24f)
            drawLine(
                color = MoneyGreenDeep,
                start = stemBottom,
                end = Offset(cx, h * 0.14f),
                strokeWidth = 4.dp.toPx(),
                cap = StrokeCap.Round
            )
            val leftLeaf = Path().apply {
                moveTo(cx, h * 0.17f)
                cubicTo(cx - 26.dp.toPx(), h * 0.05f, cx - 44.dp.toPx(), h * 0.07f, cx - 36.dp.toPx(), h * 0.19f)
                cubicTo(cx - 24.dp.toPx(), h * 0.23f, cx - 8.dp.toPx(), h * 0.20f, cx, h * 0.17f)
                close()
            }
            val rightLeaf = Path().apply {
                moveTo(cx, h * 0.16f)
                cubicTo(cx + 26.dp.toPx(), h * 0.03f, cx + 48.dp.toPx(), h * 0.07f, cx + 38.dp.toPx(), h * 0.20f)
                cubicTo(cx + 24.dp.toPx(), h * 0.23f, cx + 8.dp.toPx(), h * 0.19f, cx, h * 0.16f)
                close()
            }
            drawPath(
                path = leftLeaf,
                brush = Brush.linearGradient(listOf(Color(0xFF86EFAC), MoneyGreenMain))
            )
            drawPath(
                path = rightLeaf,
                brush = Brush.linearGradient(listOf(Color(0xFFBBF7D0), MoneyGreenDeep))
            )

            // Jar body dimensions
            val jarW = w * 0.44f
            val jarH = h * 0.56f
            val jarLeft = cx - jarW / 2f
            val jarTop = h * 0.30f

            // Jar warm golden lid
            drawRoundRect(
                brush = Brush.horizontalGradient(
                    listOf(Color(0xFFB45309), Color(0xFFFBBF24), Color(0xFFD97706))
                ),
                topLeft = Offset(cx - jarW * 0.38f, jarTop - 16.dp.toPx()),
                size = Size(jarW * 0.76f, 18.dp.toPx()),
                cornerRadius = CornerRadius(8.dp.toPx(), 8.dp.toPx())
            )

            // Glass Jar body
            drawRoundRect(
                brush = Brush.verticalGradient(
                    listOf(
                        Color(0xFFFFFFFF).copy(alpha = 0.82f),
                        Color(0xFFFEF3C7).copy(alpha = 0.90f),
                        Color(0xFFFDE68A).copy(alpha = 0.95f)
                    )
                ),
                topLeft = Offset(jarLeft, jarTop),
                size = Size(jarW, jarH),
                cornerRadius = CornerRadius(28.dp.toPx(), 28.dp.toPx())
            )

            // Stacked golden coins inside the jar
            val coinRows = listOf(
                Offset(cx - jarW * 0.22f, jarTop + jarH * 0.78f),
                Offset(cx + jarW * 0.20f, jarTop + jarH * 0.80f),
                Offset(cx - jarW * 0.25f, jarTop + jarH * 0.60f),
                Offset(cx + jarW * 0.24f, jarTop + jarH * 0.62f),
                Offset(cx - jarW * 0.16f, jarTop + jarH * 0.38f),
                Offset(cx + jarW * 0.18f, jarTop + jarH * 0.40f),
                Offset(cx, jarTop + jarH * 0.26f)
            )
            coinRows.forEach { pos ->
                drawCircle(
                    brush = Brush.radialGradient(listOf(Color(0xFFFEF08A), Color(0xFFF59E0B), Color(0xFFB45309))),
                    radius = 15.dp.toPx(),
                    center = pos
                )
            }

            // Center large glowing ₹ Gold Medallion on jar
            val medallionCenter = Offset(cx, jarTop + jarH * 0.54f)
            drawCircle(
                brush = Brush.radialGradient(
                    listOf(Color(0xFFFEF9C3), Color(0xFFF59E0B), Color(0xFFB45309))
                ),
                radius = 35.dp.toPx(),
                center = medallionCenter
            )
            drawCircle(
                color = Color.White.copy(alpha = 0.85f),
                radius = 29.dp.toPx(),
                center = medallionCenter,
                style = Stroke(width = 2.5.dp.toPx())
            )

            // Glass jar crisp rim highlight
            drawRoundRect(
                color = Color.White.copy(alpha = 0.92f),
                topLeft = Offset(jarLeft, jarTop),
                size = Size(jarW, jarH),
                cornerRadius = CornerRadius(28.dp.toPx(), 28.dp.toPx()),
                style = Stroke(width = 3.dp.toPx())
            )

            // Floating mini gold coins around jar
            val floatingCoins = listOf(
                Offset(w * 0.16f, h * 0.34f) to 11.dp.toPx(),
                Offset(w * 0.84f, h * 0.56f) to 13.dp.toPx(),
                Offset(w * 0.78f, h * 0.78f) to 15.dp.toPx()
            )
            floatingCoins.forEach { (pt, rad) ->
                drawCircle(
                    brush = Brush.radialGradient(listOf(Color(0xFFFEF08A), Color(0xFFF59E0B))),
                    radius = rad,
                    center = pt
                )
            }
        }

        Text(
            text = "₹",
            color = Color(0xFF78350F),
            fontSize = 38.sp,
            fontWeight = FontWeight.Black,
            modifier = Modifier.padding(top = 34.dp, end = 6.dp)
        )
    }
}
