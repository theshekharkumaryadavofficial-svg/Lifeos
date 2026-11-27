package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.TrackChanges
import androidx.compose.material.icons.rounded.WorkspacePremium
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.BuildConfig
import com.example.R
import com.example.data.UiState
import com.example.data.UserProfile
import com.example.ui.components.CrimsonGlowButton
import com.example.ui.components.HeroBannerCard
import com.example.ui.components.LifeGlassCard
import com.example.ui.theme.AmberGold
import com.example.ui.theme.BrightGold
import com.example.ui.theme.CrimsonGlow
import com.example.ui.theme.CrimsonPrimary
import com.example.ui.theme.EmeraldHealth
import com.example.ui.theme.InkDark
import com.example.ui.theme.InkSecondary
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.RoyalBlueStudy
import com.example.ui.theme.SaffronDevotion
import com.example.ui.theme.SettingsBorderLight
import com.example.ui.theme.SettingsCardWhite
import com.example.ui.theme.TextOnDark
import com.example.ui.theme.TextOnDarkMuted
import com.example.ui.viewmodel.LifeViewModel

@Composable
fun SettingsScreen(
    viewModel: LifeViewModel,
    contentPadding: PaddingValues
) {
    val profileUi by viewModel.profileState.collectAsStateWithLifecycle()
    val profile = (profileUi as? UiState.Success)?.data ?: UserProfile()

    var showEditProfileDialog by remember { mutableStateOf(false) }
    var showEditGoalDialog by remember { mutableStateOf(false) }
    var showPrivacyDialog by remember { mutableStateOf(false) }
    var showResetConfirmDialog by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF1F1223), ObsidianBg, ObsidianBg)
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
            // Top bar matching Screen 6
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Rounded.Settings,
                            contentDescription = "Settings",
                            tint = TextOnDark,
                            modifier = Modifier.size(26.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Settings",
                            style = MaterialTheme.typography.headlineMedium.copy(
                                color = TextOnDark,
                                fontWeight = FontWeight.ExtraBold
                            )
                        )
                    }
                    Icon(
                        imageVector = Icons.Rounded.WorkspacePremium,
                        contentDescription = "Crown",
                        tint = BrightGold,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            // Glass Profile Card matching Screen 6
            item {
                LifeGlassCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("settings_profile_card"),
                    onClick = { showEditProfileDialog = true }
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(contentAlignment = Alignment.BottomEnd) {
                            Box(
                                modifier = Modifier
                                    .size(62.dp)
                                    .clip(CircleShape)
                                    .background(
                                        Brush.linearGradient(listOf(CrimsonPrimary, AmberGold))
                                    )
                                    .border(2.dp, BrightGold, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = profile.displayName.take(1).uppercase(),
                                    style = MaterialTheme.typography.headlineSmall.copy(
                                        color = TextOnDark,
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                )
                            }
                            Icon(
                                imageVector = Icons.Rounded.CheckCircle,
                                contentDescription = "Active",
                                tint = EmeraldHealth,
                                modifier = Modifier
                                    .size(20.dp)
                                    .clip(CircleShape)
                                    .background(Color.White)
                            )
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = profile.displayName,
                                style = MaterialTheme.typography.titleLarge.copy(
                                    color = TextOnDark,
                                    fontWeight = FontWeight.ExtraBold
                                ),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = profile.tagline,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = BrightGold,
                                    fontWeight = FontWeight.Bold
                                ),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "Level ${profile.level} • ${profile.xp} XP • Offline Ready",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = TextOnDarkMuted,
                                    fontWeight = FontWeight.Bold
                                ),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                            contentDescription = "Edit profile",
                            tint = TextOnDarkMuted
                        )
                    }
                }
            }

            // Crisp Pure White Rounded Settings Card matching Screen 6
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(22.dp),
                    color = SettingsCardWhite,
                    border = BorderStroke(1.dp, SettingsBorderLight),
                    shadowElevation = 8.dp
                ) {
                    Column(modifier = Modifier.padding(vertical = 6.dp)) {
                        WhiteSettingsRow(
                            icon = Icons.Rounded.Edit,
                            iconTint = InkDark,
                            title = "Edit Profile & Tagline",
                            subtitle = "Customize your greeting & identity",
                            testTagName = "settings_edit_profile_row",
                            onClick = { showEditProfileDialog = true }
                        )
                        HorizontalDivider(
                            color = SettingsBorderLight,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                        // Motivation Mode Toggle
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.LocalFireDepartment,
                                contentDescription = "Motivation Mode",
                                tint = SaffronDevotion,
                                modifier = Modifier.size(26.dp)
                            )
                            Spacer(modifier = Modifier.width(14.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Motivation Mode",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        color = InkDark,
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                )
                                Text(
                                    text = "Daily Quotes & Motivational Motto",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = InkSecondary,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                )
                            }
                            Switch(
                                checked = profile.motivationMode,
                                onCheckedChange = { viewModel.setMotivationMode(it) },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = CrimsonPrimary
                                ),
                                modifier = Modifier.testTag("motivation_mode_switch")
                            )
                        }
                        HorizontalDivider(
                            color = SettingsBorderLight,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                        WhiteSettingsRow(
                            icon = Icons.Rounded.TrackChanges,
                            iconTint = AmberGold,
                            title = "Daily Focus Goal",
                            subtitle = profile.dailyGoalText.ifBlank { "Set your daily target" },
                            testTagName = "settings_daily_goal_row",
                            onClick = { showEditGoalDialog = true }
                        )
                        HorizontalDivider(
                            color = SettingsBorderLight,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                        WhiteSettingsRow(
                            icon = Icons.Rounded.Security,
                            iconTint = RoyalBlueStudy,
                            title = "Privacy & On-Device Storage",
                            subtitle = "Saved privately on this phone (No login needed)",
                            testTagName = "settings_privacy_row",
                            onClick = { showPrivacyDialog = true }
                        )
                        HorizontalDivider(
                            color = SettingsBorderLight,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                        WhiteSettingsRow(
                            icon = Icons.Rounded.Info,
                            iconTint = InkDark,
                            title = "About LifeOS",
                            subtitle = "Version ${BuildConfig.VERSION_NAME} (Build ${BuildConfig.VERSION_CODE}) • Study, Naam Jap & Fitness",
                            showArrow = false,
                            onClick = {}
                        )
                    }
                }
            }

            // Glossy Crimson Action Button
            item {
                CrimsonGlowButton(
                    text = "Reset Today's Counters",
                    icon = Icons.Rounded.Refresh,
                    testTagName = "reset_today_counters_button",
                    onClick = { showResetConfirmDialog = true }
                )
            }

            // Bottom "Never Give Up!" Mountain Banner matching Screen 6
            item {
                HeroBannerCard(
                    imageRes = R.drawable.img_banner_work_1795675592833,
                    title = "Never Give Up!",
                    subtitle = "Discipline today builds the life you dream of tomorrow.",
                    height = 108.dp
                )
            }
        }
    }

    if (showEditProfileDialog) {
        var nameDraft by remember(profile.displayName) { mutableStateOf(profile.displayName) }
        var tagDraft by remember(profile.tagline) { mutableStateOf(profile.tagline) }
        AlertDialog(
            onDismissRequest = { showEditProfileDialog = false },
            containerColor = Color(0xFF1A1829),
            titleContentColor = TextOnDark,
            textContentColor = TextOnDarkMuted,
            title = { Text("Edit Profile", fontWeight = FontWeight.ExtraBold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = nameDraft,
                        onValueChange = { if (it.length <= 80) nameDraft = it },
                        label = { Text("Display Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = tagDraft,
                        onValueChange = { if (it.length <= 100) tagDraft = it },
                        label = { Text("Personal Tagline") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(
                    enabled = nameDraft.trim().isNotEmpty(),
                    onClick = {
                        viewModel.updateProfileIdentity(nameDraft, tagDraft)
                        showEditProfileDialog = false
                    }
                ) {
                    Text("Save", color = BrightGold, fontWeight = FontWeight.ExtraBold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditProfileDialog = false }) {
                    Text("Cancel", color = TextOnDarkMuted, fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    if (showEditGoalDialog) {
        var goalDraft by remember(profile.dailyGoalText) { mutableStateOf(profile.dailyGoalText) }
        AlertDialog(
            onDismissRequest = { showEditGoalDialog = false },
            containerColor = Color(0xFF1A1829),
            titleContentColor = TextOnDark,
            textContentColor = TextOnDarkMuted,
            title = { Text("Daily Focus Goal", fontWeight = FontWeight.ExtraBold) },
            text = {
                OutlinedTextField(
                    value = goalDraft,
                    onValueChange = { if (it.length <= 120) goalDraft = it },
                    label = { Text("Your Daily Goal Motto") },
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
                    Text("Cancel", color = TextOnDarkMuted, fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    if (showPrivacyDialog) {
        AlertDialog(
            onDismissRequest = { showPrivacyDialog = false },
            containerColor = Color(0xFF1A1829),
            titleContentColor = TextOnDark,
            textContentColor = TextOnDarkMuted,
            title = { Text("Privacy & On-Device Storage", fontWeight = FontWeight.ExtraBold) },
            text = {
                Text(
                    "All your LifeOS study notes, subjects, Naam Jap logs, workouts, and tasks are stored privately on your phone using local SQLite (Room) storage — no login is required and everything works offline.",
                    fontWeight = FontWeight.Medium
                )
            },
            confirmButton = {
                TextButton(onClick = { showPrivacyDialog = false }) {
                    Text("Got It", color = BrightGold, fontWeight = FontWeight.ExtraBold)
                }
            }
        )
    }

    if (showResetConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showResetConfirmDialog = false },
            containerColor = Color(0xFF1A1829),
            titleContentColor = TextOnDark,
            textContentColor = TextOnDarkMuted,
            title = { Text("Reset Today's Counters?", fontWeight = FontWeight.ExtraBold) },
            text = {
                Text(
                    "This resets today's Naam Jap bead count and today's Water glasses to 0. Your lifetime Naam Jap total, tasks, notes, subjects, and workout history will stay safe.",
                    fontWeight = FontWeight.Medium
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.resetTodayCounters()
                        showResetConfirmDialog = false
                    }
                ) {
                    Text("Reset", color = CrimsonGlow, fontWeight = FontWeight.ExtraBold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetConfirmDialog = false }) {
                    Text("Cancel", color = TextOnDarkMuted, fontWeight = FontWeight.Bold)
                }
            }
        )
    }
}

@Composable
private fun WhiteSettingsRow(
    icon: ImageVector,
    iconTint: Color,
    title: String,
    subtitle: String,
    showArrow: Boolean = true,
    testTagName: String? = null,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (testTagName != null) Modifier.testTag(testTagName) else Modifier)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = title,
            tint = iconTint,
            modifier = Modifier.size(26.dp)
        )
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium.copy(
                    color = InkDark,
                    fontWeight = FontWeight.ExtraBold
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall.copy(
                    color = InkSecondary,
                    fontWeight = FontWeight.SemiBold
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        if (showArrow) {
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                contentDescription = null,
                tint = InkSecondary
            )
        }
    }
}
