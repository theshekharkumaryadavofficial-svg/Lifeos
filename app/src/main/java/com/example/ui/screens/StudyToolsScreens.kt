package com.example.ui.screens

import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Quiz
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.UiState
import com.example.ui.components.CrimsonGlowButton
import com.example.ui.components.ErrorRetryCard
import com.example.ui.components.GlowingProgressRing
import com.example.ui.components.LifeGlassCard
import com.example.ui.theme.AmberGold
import com.example.ui.theme.BrightGold
import com.example.ui.theme.CrimsonDeep
import com.example.ui.theme.CrimsonGlow
import com.example.ui.theme.CrimsonPrimary
import com.example.ui.theme.EmeraldHealth
import com.example.ui.theme.GlassBorder
import com.example.ui.theme.MysticPurple
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.TextOnDark
import com.example.ui.theme.TextOnDarkMuted
import com.example.ui.viewmodel.LifeViewModel

@Composable
private fun SubScreenTopBar(
    title: String,
    subtitle: String,
    onBack: () -> Unit,
    trailing: (@Composable () -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            onClick = onBack,
            modifier = Modifier
                .size(48.dp)
                .testTag("subscreen_back_button")
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                contentDescription = "Back",
                tint = TextOnDark
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge.copy(color = TextOnDark)
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall.copy(color = TextOnDarkMuted)
            )
        }
        trailing?.invoke()
    }
}

// ------------------------------------------------------------
// 1. REAL GEMINI AI TUTOR STREAMING CHAT SCREEN
// ------------------------------------------------------------

@Composable
fun AiTutorScreen(
    viewModel: LifeViewModel,
    onBack: () -> Unit
) {
    BackHandler(onBack = onBack)

    val messages by viewModel.chatMessages.collectAsStateWithLifecycle()
    val loading by viewModel.aiLoading.collectAsStateWithLifecycle()
    val error by viewModel.aiError.collectAsStateWithLifecycle()
    var input by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    val quickPrompts = listOf(
        "Explain Ohm's Law simply",
        "Make a 6-hour study + workout timetable",
        "NDA Maths trigonometry tips",
        "Morning Naam Jap & focus routine"
    )

    LaunchedEffect(messages.size, messages.lastOrNull()?.text?.length) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.lastIndex)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Color(0xFF1B1230), ObsidianBg)))
            .navigationBarsPadding()
            .imePadding()
    ) {
        SubScreenTopBar(
            title = "LifeOS AI Tutor",
            subtitle = "Powered by Gemini • Study, Fitness & Discipline",
            onBack = onBack
        )

        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(quickPrompts) { prompt ->
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(MysticPurple.copy(alpha = 0.22f))
                        .border(1.dp, MysticPurple.copy(alpha = 0.5f), RoundedCornerShape(50))
                        .clickable(enabled = !loading) { viewModel.sendAiMessage(prompt) }
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = prompt,
                        style = MaterialTheme.typography.labelMedium.copy(color = TextOnDark)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .widthIn(max = 600.dp)
                .align(Alignment.CenterHorizontally),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(messages, key = { it.id }) { msg ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = if (msg.fromUser) Arrangement.End else Arrangement.Start
                ) {
                    Box(
                        modifier = Modifier
                            .widthIn(max = 310.dp)
                            .clip(
                                RoundedCornerShape(
                                    topStart = 18.dp,
                                    topEnd = 18.dp,
                                    bottomStart = if (msg.fromUser) 18.dp else 4.dp,
                                    bottomEnd = if (msg.fromUser) 4.dp else 18.dp
                                )
                            )
                            .background(
                                if (msg.fromUser) {
                                    Brush.horizontalGradient(listOf(CrimsonPrimary, CrimsonDeep))
                                } else {
                                    Brush.horizontalGradient(listOf(Color(0xFF1F1D33), Color(0xFF19172A)))
                                }
                            )
                            .border(
                                1.dp,
                                if (msg.fromUser) CrimsonGlow.copy(alpha = 0.5f) else GlassBorder,
                                RoundedCornerShape(18.dp)
                            )
                            .padding(horizontal = 14.dp, vertical = 11.dp)
                    ) {
                        if (msg.streaming && msg.text.isBlank()) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    color = BrightGold,
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Thinking...",
                                    style = MaterialTheme.typography.bodySmall.copy(color = TextOnDarkMuted)
                                )
                            }
                        } else {
                            Text(
                                text = msg.text,
                                style = MaterialTheme.typography.bodyMedium.copy(color = TextOnDark)
                            )
                        }
                    }
                }
            }

            if (error != null) {
                item {
                    ErrorRetryCard(message = error!!)
                }
            }
        }

        // Input Composer
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xEE131120))
                .border(1.dp, GlassBorder)
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = input,
                onValueChange = { input = it },
                placeholder = { Text("Ask any study or life question...", color = TextOnDarkMuted) },
                modifier = Modifier
                    .weight(1f)
                    .testTag("ai_chat_input"),
                shape = RoundedCornerShape(24.dp),
                maxLines = 3,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = CrimsonGlow,
                    unfocusedBorderColor = GlassBorder,
                    focusedTextColor = TextOnDark,
                    unfocusedTextColor = TextOnDark
                )
            )
            Spacer(modifier = Modifier.width(8.dp))
            IconButton(
                onClick = {
                    val prompt = input.trim()
                    if (prompt.isNotEmpty()) {
                        input = ""
                        viewModel.sendAiMessage(prompt)
                    }
                },
                enabled = !loading && input.isNotBlank(),
                modifier = Modifier
                    .size(50.dp)
                    .clip(CircleShape)
                    .background(if (!loading && input.isNotBlank()) CrimsonPrimary else Color(0xFF2D2A3D))
                    .border(
                        1.6.dp,
                        if (!loading && input.isNotBlank()) Color.White.copy(alpha = 0.55f) else GlassBorder,
                        CircleShape
                    )
                    .testTag("ai_send_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.Send,
                    contentDescription = "Send prompt",
                    tint = TextOnDark
                )
            }
        }
    }
}

// ------------------------------------------------------------
// 2. REAL GEMINI STRUCTURED JSON QUIZ TIME SCREEN
// ------------------------------------------------------------

@Composable
fun QuizTimeScreen(
    viewModel: LifeViewModel,
    onBack: () -> Unit
) {
    BackHandler(onBack = onBack)

    val quiz by viewModel.quizState.collectAsStateWithLifecycle()
    var topicInput by remember { mutableStateOf("") }
    val quickTopics = listOf("Mathematics Algebra", "Electricity & Circuits", "NDA General Science", "Indian History")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Color(0xFF1F1435), ObsidianBg)))
            .navigationBarsPadding()
    ) {
        SubScreenTopBar(
            title = "Quiz Time",
            subtitle = "AI-generated 5-question practice tests",
            onBack = onBack
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 600.dp)
                .align(Alignment.CenterHorizontally),
            contentPadding = PaddingValues(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            if (quiz.loading) {
                item {
                    LifeGlassCard(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            CircularProgressIndicator(color = CrimsonGlow)
                            Spacer(modifier = Modifier.height(14.dp))
                            Text(
                                text = "Generating ${quiz.topic} quiz...",
                                style = MaterialTheme.typography.titleMedium.copy(color = TextOnDark)
                            )
                        }
                    }
                }
            } else if (quiz.error != null) {
                item {
                    ErrorRetryCard(
                        message = quiz.error!!,
                        onRetry = { viewModel.generateQuiz(quiz.topic) }
                    )
                }
            } else if (quiz.questions.isEmpty()) {
                item {
                    LifeGlassCard(modifier = Modifier.fillMaxWidth()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Rounded.Quiz,
                                contentDescription = null,
                                tint = MysticPurple,
                                modifier = Modifier.size(28.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Pick Any Topic to Test Yourself",
                                style = MaterialTheme.typography.titleMedium.copy(color = TextOnDark)
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedTextField(
                            value = topicInput,
                            onValueChange = { topicInput = it },
                            label = { Text("Enter topic (e.g. Trigonometry, Ohm's Law)") },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("quiz_topic_input")
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        CrimsonGlowButton(
                            text = "Generate 5-Question Quiz",
                            icon = Icons.Rounded.AutoAwesome,
                            enabled = topicInput.trim().isNotEmpty(),
                            testTagName = "start_quiz_button",
                            gradientColors = listOf(MysticPurple, CrimsonPrimary),
                            onClick = { viewModel.generateQuiz(topicInput) }
                        )
                    }
                }
                item {
                    Text(
                        text = "Quick Exam Topics",
                        style = MaterialTheme.typography.titleSmall.copy(color = BrightGold)
                    )
                }
                items(quickTopics) { preset ->
                    LifeGlassCard(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = { viewModel.generateQuiz(preset) }
                    ) {
                        Text(
                            text = preset,
                            style = MaterialTheme.typography.titleMedium.copy(color = TextOnDark)
                        )
                    }
                }
            } else if (quiz.finished) {
                item {
                    LifeGlassCard(
                        modifier = Modifier.fillMaxWidth(),
                        borderColor = BrightGold.copy(alpha = 0.6f)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Quiz Completed!",
                                style = MaterialTheme.typography.headlineSmall.copy(color = BrightGold)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            GlowingProgressRing(
                                progressFraction = quiz.score.toFloat() / quiz.questions.size.coerceAtLeast(1),
                                centerText = "${quiz.score}/${quiz.questions.size}",
                                subtitleText = "Score",
                                size = 136.dp
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            CrimsonGlowButton(
                                text = "Take Another Quiz",
                                icon = Icons.Rounded.Refresh,
                                testTagName = "new_quiz_button",
                                onClick = { viewModel.resetQuiz() }
                            )
                        }
                    }
                }
            } else {
                val question = quiz.questions[quiz.currentIndex]
                item {
                    Text(
                        text = "Question ${quiz.currentIndex + 1} of ${quiz.questions.size} • ${quiz.topic}",
                        style = MaterialTheme.typography.labelLarge.copy(color = BrightGold)
                    )
                }
                item {
                    LifeGlassCard(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = question.question,
                            style = MaterialTheme.typography.titleLarge.copy(color = TextOnDark)
                        )
                    }
                }
                items(question.options.indices.toList()) { idx ->
                    val optionText = question.options[idx]
                    val answered = quiz.selectedOption != null
                    val isSelected = quiz.selectedOption == idx
                    val isCorrect = idx == question.correctIndex
                    val borderCol = when {
                        !answered -> GlassBorder
                        isCorrect -> EmeraldHealth
                        isSelected -> CrimsonGlow
                        else -> GlassBorder
                    }
                    val bgCol = when {
                        !answered -> Color(0xCC1B192E)
                        isCorrect -> EmeraldHealth.copy(alpha = 0.22f)
                        isSelected -> CrimsonPrimary.copy(alpha = 0.22f)
                        else -> Color(0x99161426)
                    }
                    LifeGlassCard(
                        modifier = Modifier.fillMaxWidth(),
                        borderColor = borderCol,
                        backgroundColor = bgCol,
                        onClick = { viewModel.answerQuizOption(idx) }
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "${'A' + idx}.",
                                style = MaterialTheme.typography.titleMedium.copy(color = BrightGold)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = optionText,
                                style = MaterialTheme.typography.bodyLarge.copy(color = TextOnDark),
                                modifier = Modifier.weight(1f)
                            )
                            if (answered && isCorrect) {
                                Icon(
                                    imageVector = Icons.Rounded.CheckCircle,
                                    contentDescription = "Correct",
                                    tint = EmeraldHealth
                                )
                            }
                        }
                    }
                }
                if (quiz.selectedOption != null) {
                    item {
                        LifeGlassCard(
                            modifier = Modifier.fillMaxWidth(),
                            borderColor = BrightGold.copy(alpha = 0.45f)
                        ) {
                            Text(
                                text = "Explanation",
                                style = MaterialTheme.typography.labelLarge.copy(color = BrightGold)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = question.explanation,
                                style = MaterialTheme.typography.bodyMedium.copy(color = TextOnDarkMuted)
                            )
                            Spacer(modifier = Modifier.height(14.dp))
                            CrimsonGlowButton(
                                text = if (quiz.currentIndex + 1 == quiz.questions.size) "Finish Quiz" else "Next Question",
                                testTagName = "next_quiz_button",
                                onClick = { viewModel.nextQuizQuestion() }
                            )
                        }
                    }
                }
            }
        }
    }
}

// ------------------------------------------------------------
// 3. CLOUD-SYNCED STUDY NOTES SCREEN
// ------------------------------------------------------------

@Composable
fun NotesScreen(
    viewModel: LifeViewModel,
    onBack: () -> Unit
) {
    BackHandler(onBack = onBack)

    val notesUi by viewModel.notesState.collectAsStateWithLifecycle()
    val notes = (notesUi as? UiState.Success)?.data.orEmpty()
    var showAddDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Color(0xFF211810), ObsidianBg)))
            .navigationBarsPadding()
    ) {
        SubScreenTopBar(
            title = "Study Notes",
            subtitle = "Synced in your cloud vault",
            onBack = onBack,
            trailing = {
                IconButton(
                    onClick = { showAddDialog = true },
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(AmberGold)
                        .border(1.8.dp, Color.White.copy(alpha = 0.55f), CircleShape)
                        .testTag("add_note_button")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Add,
                        contentDescription = "Add Note",
                        tint = ObsidianBg
                    )
                }
            }
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 600.dp)
                .align(Alignment.CenterHorizontally),
            contentPadding = PaddingValues(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (notes.isEmpty()) {
                item {
                    LifeGlassCard(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = { showAddDialog = true }
                    ) {
                        Text(
                            text = "No study notes yet",
                            style = MaterialTheme.typography.titleMedium.copy(color = TextOnDark)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Save key formulas, NDA revision points, or chapter summaries here.",
                            style = MaterialTheme.typography.bodySmall.copy(color = TextOnDarkMuted)
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        CrimsonGlowButton(
                            text = "Create First Note",
                            icon = Icons.Rounded.Add,
                            testTagName = "create_first_note_button",
                            onClick = { showAddDialog = true }
                        )
                    }
                }
            } else {
                items(notes, key = { it.id }) { note ->
                    LifeGlassCard(
                        modifier = Modifier.fillMaxWidth(),
                        borderColor = AmberGold.copy(alpha = 0.35f)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                if (note.subjectName.isNotBlank()) {
                                    Text(
                                        text = note.subjectName.uppercase(),
                                        style = MaterialTheme.typography.labelSmall.copy(color = BrightGold)
                                    )
                                }
                                Text(
                                    text = note.title,
                                    style = MaterialTheme.typography.titleMedium.copy(color = TextOnDark)
                                )
                            }
                            IconButton(onClick = { viewModel.deleteNote(note.id) }) {
                                Icon(
                                    imageVector = Icons.Rounded.DeleteOutline,
                                    contentDescription = "Delete note",
                                    tint = TextOnDarkMuted
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = note.content,
                            style = MaterialTheme.typography.bodyMedium.copy(color = TextOnDarkMuted)
                        )
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        var title by remember { mutableStateOf("") }
        var subject by remember { mutableStateOf("") }
        var content by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            containerColor = Color(0xFF1A1829),
            titleContentColor = TextOnDark,
            textContentColor = TextOnDarkMuted,
            title = { Text("New Study Note", fontWeight = FontWeight.ExtraBold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = title,
                        onValueChange = { if (it.length <= 120) title = it },
                        label = { Text("Title") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = subject,
                        onValueChange = { if (it.length <= 80) subject = it },
                        label = { Text("Subject (optional)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = content,
                        onValueChange = { if (it.length <= 8000) content = it },
                        label = { Text("Formula / Notes") },
                        minLines = 4,
                        maxLines = 8,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(
                    enabled = title.trim().isNotEmpty() && content.trim().isNotEmpty(),
                    onClick = {
                        viewModel.addNote(title, subject, content)
                        showAddDialog = false
                    }
                ) {
                    Text("Save", color = BrightGold, fontWeight = FontWeight.ExtraBold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) {
                    Text("Cancel", color = TextOnDarkMuted)
                }
            }
        )
    }
}

// ------------------------------------------------------------
// 4. STUDY FOCUS / POMODORO TIMER SCREEN
// ------------------------------------------------------------

@Composable
fun StudyTimerScreen(
    viewModel: LifeViewModel,
    onBack: () -> Unit
) {
    BackHandler(onBack = onBack)

    val timer by viewModel.focusTimer.collectAsStateWithLifecycle()
    val subjectsUi by viewModel.subjectsState.collectAsStateWithLifecycle()
    val subjects = (subjectsUi as? UiState.Success)?.data.orEmpty()

    val minutes = timer.remainingSeconds / 60
    val seconds = timer.remainingSeconds % 60
    val formatted = "%02d:%02d".format(minutes, seconds)
    val progress = if (timer.totalSeconds > 0) {
        1f - (timer.remainingSeconds.toFloat() / timer.totalSeconds.toFloat())
    } else 0f

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Color(0xFF0E221D), ObsidianBg)))
            .navigationBarsPadding()
    ) {
        SubScreenTopBar(
            title = "Study Focus Timer",
            subtitle = "Deep work logs directly to your Study Time & XP",
            onBack = onBack
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 16.dp)
                .widthIn(max = 520.dp)
                .align(Alignment.CenterHorizontally),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Duration presets
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                listOf(15, 25, 45, 60).forEach { mins ->
                    val selected = timer.totalSeconds == mins * 60
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(if (selected) EmeraldHealth else Color.White.copy(alpha = 0.08f))
                            .clickable(enabled = !timer.running) { viewModel.selectFocusDuration(mins) }
                            .padding(horizontal = 16.dp, vertical = 10.dp)
                    ) {
                        Text(
                            text = "${mins}m",
                            style = MaterialTheme.typography.labelLarge.copy(
                                color = if (selected) ObsidianBg else TextOnDark
                            )
                        )
                    }
                }
            }

            // Giant Glowing Ring
            GlowingProgressRing(
                progressFraction = progress.coerceIn(0.02f, 1f),
                centerText = formatted,
                subtitleText = if (timer.running) "Stay Locked In" else "Ready to Focus",
                size = 228.dp,
                strokeWidth = 14.dp,
                ringColors = listOf(EmeraldHealth, BrightGold, CrimsonGlow, EmeraldHealth)
            )

            // Optional Subject Tag Selector
            if (subjects.isNotEmpty()) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Link to Subject (optional)",
                        style = MaterialTheme.typography.labelMedium.copy(color = TextOnDarkMuted)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(subjects, key = { it.id }) { sub ->
                            val selected = timer.selectedSubjectId == sub.id
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(50))
                                    .background(if (selected) CrimsonPrimary else Color.White.copy(alpha = 0.08f))
                                    .clickable {
                                        viewModel.selectFocusSubject(if (selected) null else sub.id)
                                    }
                                    .padding(horizontal = 14.dp, vertical = 8.dp)
                            ) {
                                Text(
                                    text = sub.name,
                                    style = MaterialTheme.typography.labelMedium.copy(color = TextOnDark)
                                )
                            }
                        }
                    }
                }
            }

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                CrimsonGlowButton(
                    text = if (timer.running) "Pause Focus" else "Start Focus Session",
                    icon = if (timer.running) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                    testTagName = "toggle_focus_timer_button",
                    gradientColors = if (timer.running) {
                        listOf(AmberGold, CrimsonPrimary)
                    } else {
                        listOf(EmeraldHealth, Color(0xFF059669))
                    },
                    onClick = { viewModel.toggleFocusTimer() }
                )

                if (timer.remainingSeconds < timer.totalSeconds) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .clip(RoundedCornerShape(25.dp))
                            .background(AmberGold.copy(alpha = 0.18f))
                            .border(1.5.dp, BrightGold, RoundedCornerShape(25.dp))
                            .clickable { viewModel.completeFocusSessionEarly() },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Finish & Save Elapsed Study Time",
                            style = MaterialTheme.typography.titleSmall.copy(
                                color = BrightGold,
                                fontWeight = FontWeight.ExtraBold
                            ),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}
