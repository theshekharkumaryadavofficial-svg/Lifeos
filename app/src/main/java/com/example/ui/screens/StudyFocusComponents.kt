package com.example.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.TrackChanges
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.StudyTimeSlot
import com.example.focus.findKnownAppInfo
import com.example.focus.formatHourMinute12h
import com.example.focus.isTimeSlotActiveNow
import com.example.focus.parsePackageCsv
import com.example.focus.slotDurationLabel
import com.example.ui.theme.AmberGold
import com.example.ui.theme.BrightGold
import com.example.ui.theme.CrimsonGlow
import com.example.ui.theme.CrimsonPrimary
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.EmeraldHealth
import com.example.ui.theme.MysticPurple
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.SaffronDevotion
import com.example.ui.theme.TextOnDark
import com.example.ui.theme.TextOnDarkMuted
import java.util.Locale

@Composable
fun FocusModeHeroCard(
    focusOn: Boolean,
    hasActiveSlotNow: Boolean,
    blockedCount: Int,
    onToggleFocus: (Boolean) -> Unit,
    onOpenFocusDisplay: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isLive = focusOn || hasActiveSlotNow
    val infiniteTransition = rememberInfiniteTransition(label = "focus_hero_anim")
    val brainPulse by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.12f,
        animationSpec = infiniteRepeatable(
            animation = tween(1900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "brain_pulse"
    )
    val orbitAngle by infiniteTransition.animateFloat(
        initialValue = -18f,
        targetValue = 18f,
        animationSpec = infiniteRepeatable(
            animation = tween(3600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "orbit_angle"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        Color(0xFF14112E),
                        Color(0xFF1C1442),
                        Color(0xFF110E26)
                    )
                )
            )
            .border(
                width = 1.5.dp,
                brush = Brush.horizontalGradient(
                    listOf(
                        Color(0xFF6366F1),
                        if (isLive) EmeraldHealth else MysticPurple,
                        Color(0xFF38BDF8)
                    )
                ),
                shape = RoundedCornerShape(22.dp)
            )
            .clickable {
                if (isLive) onOpenFocusDisplay() else onToggleFocus(true)
            }
            .padding(horizontal = 18.dp, vertical = 16.dp)
            .testTag("focus_mode_hero_card")
    ) {
        // Subtle glowing brain + books artwork layered cleanly on right-center
        NeonBrainAndBooksArtwork(
            pulse = brainPulse,
            orbitAngle = orbitAngle,
            isLive = isLive,
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 68.dp)
                .size(width = 132.dp, height = 104.dp)
        )

        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Rounded.Bolt,
                            contentDescription = null,
                            tint = Color(0xFF818CF8),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = "STUDY MODE",
                            color = Color(0xFFA5B4FC),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 1.1.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Focus Mode",
                        color = TextOnDark,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Black,
                        lineHeight = 28.sp
                    )
                    Text(
                        text = if (isLive) "ON" else "OFF",
                        color = if (isLive) Color(0xFF10F29A) else TextOnDarkMuted,
                        fontSize = 23.sp,
                        fontWeight = FontWeight.Black,
                        lineHeight = 26.sp
                    )
                }

                // Large tactile neon pill switch matching reference
                NeonFocusSwitch(
                    checked = isLive,
                    onCheckedChange = onToggleFocus,
                    modifier = Modifier.testTag("focus_mode_master_switch")
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = if (isLive) {
                        "Shield ON • $blockedCount apps blocked • Tap to open display"
                    } else {
                        "Block Distractions • Study Deep • Build Your Future"
                    },
                    color = if (isLive) ElectricCyan else TextOnDarkMuted,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun NeonBrainAndBooksArtwork(
    pulse: Float,
    orbitAngle: Float,
    isLive: Boolean,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val brainCenter = Offset(w * 0.66f, h * 0.46f)
        val glowRadius = (h * 0.48f) * pulse

        // Ambient neon glow behind brain
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    (if (isLive) Color(0xFF8B5CF6) else Color(0xFF4F46E5)).copy(alpha = 0.42f),
                    Color(0xFF06B6D4).copy(alpha = 0.16f),
                    Color.Transparent
                ),
                center = brainCenter,
                radius = glowRadius
            ),
            radius = glowRadius,
            center = brainCenter
        )

        // Stacked miniature glowing books on left side of artwork
        val bookBaseX = w * 0.08f
        val bookBaseY = h * 0.64f
        for (i in 0..2) {
            val yOff = bookBaseY - i * 11.dp.toPx()
            drawRoundRect(
                color = Color(0xFF312E81).copy(alpha = 0.85f),
                topLeft = Offset(bookBaseX + i * 2.dp.toPx(), yOff),
                size = Size(34.dp.toPx(), 9.dp.toPx()),
                cornerRadius = CornerRadius(3.dp.toPx(), 3.dp.toPx())
            )
            drawRoundRect(
                color = Color(0xFF818CF8).copy(alpha = 0.6f),
                topLeft = Offset(bookBaseX + i * 2.dp.toPx(), yOff),
                size = Size(34.dp.toPx(), 9.dp.toPx()),
                cornerRadius = CornerRadius(3.dp.toPx(), 3.dp.toPx()),
                style = Stroke(width = 1.dp.toPx())
            )
        }

        // Tilted neon orbital rings
        rotate(degrees = -22f + orbitAngle * 0.4f, pivot = brainCenter) {
            drawOval(
                brush = Brush.horizontalGradient(
                    listOf(Color(0xFF38BDF8), Color(0xFFC084FC), Color(0xFFF43F5E))
                ),
                topLeft = Offset(brainCenter.x - 42.dp.toPx(), brainCenter.y - 16.dp.toPx()),
                size = Size(84.dp.toPx(), 32.dp.toPx()),
                style = Stroke(width = 1.6.dp.toPx())
            )
        }

        // Stylized head silhouette halo
        drawCircle(
            brush = Brush.linearGradient(
                listOf(Color(0xFF38BDF8), Color(0xFF8B5CF6), Color(0xFFEC4899))
            ),
            radius = 27.dp.toPx(),
            center = brainCenter,
            style = Stroke(width = 2.dp.toPx())
        )

        // Glowing brain folds inside
        val brainPath = Path().apply {
            moveTo(brainCenter.x - 14.dp.toPx(), brainCenter.y + 4.dp.toPx())
            cubicTo(
                brainCenter.x - 18.dp.toPx(), brainCenter.y - 14.dp.toPx(),
                brainCenter.x - 2.dp.toPx(), brainCenter.y - 18.dp.toPx(),
                brainCenter.x, brainCenter.y - 8.dp.toPx()
            )
            cubicTo(
                brainCenter.x + 4.dp.toPx(), brainCenter.y - 18.dp.toPx(),
                brainCenter.x + 18.dp.toPx(), brainCenter.y - 12.dp.toPx(),
                brainCenter.x + 14.dp.toPx(), brainCenter.y + 4.dp.toPx()
            )
            cubicTo(
                brainCenter.x + 12.dp.toPx(), brainCenter.y + 13.dp.toPx(),
                brainCenter.x - 10.dp.toPx(), brainCenter.y + 13.dp.toPx(),
                brainCenter.x - 14.dp.toPx(), brainCenter.y + 4.dp.toPx()
            )
        }
        drawPath(
            path = brainPath,
            brush = Brush.linearGradient(
                listOf(Color(0xFF67E8F9), Color(0xFFD8B4FE), Color(0xFFF472B6))
            ),
            style = Stroke(width = 2.2.dp.toPx(), cap = StrokeCap.Round)
        )
        // Brain stem sparkle
        drawLine(
            color = Color(0xFFF472B6),
            start = Offset(brainCenter.x, brainCenter.y + 8.dp.toPx()),
            end = Offset(brainCenter.x, brainCenter.y + 19.dp.toPx()),
            strokeWidth = 2.dp.toPx(),
            cap = StrokeCap.Round
        )
    }
}

@Composable
private fun NeonFocusSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val trackColor by animateColorAsState(
        targetValue = if (checked) Color(0xFF10E58B) else Color(0xFF2D2A4A),
        animationSpec = tween(220),
        label = "neon_switch_track"
    )
    val thumbOffset by animateDpAsState(
        targetValue = if (checked) 32.dp else 4.dp,
        animationSpec = spring(dampingRatio = 0.72f, stiffness = 420f),
        label = "neon_switch_thumb"
    )

    Box(
        modifier = modifier
            .size(width = 68.dp, height = 38.dp)
            .clip(RoundedCornerShape(50))
            .background(trackColor)
            .border(
                width = 1.4.dp,
                color = if (checked) Color(0xFF6EE7B7) else Color(0x558B5CF6),
                shape = RoundedCornerShape(50)
            )
            .clickable { onCheckedChange(!checked) },
        contentAlignment = Alignment.CenterStart
    ) {
        Box(
            modifier = Modifier
                .offset(x = thumbOffset)
                .size(30.dp)
                .clip(CircleShape)
                .background(Color.White)
        )
    }
}

@Composable
fun StudyTimeSlotCard(
    slot: StudyTimeSlot,
    onToggleEnabled: () -> Unit,
    onEditSlot: () -> Unit,
    onManageSlotApps: () -> Unit,
    onTogglePomodoro: () -> Unit,
    onToggleCountdown: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isLiveNow = isTimeSlotActiveNow(slot)
    val blockedCount = parsePackageCsv(slot.blockedPackagesCsv).size
    val borderColor = when {
        isLiveNow -> Color(0xFF10F29A)
        slot.enabled -> EmeraldHealth.copy(alpha = 0.85f)
        else -> MysticPurple.copy(alpha = 0.65f)
    }
    val cardGradient = if (slot.enabled) {
        listOf(Color(0xFF0A2424), Color(0xFF11182C), Color(0xFF141126))
    } else {
        listOf(Color(0xFF171330), Color(0xFF121024), Color(0xFF141126))
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Brush.horizontalGradient(cardGradient))
            .border(1.3.dp, borderColor, RoundedCornerShape(20.dp))
            .padding(14.dp)
            .testTag("time_slot_card_${slot.id}"),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Header Row: Check Circle + Status + Time Range + Edit Pencil
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(
                        if (slot.enabled) EmeraldHealth else Color(0x228B5CF6)
                    )
                    .border(
                        width = 1.6.dp,
                        color = if (slot.enabled) Color(0xFF6EE7B7) else MysticPurple,
                        shape = CircleShape
                    )
                    .clickable(onClick = onToggleEnabled)
                    .testTag("toggle_slot_enabled_${slot.id}"),
                contentAlignment = Alignment.Center
            ) {
                if (slot.enabled) {
                    Icon(
                        imageVector = Icons.Rounded.Check,
                        contentDescription = "Slot Active",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = when {
                            isLiveNow -> "LIVE NOW"
                            slot.enabled -> "Active"
                            else -> "Paused"
                        },
                        color = when {
                            isLiveNow -> Color(0xFF10F29A)
                            slot.enabled -> EmeraldHealth
                            else -> TextOnDarkMuted
                        },
                        fontSize = 13.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                    if (isLiveNow) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF10F29A))
                        )
                    }
                }
                Text(
                    text = "${formatHourMinute12h(slot.startHour, slot.startMinute)} – ${formatHourMinute12h(slot.endHour, slot.endMinute)}",
                    color = TextOnDark,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Black,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${slot.title} • ${slotDurationLabel(slot.startHour, slot.startMinute, slot.endHour, slot.endMinute)}",
                    color = TextOnDarkMuted,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            IconButton(
                onClick = onEditSlot,
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(Color(0x1EFFFFFF))
                    .testTag("edit_slot_${slot.id}")
            ) {
                Icon(
                    imageVector = Icons.Rounded.Edit,
                    contentDescription = "Edit Time Slot",
                    tint = TextOnDarkMuted,
                    modifier = Modifier.size(19.dp)
                )
            }
        }

        // 3 Sub-cards row matching reference (Apps Blocked | Pomodoro 25+5 | Clock Countdown)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Sub-card 1: Apps Blocked
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0xB3151022))
                    .border(1.dp, CrimsonPrimary.copy(alpha = 0.55f), RoundedCornerShape(14.dp))
                    .clickable(onClick = onManageSlotApps)
                    .padding(horizontal = 10.dp, vertical = 10.dp)
                    .testTag("slot_apps_subcard_${slot.id}"),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Rounded.Block,
                        contentDescription = null,
                        tint = CrimsonGlow,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Blocked",
                        color = TextOnDark,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold,
                        maxLines = 1
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "$blockedCount apps",
                        color = TextOnDarkMuted,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                        contentDescription = null,
                        tint = CrimsonGlow,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            // Sub-card 2: Pomodoro 25+5
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0xB3171024))
                    .border(
                        1.dp,
                        if (slot.pomodoroEnabled) CrimsonGlow.copy(alpha = 0.65f) else Color(0x33FFFFFF),
                        RoundedCornerShape(14.dp)
                    )
                    .clickable(onClick = onTogglePomodoro)
                    .padding(horizontal = 10.dp, vertical = 10.dp)
                    .testTag("slot_pomodoro_subcard_${slot.id}"),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Rounded.Timer,
                        contentDescription = null,
                        tint = Color(0xFFFF5A5F),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Pomodoro",
                        color = TextOnDark,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold,
                        maxLines = 1
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "25+5",
                        color = TextOnDark,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                    MiniTogglePill(checked = slot.pomodoroEnabled, activeColor = EmeraldHealth)
                }
            }

            // Sub-card 3: Clock Countdown
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0xB314112B))
                    .border(
                        1.dp,
                        if (slot.countdownEnabled) MysticPurple.copy(alpha = 0.7f) else Color(0x33FFFFFF),
                        RoundedCornerShape(14.dp)
                    )
                    .clickable(onClick = onToggleCountdown)
                    .padding(horizontal = 10.dp, vertical = 10.dp)
                    .testTag("slot_countdown_subcard_${slot.id}"),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Rounded.Schedule,
                        contentDescription = null,
                        tint = MysticPurple,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Clock",
                        color = TextOnDark,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold,
                        maxLines = 1
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Timer",
                        color = TextOnDarkMuted,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                    MiniTogglePill(checked = slot.countdownEnabled, activeColor = MysticPurple)
                }
            }
        }
    }
}

@Composable
private fun MiniTogglePill(
    checked: Boolean,
    activeColor: Color
) {
    val thumbOffset by animateDpAsState(
        targetValue = if (checked) 15.dp else 2.dp,
        animationSpec = spring(dampingRatio = 0.75f, stiffness = 450f),
        label = "mini_toggle_thumb"
    )
    Box(
        modifier = Modifier
            .size(width = 32.dp, height = 18.dp)
            .clip(RoundedCornerShape(50))
            .background(if (checked) activeColor else Color(0xFF333152)),
        contentAlignment = Alignment.CenterStart
    ) {
        Box(
            modifier = Modifier
                .offset(x = thumbOffset)
                .size(14.dp)
                .clip(CircleShape)
                .background(Color.White)
        )
    }
}

@Composable
fun AppBlockingStripCard(
    blockedPackages: List<String>,
    onRemoveApp: (String) -> Unit,
    onManageApps: () -> Unit,
    modifier: Modifier = Modifier
) {
    val visibleApps = blockedPackages.take(7)
    val extraCount = (blockedPackages.size - visibleApps.size).coerceAtLeast(0)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0xCC131024))
            .border(1.2.dp, MysticPurple.copy(alpha = 0.55f), RoundedCornerShape(20.dp))
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 12.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (visibleApps.isEmpty()) {
            Row(
                modifier = Modifier
                    .clickable(onClick = onManageApps)
                    .padding(horizontal = 8.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Rounded.Add,
                    contentDescription = null,
                    tint = BrightGold,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Tap Manage Apps to select distracting apps to block",
                    color = TextOnDarkMuted,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        } else {
            visibleApps.forEach { pkg ->
                val info = findKnownAppInfo(pkg)
                Column(
                    modifier = Modifier
                        .width(68.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFF1A1630))
                        .border(1.dp, Color(0x30FFFFFF), RoundedCornerShape(16.dp))
                        .clickable { onRemoveApp(pkg) }
                        .padding(vertical = 10.dp, horizontal = 4.dp)
                        .testTag("blocked_app_tile_$pkg"),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(modifier = Modifier.size(44.dp)) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .align(Alignment.Center)
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    Brush.linearGradient(
                                        listOf(info.brandColor, info.accentColor)
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = info.shortBadge,
                                color = if (info.brandColor == Color(0xFFFACC15)) ObsidianBg else Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Black
                            )
                        }
                        Box(
                            modifier = Modifier
                                .size(18.dp)
                                .align(Alignment.BottomEnd)
                                .clip(CircleShape)
                                .background(CrimsonPrimary)
                                .border(1.dp, Color(0xFF1A1630), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Remove,
                                contentDescription = "Remove ${info.displayName}",
                                tint = Color.White,
                                modifier = Modifier.size(12.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = info.displayName,
                        color = TextOnDark,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // +N More / + Add Tile
            Column(
                modifier = Modifier
                    .width(68.dp)
                    .height(86.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0x288B5CF6))
                    .border(1.dp, MysticPurple.copy(alpha = 0.6f), RoundedCornerShape(16.dp))
                    .clickable(onClick = onManageApps)
                    .padding(vertical = 10.dp, horizontal = 4.dp)
                    .testTag("blocked_apps_more_tile"),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = if (extraCount > 0) "+$extraCount" else "+",
                    color = TextOnDark,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black
                )
                Text(
                    text = if (extraCount > 0) "More" else "Add",
                    color = TextOnDarkMuted,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun DailyStudyTrackerCard(
    todayStudyMinutes: Long,
    dailyTargetMinutes: Long,
    totalStudyMinutes: Long,
    streakDays: Long,
    onEditTarget: () -> Unit,
    modifier: Modifier = Modifier
) {
    val safeTarget = dailyTargetMinutes.coerceAtLeast(60L)
    val todayHours = todayStudyMinutes / 60.0
    val targetHours = safeTarget / 60
    val totalHours = totalStudyMinutes / 60.0
    val rawProgress = (todayStudyMinutes.toFloat() / safeTarget.toFloat()).coerceIn(0f, 1f)
    val animatedProgress by animateFloatAsState(
        targetValue = rawProgress,
        animationSpec = tween(700, easing = FastOutSlowInEasing),
        label = "daily_study_ring"
    )
    val percentInt = (rawProgress * 100f).toInt().coerceIn(0, 100)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(
                Brush.linearGradient(
                    listOf(Color(0xFF151026), Color(0xFF1B1228), Color(0xFF130F21))
                )
            )
            .border(1.2.dp, Color(0x4D8B5CF6), RoundedCornerShape(22.dp))
            .padding(16.dp)
            .testTag("daily_study_tracker_card"),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Card Title Row
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Rounded.BarChart,
                contentDescription = null,
                tint = MysticPurple,
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(
                    text = "Daily Study Tracker",
                    color = TextOnDark,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.ExtraBold
                )
                Text(
                    text = "Your progress today",
                    color = TextOnDarkMuted,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        // Left Ring + Target & Right 3 Stat Rows
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.width(126.dp)
            ) {
                Box(
                    modifier = Modifier.size(116.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val stroke = 11.dp.toPx()
                        val pad = stroke / 2f + 2.dp.toPx()
                        val arcSize = Size(size.width - pad * 2f, size.height - pad * 2f)

                        drawArc(
                            color = Color(0x28FFFFFF),
                            startAngle = 0f,
                            sweepAngle = 360f,
                            useCenter = false,
                            topLeft = Offset(pad, pad),
                            size = arcSize,
                            style = Stroke(width = stroke, cap = StrokeCap.Round)
                        )
                        drawArc(
                            brush = Brush.sweepGradient(
                                listOf(EmeraldHealth, Color(0xFF10F29A), ElectricCyan, EmeraldHealth)
                            ),
                            startAngle = -90f,
                            sweepAngle = (animatedProgress.coerceAtLeast(0.03f)) * 360f,
                            useCenter = false,
                            topLeft = Offset(pad, pad),
                            size = arcSize,
                            style = Stroke(width = stroke, cap = StrokeCap.Round)
                        )
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = String.format(Locale.US, "%.1fh", todayHours),
                            color = TextOnDark,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Black
                        )
                        Text(
                            text = "/ ${targetHours}h",
                            color = TextOnDarkMuted,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0x1EFFFFFF))
                        .clickable(onClick = onEditTarget)
                        .padding(horizontal = 10.dp, vertical = 5.dp)
                        .testTag("edit_study_target_button"),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Rounded.TrackChanges,
                        contentDescription = null,
                        tint = ElectricCyan,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = "Target ${targetHours}h",
                        color = TextOnDark,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }

            // Right Side: Streak, Total Study Time, Goal Progress
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TrackerMetricRow(
                    icon = Icons.Rounded.LocalFireDepartment,
                    iconTint = SaffronDevotion,
                    label = "Streak",
                    value = "$streakDays ${if (streakDays == 1L) "Day" else "Days"}"
                )
                TrackerMetricRow(
                    icon = Icons.Rounded.CalendarMonth,
                    iconTint = Color(0xFF818CF8),
                    label = "Total Study Time",
                    value = String.format(Locale.US, "%.1f hours", totalHours)
                )
                // Goal Progress with neon emerald bar
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color(0x991C1633))
                        .border(1.dp, Color(0x2EFFFFFF), RoundedCornerShape(14.dp))
                        .padding(horizontal = 10.dp, vertical = 9.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(CrimsonPrimary.copy(alpha = 0.22f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.TrackChanges,
                                contentDescription = null,
                                tint = CrimsonGlow,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Goal Progress",
                                color = TextOnDarkMuted,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "$percentInt%",
                                    color = TextOnDark,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Black
                                )
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(6.dp)
                                        .clip(RoundedCornerShape(50))
                                        .background(Color(0x33FFFFFF))
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth(animatedProgress.coerceAtLeast(0.04f))
                                            .height(6.dp)
                                            .clip(RoundedCornerShape(50))
                                            .background(
                                                Brush.horizontalGradient(
                                                    listOf(EmeraldHealth, ElectricCyan)
                                                )
                                            )
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TrackerMetricRow(
    icon: ImageVector,
    iconTint: Color,
    label: String,
    value: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0x991C1633))
            .border(1.dp, Color(0x2EFFFFFF), RoundedCornerShape(14.dp))
            .padding(horizontal = 10.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(iconTint.copy(alpha = 0.22f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(16.dp)
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                color = TextOnDarkMuted,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = value,
                color = TextOnDark,
                fontSize = 14.sp,
                fontWeight = FontWeight.Black,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
