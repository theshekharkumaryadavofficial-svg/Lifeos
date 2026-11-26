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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Calculate
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Psychology
import androidx.compose.material.icons.rounded.Quiz
import androidx.compose.material.icons.rounded.School
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.data.StudySubject
import com.example.data.SubjectAccent
import com.example.data.UiState
import com.example.ui.components.GlowingProgressBar
import com.example.ui.components.HeroBannerCard
import com.example.ui.components.LifeGlassCard
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

@Composable
fun StudyScreen(
    viewModel: LifeViewModel,
    onOpenAiTutor: () -> Unit,
    onOpenQuiz: () -> Unit,
    onOpenNotes: () -> Unit,
    onOpenTimer: () -> Unit,
    contentPadding: PaddingValues
) {
    val subjectsUi by viewModel.subjectsState.collectAsStateWithLifecycle()
    val subjects = (subjectsUi as? UiState.Success)?.data.orEmpty()

    var showAddSubjectDialog by remember { mutableStateOf(false) }
    var editingSubject by remember { mutableStateOf<StudySubject?>(null) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF1A1025), ObsidianBg, ObsidianBg)
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
            // Header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Rounded.School,
                            contentDescription = "Study Center",
                            tint = BrightGold,
                            modifier = Modifier.size(26.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Study Center",
                            style = MaterialTheme.typography.headlineMedium.copy(color = TextOnDark)
                        )
                    }
                    IconButton(
                        onClick = { showAddSubjectDialog = true },
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(CrimsonPrimary)
                            .border(1.8.dp, Color.White.copy(alpha = 0.55f), CircleShape)
                            .testTag("add_subject_button")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Add,
                            contentDescription = "Add Subject",
                            tint = TextOnDark
                        )
                    }
                }
            }

            // Hero Banner matching Screen 3 ("Learn Today, Lead Tomorrow")
            item {
                HeroBannerCard(
                    imageRes = R.drawable.img_banner_study_1795675590733,
                    title = "Learn Today,\nLead Tomorrow",
                    subtitle = "Master every chapter with AI Tutor, Quizzes & Deep Focus",
                    badge = "STUDY COMMAND"
                )
            }

            // 2x2 Study Tools Grid matching Screen 3
            item {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "Study Tools",
                        style = MaterialTheme.typography.titleLarge.copy(color = TextOnDark)
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        StudyToolCard(
                            title = "AI Tutor",
                            subtitle = "Ask Anything",
                            icon = Icons.Rounded.AutoAwesome,
                            accent = CrimsonGlow,
                            bgTint = Color(0x45EF233C),
                            onClick = onOpenAiTutor,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("tool_ai_tutor")
                        )
                        StudyToolCard(
                            title = "Quiz Time",
                            subtitle = "Test Yourself",
                            icon = Icons.Rounded.Quiz,
                            accent = MysticPurple,
                            bgTint = Color(0x458B5CF6),
                            onClick = onOpenQuiz,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("tool_quiz_time")
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        StudyToolCard(
                            title = "Notes & Docs",
                            subtitle = "Organize",
                            icon = Icons.Rounded.Description,
                            accent = AmberGold,
                            bgTint = Color(0x40F59E0B),
                            onClick = onOpenNotes,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("tool_notes")
                        )
                        StudyToolCard(
                            title = "Study Timer",
                            subtitle = "Focus Mode",
                            icon = Icons.Rounded.Timer,
                            accent = EmeraldHealth,
                            bgTint = Color(0x3D16B981),
                            onClick = onOpenTimer,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("tool_study_timer")
                        )
                    }
                }
            }

            // Your Subjects Header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Your Subjects",
                        style = MaterialTheme.typography.titleLarge.copy(color = TextOnDark)
                    )
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(AmberGold.copy(alpha = 0.2f))
                            .border(1.5.dp, BrightGold, RoundedCornerShape(12.dp))
                            .clickable { showAddSubjectDialog = true }
                            .padding(horizontal = 12.dp, vertical = 7.dp)
                    ) {
                        Text(
                            text = "+ Add Subject",
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
                        onClick = { showAddSubjectDialog = true }
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Rounded.Psychology,
                                contentDescription = null,
                                tint = BrightGold,
                                modifier = Modifier.size(28.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Track Your Subjects",
                                    style = MaterialTheme.typography.titleMedium.copy(color = TextOnDark)
                                )
                                Text(
                                    text = "Tap to add Mathematics, Physics, Electricity, NDA, or any subject you study.",
                                    style = MaterialTheme.typography.bodySmall.copy(color = TextOnDarkMuted)
                                )
                            }
                        }
                    }
                }
            } else {
                items(subjects, key = { it.id }) { subject ->
                    SubjectProgressCard(
                        subject = subject,
                        onEdit = { editingSubject = subject },
                        onDelete = { viewModel.deleteSubject(subject.id) }
                    )
                }
            }
        }
    }

    if (showAddSubjectDialog) {
        AddSubjectDialog(
            onDismiss = { showAddSubjectDialog = false },
            onSave = { name, progress, accent ->
                viewModel.addSubject(name, progress, accent)
                showAddSubjectDialog = false
            }
        )
    }

    editingSubject?.let { subject ->
        var sliderVal by remember(subject.id) { mutableFloatStateOf(subject.progressPercent.toFloat()) }
        AlertDialog(
            onDismissRequest = { editingSubject = null },
            containerColor = Color(0xFF1A1829),
            titleContentColor = TextOnDark,
            textContentColor = TextOnDarkMuted,
            title = { Text("Update ${subject.name}", fontWeight = FontWeight.ExtraBold) },
            text = {
                Column {
                    Text(
                        text = "Syllabus mastery: ${sliderVal.toInt()}%",
                        style = MaterialTheme.typography.titleMedium.copy(color = BrightGold)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Slider(
                        value = sliderVal,
                        onValueChange = { sliderVal = it },
                        valueRange = 0f..100f,
                        colors = SliderDefaults.colors(
                            thumbColor = CrimsonGlow,
                            activeTrackColor = CrimsonPrimary
                        )
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.updateSubjectProgress(subject, sliderVal.toLong())
                        editingSubject = null
                    }
                ) {
                    Text("Save", color = BrightGold, fontWeight = FontWeight.ExtraBold)
                }
            },
            dismissButton = {
                TextButton(onClick = { editingSubject = null }) {
                    Text("Cancel", color = TextOnDarkMuted)
                }
            }
        )
    }
}

@Composable
private fun StudyToolCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    accent: Color,
    bgTint: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(20.dp)
    Column(
        modifier = modifier
            .clip(shape)
            .background(
                Brush.verticalGradient(
                    listOf(bgTint, Color(0xF0171525))
                )
            )
            .border(1.8.dp, accent.copy(alpha = 0.8f), shape)
            .clickable(onClick = onClick)
            .padding(vertical = 18.dp, horizontal = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(accent.copy(alpha = 0.25f))
                .border(1.5.dp, accent, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = TextOnDark,
                modifier = Modifier.size(26.dp)
            )
        }
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium.copy(
                color = TextOnDark,
                fontWeight = FontWeight.ExtraBold
            ),
            maxLines = 1
        )
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodySmall.copy(color = TextOnDarkMuted),
            maxLines = 1
        )
    }
}

@Composable
private fun SubjectProgressCard(
    subject: StudySubject,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val accentTint = when (subject.accentKey) {
        "GOLD" -> BrightGold
        "CYAN" -> ElectricCyan
        "PURPLE" -> MysticPurple
        "GREEN" -> EmeraldHealth
        else -> CrimsonGlow
    }
    LifeGlassCard(
        modifier = Modifier.fillMaxWidth(),
        borderColor = accentTint.copy(alpha = 0.3f),
        onClick = onEdit
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(accentTint.copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.Calculate,
                    contentDescription = subject.name,
                    tint = accentTint,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = subject.name,
                        style = MaterialTheme.typography.titleMedium.copy(color = TextOnDark),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = "${subject.progressPercent}%",
                        style = MaterialTheme.typography.titleMedium.copy(
                            color = accentTint,
                            fontWeight = FontWeight.ExtraBold
                        )
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                GlowingProgressBar(
                    progressFraction = subject.progressPercent / 100f,
                    accentKey = subject.accentKey
                )
            }
            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Rounded.DeleteOutline,
                    contentDescription = "Delete subject",
                    tint = TextOnDarkMuted,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
private fun AddSubjectDialog(
    onDismiss: () -> Unit,
    onSave: (String, Long, SubjectAccent) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var progress by remember { mutableFloatStateOf(20f) }
    var selectedAccent by remember { mutableStateOf(SubjectAccent.CRIMSON) }
    val presets = listOf("Mathematics", "Electricity", "Physics", "NDA General", "Science")

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF1A1829),
        titleContentColor = TextOnDark,
        textContentColor = TextOnDarkMuted,
        title = { Text("Add Study Subject", fontWeight = FontWeight.ExtraBold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { if (it.length <= 80) name = it },
                    label = { Text("Subject Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    presets.take(3).forEach { preset ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(Color.White.copy(alpha = 0.08f))
                                .clickable { name = preset }
                                .padding(horizontal = 10.dp, vertical = 5.dp)
                        ) {
                            Text(
                                text = preset,
                                style = MaterialTheme.typography.labelSmall.copy(color = BrightGold)
                            )
                        }
                    }
                }
                Text(
                    text = "Current Progress: ${progress.toInt()}%",
                    style = MaterialTheme.typography.labelLarge.copy(color = TextOnDark)
                )
                Slider(
                    value = progress,
                    onValueChange = { progress = it },
                    valueRange = 0f..100f
                )
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    SubjectAccent.entries.forEach { accent ->
                        val color = when (accent) {
                            SubjectAccent.CRIMSON -> CrimsonGlow
                            SubjectAccent.GOLD -> BrightGold
                            SubjectAccent.CYAN -> ElectricCyan
                            SubjectAccent.PURPLE -> MysticPurple
                            SubjectAccent.GREEN -> EmeraldHealth
                        }
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(color)
                                .border(
                                    width = if (selectedAccent == accent) 3.dp else 0.dp,
                                    color = Color.White,
                                    shape = CircleShape
                                )
                                .clickable { selectedAccent = accent }
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = name.trim().isNotEmpty(),
                onClick = { onSave(name, progress.toLong(), selectedAccent) }
            ) {
                Text("Add", color = BrightGold, fontWeight = FontWeight.ExtraBold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = TextOnDarkMuted)
            }
        }
    )
}
