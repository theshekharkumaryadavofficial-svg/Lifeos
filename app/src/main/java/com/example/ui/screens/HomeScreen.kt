package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.FitnessCenter
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material.icons.rounded.SelfImprovement
import androidx.compose.material.icons.rounded.TrackChanges
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.material.icons.rounded.WorkspacePremium
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.data.LifeTask
import com.example.data.UiState
import com.example.data.UserProfile
import com.example.ui.components.CrimsonGlowButton
import com.example.ui.components.GlowingProgressRing
import com.example.ui.components.LifeGlassCard
import com.example.ui.theme.AmberGold
import com.example.ui.theme.BrightGold
import com.example.ui.theme.CrimsonGlow
import com.example.ui.theme.CrimsonPrimary
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.EmeraldHealth
import com.example.ui.theme.GlassBorder
import com.example.ui.theme.GlassBorderWarm
import com.example.ui.theme.MysticPurple
import com.example.money.formatInrCompact
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.SaffronDevotion
import com.example.ui.theme.TextOnDark
import com.example.ui.theme.TextOnDarkMuted
import com.example.ui.viewmodel.LifeViewModel
import java.util.Calendar

@Composable
fun HomeScreen(
    viewModel: LifeViewModel,
    onNavigateToMoneyTracker: () -> Unit,
    onNavigateToStudy: () -> Unit,
    onNavigateToNaamJap: () -> Unit,
    onNavigateToFitness: () -> Unit,
    onNavigateToAiTutor: () -> Unit,
    onNavigateToTasks: () -> Unit,
    onNavigateToTimer: () -> Unit,
    contentPadding: PaddingValues
) {
    val profileUi by viewModel.profileState.collectAsStateWithLifecycle()
    val tasksUi by viewModel.tasksState.collectAsStateWithLifecycle()
    val workoutsUi by viewModel.workoutsState.collectAsStateWithLifecycle()
    val moneySummary by viewModel.moneySummary.collectAsStateWithLifecycle()

    val profile = (profileUi as? UiState.Success)?.data ?: UserProfile()
    val tasks = (tasksUi as? UiState.Success)?.data.orEmpty()
    val workouts = (workoutsUi as? UiState.Success)?.data.orEmpty()

    var showEditGoalDialog by remember { mutableStateOf(false) }

    val greeting = remember {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        when {
            hour < 12 -> "Good Morning"
            hour < 17 -> "Good Afternoon"
            else -> "Good Evening"
        }
    }
    val firstName = profile.displayName.trim().substringBefore(" ").ifEmpty { "Achiever" }

    val completedCount = tasks.count { it.completed }
    val progressPercent = remember(tasks, profile.todayMalas, profile.todayWaterGlasses, workouts) {
        if (tasks.isNotEmpty()) {
            ((completedCount * 100f) / tasks.size).toInt().coerceIn(0, 100)
        } else {
            val signals = listOf(
                if (profile.todayNaamJapCount > 0) 35 else 0,
                if (profile.todayWaterGlasses >= 4) 30 else (profile.todayWaterGlasses * 7).toInt(),
                if (workouts.isNotEmpty()) 35 else 0
            ).sum()
            signals.coerceIn(0, 100)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ObsidianBg)
    ) {
        // Dramatic dawn summit hero background mirroring Screen 2
        Image(
            painter = painterResource(id = R.drawable.img_hero_summit_1795675589565),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxWidth()
                .height(380.dp)
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(400.dp)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            ObsidianBg.copy(alpha = 0.45f),
                            ObsidianBg.copy(alpha = 0.52f),
                            ObsidianBg.copy(alpha = 0.92f),
                            ObsidianBg
                        )
                    )
                )
        )

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
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // 1. Greeting Header + Golden Lv Badge
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Rounded.WorkspacePremium,
                                contentDescription = "Crown",
                                tint = BrightGold,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = greeting,
                                style = MaterialTheme.typography.titleMedium.copy(
                                    color = BrightGold,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            )
                        }
                        Text(
                            text = "$firstName!",
                            style = MaterialTheme.typography.headlineLarge.copy(
                                color = TextOnDark,
                                fontSize = 32.sp
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = if (profile.motivationMode) "Dream Big... Work Hard... Win Big..." else profile.tagline,
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = TextOnDarkMuted,
                                fontWeight = FontWeight.SemiBold
                            )
                        )
                    }

                    // Golden Level pill badge
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(
                                Brush.horizontalGradient(
                                    listOf(Color(0xFF452C0A), Color(0xFF784A0E))
                                )
                            )
                            .border(1.2.dp, BrightGold, RoundedCornerShape(50))
                            .padding(horizontal = 12.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.WorkspacePremium,
                            contentDescription = "Level",
                            tint = BrightGold,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Lv.${profile.level}",
                            style = MaterialTheme.typography.labelLarge.copy(
                                color = BrightGold,
                                fontWeight = FontWeight.ExtraBold
                            )
                        )
                    }
                }
            }

            // 2. Today's Goal Card (editable!)
            item {
                LifeGlassCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("home_daily_goal_card"),
                    borderColor = GlassBorderWarm,
                    backgroundColor = Color(0xD91A1626),
                    onClick = { showEditGoalDialog = true }
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(46.dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.radialGradient(
                                        listOf(CrimsonGlow, CrimsonPrimary.copy(alpha = 0.3f))
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.TrackChanges,
                                contentDescription = "Today's Goal",
                                tint = TextOnDark,
                                modifier = Modifier.size(26.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Today's Goal",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    color = BrightGold
                                )
                            )
                            Text(
                                text = profile.dailyGoalText.ifBlank { "Be 1% Better Than Yesterday" },
                                style = MaterialTheme.typography.titleMedium.copy(
                                    color = TextOnDark,
                                    fontWeight = FontWeight.ExtraBold
                                ),
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                            contentDescription = "Edit goal",
                            tint = BrightGold
                        )
                    }
                }
            }

            // 3. Today's Progress Ring + Live Interactive Checklist (Exact Screen 2 Card!)
            item {
                LifeGlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    borderColor = GlassBorder,
                    backgroundColor = Color(0xDE161524)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Today's Progress",
                            style = MaterialTheme.typography.titleLarge.copy(
                                color = TextOnDark
                            )
                        )
                        Text(
                            text = "${profile.streakDays}d Streak",
                            style = MaterialTheme.typography.labelMedium.copy(
                                color = BrightGold
                            )
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        GlowingProgressRing(
                            progressFraction = progressPercent / 100f,
                            centerText = "$progressPercent%",
                            size = 112.dp,
                            strokeWidth = 11.dp,
                            ringColors = listOf(EmeraldHealth, BrightGold, CrimsonGlow, EmeraldHealth)
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (tasks.isEmpty()) {
                                Text(
                                    text = "No daily tasks yet.",
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        color = TextOnDark,
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                                Text(
                                    text = "Add Study, Workout, or Naam Jap tasks in your Hub to track completion live.",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = TextOnDarkMuted
                                    )
                                )
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(CrimsonPrimary.copy(alpha = 0.25f))
                                        .border(1.5.dp, BrightGold, RoundedCornerShape(12.dp))
                                        .clickable(onClick = onNavigateToTasks)
                                        .padding(horizontal = 12.dp, vertical = 7.dp)
                                ) {
                                    Text(
                                        text = "+ Add First Task",
                                        style = MaterialTheme.typography.labelLarge.copy(
                                            color = BrightGold,
                                            fontWeight = FontWeight.ExtraBold
                                        )
                                    )
                                }
                            } else {
                                tasks.take(3).forEach { task ->
                                    HomeChecklistRow(
                                        task = task,
                                        onToggle = { viewModel.toggleTask(task) }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 4. Glossy Crimson CTA Pill ("Continue Your Journey ▶")
            item {
                CrimsonGlowButton(
                    text = "Continue Your Journey",
                    trailingIcon = Icons.Rounded.PlayArrow,
                    testTagName = "continue_journey_button",
                    onClick = onNavigateToTimer
                )
            }

            // 5. Four Glowing Quick-Action Tiles (Real Balance Money Tracker, Naam Jap, Fitness, AI Tutor)
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    MoneyBalanceQuickTile(
                        balanceText = formatInrCompact(moneySummary.realBalance),
                        onClick = onNavigateToMoneyTracker,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("home_tile_money_tracker")
                    )
                    QuickActionTile(
                        title = "Naam Jap",
                        icon = Icons.Rounded.SelfImprovement,
                        glowColor = SaffronDevotion,
                        onClick = onNavigateToNaamJap,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("home_tile_naamjap")
                    )
                    QuickActionTile(
                        title = "Fitness",
                        icon = Icons.Rounded.FitnessCenter,
                        glowColor = EmeraldHealth,
                        onClick = onNavigateToFitness,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("home_tile_fitness")
                    )
                    QuickActionTile(
                        title = "AI Tutor",
                        icon = Icons.Rounded.AutoAwesome,
                        glowColor = MysticPurple,
                        onClick = onNavigateToAiTutor,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("home_tile_aitutor")
                    )
                }
            }

            // 6. Compact Daily Pulse Strip (Naam Jap + Water + Study + Workouts)
            item {
                LifeGlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    borderColor = Color.White.copy(alpha = 0.14f)
                ) {
                    Text(
                        text = "Today's Life Pulse",
                        style = MaterialTheme.typography.titleMedium.copy(color = TextOnDark)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        PulseStatPill(
                            label = "Naam Jap",
                            value = "${profile.todayNaamJapCount}",
                            sub = "${profile.todayMalas} malas",
                            icon = Icons.Rounded.SelfImprovement,
                            tint = SaffronDevotion,
                            onClick = onNavigateToNaamJap,
                            modifier = Modifier.weight(1f)
                        )
                        PulseStatPill(
                            label = "Hydration",
                            value = "${profile.todayWaterGlasses}/8",
                            sub = "glasses",
                            icon = Icons.Rounded.WaterDrop,
                            tint = ElectricCyan,
                            onClick = { viewModel.adjustWaterGlasses(1) },
                            modifier = Modifier.weight(1f)
                        )
                        PulseStatPill(
                            label = "XP Power",
                            value = "${profile.xp}",
                            sub = "${profile.totalStudyMinutes}m study",
                            icon = Icons.Rounded.LocalFireDepartment,
                            tint = BrightGold,
                            onClick = onNavigateToStudy,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // 7. Compact AI Assistant Card (non-intrusive per design spec)
            item {
                LifeGlassCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("home_ai_card"),
                    borderColor = MysticPurple.copy(alpha = 0.45f),
                    backgroundColor = Color(0xCC1E1833),
                    onClick = onNavigateToAiTutor
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(
                                    Brush.linearGradient(listOf(MysticPurple, CrimsonGlow))
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.AutoAwesome,
                                contentDescription = "AI Tutor",
                                tint = TextOnDark,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "LifeOS AI Coach",
                                style = MaterialTheme.typography.titleSmall.copy(color = TextOnDark)
                            )
                            Text(
                                text = "Ask a Maths doubt, generate a quiz, or plan your day",
                                style = MaterialTheme.typography.bodySmall.copy(color = TextOnDarkMuted),
                                maxLines = 2
                            )
                        }
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                            contentDescription = "Open AI Coach",
                            tint = TextOnDarkMuted
                        )
                    }
                }
            }
        }
    }

    if (showEditGoalDialog) {
        var goalDraft by remember(profile.dailyGoalText) { mutableStateOf(profile.dailyGoalText) }
        AlertDialog(
            onDismissRequest = { showEditGoalDialog = false },
            containerColor = Color(0xFF1A1829),
            titleContentColor = TextOnDark,
            textContentColor = TextOnDarkMuted,
            title = { Text("Set Today's Goal", fontWeight = FontWeight.ExtraBold) },
            text = {
                OutlinedTextField(
                    value = goalDraft,
                    onValueChange = { if (it.length <= 120) goalDraft = it },
                    label = { Text("Daily focus motto") },
                    singleLine = false,
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.updateDailyGoal(goalDraft)
                        showEditGoalDialog = false
                    }
                ) {
                    Text("Save", color = BrightGold, fontWeight = FontWeight.ExtraBold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditGoalDialog = false }) {
                    Text("Cancel", color = TextOnDarkMuted)
                }
            }
        )
    }
}

@Composable
private fun HomeChecklistRow(
    task: LifeTask,
    onToggle: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onToggle)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = if (task.completed) Icons.Rounded.CheckCircle else Icons.Rounded.RadioButtonUnchecked,
            contentDescription = if (task.completed) "Completed" else "Pending",
            tint = if (task.completed) EmeraldHealth else TextOnDarkMuted,
            modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = task.title,
            style = MaterialTheme.typography.bodyMedium.copy(
                color = if (task.completed) TextOnDarkMuted else TextOnDark,
                fontWeight = FontWeight.Bold,
                textDecoration = if (task.completed) TextDecoration.LineThrough else TextDecoration.None
            ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun QuickActionTile(
    title: String,
    icon: ImageVector,
    glowColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(18.dp)
    Column(
        modifier = modifier
            .clip(shape)
            .background(Color(0xEB1B182C))
            .border(1.6.dp, glowColor.copy(alpha = 0.7f), shape)
            .clickable(onClick = onClick)
            .padding(vertical = 14.dp, horizontal = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(RoundedCornerShape(13.dp))
                .background(
                    Brush.radialGradient(
                        listOf(glowColor.copy(alpha = 0.95f), glowColor.copy(alpha = 0.28f))
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = TextOnDark,
                modifier = Modifier.size(23.dp)
            )
        }
        Spacer(modifier = Modifier.height(7.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.labelMedium.copy(
                color = TextOnDark,
                fontWeight = FontWeight.ExtraBold
            ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun MoneyBalanceQuickTile(
    balanceText: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(18.dp)
    Column(
        modifier = modifier
            .clip(shape)
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF0A3A24), Color(0xFF0C231A))
                )
            )
            .border(1.8.dp, EmeraldHealth.copy(alpha = 0.85f), shape)
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp, horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(13.dp))
                .background(
                    Brush.radialGradient(
                        listOf(Color(0xFF1CD172), EmeraldHealth.copy(alpha = 0.45f))
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Rounded.AccountBalanceWallet,
                contentDescription = "Money Tracker",
                tint = Color.White,
                modifier = Modifier.size(22.dp)
            )
        }
        Spacer(modifier = Modifier.height(5.dp))
        Text(
            text = balanceText,
            color = Color(0xFF6EF5B1),
            fontSize = 13.sp,
            fontWeight = FontWeight.Black,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = "Balance",
            color = TextOnDark,
            fontSize = 10.sp,
            fontWeight = FontWeight.ExtraBold,
            maxLines = 1
        )
    }
}

@Composable
private fun PulseStatPill(
    label: String,
    value: String,
    sub: String,
    icon: ImageVector,
    tint: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(Color.White.copy(alpha = 0.05f))
            .border(1.dp, tint.copy(alpha = 0.3f), RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(10.dp),
        horizontalAlignment = Alignment.Start
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = tint,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium.copy(
                color = TextOnDark,
                fontWeight = FontWeight.ExtraBold
            ),
            maxLines = 1
        )
        Text(
            text = sub,
            style = MaterialTheme.typography.labelSmall.copy(
                color = TextOnDarkMuted,
                fontSize = 10.sp
            ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
