package com.example.focus

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.MenuBook
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.MainActivity
import com.example.data.DEFAULT_FOCUS_QUOTE
import com.example.ui.theme.AmberGold
import com.example.ui.theme.BrightGold
import com.example.ui.theme.CrimsonGlow
import com.example.ui.theme.CrimsonPrimary
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.EmeraldHealth
import com.example.ui.theme.LifeOSTheme
import com.example.ui.theme.MysticPurple
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.TextOnDark
import com.example.ui.theme.TextOnDarkMuted
import kotlinx.coroutines.delay

class FocusBlockerActivity : ComponentActivity() {

    private var reasonState by mutableStateOf(REASON_APP_BLOCKED)
    private var blockedAppNameState by mutableStateOf("")
    private var slotTitleState by mutableStateOf("Study Focus")
    private var quoteState by mutableStateOf(DEFAULT_FOCUS_QUOTE)
    private var pomodoroEnabledState by mutableStateOf(true)
    private var countdownEnabledState by mutableStateOf(true)
    private var slotRemainingState by mutableIntStateOf(0)
    private var blockedCsvState by mutableStateOf("")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        applyExtras(intent)

        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT)
        )

        setContent {
            LifeOSTheme {
                BackHandler {
                    returnToLifeOsFocus()
                }
                FocusBlockerOverlayContent(
                    reason = reasonState,
                    blockedAppName = blockedAppNameState,
                    slotTitle = slotTitleState,
                    quote = quoteState,
                    pomodoroEnabled = pomodoroEnabledState,
                    countdownEnabled = countdownEnabledState,
                    initialSlotRemainingSeconds = slotRemainingState,
                    blockedPackages = parsePackageCsv(blockedCsvState),
                    onReturnToStudy = { returnToLifeOsFocus() },
                    onGoHome = { goHomeLauncher() }
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        applyExtras(intent)
    }

    private fun applyExtras(intent: Intent?) {
        if (intent == null) return
        reasonState = intent.getStringExtra(EXTRA_REASON) ?: REASON_APP_BLOCKED
        blockedAppNameState = intent.getStringExtra(EXTRA_BLOCKED_APP_NAME).orEmpty()
        slotTitleState = intent.getStringExtra(EXTRA_SLOT_TITLE)?.ifBlank { "Study Focus" } ?: "Study Focus"
        quoteState = intent.getStringExtra(EXTRA_QUOTE)?.ifBlank { DEFAULT_FOCUS_QUOTE } ?: DEFAULT_FOCUS_QUOTE
        pomodoroEnabledState = intent.getBooleanExtra(EXTRA_POMODORO_ENABLED, true)
        countdownEnabledState = intent.getBooleanExtra(EXTRA_COUNTDOWN_ENABLED, true)
        slotRemainingState = intent.getIntExtra(EXTRA_SLOT_REMAINING_SECONDS, 0)
        blockedCsvState = intent.getStringExtra(EXTRA_BLOCKED_CSV).orEmpty()
    }

    private fun returnToLifeOsFocus() {
        val mainIntent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            putExtra(FocusMonitorService.EXTRA_OPEN_FOCUS_SCREEN, true)
        }
        startActivity(mainIntent)
        finish()
    }

    private fun goHomeLauncher() {
        val homeIntent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_HOME)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        startActivity(homeIntent)
        finish()
    }

    companion object {
        const val EXTRA_REASON = "extra_reason"
        const val EXTRA_BLOCKED_APP_NAME = "extra_blocked_app_name"
        const val EXTRA_SLOT_TITLE = "extra_slot_title"
        const val EXTRA_QUOTE = "extra_quote"
        const val EXTRA_POMODORO_ENABLED = "extra_pomodoro_enabled"
        const val EXTRA_COUNTDOWN_ENABLED = "extra_countdown_enabled"
        const val EXTRA_SLOT_REMAINING_SECONDS = "extra_slot_remaining_seconds"
        const val EXTRA_BLOCKED_CSV = "extra_blocked_csv"

        const val REASON_APP_BLOCKED = "app_blocked"
        const val REASON_SLOT_STARTED = "slot_started"
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FocusBlockerOverlayContent(
    reason: String,
    blockedAppName: String,
    slotTitle: String,
    quote: String,
    pomodoroEnabled: Boolean,
    countdownEnabled: Boolean,
    initialSlotRemainingSeconds: Int,
    blockedPackages: List<String>,
    onReturnToStudy: () -> Unit,
    onGoHome: () -> Unit
) {
    var visible by remember { mutableStateOf(false) }
    var pomodoroRemaining by remember { mutableIntStateOf(25 * 60) }
    var isBreakPhase by remember { mutableStateOf(false) }
    var slotRemaining by remember(initialSlotRemainingSeconds) {
        mutableIntStateOf(initialSlotRemainingSeconds)
    }

    LaunchedEffect(Unit) {
        visible = true
        while (true) {
            delay(1000L)
            if (pomodoroEnabled) {
                if (pomodoroRemaining > 1) {
                    pomodoroRemaining -= 1
                } else {
                    isBreakPhase = !isBreakPhase
                    pomodoroRemaining = if (isBreakPhase) 5 * 60 else 25 * 60
                }
            }
            if (slotRemaining > 0) {
                slotRemaining -= 1
            }
        }
    }

    val pulseTransition = rememberInfiniteTransition(label = "blocker_pulse")
    val glowPulse by pulseTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.12f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow_pulse"
    )

    val phaseTotal = if (isBreakPhase) 5 * 60 else 25 * 60
    val pomodoroProgress = if (pomodoroEnabled) {
        1f - (pomodoroRemaining.toFloat() / phaseTotal.toFloat()).coerceIn(0f, 1f)
    } else {
        1f
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        androidx.compose.ui.graphics.Color(0xFF0E0B1E),
                        ObsidianBg,
                        androidx.compose.ui.graphics.Color(0xFF190A18)
                    )
                )
            )
            .statusBarsPadding()
            .padding(horizontal = 20.dp, vertical = 18.dp)
    ) {
        AnimatedVisibility(
            visible = visible,
            enter = fadeIn(tween(320)) + scaleIn(initialScale = 0.94f, animationSpec = tween(320))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Top Motivational Quote Banner
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(22.dp))
                        .background(
                            Brush.linearGradient(
                                listOf(
                                    MysticPurple.copy(alpha = 0.28f),
                                    androidx.compose.ui.graphics.Color(0xCC181430),
                                    AmberGold.copy(alpha = 0.18f)
                                )
                            )
                        )
                        .border(
                            width = 1.4.dp,
                            brush = Brush.horizontalGradient(
                                listOf(ElectricCyan, MysticPurple, BrightGold)
                            ),
                            shape = RoundedCornerShape(22.dp)
                        )
                        .padding(horizontal = 18.dp, vertical = 16.dp)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.AutoAwesome,
                                contentDescription = null,
                                tint = BrightGold,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "DAILY FOCUS MOTIVATION",
                                color = BrightGold,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 1.2.sp
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "“$quote”",
                            color = TextOnDark,
                            fontSize = 18.sp,
                            lineHeight = 25.sp,
                            fontWeight = FontWeight.ExtraBold,
                            textAlign = TextAlign.Center
                        )
                    }
                }

                // Shield Alert Pill
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(CrimsonPrimary.copy(alpha = 0.22f))
                        .border(1.2.dp, CrimsonGlow.copy(alpha = 0.7f), RoundedCornerShape(50))
                        .padding(horizontal = 16.dp, vertical = 9.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Shield,
                        contentDescription = null,
                        tint = CrimsonGlow,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (reason == FocusBlockerActivity.REASON_APP_BLOCKED && blockedAppName.isNotBlank()) {
                            "$blockedAppName is Blocked • $slotTitle"
                        } else {
                            "$slotTitle Started • Distractions Blocked"
                        },
                        color = TextOnDark,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }

                // Animated Pomodoro / Countdown Ring
                Box(
                    modifier = Modifier.size(224.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val strokeW = 14.dp.toPx()
                        val pad = strokeW + 10.dp.toPx()
                        val arcSize = Size(size.width - pad * 2f, size.height - pad * 2f)

                        // Breathing neon outer halo
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    (if (isBreakPhase) EmeraldHealth else MysticPurple).copy(alpha = 0.26f),
                                    androidx.compose.ui.graphics.Color.Transparent
                                ),
                                center = center,
                                radius = (size.minDimension / 2f) * glowPulse
                            ),
                            radius = (size.minDimension / 2f) * glowPulse
                        )

                        drawArc(
                            color = androidx.compose.ui.graphics.Color(0x33FFFFFF),
                            startAngle = 0f,
                            sweepAngle = 360f,
                            useCenter = false,
                            topLeft = Offset(pad, pad),
                            size = arcSize,
                            style = Stroke(width = strokeW, cap = StrokeCap.Round)
                        )

                        drawArc(
                            brush = Brush.sweepGradient(
                                listOf(ElectricCyan, EmeraldHealth, MysticPurple, ElectricCyan)
                            ),
                            startAngle = -90f,
                            sweepAngle = (pomodoroProgress.coerceIn(0.06f, 1f)) * 360f,
                            useCenter = false,
                            topLeft = Offset(pad, pad),
                            size = arcSize,
                            style = Stroke(width = strokeW, cap = StrokeCap.Round)
                        )
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = if (pomodoroEnabled) Icons.Rounded.Timer else Icons.Rounded.Schedule,
                            contentDescription = null,
                            tint = if (isBreakPhase) EmeraldHealth else ElectricCyan,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (pomodoroEnabled) {
                                formatTimerMmSs(pomodoroRemaining)
                            } else {
                                formatTimerHhMmSs(slotRemaining)
                            },
                            color = TextOnDark,
                            fontSize = 38.sp,
                            fontWeight = FontWeight.Black
                        )
                        Text(
                            text = when {
                                pomodoroEnabled && isBreakPhase -> "POMODORO BREAK (5m)"
                                pomodoroEnabled -> "POMODORO FOCUS (25+5)"
                                else -> "SLOT COUNTDOWN"
                            },
                            color = if (isBreakPhase) EmeraldHealth else ElectricCyan,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 0.8.sp
                        )
                    }
                }

                if (countdownEnabled && slotRemaining > 0) {
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(androidx.compose.ui.graphics.Color(0xCC191532))
                            .border(1.dp, MysticPurple.copy(alpha = 0.55f), RoundedCornerShape(16.dp))
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Schedule,
                            contentDescription = null,
                            tint = MysticPurple,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Time Slot Remaining: ${formatTimerHhMmSs(slotRemaining)}",
                            color = TextOnDark,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Restricted Apps List
                if (blockedPackages.isNotEmpty()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(20.dp))
                            .background(androidx.compose.ui.graphics.Color(0xB3161327))
                            .border(1.dp, androidx.compose.ui.graphics.Color(0x33FFFFFF), RoundedCornerShape(20.dp))
                            .padding(14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Rounded.Block,
                                contentDescription = null,
                                tint = CrimsonGlow,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "${blockedPackages.size} Restricted Apps Locked",
                                color = TextOnDarkMuted,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            blockedPackages.take(8).forEach { pkg ->
                                val info = findKnownAppInfo(pkg)
                                Row(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(50))
                                        .background(info.brandColor.copy(alpha = 0.22f))
                                        .border(1.dp, info.brandColor.copy(alpha = 0.6f), RoundedCornerShape(50))
                                        .padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(CrimsonGlow)
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
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Primary Return to Study CTA
                Button(
                    onClick = onReturnToStudy,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .testTag("blocker_return_to_study_button"),
                    shape = RoundedCornerShape(18.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = EmeraldHealth,
                        contentColor = ObsidianBg
                    )
                ) {
                    Icon(
                        imageVector = Icons.Rounded.MenuBook,
                        contentDescription = null,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Return to Study Focus",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }

                OutlinedButton(
                    onClick = onGoHome,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("blocker_go_home_button"),
                    shape = RoundedCornerShape(16.dp),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        androidx.compose.ui.graphics.Color(0x44FFFFFF)
                    )
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Home,
                        contentDescription = null,
                        tint = TextOnDarkMuted,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Go to Phone Home",
                        color = TextOnDarkMuted,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
