package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.focus.canDrawOverlays
import com.example.focus.findKnownAppInfo
import com.example.focus.formatTimerHhMmSs
import com.example.focus.formatTimerMmSs
import com.example.focus.hasUsageStatsPermission
import com.example.focus.openOverlaySettings
import com.example.focus.openUsageAccessSettings
import com.example.ui.theme.AmberGold
import com.example.ui.theme.BrightGold
import com.example.ui.theme.CrimsonGlow
import com.example.ui.theme.CrimsonPrimary
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.EmeraldHealth
import com.example.ui.theme.MysticPurple
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.TextOnDark
import com.example.ui.theme.TextOnDarkMuted
import com.example.ui.viewmodel.LifeViewModel

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FocusModeScreen(
    viewModel: LifeViewModel,
    onMinimize: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val session by viewModel.liveFocusSession.collectAsStateWithLifecycle()

    var hasUsageAccess by remember { mutableStateOf(hasUsageStatsPermission(context)) }
    var hasOverlayAccess by remember { mutableStateOf(canDrawOverlays(context)) }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                hasUsageAccess = hasUsageStatsPermission(context)
                hasOverlayAccess = canDrawOverlays(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    BackHandler { onMinimize() }

    val pulseTransition = rememberInfiniteTransition(label = "focus_screen_pulse")
    val haloScale by pulseTransition.animateFloat(
        initialValue = 0.88f,
        targetValue = 1.09f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "halo_scale"
    )
    val orbitalSweep by pulseTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(9000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "orbital_sweep"
    )

    val phaseTotal = if (session.pomodoroIsBreak) 5 * 60 else 25 * 60
    val pomodoroProgress = if (session.pomodoroEnabled) {
        1f - (session.pomodoroRemainingSeconds.toFloat() / phaseTotal.toFloat()).coerceIn(0f, 1f)
    } else {
        1f
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color(0xFF0E0B22),
                        ObsidianBg,
                        Color(0xFF140A1E)
                    )
                )
            )
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Top Header Controls
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(EmeraldHealth.copy(alpha = 0.2f))
                        .border(1.2.dp, EmeraldHealth.copy(alpha = 0.7f), RoundedCornerShape(50))
                        .padding(horizontal = 12.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Bolt,
                        contentDescription = null,
                        tint = EmeraldHealth,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "FOCUS MODE ON • ${session.activeSlotTitle}",
                        color = TextOnDark,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }

                IconButton(
                    onClick = onMinimize,
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(Color(0x28FFFFFF))
                        .testTag("focus_minimize_button")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.KeyboardArrowDown,
                        contentDescription = "Minimize Focus Mode",
                        tint = TextOnDark
                    )
                }
            }

            // TOP DISPLAY MOTIVATIONAL QUOTE CARD
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(22.dp))
                    .background(
                        Brush.linearGradient(
                            listOf(
                                MysticPurple.copy(alpha = 0.28f),
                                Color(0xCC181330),
                                AmberGold.copy(alpha = 0.16f)
                            )
                        )
                    )
                    .border(
                        width = 1.5.dp,
                        brush = Brush.horizontalGradient(
                            listOf(ElectricCyan, MysticPurple, BrightGold)
                        ),
                        shape = RoundedCornerShape(22.dp)
                    )
                    .clickable { viewModel.cycleMotivationalQuote() }
                    .padding(horizontal = 18.dp, vertical = 16.dp)
                    .testTag("focus_motivational_quote_card")
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Rounded.AutoAwesome,
                                contentDescription = null,
                                tint = BrightGold,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "MOTIVATION FOR THIS SESSION",
                                color = BrightGold,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 1.1.sp
                            )
                        }
                        Icon(
                            imageVector = Icons.Rounded.Refresh,
                            contentDescription = "Next Motivational Quote",
                            tint = TextOnDarkMuted,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    AnimatedContent(
                        targetState = session.motivationalQuote,
                        transitionSpec = { fadeIn(tween(260)) togetherWith fadeOut(tween(180)) },
                        label = "quote_transition"
                    ) { quoteText ->
                        Text(
                            text = "“$quoteText”",
                            color = TextOnDark,
                            fontSize = 18.sp,
                            lineHeight = 25.sp,
                            fontWeight = FontWeight.ExtraBold,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            // ANIMATED POMODORO (25+5) & COUNTDOWN RING
            Box(
                modifier = Modifier.size(248.dp),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val strokeW = 15.dp.toPx()
                    val pad = strokeW + 14.dp.toPx()
                    val arcSize = Size(size.width - pad * 2f, size.height - pad * 2f)

                    // Breathing neon halo
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                (if (session.pomodoroIsBreak) EmeraldHealth else ElectricCyan).copy(alpha = 0.28f),
                                MysticPurple.copy(alpha = 0.14f),
                                Color.Transparent
                            ),
                            center = center,
                            radius = (size.minDimension / 2f) * haloScale
                        ),
                        radius = (size.minDimension / 2f) * haloScale
                    )

                    // Animated faint orbital ring
                    drawArc(
                        color = MysticPurple.copy(alpha = 0.35f),
                        startAngle = orbitalSweep,
                        sweepAngle = 110f,
                        useCenter = false,
                        topLeft = Offset(8.dp.toPx(), 8.dp.toPx()),
                        size = Size(size.width - 16.dp.toPx(), size.height - 16.dp.toPx()),
                        style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round)
                    )

                    // Track
                    drawArc(
                        color = Color(0x30FFFFFF),
                        startAngle = 0f,
                        sweepAngle = 360f,
                        useCenter = false,
                        topLeft = Offset(pad, pad),
                        size = arcSize,
                        style = Stroke(width = strokeW, cap = StrokeCap.Round)
                    )

                    // Active neon sweep
                    drawArc(
                        brush = Brush.sweepGradient(
                            if (session.pomodoroIsBreak) {
                                listOf(EmeraldHealth, ElectricCyan, EmeraldHealth)
                            } else {
                                listOf(ElectricCyan, MysticPurple, CrimsonGlow, ElectricCyan)
                            }
                        ),
                        startAngle = -90f,
                        sweepAngle = pomodoroProgress.coerceIn(0.05f, 1f) * 360f,
                        useCenter = false,
                        topLeft = Offset(pad, pad),
                        size = arcSize,
                        style = Stroke(width = strokeW, cap = StrokeCap.Round)
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = if (session.pomodoroEnabled) Icons.Rounded.Timer else Icons.Rounded.Schedule,
                        contentDescription = null,
                        tint = if (session.pomodoroIsBreak) EmeraldHealth else ElectricCyan,
                        modifier = Modifier.size(26.dp)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (session.pomodoroEnabled) {
                            formatTimerMmSs(session.pomodoroRemainingSeconds)
                        } else if (session.slotRemainingSeconds > 0) {
                            formatTimerHhMmSs(session.slotRemainingSeconds)
                        } else {
                            formatTimerHhMmSs(session.elapsedFocusSeconds)
                        },
                        color = TextOnDark,
                        fontSize = 42.sp,
                        fontWeight = FontWeight.Black
                    )
                    Text(
                        text = when {
                            session.pomodoroEnabled && session.pomodoroIsBreak -> "BREAK TIME • 5 MIN"
                            session.pomodoroEnabled -> "POMODORO FOCUS • 25+5"
                            session.slotRemainingSeconds > 0 -> "SLOT COUNTDOWN"
                            else -> "ELAPSED STUDY TIME"
                        },
                        color = if (session.pomodoroIsBreak) EmeraldHealth else ElectricCyan,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 0.8.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Logged: ${session.elapsedFocusSeconds / 60}m • Cycles: ${session.completedPomodoros}",
                        color = TextOnDarkMuted,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Slot Countdown Pill (when a scheduled Time Slot is active)
            if (session.countdownEnabled && session.slotRemainingSeconds > 0) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .background(Color(0xCC191532))
                        .border(1.2.dp, MysticPurple.copy(alpha = 0.65f), RoundedCornerShape(18.dp))
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Rounded.Schedule,
                            contentDescription = null,
                            tint = MysticPurple,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "${session.activeSlotTitle} Countdown",
                            color = TextOnDark,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Text(
                        text = formatTimerHhMmSs(session.slotRemainingSeconds),
                        color = BrightGold,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }

            // Pomodoro Quick Controls
            if (session.pomodoroEnabled) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = { viewModel.toggleLivePomodoroRunning() },
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp)
                            .testTag("focus_pomodoro_toggle_button"),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (session.pomodoroRunning) AmberGold else EmeraldHealth,
                            contentColor = ObsidianBg
                        )
                    ) {
                        Icon(
                            imageVector = if (session.pomodoroRunning) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (session.pomodoroRunning) "Pause" else "Resume",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }

                    OutlinedButton(
                        onClick = { viewModel.switchLivePomodoroPhase() },
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp)
                            .testTag("focus_phase_switch_button"),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.2.dp, ElectricCyan.copy(alpha = 0.65f))
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.SwapHoriz,
                            contentDescription = null,
                            tint = ElectricCyan,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (session.pomodoroIsBreak) "Focus 25m" else "Break 5m",
                            color = TextOnDark,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }
            }

            // BLOCKED APPS SHIELD PANEL
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color(0xCC161229))
                    .border(1.2.dp, CrimsonPrimary.copy(alpha = 0.5f), RoundedCornerShape(20.dp))
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Rounded.Shield,
                            contentDescription = null,
                            tint = CrimsonGlow,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Restricted Apps Blocked (${session.blockedPackages.size})",
                            color = TextOnDark,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                    if (hasUsageAccess && hasOverlayAccess) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Rounded.CheckCircle,
                                contentDescription = null,
                                tint = EmeraldHealth,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Phone Shield ON",
                                color = EmeraldHealth,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    }
                }

                if (session.blockedPackages.isEmpty()) {
                    Text(
                        text = "No apps selected yet. Go to Study Center → Manage Apps to block distractions.",
                        color = TextOnDarkMuted,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                } else {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        session.blockedPackages.forEach { pkg ->
                            val info = findKnownAppInfo(pkg)
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(info.brandColor.copy(alpha = 0.2f))
                                    .border(1.dp, info.brandColor.copy(alpha = 0.55f), RoundedCornerShape(12.dp))
                                    .padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Block,
                                    contentDescription = null,
                                    tint = CrimsonGlow,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = info.displayName,
                                    color = TextOnDark,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                if (!hasUsageAccess || !hasOverlayAccess) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (!hasUsageAccess) {
                            OutlinedButton(
                                onClick = { openUsageAccessSettings(context) },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, BrightGold)
                            ) {
                                Text(
                                    text = "Enable Usage Access",
                                    color = BrightGold,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }
                        }
                        if (!hasOverlayAccess) {
                            OutlinedButton(
                                onClick = { openOverlaySettings(context) },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, ElectricCyan)
                            ) {
                                Text(
                                    text = "Allow Display Over Apps",
                                    color = ElectricCyan,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }
                        }
                    }
                }
            }

            // End Focus & Save Study Button
            Button(
                onClick = { viewModel.endFocusAndLogStudy() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .testTag("focus_end_and_save_button"),
                shape = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = CrimsonPrimary,
                    contentColor = TextOnDark
                )
            ) {
                Icon(
                    imageVector = Icons.Rounded.Close,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "End Focus Mode & Save Study",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }
        }
    }
}
