package com.example.ui.screens

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
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
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.CloudUpload
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.SystemUpdate
import androidx.compose.material.icons.rounded.TrackChanges
import androidx.compose.material.icons.rounded.WorkspacePremium
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
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

private const val UPDATE_ADMIN_EMAIL = "the.shekharkumaryadavofficial@gmail.com"

fun openSafeHttpsDownloadUrl(context: Context, rawUrl: String) {
    val trimmed = rawUrl.trim()
    val uri = runCatching { Uri.parse(trimmed) }.getOrNull()
    if (uri == null || !uri.scheme.equals("https", ignoreCase = true) || uri.host.isNullOrBlank()) {
        Toast.makeText(context, "Invalid or non-HTTPS update link.", Toast.LENGTH_SHORT).show()
        return
    }
    try {
        val intent = Intent(Intent.ACTION_VIEW, uri).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    } catch (_: ActivityNotFoundException) {
        Toast.makeText(context, "No browser app found to open update link.", Toast.LENGTH_SHORT).show()
    }
}

@Composable
fun SettingsScreen(
    viewModel: LifeViewModel,
    userEmail: String?,
    photoUrl: String?,
    onLogout: () -> Unit,
    contentPadding: PaddingValues
) {
    val context = LocalContext.current
    val profileUi by viewModel.profileState.collectAsStateWithLifecycle()
    val profile = (profileUi as? UiState.Success)?.data ?: UserProfile()
    val updateConfig by viewModel.appUpdateConfig.collectAsStateWithLifecycle()
    val checkingForUpdate by viewModel.checkingForUpdate.collectAsStateWithLifecycle()
    val publishingUpdate by viewModel.publishingUpdate.collectAsStateWithLifecycle()
    val lastUpdateStatus by viewModel.lastUpdateStatus.collectAsStateWithLifecycle()

    val updateAvailable = viewModel.hasNewerUpdate(updateConfig)
    val isUpdateAdmin = userEmail.equals(UPDATE_ADMIN_EMAIL, ignoreCase = true)

    var showEditProfileDialog by remember { mutableStateOf(false) }
    var showEditGoalDialog by remember { mutableStateOf(false) }
    var showPrivacyDialog by remember { mutableStateOf(false) }
    var showUpdateDialog by remember { mutableStateOf(false) }
    var showPublishUpdateDialog by remember { mutableStateOf(false) }

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
                            style = MaterialTheme.typography.headlineMedium.copy(color = TextOnDark)
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
                            if (!photoUrl.isNullOrBlank()) {
                                AsyncImage(
                                    model = photoUrl,
                                    contentDescription = "User profile photo",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .size(62.dp)
                                        .clip(CircleShape)
                                        .border(2.dp, BrightGold, CircleShape)
                                )
                            } else {
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
                                        style = MaterialTheme.typography.headlineSmall.copy(color = TextOnDark)
                                    )
                                }
                            }
                            Icon(
                                imageVector = Icons.Rounded.CheckCircle,
                                contentDescription = "Verified",
                                tint = CrimsonGlow,
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
                                style = MaterialTheme.typography.titleLarge.copy(color = TextOnDark),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = profile.tagline,
                                style = MaterialTheme.typography.bodySmall.copy(color = BrightGold),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (!userEmail.isNullOrBlank()) {
                                Text(
                                    text = userEmail,
                                    style = MaterialTheme.typography.labelSmall.copy(color = TextOnDarkMuted),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                            contentDescription = "Edit profile",
                            tint = TextOnDarkMuted
                        )
                    }
                }
            }

            // Crisp Pure White Rounded Settings Card matching Screen 6!
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
                                    style = MaterialTheme.typography.bodySmall.copy(color = InkSecondary)
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
                            onClick = { showEditGoalDialog = true }
                        )
                        HorizontalDivider(
                            color = SettingsBorderLight,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                        WhiteSettingsRow(
                            icon = Icons.Rounded.SystemUpdate,
                            iconTint = if (updateAvailable) CrimsonPrimary else EmeraldHealth,
                            title = "App Updates (Firebase)",
                            subtitle = if (updateAvailable) {
                                "New v${updateConfig?.latestVersionName} available — Tap to update"
                            } else {
                                "v${BuildConfig.VERSION_NAME} • Check Firebase for new APK"
                            },
                            badgeText = if (updateAvailable) "NEW" else null,
                            testTagName = "settings_app_updates_row",
                            onClick = {
                                showUpdateDialog = true
                                viewModel.checkForAppUpdate()
                            }
                        )
                        if (isUpdateAdmin) {
                            HorizontalDivider(
                                color = SettingsBorderLight,
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )
                            WhiteSettingsRow(
                                icon = Icons.Rounded.CloudUpload,
                                iconTint = RoyalBlueStudy,
                                title = "Publish App Update (Admin)",
                                subtitle = "Push new version & APK link to Firebase",
                                testTagName = "settings_publish_update_row",
                                onClick = { showPublishUpdateDialog = true }
                            )
                        }
                        HorizontalDivider(
                            color = SettingsBorderLight,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                        WhiteSettingsRow(
                            icon = Icons.Rounded.Security,
                            iconTint = RoyalBlueStudy,
                            title = "Privacy & Cloud Security",
                            subtitle = "Private per-account Firebase sync",
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

            // Glossy Crimson Logout Button matching Screen 6
            item {
                CrimsonGlowButton(
                    text = "Logout",
                    icon = Icons.AutoMirrored.Rounded.Logout,
                    testTagName = "logout_button",
                    onClick = onLogout
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
                    Text("Cancel", color = TextOnDarkMuted)
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
                    Text("Cancel", color = TextOnDarkMuted)
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
            title = { Text("Privacy & Cloud Security", fontWeight = FontWeight.ExtraBold) },
            text = {
                Text(
                    "All your LifeOS study notes, subjects, Naam Jap logs, workouts, and tasks are protected with Google Sign-In and strict per-user Firestore rules so only your signed-in account can read or write your data."
                )
            },
            confirmButton = {
                TextButton(onClick = { showPrivacyDialog = false }) {
                    Text("Got It", color = BrightGold, fontWeight = FontWeight.ExtraBold)
                }
            }
        )
    }

    if (showUpdateDialog) {
        val cfg = updateConfig
        AlertDialog(
            onDismissRequest = { showUpdateDialog = false },
            containerColor = Color(0xFF1A1829),
            titleContentColor = TextOnDark,
            textContentColor = TextOnDarkMuted,
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Rounded.SystemUpdate,
                        contentDescription = null,
                        tint = if (updateAvailable) BrightGold else EmeraldHealth,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = if (updateAvailable) "Update Available!" else "Firebase App Updates",
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Installed: v${BuildConfig.VERSION_NAME} (Build ${BuildConfig.VERSION_CODE})",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = TextOnDark,
                            fontWeight = FontWeight.Bold
                        )
                    )
                    if (checkingForUpdate) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                color = BrightGold,
                                strokeWidth = 2.5.dp
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Checking Firebase for latest version...",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = BrightGold,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        }
                    } else if (updateAvailable && cfg != null) {
                        Text(
                            text = "Latest on Firebase: v${cfg.latestVersionName} (Build ${cfg.latestVersionCode})",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = BrightGold,
                                fontWeight = FontWeight.ExtraBold
                            )
                        )
                        if (cfg.releaseNotes.isNotBlank()) {
                            Text(
                                text = cfg.releaseNotes,
                                style = MaterialTheme.typography.bodySmall.copy(color = TextOnDark)
                            )
                        }
                        if (cfg.downloadUrl.isBlank()) {
                            Text(
                                text = "Download link has not been set yet.",
                                style = MaterialTheme.typography.bodySmall.copy(color = TextOnDarkMuted)
                            )
                        }
                    } else {
                        Text(
                            text = lastUpdateStatus ?: "You are running the latest version of LifeOS.",
                            style = MaterialTheme.typography.bodySmall.copy(color = EmeraldHealth)
                        )
                    }
                }
            },
            confirmButton = {
                if (updateAvailable && !cfg?.downloadUrl.isNullOrBlank()) {
                    TextButton(
                        onClick = {
                            openSafeHttpsDownloadUrl(context, cfg!!.downloadUrl)
                            showUpdateDialog = false
                        },
                        modifier = Modifier.testTag("download_update_button")
                    ) {
                        Text("Download Update", color = BrightGold, fontWeight = FontWeight.ExtraBold)
                    }
                } else {
                    TextButton(
                        enabled = !checkingForUpdate,
                        onClick = { viewModel.checkForAppUpdate() },
                        modifier = Modifier.testTag("recheck_update_button")
                    ) {
                        Text("Check Again", color = BrightGold, fontWeight = FontWeight.ExtraBold)
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { showUpdateDialog = false }) {
                    Text("Close", color = TextOnDarkMuted, fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    if (showPublishUpdateDialog && isUpdateAdmin) {
        val currentCfg = updateConfig
        var versionCodeDraft by remember(currentCfg) {
            mutableStateOf(((currentCfg?.latestVersionCode ?: BuildConfig.VERSION_CODE.toLong()) + 1L).toString())
        }
        var versionNameDraft by remember(currentCfg) {
            mutableStateOf(currentCfg?.latestVersionName?.ifBlank { "1.1" } ?: "1.1")
        }
        var releaseNotesDraft by remember(currentCfg) {
            mutableStateOf(currentCfg?.releaseNotes ?: "New improvements and bug fixes.")
        }
        var downloadUrlDraft by remember(currentCfg) {
            mutableStateOf(
                currentCfg?.downloadUrl?.ifBlank {
                    "https://github.com/YOUR_USERNAME/Lifeos/releases/latest/download/LifeOS-latest.apk"
                } ?: "https://github.com/YOUR_USERNAME/Lifeos/releases/latest/download/LifeOS-latest.apk"
            )
        }
        var forceUpdateDraft by remember(currentCfg) {
            mutableStateOf(currentCfg?.forceUpdate ?: false)
        }
        val parsedCode = versionCodeDraft.trim().toLongOrNull()
        val urlTrimmed = downloadUrlDraft.trim()
        val isUrlValid = urlTrimmed.isEmpty() || urlTrimmed.startsWith("https://")
        val canPublish = !publishingUpdate &&
            parsedCode != null && parsedCode in 1L..100_000L &&
            versionNameDraft.trim().isNotEmpty() &&
            isUrlValid

        AlertDialog(
            onDismissRequest = { if (!publishingUpdate) showPublishUpdateDialog = false },
            containerColor = Color(0xFF1A1829),
            titleContentColor = TextOnDark,
            textContentColor = TextOnDarkMuted,
            title = { Text("Publish Firebase Update", fontWeight = FontWeight.ExtraBold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = versionCodeDraft,
                        onValueChange = { input ->
                            if (input.length <= 6 && input.all { it.isDigit() }) {
                                versionCodeDraft = input
                            }
                        },
                        label = { Text("Version Code (Current: ${BuildConfig.VERSION_CODE})") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = versionNameDraft,
                        onValueChange = { if (it.length <= 30) versionNameDraft = it },
                        label = { Text("Version Name (e.g. 1.1)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = downloadUrlDraft,
                        onValueChange = { if (it.length <= 500) downloadUrlDraft = it },
                        label = { Text("HTTPS APK / GitHub Release URL") },
                        isError = !isUrlValid,
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = releaseNotesDraft,
                        onValueChange = { if (it.length <= 1000) releaseNotesDraft = it },
                        label = { Text("What's New (Release Notes)") },
                        maxLines = 3,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { forceUpdateDraft = !forceUpdateDraft }
                    ) {
                        Checkbox(
                            checked = forceUpdateDraft,
                            onCheckedChange = { forceUpdateDraft = it },
                            colors = CheckboxDefaults.colors(checkedColor = CrimsonPrimary)
                        )
                        Text(
                            text = "Highlight as Urgent Update",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = TextOnDark,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    enabled = canPublish,
                    onClick = {
                        viewModel.publishAppUpdate(
                            versionCode = parsedCode ?: return@TextButton,
                            versionName = versionNameDraft,
                            releaseNotes = releaseNotesDraft,
                            downloadUrl = downloadUrlDraft,
                            forceUpdate = forceUpdateDraft,
                            onSuccess = { showPublishUpdateDialog = false }
                        )
                    },
                    modifier = Modifier.testTag("confirm_publish_update_button")
                ) {
                    if (publishingUpdate) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            color = BrightGold,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text("Publish", color = BrightGold, fontWeight = FontWeight.ExtraBold)
                    }
                }
            },
            dismissButton = {
                TextButton(
                    enabled = !publishingUpdate,
                    onClick = { showPublishUpdateDialog = false }
                ) {
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
    badgeText: String? = null,
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
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium.copy(
                        color = InkDark,
                        fontWeight = FontWeight.ExtraBold
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                if (badgeText != null) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Surface(
                        color = CrimsonPrimary,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = badgeText,
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = Color.White,
                                fontWeight = FontWeight.ExtraBold
                            ),
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                        )
                    }
                }
            }
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall.copy(color = InkSecondary),
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
