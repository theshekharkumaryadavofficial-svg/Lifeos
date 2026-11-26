package com.example.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.AmberGold
import com.example.ui.theme.BrightGold
import com.example.ui.theme.CrimsonDeep
import com.example.ui.theme.CrimsonGlow
import com.example.ui.theme.CrimsonPrimary
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.EmeraldHealth
import com.example.ui.theme.GlassBorder
import com.example.ui.theme.GlassSurface
import com.example.ui.theme.MysticPurple
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.TextOnDark
import com.example.ui.theme.TextOnDarkMuted

@Composable
fun LifeGlassCard(
    modifier: Modifier = Modifier,
    borderColor: Color = GlassBorder,
    backgroundColor: Color = GlassSurface,
    cornerRadius: Dp = 20.dp,
    contentPadding: Dp = 16.dp,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val shape = RoundedCornerShape(cornerRadius)
    Surface(
        modifier = modifier
            .clip(shape)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        shape = shape,
        color = backgroundColor,
        border = BorderStroke(1.dp, borderColor),
        shadowElevation = 6.dp
    ) {
        Column(
            modifier = Modifier.padding(contentPadding),
            content = content
        )
    }
}

@Composable
fun CrimsonGlowButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    trailingIcon: ImageVector? = null,
    loading: Boolean = false,
    enabled: Boolean = true,
    testTagName: String = "primary_action_button",
    gradientColors: List<Color> = listOf(CrimsonGlow, CrimsonPrimary, CrimsonDeep)
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed && enabled && !loading) 0.97f else 1f,
        animationSpec = spring(stiffness = 600f),
        label = "button_scale"
    )
    val shape = RoundedCornerShape(28.dp)

    Box(
        modifier = modifier
            .scale(scale)
            .fillMaxWidth()
            .height(56.dp)
            .shadow(
                elevation = if (pressed) 5.dp else 14.dp,
                shape = shape,
                ambientColor = gradientColors.first(),
                spotColor = gradientColors.first()
            )
            .clip(shape)
            .background(
                brush = Brush.horizontalGradient(
                    if (enabled) gradientColors else listOf(Color(0xFF3E3A4D), Color(0xFF2D2A3A))
                )
            )
            .border(
                width = 1.8.dp,
                brush = Brush.verticalGradient(
                    listOf(Color.White.copy(alpha = 0.65f), Color.White.copy(alpha = 0.14f))
                ),
                shape = shape
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled && !loading,
                onClick = onClick
            )
            .testTag(testTagName),
        contentAlignment = Alignment.Center
    ) {
        if (loading) {
            CircularProgressIndicator(
                modifier = Modifier.size(22.dp),
                color = TextOnDark,
                strokeWidth = 2.8.dp
            )
        } else {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.padding(horizontal = 20.dp)
            ) {
                if (icon != null) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = TextOnDark,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text(
                    text = text,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Black,
                        fontSize = 16.5.sp,
                        letterSpacing = 0.3.sp,
                        color = TextOnDark
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (trailingIcon != null) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        imageVector = trailingIcon,
                        contentDescription = null,
                        tint = TextOnDark,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun GlowingProgressRing(
    progressFraction: Float,
    centerText: String,
    subtitleText: String? = null,
    modifier: Modifier = Modifier,
    size: Dp = 112.dp,
    strokeWidth: Dp = 11.dp,
    ringColors: List<Color> = listOf(EmeraldHealth, BrightGold, CrimsonGlow, EmeraldHealth)
) {
    var hasEntered by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { hasEntered = true }
    val animatedProgress by animateFloatAsState(
        targetValue = if (hasEntered) progressFraction.coerceIn(0f, 1f) else 0f,
        animationSpec = tween(durationMillis = 850, easing = FastOutSlowInEasing),
        label = "ring_progress"
    )

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokePx = strokeWidth.toPx()
            val inset = strokePx / 2f + 4.dp.toPx()
            val arcSize = Size(this.size.width - inset * 2, this.size.height - inset * 2)
            val topLeft = Offset(inset, inset)

            // Outer ambient halo
            drawArc(
                color = ringColors.first().copy(alpha = 0.16f),
                startAngle = -90f,
                sweepAngle = 360f * animatedProgress,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokePx * 1.65f, cap = StrokeCap.Round)
            )
            // Track background
            drawArc(
                color = Color.White.copy(alpha = 0.12f),
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokePx, cap = StrokeCap.Round)
            )
            // Main glowing progress sweep
            if (animatedProgress > 0.005f) {
                drawArc(
                    brush = Brush.sweepGradient(ringColors),
                    startAngle = -90f,
                    sweepAngle = 360f * animatedProgress,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = strokePx, cap = StrokeCap.Round)
                )
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = centerText,
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.ExtraBold,
                    color = TextOnDark
                ),
                maxLines = 1
            )
            if (subtitleText != null) {
                Text(
                    text = subtitleText,
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = TextOnDarkMuted,
                        fontSize = 10.sp
                    ),
                    textAlign = TextAlign.Center,
                    maxLines = 2
                )
            }
        }
    }
}

@Composable
fun GlowingProgressBar(
    progressFraction: Float,
    accentKey: String,
    modifier: Modifier = Modifier
) {
    val animated by animateFloatAsState(
        targetValue = progressFraction.coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 650, easing = FastOutSlowInEasing),
        label = "bar_progress"
    )
    val colors = when (accentKey) {
        "GOLD" -> listOf(AmberGold, BrightGold)
        "CYAN" -> listOf(Color(0xFF0284C7), ElectricCyan)
        "PURPLE" -> listOf(Color(0xFF6D28D9), MysticPurple)
        "GREEN" -> listOf(Color(0xFF059669), EmeraldHealth)
        else -> listOf(CrimsonDeep, CrimsonGlow)
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(9.dp)
            .clip(CircleShape)
            .background(Color.White.copy(alpha = 0.12f))
    ) {
        if (animated > 0.01f) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(animated)
                    .height(9.dp)
                    .clip(CircleShape)
                    .background(Brush.horizontalGradient(colors))
            )
        }
    }
}

@Composable
fun HeroBannerCard(
    @DrawableRes imageRes: Int,
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    height: Dp = 132.dp,
    badge: String? = null,
    contentOverlay: (@Composable BoxScope.() -> Unit)? = null
) {
    val shape = RoundedCornerShape(20.dp)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .clip(shape)
            .border(1.dp, Color.White.copy(alpha = 0.22f), shape)
    ) {
        Image(
            painter = painterResource(id = imageRes),
            contentDescription = title,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            ObsidianBg.copy(alpha = 0.86f),
                            ObsidianBg.copy(alpha = 0.45f),
                            Color.Transparent
                        )
                    )
                )
        )
        Column(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(horizontal = 18.dp, vertical = 14.dp)
                .fillMaxWidth(0.72f),
            verticalArrangement = Arrangement.Center
        ) {
            if (badge != null) {
                Text(
                    text = badge,
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = BrightGold,
                        fontWeight = FontWeight.ExtraBold
                    )
                )
                Spacer(modifier = Modifier.height(4.dp))
            }
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall.copy(
                    color = TextOnDark,
                    fontWeight = FontWeight.ExtraBold
                ),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall.copy(
                    color = TextOnDarkMuted
                ),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
        contentOverlay?.invoke(this)
    }
}

data class BottomNavDestination(
    val route: String,
    val label: String,
    val icon: ImageVector,
    val activeIcon: ImageVector
)

@Composable
fun LifeBottomBar(
    destinations: List<BottomNavDestination>,
    currentRoute: String,
    onSelect: (String) -> Unit
) {
    Surface(
        color = Color(0xF011101C),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.12f)),
        shadowElevation = 16.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 6.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            destinations.forEach { item ->
                val selected = currentRoute == item.route
                Column(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .clickable { onSelect(item.route) }
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                        .testTag("nav_${item.route}"),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(
                                if (selected) {
                                    Brush.radialGradient(
                                        listOf(
                                            CrimsonGlow.copy(alpha = 0.95f),
                                            CrimsonDeep.copy(alpha = 0.75f)
                                        )
                                    )
                                } else {
                                    Brush.radialGradient(listOf(Color.Transparent, Color.Transparent))
                                }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (selected) item.activeIcon else item.icon,
                            contentDescription = item.label,
                            tint = if (selected) TextOnDark else TextOnDarkMuted,
                            modifier = Modifier.size(21.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = item.label,
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = if (selected) TextOnDark else TextOnDarkMuted,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 11.sp
                        ),
                        maxLines = 1
                    )
                }
            }
        }
    }
}

@Composable
fun ErrorRetryCard(
    message: String,
    onRetry: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    LifeGlassCard(
        modifier = modifier.fillMaxWidth(),
        borderColor = CrimsonPrimary.copy(alpha = 0.55f)
    ) {
        Text(
            text = "Something went wrong",
            style = MaterialTheme.typography.titleMedium.copy(color = CrimsonGlow)
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.bodySmall.copy(color = TextOnDarkMuted)
        )
        if (onRetry != null) {
            Spacer(modifier = Modifier.height(12.dp))
            CrimsonGlowButton(
                text = "Try Again",
                onClick = onRetry,
                testTagName = "retry_button"
            )
        }
    }
}
