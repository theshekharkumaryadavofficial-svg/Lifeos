package com.example.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.DirectionsRun
import androidx.compose.material.icons.rounded.FitnessCenter
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material.icons.rounded.SelfImprovement
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.material.icons.rounded.Work
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.data.LifeTask
import com.example.data.SadhanaLog
import com.example.data.TaskCategory
import com.example.data.UiState
import com.example.data.UserProfile
import com.example.data.WorkoutLog
import com.example.data.WorkoutType
import com.example.ui.components.CrimsonGlowButton
import com.example.ui.components.GlowingProgressRing
import com.example.ui.components.HeroBannerCard
import com.example.ui.components.LifeGlassCard
import com.example.ui.theme.AmberGold
import com.example.ui.theme.BrightGold
import com.example.ui.theme.CrimsonDeep
import com.example.ui.theme.CrimsonGlow
import com.example.ui.theme.CrimsonPrimary
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.EmeraldHealth
import com.example.ui.theme.GlassBorder
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.SaffronDevotion
import com.example.ui.theme.TextOnDark
import com.example.ui.theme.TextOnDarkMuted
import com.example.ui.viewmodel.LifeViewModel

enum class HubSection(val label: String) {
    TASKS("Tasks"),
    NAAM_JAP("Naam Jap"),
    FITNESS("Fitness")
}

@Composable
fun LifeHubScreen(
    viewModel: LifeViewModel,
    selectedSection: HubSection,
    onSelectSection: (HubSection) -> Unit,
    contentPadding: PaddingValues
) {
    val profileUi by viewModel.profileState.collectAsStateWithLifecycle()
    val tasksUi by viewModel.tasksState.collectAsStateWithLifecycle()
    val workoutsUi by viewModel.workoutsState.collectAsStateWithLifecycle()
    val sadhanaUi by viewModel.sadhanaLogsState.collectAsStateWithLifecycle()
    val sessionJap by viewModel.sessionJapCount.collectAsStateWithLifecycle()

    val profile = (profileUi as? UiState.Success)?.data ?: UserProfile()
    val tasks = (tasksUi as? UiState.Success)?.data.orEmpty()
    val workouts = (workoutsUi as? UiState.Success)?.data.orEmpty()
    val sadhanaLogs = (sadhanaUi as? UiState.Success)?.data.orEmpty()

    var showAddTaskDialog by remember { mutableStateOf(false) }
    var showAddWorkoutDialog by remember { mutableStateOf(false) }
    var showCustomMantraDialog by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    when (selectedSection) {
                        HubSection.TASKS -> listOf(Color(0xFF231018), ObsidianBg, ObsidianBg)
                        HubSection.NAAM_JAP -> listOf(Color(0xFF241A10), ObsidianBg, ObsidianBg)
                        HubSection.FITNESS -> listOf(Color(0xFF0E211D), ObsidianBg, ObsidianBg)
                    }
                )
            )
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .widthIn(max = 600.dp)
                .align(Alignment.TopCenter),
            contentPadding = PaddingValues(
                start = 18.dp,
                end = 18.dp,
                top = 12.dp,
                bottom = contentPadding.calculateBottomPadding() + 24.dp
            ),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header + 3-Segment Switcher
            item {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = when (selectedSection) {
                                HubSection.TASKS -> Icons.Rounded.Work
                                HubSection.NAAM_JAP -> Icons.Rounded.SelfImprovement
                                HubSection.FITNESS -> Icons.Rounded.FitnessCenter
                            },
                            contentDescription = null,
                            tint = when (selectedSection) {
                                HubSection.TASKS -> CrimsonGlow
                                HubSection.NAAM_JAP -> SaffronDevotion
                                HubSection.FITNESS -> EmeraldHealth
                            },
                            modifier = Modifier.size(26.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = when (selectedSection) {
                                HubSection.TASKS -> "Work & Task Hub"
                                HubSection.NAAM_JAP -> "Naam Jap & Sadhana"
                                HubSection.FITNESS -> "Fitness & Health"
                            },
                            style = MaterialTheme.typography.headlineMedium.copy(color = TextOnDark)
                        )
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color(0xE0171525))
                            .border(1.5.dp, GlassBorder, RoundedCornerShape(16.dp))
                            .padding(5.dp),
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        HubSection.entries.forEach { sec ->
                            val active = sec == selectedSection
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(
                                        if (active) {
                                            Brush.horizontalGradient(listOf(CrimsonGlow, CrimsonDeep))
                                        } else {
                                            Brush.horizontalGradient(listOf(Color.Transparent, Color.Transparent))
                                        }
                                    )
                                    .border(
                                        width = if (active) 1.5.dp else 0.dp,
                                        color = if (active) Color.White.copy(alpha = 0.5f) else Color.Transparent,
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    .clickable { onSelectSection(sec) }
                                    .padding(vertical = 11.dp)
                                    .testTag("hub_tab_${sec.name.lowercase()}"),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = sec.label,
                                    style = MaterialTheme.typography.labelLarge.copy(
                                        color = if (active) TextOnDark else TextOnDarkMuted,
                                        fontWeight = FontWeight.Black
                                    )
                                )
                            }
                        }
                    }
                }
            }

            when (selectedSection) {
                HubSection.TASKS -> {
                    item {
                        HeroBannerCard(
                            imageRes = R.drawable.img_banner_work_1795675592833,
                            title = "Plan • Organize • Execute",
                            subtitle = "Let's get things done across Study, Fitness & Discipline",
                            badge = "DAILY ACTION"
                        )
                    }
                    item {
                        CrimsonGlowButton(
                            text = "Add New Task",
                            icon = Icons.Rounded.Add,
                            testTagName = "add_new_task_button",
                            onClick = { showAddTaskDialog = true }
                        )
                    }
                    item {
                        Text(
                            text = "My Tasks (${tasks.count { !it.completed }} pending)",
                            style = MaterialTheme.typography.titleLarge.copy(color = TextOnDark)
                        )
                    }
                    if (tasks.isEmpty()) {
                        item {
                            LifeGlassCard(
                                modifier = Modifier.fillMaxWidth(),
                                onClick = { showAddTaskDialog = true }
                            ) {
                                Text(
                                    text = "Your action list is clear!",
                                    style = MaterialTheme.typography.titleMedium.copy(color = TextOnDark)
                                )
                                Text(
                                    text = "Add study chapters, NDA notes, morning workouts, or Brahmacharya habits.",
                                    style = MaterialTheme.typography.bodySmall.copy(color = TextOnDarkMuted)
                                )
                            }
                        }
                    } else {
                        items(tasks, key = { it.id }) { task ->
                            TaskRowCard(
                                task = task,
                                onToggle = { viewModel.toggleTask(task) },
                                onDelete = { viewModel.deleteTask(task.id) }
                            )
                        }
                    }
                }

                HubSection.NAAM_JAP -> {
                    item {
                        HeroBannerCard(
                            imageRes = R.drawable.img_banner_naamjap_1795675592678,
                            title = "Naam Jap & Inner Peace",
                            subtitle = "108 beads = 1 Mala • Today: ${profile.todayMalas} Malas (${profile.todayNaamJapCount} Jap)",
                            badge = "SACRED SADHANA"
                        )
                    }
                    item {
                        NaamJapCounterCard(
                            activeMantra = profile.activeMantra,
                            sessionCount = sessionJap,
                            lifetimeTotal = profile.totalNaamJapCount + sessionJap,
                            onSelectMantra = { viewModel.setActiveMantra(it) },
                            onCustomMantra = { showCustomMantraDialog = true },
                            onTapBead = { viewModel.incrementSessionJap() },
                            onSaveSession = { viewModel.saveNaamJapSession(meditationMinutes = (sessionJap / 15L).coerceAtLeast(1L)) },
                            onResetSession = { viewModel.resetSessionJap() }
                        )
                    }
                    if (sadhanaLogs.isNotEmpty()) {
                        item {
                            Text(
                                text = "Recent Sadhana Sessions",
                                style = MaterialTheme.typography.titleLarge.copy(color = TextOnDark)
                            )
                        }
                        items(sadhanaLogs.take(8), key = { it.id }) { log ->
                            SadhanaLogRow(log)
                        }
                    }
                }

                HubSection.FITNESS -> {
                    item {
                        HeroBannerCard(
                            imageRes = R.drawable.img_banner_work_1795675592833,
                            title = "Strong Body, Sharp Mind",
                            subtitle = "Track Running, Workouts, Daily Hydration & Restful Sleep",
                            badge = "FITNESS & HEALTH"
                        )
                    }
                    // Hydration + Sleep Row
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            HydrationCard(
                                glasses = profile.todayWaterGlasses,
                                onDecrement = { viewModel.adjustWaterGlasses(-1) },
                                onIncrement = { viewModel.adjustWaterGlasses(1) },
                                modifier = Modifier.weight(1f)
                            )
                            SleepCard(
                                sleepHours = profile.todaySleepHours,
                                onAdjust = { delta ->
                                    viewModel.updateSleepHours(profile.todaySleepHours + delta)
                                },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                    item {
                        CrimsonGlowButton(
                            text = "Log Run or Workout",
                            icon = Icons.Rounded.DirectionsRun,
                            testTagName = "log_workout_button",
                            gradientColors = listOf(EmeraldHealth, Color(0xFF047857)),
                            onClick = { showAddWorkoutDialog = true }
                        )
                    }
                    item {
                        Text(
                            text = "Workout History (${workouts.size})",
                            style = MaterialTheme.typography.titleLarge.copy(color = TextOnDark)
                        )
                    }
                    if (workouts.isEmpty()) {
                        item {
                            LifeGlassCard(
                                modifier = Modifier.fillMaxWidth(),
                                onClick = { showAddWorkoutDialog = true }
                            ) {
                                Text(
                                    text = "No workouts logged yet",
                                    style = MaterialTheme.typography.titleMedium.copy(color = TextOnDark)
                                )
                                Text(
                                    text = "Log your morning run, push-ups, walking, or yoga to build your streak.",
                                    style = MaterialTheme.typography.bodySmall.copy(color = TextOnDarkMuted)
                                )
                            }
                        }
                    } else {
                        items(workouts, key = { it.id }) { workout ->
                            WorkoutRowCard(
                                workout = workout,
                                onDelete = { viewModel.deleteWorkout(workout.id) }
                            )
                        }
                    }
                }
            }
        }
    }

    if (showAddTaskDialog) {
        AddTaskDialog(
            onDismiss = { showAddTaskDialog = false },
            onSave = { title, category, priority, due ->
                viewModel.addTask(title, category, priority, due)
                showAddTaskDialog = false
            }
        )
    }

    if (showAddWorkoutDialog) {
        AddWorkoutDialog(
            onDismiss = { showAddWorkoutDialog = false },
            onSave = { title, type, mins, km, cal ->
                viewModel.logWorkout(title, type, mins, km, cal)
                showAddWorkoutDialog = false
            }
        )
    }

    if (showCustomMantraDialog) {
        var mantraDraft by remember(profile.activeMantra) { mutableStateOf(profile.activeMantra) }
        AlertDialog(
            onDismissRequest = { showCustomMantraDialog = false },
            containerColor = Color(0xFF1A1829),
            titleContentColor = TextOnDark,
            textContentColor = TextOnDarkMuted,
            title = { Text("Custom Mantra", fontWeight = FontWeight.ExtraBold) },
            text = {
                OutlinedTextField(
                    value = mantraDraft,
                    onValueChange = { if (it.length <= 60) mantraDraft = it },
                    label = { Text("Enter Mantra Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(
                    enabled = mantraDraft.trim().isNotEmpty(),
                    onClick = {
                        viewModel.setActiveMantra(mantraDraft)
                        showCustomMantraDialog = false
                    }
                ) {
                    Text("Select", color = BrightGold, fontWeight = FontWeight.ExtraBold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCustomMantraDialog = false }) {
                    Text("Cancel", color = TextOnDarkMuted)
                }
            }
        )
    }
}

@Composable
private fun NaamJapCounterCard(
    activeMantra: String,
    sessionCount: Int,
    lifetimeTotal: Long,
    onSelectMantra: (String) -> Unit,
    onCustomMantra: () -> Unit,
    onTapBead: () -> Boolean,
    onSaveSession: () -> Unit,
    onResetSession: () -> Unit
) {
    val haptics = LocalHapticFeedback.current
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.94f else 1f,
        animationSpec = spring(stiffness = 700f),
        label = "mala_scale"
    )
    val beadInMala = sessionCount % 108
    val completedMalas = sessionCount / 108
    val presets = listOf("Radha Radha", "Ram Ram", "Om Namah Shivaya", "Hare Krishna", "Waheguru")

    LifeGlassCard(
        modifier = Modifier.fillMaxWidth(),
        borderColor = SaffronDevotion.copy(alpha = 0.55f)
    ) {
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(presets) { mantra ->
                val selected = mantra == activeMantra
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(if (selected) SaffronDevotion else Color.White.copy(alpha = 0.08f))
                        .clickable { onSelectMantra(mantra) }
                        .padding(horizontal = 12.dp, vertical = 7.dp)
                ) {
                    Text(
                        text = mantra,
                        style = MaterialTheme.typography.labelMedium.copy(
                            color = if (selected) ObsidianBg else TextOnDark,
                            fontWeight = FontWeight.ExtraBold
                        )
                    )
                }
            }
            item {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .border(1.dp, BrightGold.copy(alpha = 0.5f), RoundedCornerShape(50))
                        .clickable(onClick = onCustomMantra)
                        .padding(horizontal = 12.dp, vertical = 7.dp)
                ) {
                    Text(
                        text = "+ Custom",
                        style = MaterialTheme.typography.labelMedium.copy(color = BrightGold)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = activeMantra,
                style = MaterialTheme.typography.headlineSmall.copy(
                    color = BrightGold,
                    fontWeight = FontWeight.ExtraBold
                )
            )
            Text(
                text = "Tap the glowing Mala circle for each Jap",
                style = MaterialTheme.typography.bodySmall.copy(color = TextOnDarkMuted)
            )
            Spacer(modifier = Modifier.height(16.dp))

            // Tactile Mala Tap Area
            Box(
                modifier = Modifier
                    .scale(scale)
                    .size(196.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            listOf(
                                SaffronDevotion.copy(alpha = 0.3f),
                                Color(0xFF1A1424)
                            )
                        )
                    )
                    .clickable(
                        interactionSource = interactionSource,
                        indication = null
                    ) {
                        val finishedMala = onTapBead()
                        haptics.performHapticFeedback(
                            if (finishedMala) HapticFeedbackType.LongPress else HapticFeedbackType.TextHandleMove
                        )
                    }
                    .testTag("naam_jap_tap_button"),
                contentAlignment = Alignment.Center
            ) {
                GlowingProgressRing(
                    progressFraction = beadInMala / 108f,
                    centerText = "$beadInMala / 108",
                    subtitleText = "Malas: $completedMalas",
                    size = 186.dp,
                    strokeWidth = 12.dp,
                    ringColors = listOf(SaffronDevotion, BrightGold, CrimsonGlow, SaffronDevotion)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))
            Text(
                text = "Session: $sessionCount beads  •  Lifetime: $lifetimeTotal",
                style = MaterialTheme.typography.labelLarge.copy(color = TextOnDarkMuted)
            )
            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                CrimsonGlowButton(
                    text = "Save to Cloud",
                    icon = Icons.Rounded.Save,
                    enabled = sessionCount > 0,
                    testTagName = "save_naam_jap_button",
                    gradientColors = listOf(SaffronDevotion, AmberGold),
                    onClick = onSaveSession,
                    modifier = Modifier.weight(1f)
                )
                if (sessionCount > 0) {
                    Box(
                        modifier = Modifier
                            .height(56.dp)
                            .clip(RoundedCornerShape(28.dp))
                            .background(Color.White.copy(alpha = 0.12f))
                            .border(1.5.dp, Color.White.copy(alpha = 0.35f), RoundedCornerShape(28.dp))
                            .clickable(onClick = onResetSession)
                            .padding(horizontal = 18.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Reset",
                            style = MaterialTheme.typography.titleSmall.copy(
                                color = TextOnDark,
                                fontWeight = FontWeight.ExtraBold
                            )
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SadhanaLogRow(log: SadhanaLog) {
    LifeGlassCard(
        modifier = Modifier.fillMaxWidth(),
        borderColor = SaffronDevotion.copy(alpha = 0.25f),
        contentPadding = 13.dp
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = log.mantra,
                    style = MaterialTheme.typography.titleSmall.copy(color = TextOnDark)
                )
                Text(
                    text = "${log.dateLabel} • ${log.malas} Malas completed",
                    style = MaterialTheme.typography.bodySmall.copy(color = TextOnDarkMuted)
                )
            }
            Text(
                text = "${log.count} Jap",
                style = MaterialTheme.typography.titleMedium.copy(
                    color = BrightGold,
                    fontWeight = FontWeight.ExtraBold
                )
            )
        }
    }
}

@Composable
private fun HydrationCard(
    glasses: Long,
    onDecrement: () -> Unit,
    onIncrement: () -> Unit,
    modifier: Modifier = Modifier
) {
    LifeGlassCard(
        modifier = modifier,
        borderColor = ElectricCyan.copy(alpha = 0.4f),
        contentPadding = 14.dp
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Rounded.WaterDrop,
                contentDescription = "Water",
                tint = ElectricCyan,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "Water",
                style = MaterialTheme.typography.titleSmall.copy(color = TextOnDark)
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "$glasses / 8",
            style = MaterialTheme.typography.headlineSmall.copy(
                color = ElectricCyan,
                fontWeight = FontWeight.ExtraBold
            )
        )
        Text(
            text = "glasses today",
            style = MaterialTheme.typography.labelSmall.copy(color = TextOnDarkMuted)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            IconButton(
                onClick = onDecrement,
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.16f))
                    .border(1.5.dp, Color.White.copy(alpha = 0.35f), CircleShape)
            ) {
                Icon(Icons.Rounded.Remove, contentDescription = "Remove glass", tint = TextOnDark)
            }
            IconButton(
                onClick = onIncrement,
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(ElectricCyan)
                    .border(1.5.dp, Color.White.copy(alpha = 0.55f), CircleShape)
                    .testTag("add_water_glass_button")
            ) {
                Icon(Icons.Rounded.Add, contentDescription = "Add glass", tint = ObsidianBg)
            }
        }
    }
}

@Composable
private fun SleepCard(
    sleepHours: Double,
    onAdjust: (Double) -> Unit,
    modifier: Modifier = Modifier
) {
    LifeGlassCard(
        modifier = modifier,
        borderColor = BrightGold.copy(alpha = 0.35f),
        contentPadding = 14.dp
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Rounded.Bedtime,
                contentDescription = "Sleep",
                tint = BrightGold,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "Sleep",
                style = MaterialTheme.typography.titleSmall.copy(color = TextOnDark)
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "${"%.1f".format(sleepHours)}h",
            style = MaterialTheme.typography.headlineSmall.copy(
                color = BrightGold,
                fontWeight = FontWeight.ExtraBold
            )
        )
        Text(
            text = "last night",
            style = MaterialTheme.typography.labelSmall.copy(color = TextOnDarkMuted)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            IconButton(
                onClick = { onAdjust(-0.5) },
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.16f))
                    .border(1.5.dp, Color.White.copy(alpha = 0.35f), CircleShape)
            ) {
                Icon(Icons.Rounded.Remove, contentDescription = "Decrease sleep", tint = TextOnDark)
            }
            IconButton(
                onClick = { onAdjust(0.5) },
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(AmberGold)
                    .border(1.5.dp, Color.White.copy(alpha = 0.55f), CircleShape)
            ) {
                Icon(Icons.Rounded.Add, contentDescription = "Increase sleep", tint = ObsidianBg)
            }
        }
    }
}

@Composable
private fun TaskRowCard(
    task: LifeTask,
    onToggle: () -> Unit,
    onDelete: () -> Unit
) {
    val categoryColor = when (TaskCategory.fromKey(task.category)) {
        TaskCategory.STUDY -> CrimsonGlow
        TaskCategory.FITNESS -> EmeraldHealth
        TaskCategory.DISCIPLINE -> SaffronDevotion
        TaskCategory.WORK -> BrightGold
    }
    LifeGlassCard(
        modifier = Modifier.fillMaxWidth(),
        borderColor = if (task.completed) EmeraldHealth.copy(alpha = 0.35f) else GlassBorder,
        contentPadding = 13.dp,
        onClick = onToggle
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (task.completed) Icons.Rounded.CheckCircle else Icons.Rounded.RadioButtonUnchecked,
                contentDescription = if (task.completed) "Completed" else "Pending",
                tint = if (task.completed) EmeraldHealth else TextOnDarkMuted,
                modifier = Modifier.size(26.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = task.title,
                    style = MaterialTheme.typography.titleSmall.copy(
                        color = if (task.completed) TextOnDarkMuted else TextOnDark,
                        textDecoration = if (task.completed) TextDecoration.LineThrough else TextDecoration.None
                    ),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = TaskCategory.fromKey(task.category).label,
                        style = MaterialTheme.typography.labelSmall.copy(color = categoryColor)
                    )
                    if (task.dueDate.isNotBlank()) {
                        Text(
                            text = "• ${task.dueDate}",
                            style = MaterialTheme.typography.labelSmall.copy(color = TextOnDarkMuted)
                        )
                    }
                }
            }
            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Rounded.DeleteOutline,
                    contentDescription = "Delete task",
                    tint = TextOnDarkMuted,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
private fun WorkoutRowCard(
    workout: WorkoutLog,
    onDelete: () -> Unit
) {
    LifeGlassCard(
        modifier = Modifier.fillMaxWidth(),
        borderColor = EmeraldHealth.copy(alpha = 0.3f),
        contentPadding = 13.dp
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(EmeraldHealth.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.DirectionsRun,
                    contentDescription = workout.activityType,
                    tint = EmeraldHealth
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = workout.title,
                    style = MaterialTheme.typography.titleSmall.copy(color = TextOnDark)
                )
                val distPart = if (workout.distanceKm > 0.0) " • ${"%.1f".format(workout.distanceKm)} km" else ""
                Text(
                    text = "${WorkoutType.fromKey(workout.activityType).label} • ${workout.durationMinutes} min$distPart • ${workout.caloriesBurned} kcal",
                    style = MaterialTheme.typography.bodySmall.copy(color = TextOnDarkMuted),
                    fontSize = 11.5.sp
                )
            }
            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Rounded.DeleteOutline,
                    contentDescription = "Delete workout",
                    tint = TextOnDarkMuted,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
private fun AddTaskDialog(
    onDismiss: () -> Unit,
    onSave: (String, TaskCategory, String, String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var selectedCat by remember { mutableStateOf(TaskCategory.STUDY) }
    var dueDate by remember { mutableStateOf("Today") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF1A1829),
        titleContentColor = TextOnDark,
        textContentColor = TextOnDarkMuted,
        title = { Text("Add New Task", fontWeight = FontWeight.ExtraBold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { if (it.length <= 120) title = it },
                    label = { Text("Task (e.g. Revise Maths, 5K Run, 2 Malas)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    TaskCategory.entries.forEach { cat ->
                        val active = selectedCat == cat
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(if (active) CrimsonPrimary else Color.White.copy(alpha = 0.08f))
                                .clickable { selectedCat = cat }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = cat.label,
                                style = MaterialTheme.typography.labelSmall.copy(color = TextOnDark)
                            )
                        }
                    }
                }
                OutlinedTextField(
                    value = dueDate,
                    onValueChange = { if (it.length <= 30) dueDate = it },
                    label = { Text("When") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                enabled = title.trim().isNotEmpty(),
                onClick = { onSave(title, selectedCat, "HIGH", dueDate) }
            ) {
                Text("Add Task", color = BrightGold, fontWeight = FontWeight.ExtraBold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = TextOnDarkMuted)
            }
        }
    )
}

@Composable
private fun AddWorkoutDialog(
    onDismiss: () -> Unit,
    onSave: (String, WorkoutType, Long, Double, Long) -> Unit
) {
    var title by remember { mutableStateOf("Morning Run") }
    var selectedType by remember { mutableStateOf(WorkoutType.RUNNING) }
    var durationText by remember { mutableStateOf("30") }
    var distanceText by remember { mutableStateOf("3.5") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF1A1829),
        titleContentColor = TextOnDark,
        textContentColor = TextOnDarkMuted,
        title = { Text("Log Fitness Activity", fontWeight = FontWeight.ExtraBold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { if (it.length <= 100) title = it },
                    label = { Text("Activity Title") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(WorkoutType.entries) { wt ->
                        val active = wt == selectedType
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(if (active) EmeraldHealth else Color.White.copy(alpha = 0.08f))
                                .clickable { selectedType = wt }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = wt.label,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = if (active) ObsidianBg else TextOnDark,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        }
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = durationText,
                        onValueChange = { durationText = it.filter { ch -> ch.isDigit() }.take(3) },
                        label = { Text("Minutes") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = distanceText,
                        onValueChange = { distanceText = it.filter { ch -> ch.isDigit() || ch == '.' }.take(5) },
                        label = { Text("Km (opt)") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = title.trim().isNotEmpty(),
                onClick = {
                    val mins = (durationText.toLongOrNull() ?: 20L).coerceIn(1L, 600L)
                    val km = (distanceText.toDoubleOrNull() ?: 0.0).coerceIn(0.0, 500.0)
                    val estCal = (mins * 8L).coerceIn(10L, 5000L)
                    onSave(title, selectedType, mins, km, estCal)
                }
            ) {
                Text("Log Activity", color = EmeraldHealth, fontWeight = FontWeight.ExtraBold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = TextOnDarkMuted)
            }
        }
    )
}
