package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Quiz
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.School
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.StudySubject
import com.example.data.StudyTimeSlot
import com.example.data.SubjectAccent
import com.example.data.UiState
import com.example.data.UserProfile
import com.example.focus.canDrawOverlays
import com.example.focus.hasUsageStatsPermission
import com.example.focus.isTimeSlotActiveNow
import com.example.focus.openOverlaySettings
import com.example.focus.openUsageAccessSettings
import com.example.focus.parsePackageCsv
import com.example.ui.components.ErrorRetryCard
import com.example.ui.components.GlowingProgressBar
import com.example.ui.components.LifeGlassCard
import com.example.ui.theme.AmberGold
import com.example.ui.theme.BrightGold
import com.example.ui.theme.CrimsonGlow
import com.example.ui.theme.CrimsonPrimary
import com.example.ui.theme.DeepTwilight
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.EmeraldHealth
import com.example.ui.theme.MysticPurple
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.RoyalBlueStudy
import com.example.ui.theme.TextOnDark
import com.example.ui.theme.TextOnDarkMuted
import com.example.ui.viewmodel.LifeViewModel

@Composable
fun StudyScreen(
    viewModel: LifeViewModel,
    onOpenAiTutor: () -> Unit,
    onOpenQuiz: () -> Unit,
    onOpenNotes: () -> Unit,
    onOpenTimer: () -> Unit,
    contentPadding: PaddingValues = PaddingValues()
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val profileState by viewModel.profileState.collectAsStateWithLifecycle()
    val timeSlotsState by viewModel.timeSlotsState.collectAsStateWithLifecycle()
    val subjectsState by viewModel.subjectsState.collectAsStateWithLifecycle()
    val liveFocus by viewModel.liveFocusSession.collectAsStateWithLifecycle()

    val profile = (profileState as? UiState.Success)?.data ?: UserProfile()
    val timeSlots = (timeSlotsState as? UiState.Success)?.data.orEmpty()
    val globalBlockedPackages = remember(profile.globalBlockedPackagesCsv) {
        parsePackageCsv(profile.globalBlockedPackagesCsv)
    }

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

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { /* Service runs regardless; permission enables visible notification on Android 13+ */ }

    var showAddSlotDialog by remember { mutableStateOf(false) }
    var editingSlot by remember { mutableStateOf<StudyTimeSlot?>(null) }
    var editingAppsForSlot by remember { mutableStateOf<StudyTimeSlot?>(null) }
    var showManageGlobalApps by remember { mutableStateOf(false) }
    var showDailyTargetDialog by remember { mutableStateOf(false) }
    var showAddSubjectDialog by remember { mutableStateOf(false) }

    fun ensureNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    val activeSlotNow = timeSlots.firstOrNull { isTimeSlotActiveNow(it) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(ObsidianBg)
            .statusBarsPadding(),
        contentPadding = PaddingValues(
            start = 16.dp,
            end = 16.dp,
            top = 12.dp,
            bottom = contentPadding.calculateBottomPadding() + 28.dp
        ),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. TOP HEADER (Gold Graduation Cap + "Study Center" + Crimson Circle "+")
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Rounded.School,
                        contentDescription = null,
                        tint = BrightGold,
                        modifier = Modifier.size(30.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Study Center",
                        color = TextOnDark,
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Black
                    )
                }

                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(listOf(CrimsonGlow, CrimsonPrimary))
                        )
                        .border(1.4.dp, Color(0x66FFFFFF), CircleShape)
                        .clickable { showAddSlotDialog = true }
                        .testTag("study_top_add_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Add,
                        contentDescription = "Add Study Time Slot",
                        tint = Color.White,
                        modifier = Modifier.size(26.dp)
                    )
                }
            }
        }

        // 2. FOCUS MODE HERO CARD
        item {
            FocusModeHeroCard(
                focusOn = profile.focusModeEnabled || liveFocus.isActive,
                hasActiveSlotNow = activeSlotNow != null,
                blockedCount = globalBlockedPackages.size,
                onToggleFocus = { nextOn ->
                    if (nextOn) ensureNotificationPermission()
                    viewModel.toggleFocusMode(nextOn)
                },
                onOpenFocusDisplay = { viewModel.openFocusOverlay() }
            )
        }

        // 3. TIME SLOT SETUP HEADER
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Schedule,
                        contentDescription = null,
                        tint = CrimsonGlow,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "Time Slot Setup",
                            color = TextOnDark,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Text(
                            text = "Set your study time and block distracting apps",
                            color = TextOnDarkMuted,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
                Spacer(modifier = Modifier.width(8.dp))
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(Color(0x22F59E0B))
                        .border(1.2.dp, BrightGold, RoundedCornerShape(50))
                        .clickable { showAddSlotDialog = true }
                        .padding(horizontal = 12.dp, vertical = 9.dp)
                        .testTag("add_time_slot_pill_button"),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Add,
                        contentDescription = null,
                        tint = BrightGold,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Add Slot",
                        color = BrightGold,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }
        }

        // Time Slot Cards
        when (val state = timeSlotsState) {
            is UiState.Loading -> {
                item { StudyLoadingPlaceholder(height = 120.dp) }
            }
            is UiState.Error -> {
                item { ErrorRetryCard(message = state.message) }
            }
            is UiState.Success -> {
                if (state.data.isEmpty()) {
                    item {
                        EmptyTimeSlotSetupCard(
                            onCreateSlot = { showAddSlotDialog = true },
                            onQuickCreateSlot = { title, sh, sm, eh, em ->
                                ensureNotificationPermission()
                                viewModel.saveTimeSlot(
                                    StudyTimeSlot(
                                        title = title,
                                        startHour = sh,
                                        startMinute = sm,
                                        endHour = eh,
                                        endMinute = em,
                                        enabled = true,
                                        pomodoroEnabled = true,
                                        countdownEnabled = true,
                                        blockedPackagesCsv = profile.globalBlockedPackagesCsv,
                                        motivationalQuote = profile.focusMotivationQuote
                                    )
                                )
                            }
                        )
                    }
                } else {
                    items(state.data, key = { it.id }) { slot ->
                        StudyTimeSlotCard(
                            slot = slot,
                            onToggleEnabled = {
                                if (!slot.enabled) ensureNotificationPermission()
                                viewModel.toggleTimeSlotEnabled(slot)
                            },
                            onEditSlot = { editingSlot = slot },
                            onManageSlotApps = { editingAppsForSlot = slot },
                            onTogglePomodoro = { viewModel.toggleTimeSlotPomodoro(slot) },
                            onToggleCountdown = { viewModel.toggleTimeSlotCountdown(slot) }
                        )
                    }
                }
            }
        }

        // 4. APP BLOCKING SECTION HEADER + STRIP
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Block,
                            contentDescription = null,
                            tint = CrimsonGlow,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "App Blocking",
                                color = TextOnDark,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                            Text(
                                text = "Select apps to block during study time",
                                color = TextOnDarkMuted,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(Color(0x22F59E0B))
                            .border(1.2.dp, BrightGold, RoundedCornerShape(50))
                            .clickable { showManageGlobalApps = true }
                            .padding(horizontal = 12.dp, vertical = 9.dp)
                            .testTag("manage_blocked_apps_button"),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Settings,
                            contentDescription = null,
                            tint = BrightGold,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = "Manage Apps",
                            color = BrightGold,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }

                // Compact Permission Guidance Banner if system permissions aren't granted yet
                if (!hasUsageAccess || !hasOverlayAccess) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color(0x24F59E0B))
                            .border(1.dp, BrightGold.copy(alpha = 0.6f), RoundedCornerShape(16.dp))
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Security,
                            contentDescription = null,
                            tint = BrightGold,
                            modifier = Modifier.size(18.dp)
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Enable System App Blocker",
                                color = TextOnDark,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                            Text(
                                text = "Allow Usage Access & Display Over Apps to auto-block restricted apps.",
                                color = TextOnDarkMuted,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                        if (!hasUsageAccess) {
                            OutlinedButton(
                                onClick = { openUsageAccessSettings(context) },
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                border = BorderStroke(1.dp, BrightGold),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.testTag("grant_usage_access_button")
                            ) {
                                Text("Usage", color = BrightGold, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold)
                            }
                        } else if (!hasOverlayAccess) {
                            OutlinedButton(
                                onClick = { openOverlaySettings(context) },
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                border = BorderStroke(1.dp, ElectricCyan),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.testTag("grant_overlay_access_button")
                            ) {
                                Text("Overlay", color = ElectricCyan, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold)
                            }
                        }
                    }
                }

                AppBlockingStripCard(
                    blockedPackages = globalBlockedPackages,
                    onRemoveApp = { viewModel.removeGlobalBlockedApp(it) },
                    onManageApps = { showManageGlobalApps = true }
                )
            }
        }

        // 5. DAILY STUDY TRACKER CARD (Real Room Data)
        item {
            DailyStudyTrackerCard(
                todayStudyMinutes = profile.todayStudyMinutes,
                dailyTargetMinutes = profile.dailyStudyTargetMinutes,
                totalStudyMinutes = profile.totalStudyMinutes,
                streakDays = profile.streakDays,
                onEditTarget = { showDailyTargetDialog = true }
            )
        }

        // 6. PRESERVED STUDY TOOLS (AI Tutor, Quiz Time, Notes & Docs, Study Timer)
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Study Tools",
                    color = TextOnDark,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.ExtraBold
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    StudyToolTile(
                        title = "AI Tutor",
                        subtitle = "Ask doubts",
                        icon = Icons.Rounded.AutoAwesome,
                        accent = BrightGold,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("study_tool_ai_tutor"),
                        onClick = onOpenAiTutor
                    )
                    StudyToolTile(
                        title = "Quiz Time",
                        subtitle = "AI MCQ test",
                        icon = Icons.Rounded.Quiz,
                        accent = CrimsonGlow,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("study_tool_quiz"),
                        onClick = onOpenQuiz
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    StudyToolTile(
                        title = "Notes & Docs",
                        subtitle = "Quick notes",
                        icon = Icons.Rounded.Description,
                        accent = ElectricCyan,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("study_tool_notes"),
                        onClick = onOpenNotes
                    )
                    StudyToolTile(
                        title = "Study Timer",
                        subtitle = "Custom focus",
                        icon = Icons.Rounded.Timer,
                        accent = EmeraldHealth,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("study_tool_timer"),
                        onClick = onOpenTimer
                    )
                }
            }
        }

        // 7. PRESERVED SUBJECTS SECTION
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Your Subjects",
                    color = TextOnDark,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.ExtraBold
                )
                TextButton(
                    onClick = { showAddSubjectDialog = true },
                    modifier = Modifier.testTag("study_add_subject_text_button")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Add,
                        contentDescription = null,
                        tint = BrightGold,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Add Subject", color = BrightGold, fontWeight = FontWeight.ExtraBold)
                }
            }
        }

        when (val state = subjectsState) {
            is UiState.Loading -> {
                item { StudyLoadingPlaceholder(height = 88.dp) }
            }
            is UiState.Error -> {
                item { ErrorRetryCard(message = state.message) }
            }
            is UiState.Success -> {
                if (state.data.isEmpty()) {
                    item {
                        LifeGlassCard(
                            modifier = Modifier.fillMaxWidth(),
                            borderColor = Color(0x33FFFFFF)
                        ) {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Rounded.MenuBook,
                                    contentDescription = null,
                                    tint = BrightGold,
                                    modifier = Modifier.size(34.dp)
                                )
                                Text(
                                    text = "No Subjects Added Yet",
                                    color = TextOnDark,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.ExtraBold
                                )
                                Text(
                                    text = "Track syllabus mastery for Physics, Maths, NDA, or Coding.",
                                    color = TextOnDarkMuted,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                Button(
                                    onClick = { showAddSubjectDialog = true },
                                    colors = ButtonDefaults.buttonColors(containerColor = CrimsonPrimary),
                                    shape = RoundedCornerShape(14.dp)
                                ) {
                                    Text("Add First Subject", fontWeight = FontWeight.ExtraBold)
                                }
                            }
                        }
                    }
                } else {
                    items(state.data, key = { it.id }) { subject ->
                        SubjectCard(
                            subject = subject,
                            onBoostProgress = {
                                val next = (subject.progressPercent + 5L).coerceAtMost(100L)
                                viewModel.updateSubjectProgress(subject, next)
                            },
                            onDelete = { viewModel.deleteSubject(subject.id) }
                        )
                    }
                }
            }
        }
    }

    // --- DIALOGS ---

    if (showAddSlotDialog) {
        AddEditTimeSlotDialog(
            initialSlot = null,
            defaultBlockedCsv = profile.globalBlockedPackagesCsv,
            defaultSessionNumber = timeSlots.size + 1,
            onDismiss = { showAddSlotDialog = false },
            onSave = { slot ->
                ensureNotificationPermission()
                viewModel.saveTimeSlot(slot)
            }
        )
    }

    editingSlot?.let { slot ->
        AddEditTimeSlotDialog(
            initialSlot = slot,
            defaultBlockedCsv = profile.globalBlockedPackagesCsv,
            defaultSessionNumber = 1,
            onDismiss = { editingSlot = null },
            onSave = { updated ->
                ensureNotificationPermission()
                viewModel.saveTimeSlot(updated)
            },
            onDelete = { id -> viewModel.deleteTimeSlot(id) }
        )
    }

    editingAppsForSlot?.let { slot ->
        ManageBlockedAppsDialog(
            title = "Blocked Apps • ${slot.title}",
            initialSelected = parsePackageCsv(slot.blockedPackagesCsv).toSet(),
            onDismiss = { editingAppsForSlot = null },
            onSave = { selected ->
                viewModel.updateTimeSlotBlockedApps(slot, selected)
                editingAppsForSlot = null
            }
        )
    }

    if (showManageGlobalApps) {
        ManageBlockedAppsDialog(
            title = "App Blocking List",
            initialSelected = globalBlockedPackages.toSet(),
            onDismiss = { showManageGlobalApps = false },
            onSave = { selected ->
                viewModel.updateGlobalBlockedApps(selected)
                showManageGlobalApps = false
            }
        )
    }

    if (showDailyTargetDialog) {
        DailyStudyTargetDialog(
            currentHours = (profile.dailyStudyTargetMinutes / 60L).toInt().coerceIn(1, 16),
            onDismiss = { showDailyTargetDialog = false },
            onSave = { hrs -> viewModel.updateDailyStudyTargetHours(hrs) }
        )
    }

    if (showAddSubjectDialog) {
        AddSubjectDialog(
            onDismiss = { showAddSubjectDialog = false },
            onAdd = { name, progress, accent ->
                viewModel.addSubject(name, progress, accent)
                showAddSubjectDialog = false
            }
        )
    }
}

@Composable
private fun EmptyTimeSlotSetupCard(
    onCreateSlot: () -> Unit,
    onQuickCreateSlot: (title: String, sh: Int, sm: Int, eh: Int, em: Int) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0xCC161229))
            .border(1.2.dp, MysticPurple.copy(alpha = 0.55f), RoundedCornerShape(20.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            text = "No Study Time Slots Scheduled Yet",
            color = TextOnDark,
            fontSize = 15.sp,
            fontWeight = FontWeight.ExtraBold
        )
        Text(
            text = "Add a slot so Pomodoro (25+5), Motivational Display, and App Blocking trigger automatically when study time begins.",
            color = TextOnDarkMuted,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = { onQuickCreateSlot("Study Session 1", 19, 0, 22, 0) },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, EmeraldHealth)
            ) {
                Text(
                    text = "+ 7PM–10PM",
                    color = EmeraldHealth,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }
            OutlinedButton(
                onClick = { onQuickCreateSlot("Study Session 2", 22, 0, 0, 0) },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, MysticPurple)
            ) {
                Text(
                    text = "+ 10PM–12AM",
                    color = Color(0xFFC4B5FD),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }
        }
        Button(
            onClick = onCreateSlot,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag("empty_create_custom_slot_button"),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = EmeraldHealth,
                contentColor = ObsidianBg
            )
        ) {
            Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Create Custom Time Slot", fontWeight = FontWeight.ExtraBold)
        }
    }
}

@Composable
private fun StudyToolTile(
    title: String,
    subtitle: String,
    icon: ImageVector,
    accent: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .background(Color(0xCC17132A))
            .border(1.dp, accent.copy(alpha = 0.45f), RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(accent.copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = accent,
                modifier = Modifier.size(22.dp)
            )
        }
        Spacer(modifier = Modifier.width(10.dp))
        Column {
            Text(
                text = title,
                color = TextOnDark,
                fontSize = 14.sp,
                fontWeight = FontWeight.ExtraBold
            )
            Text(
                text = subtitle,
                color = TextOnDarkMuted,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun SubjectCard(
    subject: StudySubject,
    onBoostProgress: () -> Unit,
    onDelete: () -> Unit
) {
    val accent = when (SubjectAccent.fromKey(subject.accentKey)) {
        SubjectAccent.CRIMSON -> CrimsonPrimary
        SubjectAccent.GOLD -> AmberGold
        SubjectAccent.CYAN -> ElectricCyan
        SubjectAccent.PURPLE -> MysticPurple
        SubjectAccent.GREEN -> EmeraldHealth
    }

    LifeGlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("subject_card_${subject.id}"),
        borderColor = accent.copy(alpha = 0.55f)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(accent.copy(alpha = 0.22f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.MenuBook,
                        contentDescription = null,
                        tint = accent,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = subject.name,
                        color = TextOnDark,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text(
                        text = "${subject.studyMinutes}m studied • ${subject.progressPercent}% mastery",
                        color = TextOnDarkMuted,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                TextButton(onClick = onBoostProgress) {
                    Text("+5%", color = accent, fontWeight = FontWeight.ExtraBold)
                }
                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Rounded.Delete,
                        contentDescription = "Delete subject",
                        tint = TextOnDarkMuted,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
            GlowingProgressBar(
                progressFraction = subject.progressPercent / 100f,
                accentKey = subject.accentKey
            )
        }
    }
}

@Composable
private fun StudyLoadingPlaceholder(height: androidx.compose.ui.unit.Dp) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(height)
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0x26FFFFFF))
    )
}

@Composable
private fun AddSubjectDialog(
    onDismiss: () -> Unit,
    onAdd: (String, Long, SubjectAccent) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var progress by remember { mutableFloatStateOf(20f) }
    var accent by remember { mutableStateOf(SubjectAccent.CRIMSON) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DeepTwilight,
        titleContentColor = TextOnDark,
        textContentColor = TextOnDark,
        title = { Text("Add Subject", fontWeight = FontWeight.ExtraBold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Subject Name (e.g. Physics, NDA)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = BrightGold,
                        unfocusedBorderColor = Color(0x44FFFFFF),
                        focusedTextColor = TextOnDark,
                        unfocusedTextColor = TextOnDark
                    )
                )
                Text(
                    text = "Current Mastery: ${progress.toInt()}%",
                    color = BrightGold,
                    fontWeight = FontWeight.Bold
                )
                Slider(
                    value = progress,
                    onValueChange = { progress = it },
                    valueRange = 0f..100f,
                    colors = SliderDefaults.colors(
                        thumbColor = BrightGold,
                        activeTrackColor = CrimsonPrimary
                    )
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(
                        SubjectAccent.CRIMSON to CrimsonPrimary,
                        SubjectAccent.GOLD to AmberGold,
                        SubjectAccent.CYAN to ElectricCyan,
                        SubjectAccent.PURPLE to MysticPurple,
                        SubjectAccent.GREEN to RoyalBlueStudy
                    ).forEach { (acc, col) ->
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(col)
                                .border(
                                    width = if (accent == acc) 2.5.dp else 0.dp,
                                    color = Color.White,
                                    shape = CircleShape
                                )
                                .clickable { accent = acc }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        onAdd(name.trim(), progress.toLong(), accent)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = CrimsonPrimary)
            ) {
                Text("Add Subject", fontWeight = FontWeight.ExtraBold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = TextOnDarkMuted, fontWeight = FontWeight.Bold)
            }
        }
    )
}
