package com.example.ui.screens

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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.FitnessCenter
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.MenuBook
import androidx.compose.material.icons.rounded.SelfImprovement
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.UiState
import com.example.data.UserProfile
import com.example.ui.components.ErrorRetryCard
import com.example.ui.components.GlowingProgressBar
import com.example.ui.components.GlowingProgressRing
import com.example.ui.components.LifeGlassCard
import com.example.ui.theme.AmberGold
import com.example.ui.theme.BrightGold
import com.example.ui.theme.CrimsonGlow
import com.example.ui.theme.CrimsonPrimary
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.EmeraldHealth
import com.example.ui.theme.GlassBorderWarm
import com.example.ui.theme.MysticPurple
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.SaffronDevotion
import com.example.ui.theme.TextOnDark
import com.example.ui.theme.TextOnDarkMuted
import com.example.ui.viewmodel.LifeViewModel

@Composable
fun ProgressScreen(
    viewModel: LifeViewModel,
    onOpenStudy: () -> Unit,
    contentPadding: PaddingValues
) {
    val profileUi by viewModel.profileState.collectAsStateWithLifecycle()
    val tasksUi by viewModel.tasksState.collectAsStateWithLifecycle()
    val subjectsUi by viewModel.subjectsState.collectAsStateWithLifecycle()
    val workoutsUi by viewModel.workoutsState.collectAsStateWithLifecycle()
    val insightUi by viewModel.progressInsight.collectAsStateWithLifecycle()

    val profile = (profileUi as? UiState.Success)?.data ?: UserProfile()
    val tasks = (tasksUi as? UiState.Success)?.data.orEmpty()
    val subjects = (subjectsUi as? UiState.Success)?.data.orEmpty()
    val workouts = (workoutsUi as? UiState.Success)?.data.orEmpty()

    val completedTasks = tasks.count { it.completed }
    val totalWorkoutMins = workouts.sumOf { it.durationMinutes }
    val studyHoursText = remember(profile.totalStudyMinutes) {
        if (profile.totalStudyMinutes >= 60) {
            "${"%.1f".format(profile.totalStudyMinutes / 60.0)}h"
        } else {
            "${profile.totalStudyMinutes}m"
        }
    }

    val overallScore = remember(tasks, subjects, profile, workouts) {
        val taskRatio = if (tasks.isNotEmpty()) (completedTasks * 100) / tasks.size else 0
        val subjectAvg = if (subjects.isNotEmpty()) subjects.map { it.progressPercent }.average().toInt() else 0
        val sadhanaScore = (profile.todayNaamJapCount.coerceAtMost(108L) * 100 / 108L).toInt()
        val fitnessScore = if (workouts.isNotEmpty()) 100 else (profile.todayWaterGlasses.coerceAtMost(8L) * 12).toInt()
        val activeSignals = listOf(taskRatio, subjectAvg, sadhanaScore, fitnessScore).filter { it > 0 }
        if (activeSignals.isEmpty()) 0 else activeSignals.average().toInt().coerceIn(0, 100)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF24111E), ObsidianBg, ObsidianBg)
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
            // Header matching Screen 4
            item {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Rounded.BarChart,
                            contentDescription = "My Progress",
                            tint = CrimsonGlow,
                            modifier = Modifier.size(26.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "My Progress",
                            style = MaterialTheme.typography.headlineMedium.copy(color = TextOnDark)
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Keep Going!",
                        style = MaterialTheme.typography.headlineSmall.copy(
                            color = BrightGold,
                            fontStyle = FontStyle.Italic,
                            fontWeight = FontWeight.ExtraBold
                        )
                    )
                    Text(
                        text = "You're on the right path • Level ${profile.level}",
                        style = MaterialTheme.typography.bodySmall.copy(color = TextOnDarkMuted)
                    )
                }
            }

            // Fiery Glowing Ring + 3 Stat Boxes (Exact Screen 4 Layout!)
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Box(
                        modifier = Modifier.weight(1.05f),
                        contentAlignment = Alignment.Center
                    ) {
                        GlowingProgressRing(
                            progressFraction = overallScore / 100f,
                            centerText = "$overallScore%",
                            subtitleText = "Overall\nProgress",
                            size = 152.dp,
                            strokeWidth = 13.dp,
                            ringColors = listOf(BrightGold, CrimsonGlow, CrimsonPrimary, BrightGold)
                        )
                    }

                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        StatMiniCard(
                            icon = Icons.Rounded.MenuBook,
                            value = studyHoursText,
                            label = "Study Time",
                            tint = CrimsonGlow
                        )
                        StatMiniCard(
                            icon = Icons.Rounded.CheckCircle,
                            value = "$completedTasks",
                            label = "Tasks Done",
                            tint = EmeraldHealth
                        )
                        StatMiniCard(
                            icon = Icons.Rounded.LocalFireDepartment,
                            value = "${profile.streakDays} Day",
                            label = "Active Streak",
                            tint = BrightGold
                        )
                    }
                }
            }

            // Discipline, Naam Jap & Fitness Analytics Card
            item {
                LifeGlassCard(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Sadhana & Fitness Summary",
                        style = MaterialTheme.typography.titleMedium.copy(color = TextOnDark)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        AnalyticsMetricBox(
                            icon = Icons.Rounded.SelfImprovement,
                            value = "${profile.totalNaamJapCount}",
                            label = "Lifetime Jap",
                            tint = SaffronDevotion,
                            modifier = Modifier.weight(1f)
                        )
                        AnalyticsMetricBox(
                            icon = Icons.Rounded.FitnessCenter,
                            value = "${totalWorkoutMins}m",
                            label = "Workout Time",
                            tint = EmeraldHealth,
                            modifier = Modifier.weight(1f)
                        )
                        AnalyticsMetricBox(
                            icon = Icons.Rounded.WaterDrop,
                            value = "${profile.todayWaterGlasses}/8",
                            label = "Water Today",
                            tint = ElectricCyan,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // Subject Performance Bars (Exact Screen 4)
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Subject Performance",
                        style = MaterialTheme.typography.titleLarge.copy(color = TextOnDark)
                    )
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(AmberGold.copy(alpha = 0.2f))
                            .border(1.5.dp, BrightGold, RoundedCornerShape(12.dp))
                            .clickable(onClick = onOpenStudy)
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "Manage >",
                            style = MaterialTheme.typography.labelLarge.copy(
                                color = BrightGold,
                                fontWeight = FontWeight.ExtraBold
                            )
                        )
                    }
                }
            }

            if (subjects.isEmpty()) {
                item {
                    LifeGlassCard(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = onOpenStudy
                    ) {
                        Text(
                            text = "No subjects tracked yet",
                            style = MaterialTheme.typography.titleMedium.copy(color = TextOnDark)
                        )
                        Text(
                            text = "Add your subjects in Study Center to see live mastery bars here.",
                            style = MaterialTheme.typography.bodySmall.copy(color = TextOnDarkMuted)
                        )
                    }
                }
            } else {
                items(subjects, key = { it.id }) { sub ->
                    LifeGlassCard(
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = 14.dp
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = sub.name,
                                style = MaterialTheme.typography.titleSmall.copy(color = TextOnDark),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                text = "${sub.progressPercent}%",
                                style = MaterialTheme.typography.titleSmall.copy(
                                    color = BrightGold,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        GlowingProgressBar(
                            progressFraction = sub.progressPercent / 100f,
                            accentKey = sub.accentKey
                        )
                    }
                }
            }

            // Golden Trophy Banner -> Triggers Real Gemini AI Progress Analysis!
            item {
                val shape = RoundedCornerShape(20.dp)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(shape)
                        .background(
                            Brush.horizontalGradient(
                                listOf(Color(0xFF472C09), Color(0xFF281912))
                            )
                        )
                        .border(1.2.dp, GlassBorderWarm, shape)
                        .clickable { viewModel.requestProgressInsight() }
                        .padding(16.dp)
                        .testTag("ai_progress_review_banner")
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.EmojiEvents,
                            contentDescription = "Trophy",
                            tint = BrightGold,
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Great Progress! Keep the Fire Alive",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    color = TextOnDark,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            )
                            Text(
                                text = "Tap for AI Coach analysis of your real stats",
                                style = MaterialTheme.typography.bodySmall.copy(color = BrightGold)
                            )
                        }
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                            contentDescription = "Analyze progress",
                            tint = BrightGold
                        )
                    }
                }
            }

            // Live AI Insight Result
            when (val insight = insightUi) {
                is UiState.Loading -> {
                    item {
                        LifeGlassCard(modifier = Modifier.fillMaxWidth()) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    color = BrightGold,
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = "Analyzing your Study, Naam Jap & Fitness momentum...",
                                    style = MaterialTheme.typography.bodyMedium.copy(color = TextOnDarkMuted)
                                )
                            }
                        }
                    }
                }

                is UiState.Success -> {
                    item {
                        LifeGlassCard(
                            modifier = Modifier.fillMaxWidth(),
                            borderColor = MysticPurple.copy(alpha = 0.55f)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Rounded.AutoAwesome,
                                    contentDescription = null,
                                    tint = BrightGold,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "AI Coach Insights",
                                    style = MaterialTheme.typography.titleMedium.copy(color = BrightGold)
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = insight.data,
                                style = MaterialTheme.typography.bodyMedium.copy(color = TextOnDark)
                            )
                        }
                    }
                }

                is UiState.Error -> {
                    item {
                        ErrorRetryCard(
                            message = insight.message,
                            onRetry = { viewModel.requestProgressInsight() }
                        )
                    }
                }

                null -> Unit
            }
        }
    }
}

@Composable
private fun StatMiniCard(
    icon: ImageVector,
    value: String,
    label: String,
    tint: Color
) {
    LifeGlassCard(
        modifier = Modifier.fillMaxWidth(),
        borderColor = tint.copy(alpha = 0.3f),
        contentPadding = 11.dp
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = tint,
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(
                    text = value,
                    style = MaterialTheme.typography.titleMedium.copy(
                        color = TextOnDark,
                        fontWeight = FontWeight.ExtraBold
                    ),
                    maxLines = 1
                )
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall.copy(color = TextOnDarkMuted),
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
private fun AnalyticsMetricBox(
    icon: ImageVector,
    value: String,
    label: String,
    tint: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(Color.White.copy(alpha = 0.05f))
            .border(1.dp, tint.copy(alpha = 0.35f), RoundedCornerShape(14.dp))
            .padding(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = tint,
            modifier = Modifier.size(20.dp)
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
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(color = TextOnDarkMuted),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
