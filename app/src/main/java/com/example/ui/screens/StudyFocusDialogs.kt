package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.DEFAULT_FOCUS_QUOTE
import com.example.data.StudyTimeSlot
import com.example.focus.BlockableAppInfo
import com.example.focus.POPULAR_DISTRACTING_APPS
import com.example.focus.STUDY_MOTIVATIONAL_QUOTES
import com.example.focus.canDrawOverlays
import com.example.focus.formatHourMinute12h
import com.example.focus.hasUsageStatsPermission
import com.example.focus.loadBlockableApps
import com.example.focus.openOverlaySettings
import com.example.focus.openUsageAccessSettings
import com.example.focus.parsePackageCsv
import com.example.focus.slotDurationLabel
import com.example.focus.toPackageCsv
import com.example.ui.theme.AmberGold
import com.example.ui.theme.BrightGold
import com.example.ui.theme.CrimsonGlow
import com.example.ui.theme.CrimsonPrimary
import com.example.ui.theme.DeepTwilight
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.EmeraldHealth
import com.example.ui.theme.MysticPurple
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.TextOnDark
import com.example.ui.theme.TextOnDarkMuted
import java.util.Calendar

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AddEditTimeSlotDialog(
    initialSlot: StudyTimeSlot?,
    defaultBlockedCsv: String,
    defaultSessionNumber: Int,
    onDismiss: () -> Unit,
    onSave: (StudyTimeSlot) -> Unit,
    onDelete: ((String) -> Unit)? = null
) {
    var title by remember {
        mutableStateOf(initialSlot?.title ?: "Study Session $defaultSessionNumber")
    }
    var startHour by remember { mutableIntStateOf(initialSlot?.startHour ?: 19) }
    var startMinute by remember { mutableIntStateOf(initialSlot?.startMinute ?: 0) }
    var endHour by remember { mutableIntStateOf(initialSlot?.endHour ?: 22) }
    var endMinute by remember { mutableIntStateOf(initialSlot?.endMinute ?: 0) }
    var enabled by remember { mutableStateOf(initialSlot?.enabled ?: true) }
    var pomodoroEnabled by remember { mutableStateOf(initialSlot?.pomodoroEnabled ?: true) }
    var countdownEnabled by remember { mutableStateOf(initialSlot?.countdownEnabled ?: true) }
    var quote by remember {
        mutableStateOf(initialSlot?.motivationalQuote?.ifBlank { DEFAULT_FOCUS_QUOTE } ?: DEFAULT_FOCUS_QUOTE)
    }
    var selectedPackages by remember {
        mutableStateOf(parsePackageCsv(initialSlot?.blockedPackagesCsv ?: defaultBlockedCsv).toSet())
    }
    var showSlotAppsPicker by remember { mutableStateOf(false) }

    if (showSlotAppsPicker) {
        ManageBlockedAppsDialog(
            title = "Apps Blocked for $title",
            initialSelected = selectedPackages,
            onDismiss = { showSlotAppsPicker = false },
            onSave = { updated ->
                selectedPackages = updated
                showSlotAppsPicker = false
            }
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DeepTwilight,
        titleContentColor = TextOnDark,
        textContentColor = TextOnDark,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = if (initialSlot == null) "Add Study Time Slot" else "Edit Time Slot",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 19.sp
                )
                if (initialSlot != null && onDelete != null) {
                    IconButton(
                        onClick = {
                            onDelete(initialSlot.id)
                            onDismiss()
                        },
                        modifier = Modifier.testTag("delete_time_slot_button")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Delete,
                            contentDescription = "Delete Time Slot",
                            tint = CrimsonGlow
                        )
                    }
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Session Name", fontWeight = FontWeight.Bold) },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("time_slot_title_input"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = EmeraldHealth,
                        unfocusedBorderColor = Color(0x44FFFFFF),
                        focusedTextColor = TextOnDark,
                        unfocusedTextColor = TextOnDark,
                        focusedLabelColor = EmeraldHealth,
                        unfocusedLabelColor = TextOnDarkMuted
                    )
                )

                // Quick Presets
                Text(
                    text = "Quick Presets",
                    color = TextOnDarkMuted,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    SlotPresetChip("7PM–10PM") {
                        startHour = 19; startMinute = 0; endHour = 22; endMinute = 0
                    }
                    SlotPresetChip("10PM–12AM") {
                        startHour = 22; startMinute = 0; endHour = 0; endMinute = 0
                    }
                    SlotPresetChip("6AM–8AM") {
                        startHour = 6; startMinute = 0; endHour = 8; endMinute = 0
                    }
                    SlotPresetChip("Start Now (1h)") {
                        val cal = Calendar.getInstance()
                        startHour = cal.get(Calendar.HOUR_OF_DAY)
                        startMinute = cal.get(Calendar.MINUTE)
                        cal.add(Calendar.HOUR_OF_DAY, 1)
                        endHour = cal.get(Calendar.HOUR_OF_DAY)
                        endMinute = cal.get(Calendar.MINUTE)
                    }
                }

                // Start & End Time Pickers
                TimeStepperRow(
                    label = "Start Time",
                    hour = startHour,
                    minute = startMinute,
                    onChange = { h, m -> startHour = h; startMinute = m }
                )
                TimeStepperRow(
                    label = "End Time",
                    hour = endHour,
                    minute = endMinute,
                    onChange = { h, m -> endHour = h; endMinute = m }
                )

                Text(
                    text = "Duration: ${slotDurationLabel(startHour, startMinute, endHour, endMinute)}",
                    color = BrightGold,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.ExtraBold
                )

                // Per-slot Apps Blocked button
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color(0x28EF233C))
                        .border(1.dp, CrimsonPrimary.copy(alpha = 0.6f), RoundedCornerShape(14.dp))
                        .clickable { showSlotAppsPicker = true }
                        .padding(horizontal = 14.dp, vertical = 12.dp)
                        .testTag("slot_select_apps_button"),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Rounded.Block,
                            contentDescription = null,
                            tint = CrimsonGlow,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "Apps to Block in This Slot",
                                color = TextOnDark,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                            Text(
                                text = "${selectedPackages.size} apps selected",
                                color = TextOnDarkMuted,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    Text(
                        text = "Select >",
                        color = BrightGold,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }

                // Pomodoro & Countdown Switches
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("Auto-Start Pomodoro (25+5)", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text("25 min deep focus + 5 min break", color = TextOnDarkMuted, fontSize = 12.sp)
                    }
                    Switch(
                        checked = pomodoroEnabled,
                        onCheckedChange = { pomodoroEnabled = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = EmeraldHealth
                        )
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("Slot Countdown Clock", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text("Live remaining slot countdown on screen", color = TextOnDarkMuted, fontSize = 12.sp)
                    }
                    Switch(
                        checked = countdownEnabled,
                        onCheckedChange = { countdownEnabled = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = MysticPurple
                        )
                    )
                }

                // Motivational Quote shown at top of display
                OutlinedTextField(
                    value = quote,
                    onValueChange = { quote = it },
                    label = { Text("Top Display Motivational Quote", fontWeight = FontWeight.Bold) },
                    minLines = 2,
                    maxLines = 3,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("time_slot_quote_input"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = BrightGold,
                        unfocusedBorderColor = Color(0x44FFFFFF),
                        focusedTextColor = TextOnDark,
                        unfocusedTextColor = TextOnDark,
                        focusedLabelColor = BrightGold,
                        unfocusedLabelColor = TextOnDarkMuted
                    )
                )

                TextButton(
                    onClick = {
                        val idx = STUDY_MOTIVATIONAL_QUOTES.indexOf(quote)
                        quote = STUDY_MOTIVATIONAL_QUOTES[(idx + 1) % STUDY_MOTIVATIONAL_QUOTES.size]
                    }
                ) {
                    Text("Cycle Inspiring Quote", color = ElectricCyan, fontWeight = FontWeight.ExtraBold)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val finalSlot = (initialSlot ?: StudyTimeSlot()).copy(
                        title = title.trim().ifEmpty { "Study Session $defaultSessionNumber" },
                        startHour = startHour,
                        startMinute = startMinute,
                        endHour = endHour,
                        endMinute = endMinute,
                        enabled = enabled,
                        pomodoroEnabled = pomodoroEnabled,
                        countdownEnabled = countdownEnabled,
                        blockedPackagesCsv = toPackageCsv(selectedPackages),
                        motivationalQuote = quote.trim().ifEmpty { DEFAULT_FOCUS_QUOTE }
                    )
                    onSave(finalSlot)
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = EmeraldHealth,
                    contentColor = ObsidianBg
                ),
                modifier = Modifier.testTag("save_time_slot_button")
            ) {
                Text("Save Slot", fontWeight = FontWeight.ExtraBold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = TextOnDarkMuted, fontWeight = FontWeight.Bold)
            }
        }
    )
}

@Composable
private fun SlotPresetChip(label: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(Color(0x288B5CF6))
            .border(1.dp, MysticPurple.copy(alpha = 0.65f), RoundedCornerShape(50))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Text(
            text = label,
            color = TextOnDark,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun TimeStepperRow(
    label: String,
    hour: Int,
    minute: Int,
    onChange: (Int, Int) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0x22FFFFFF))
            .border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(14.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Text(label, color = TextOnDarkMuted, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            Text(
                text = formatHourMinute12h(hour, minute),
                color = TextOnDark,
                fontSize = 16.sp,
                fontWeight = FontWeight.ExtraBold
            )
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            IconButton(
                onClick = {
                    val total = (hour * 60 + minute - 15 + 24 * 60) % (24 * 60)
                    onChange(total / 60, total % 60)
                },
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(Color(0x28FFFFFF))
            ) {
                Icon(Icons.Rounded.Remove, contentDescription = "Minus 15 minutes", tint = TextOnDark)
            }
            Text("-15m/+15m", color = TextOnDarkMuted, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            IconButton(
                onClick = {
                    val total = (hour * 60 + minute + 15) % (24 * 60)
                    onChange(total / 60, total % 60)
                },
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(Color(0x28FFFFFF))
            ) {
                Icon(Icons.Rounded.Add, contentDescription = "Plus 15 minutes", tint = TextOnDark)
            }
        }
    }
}

@Composable
fun ManageBlockedAppsDialog(
    title: String = "Manage Blocked Apps",
    initialSelected: Set<String>,
    onDismiss: () -> Unit,
    onSave: (Set<String>) -> Unit
) {
    val context = LocalContext.current
    val allApps = remember(context) { loadBlockableApps(context) }
    var selected by remember { mutableStateOf(initialSelected) }
    var searchQuery by remember { mutableStateOf("") }

    val hasUsage = hasUsageStatsPermission(context)
    val hasOverlay = canDrawOverlays(context)

    val filtered = remember(allApps, searchQuery) {
        val q = searchQuery.trim().lowercase()
        if (q.isEmpty()) allApps
        else allApps.filter {
            it.displayName.lowercase().contains(q) || it.packageName.lowercase().contains(q)
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DeepTwilight,
        titleContentColor = TextOnDark,
        textContentColor = TextOnDark,
        title = {
            Column {
                Text(title, fontWeight = FontWeight.ExtraBold, fontSize = 19.sp)
                Text(
                    text = "${selected.size} distracting apps selected",
                    color = BrightGold,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // System Permission Status Card
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color(0x2610B981))
                        .border(1.dp, EmeraldHealth.copy(alpha = 0.5f), RoundedCornerShape(14.dp))
                        .padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Rounded.Security,
                            contentDescription = null,
                            tint = EmeraldHealth,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Phone-Wide Blocking Permissions",
                            color = TextOnDark,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        PermissionPillButton(
                            label = if (hasUsage) "Usage Access ✓" else "Grant Usage Access",
                            granted = hasUsage,
                            modifier = Modifier.weight(1f),
                            onClick = { openUsageAccessSettings(context) }
                        )
                        PermissionPillButton(
                            label = if (hasOverlay) "Overlay ✓" else "Allow Overlay",
                            granted = hasOverlay,
                            modifier = Modifier.weight(1f),
                            onClick = { openOverlaySettings(context) }
                        )
                    }
                }

                // Search bar
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search installed or popular apps...", color = TextOnDarkMuted) },
                    leadingIcon = {
                        Icon(Icons.Rounded.Search, contentDescription = null, tint = TextOnDarkMuted)
                    },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("search_blocked_apps_input"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = ElectricCyan,
                        unfocusedBorderColor = Color(0x44FFFFFF),
                        focusedTextColor = TextOnDark,
                        unfocusedTextColor = TextOnDark
                    )
                )

                // Quick Select / Clear
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            selected = selected + POPULAR_DISTRACTING_APPS.take(7).map { it.packageName }
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, BrightGold.copy(alpha = 0.7f))
                    ) {
                        Text("Block Top 7", color = BrightGold, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold)
                    }
                    OutlinedButton(
                        onClick = { selected = emptySet() },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, Color(0x44FFFFFF))
                    ) {
                        Text("Clear All", color = TextOnDarkMuted, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 280.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(filtered, key = { it.packageName }) { app ->
                        val isChecked = app.packageName in selected
                        BlockableAppRow(
                            app = app,
                            checked = isChecked,
                            onToggle = {
                                selected = if (isChecked) {
                                    selected - app.packageName
                                } else {
                                    selected + app.packageName
                                }
                            }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(selected) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = EmeraldHealth,
                    contentColor = ObsidianBg
                ),
                modifier = Modifier.testTag("save_blocked_apps_button")
            ) {
                Icon(Icons.Rounded.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Save (${selected.size})", fontWeight = FontWeight.ExtraBold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = TextOnDarkMuted, fontWeight = FontWeight.Bold)
            }
        }
    )
}

@Composable
private fun PermissionPillButton(
    label: String,
    granted: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(if (granted) EmeraldHealth.copy(alpha = 0.25f) else AmberGold.copy(alpha = 0.2f))
            .border(
                1.dp,
                if (granted) EmeraldHealth else BrightGold,
                RoundedCornerShape(10.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        if (granted) {
            Icon(
                imageVector = Icons.Rounded.CheckCircle,
                contentDescription = null,
                tint = EmeraldHealth,
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
        }
        Text(
            text = label,
            color = if (granted) EmeraldHealth else BrightGold,
            fontSize = 11.sp,
            fontWeight = FontWeight.ExtraBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun BlockableAppRow(
    app: BlockableAppInfo,
    checked: Boolean,
    onToggle: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(if (checked) Color(0x28EF233C) else Color(0x18FFFFFF))
            .border(
                1.dp,
                if (checked) CrimsonPrimary.copy(alpha = 0.65f) else Color(0x22FFFFFF),
                RoundedCornerShape(14.dp)
            )
            .clickable(onClick = onToggle)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(RoundedCornerShape(11.dp))
                .background(Brush.linearGradient(listOf(app.brandColor, app.accentColor))),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = app.shortBadge,
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.Black
            )
        }
        Spacer(modifier = Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = app.displayName,
                color = TextOnDark,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = if (app.isInstalled) "Installed • Ready to block" else "Popular distraction",
                color = if (app.isInstalled) EmeraldHealth else TextOnDarkMuted,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
        Checkbox(
            checked = checked,
            onCheckedChange = { onToggle() },
            colors = CheckboxDefaults.colors(
                checkedColor = CrimsonPrimary,
                uncheckedColor = TextOnDarkMuted,
                checkmarkColor = Color.White
            )
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DailyStudyTargetDialog(
    currentHours: Int,
    onDismiss: () -> Unit,
    onSave: (Int) -> Unit
) {
    var hours by remember { mutableIntStateOf(currentHours.coerceIn(1, 16)) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DeepTwilight,
        titleContentColor = TextOnDark,
        textContentColor = TextOnDark,
        title = {
            Text("Daily Study Target", fontWeight = FontWeight.ExtraBold, fontSize = 19.sp)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "Set your daily study goal hours:",
                    color = TextOnDarkMuted,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(2, 4, 6, 8, 10, 12).forEach { preset ->
                        val selected = hours == preset
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (selected) EmeraldHealth else Color(0x22FFFFFF))
                                .border(
                                    1.2.dp,
                                    if (selected) EmeraldHealth else Color(0x33FFFFFF),
                                    RoundedCornerShape(12.dp)
                                )
                                .clickable { hours = preset }
                                .padding(horizontal = 16.dp, vertical = 10.dp)
                        ) {
                            Text(
                                text = "${preset}h",
                                color = if (selected) ObsidianBg else TextOnDark,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(hours)
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = EmeraldHealth,
                    contentColor = ObsidianBg
                )
            ) {
                Text("Save Target", fontWeight = FontWeight.ExtraBold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = TextOnDarkMuted, fontWeight = FontWeight.Bold)
            }
        }
    )
}
