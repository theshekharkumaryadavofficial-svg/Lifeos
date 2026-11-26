package com.example.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AiChatMessage
import com.example.data.AiTutorRepository
import com.example.data.LifeRepository
import com.example.data.LifeTask
import com.example.data.QuizQuestion
import com.example.data.SadhanaLog
import com.example.data.StudyNote
import com.example.data.StudySubject
import com.example.data.SubjectAccent
import com.example.data.TaskCategory
import com.example.data.UiState
import com.example.data.UserProfile
import com.example.data.WorkoutLog
import com.example.data.WorkoutType
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

private const val TAG = "LifeViewModel"
private const val STOP_TIMEOUT_MS = 5000L

data class FocusTimerState(
    val totalSeconds: Int = 25 * 60,
    val remainingSeconds: Int = 25 * 60,
    val running: Boolean = false,
    val selectedSubjectId: String? = null
)

data class QuizUiState(
    val topic: String = "",
    val loading: Boolean = false,
    val questions: List<QuizQuestion> = emptyList(),
    val currentIndex: Int = 0,
    val selectedOption: Int? = null,
    val score: Int = 0,
    val finished: Boolean = false,
    val error: String? = null
)

class LifeViewModel(
    private val repository: LifeRepository,
    private val aiRepository: AiTutorRepository,
    private val currentUserId: String,
    googleDisplayName: String?
) : ViewModel() {

    private fun <T> Flow<T>.toSafeUiState(label: String): StateFlow<UiState<T>> =
        this.map<T, UiState<T>> { UiState.Success(it) }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                val rootError = firestoreError ?: error
                Log.w(TAG, "Error observing $label", rootError)
                emit(UiState.Error(rootError.message ?: "Failed to load $label"))
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
                initialValue = UiState.Loading
            )

    val profileState: StateFlow<UiState<UserProfile?>> =
        repository.observeProfile(currentUserId).toSafeUiState("profile")

    val tasksState: StateFlow<UiState<List<LifeTask>>> =
        repository.observeTasks(currentUserId).toSafeUiState("tasks")

    val subjectsState: StateFlow<UiState<List<StudySubject>>> =
        repository.observeSubjects(currentUserId).toSafeUiState("subjects")

    val notesState: StateFlow<UiState<List<StudyNote>>> =
        repository.observeNotes(currentUserId).toSafeUiState("notes")

    val workoutsState: StateFlow<UiState<List<WorkoutLog>>> =
        repository.observeWorkouts(currentUserId).toSafeUiState("workouts")

    val sadhanaLogsState: StateFlow<UiState<List<SadhanaLog>>> =
        repository.observeSadhanaLogs(currentUserId).toSafeUiState("sadhana logs")

    private val _snackbarMessage = MutableStateFlow<String?>(null)
    val snackbarMessage: StateFlow<String?> = _snackbarMessage.asStateFlow()

    // Live Naam Jap session bead counter (syncs smoothly to Firestore)
    private val _sessionJapCount = MutableStateFlow(0)
    val sessionJapCount: StateFlow<Int> = _sessionJapCount.asStateFlow()

    // Study Pomodoro Focus Timer
    private val _focusTimer = MutableStateFlow(FocusTimerState())
    val focusTimer: StateFlow<FocusTimerState> = _focusTimer.asStateFlow()
    private var timerJob: Job? = null

    // AI Tutor streaming chat state
    private val _chatMessages = MutableStateFlow(
        listOf(
            AiChatMessage(
                id = "welcome",
                fromUser = false,
                text = "Namaste! I am your LifeOS AI Tutor & Coach. Ask me any Study doubt (Maths, Physics, Electricity, NDA), or ask for a Fitness, Study, or Daily Discipline plan."
            )
        )
    )
    val chatMessages: StateFlow<List<AiChatMessage>> = _chatMessages.asStateFlow()

    private val _aiLoading = MutableStateFlow(false)
    val aiLoading: StateFlow<Boolean> = _aiLoading.asStateFlow()

    private val _aiError = MutableStateFlow<String?>(null)
    val aiError: StateFlow<String?> = _aiError.asStateFlow()

    // AI Quiz Generator state
    private val _quizState = MutableStateFlow(QuizUiState())
    val quizState: StateFlow<QuizUiState> = _quizState.asStateFlow()

    // AI Progress Insight state
    private val _progressInsight = MutableStateFlow<UiState<String>?>(null)
    val progressInsight: StateFlow<UiState<String>?> = _progressInsight.asStateFlow()

    init {
        viewModelScope.launch {
            val res = repository.ensureUserProfile(currentUserId, googleDisplayName)
            res.onFailure {
                _snackbarMessage.value = it.localizedMessage ?: "Could not sync profile."
            }
        }
    }

    fun clearSnackbar() {
        _snackbarMessage.value = null
    }

    private fun currentProfileOrDefault(): UserProfile =
        (profileState.value as? UiState.Success)?.data ?: UserProfile(userId = currentUserId)

    // --- PROFILE ACTIONS ---

    fun updateDailyGoal(newGoal: String) {
        val clean = newGoal.trim().take(120)
        if (clean.isEmpty()) return
        viewModelScope.launch {
            repository.updateProfileFields(currentUserId, mapOf("dailyGoalText" to clean))
                .onSuccess { _snackbarMessage.value = "Daily goal updated!" }
                .onFailure { _snackbarMessage.value = it.localizedMessage ?: "Failed to update goal." }
        }
    }

    fun updateProfileIdentity(displayName: String, tagline: String) {
        val cleanName = displayName.trim().take(80)
        val cleanTag = tagline.trim().take(100)
        if (cleanName.isEmpty()) return
        viewModelScope.launch {
            repository.updateProfileFields(
                currentUserId,
                mapOf("displayName" to cleanName, "tagline" to cleanTag)
            ).onSuccess { _snackbarMessage.value = "Profile saved!" }
                .onFailure { _snackbarMessage.value = it.localizedMessage ?: "Failed to save profile." }
        }
    }

    fun setMotivationMode(enabled: Boolean) {
        viewModelScope.launch {
            repository.updateProfileFields(currentUserId, mapOf("motivationMode" to enabled))
                .onFailure { _snackbarMessage.value = it.localizedMessage ?: "Failed to update setting." }
        }
    }

    fun adjustWaterGlasses(delta: Int) {
        val profile = currentProfileOrDefault()
        val updated = (profile.todayWaterGlasses + delta).coerceIn(0L, 50L)
        viewModelScope.launch {
            repository.updateProfileFields(currentUserId, mapOf("todayWaterGlasses" to updated))
                .onFailure { _snackbarMessage.value = it.localizedMessage ?: "Could not update water." }
        }
    }

    fun updateSleepHours(hours: Double) {
        val bounded = hours.coerceIn(0.0, 24.0)
        viewModelScope.launch {
            repository.updateProfileFields(currentUserId, mapOf("todaySleepHours" to bounded))
                .onSuccess { _snackbarMessage.value = "Sleep logged: ${"%.1f".format(bounded)} hrs" }
                .onFailure { _snackbarMessage.value = it.localizedMessage ?: "Could not update sleep." }
        }
    }

    fun setActiveMantra(mantra: String) {
        val clean = mantra.trim().take(60)
        if (clean.isEmpty()) return
        viewModelScope.launch {
            repository.updateProfileFields(currentUserId, mapOf("activeMantra" to clean))
                .onFailure { _snackbarMessage.value = it.localizedMessage ?: "Could not switch mantra." }
        }
    }

    // --- NAAM JAP ACTIONS ---

    fun incrementSessionJap(): Boolean {
        val next = _sessionJapCount.value + 1
        _sessionJapCount.value = next
        return next % 108 == 0
    }

    fun resetSessionJap() {
        _sessionJapCount.value = 0
    }

    fun saveNaamJapSession(meditationMinutes: Long = 0L) {
        val count = _sessionJapCount.value.toLong()
        if (count <= 0L) {
            _snackbarMessage.value = "Tap the Mala counter first to record beads."
            return
        }
        val profile = currentProfileOrDefault()
        val malasEarned = count / 108L
        val newTodayCount = (profile.todayNaamJapCount + count).coerceAtMost(1_000_000L)
        val newTotalCount = (profile.totalNaamJapCount + count).coerceAtMost(100_000_000L)
        val newTodayMalas = (newTodayCount / 108L).coerceAtMost(10_000L)
        val earnedXp = (count / 10L).coerceAtLeast(10L)
        val newXp = (profile.xp + earnedXp).coerceAtMost(10_000_000L)
        val newLevel = (1L + newXp / 250L).coerceIn(1L, 999L)

        viewModelScope.launch {
            val logRes = repository.logSadhanaSession(
                userId = currentUserId,
                mantra = profile.activeMantra,
                count = count,
                malas = malasEarned,
                meditationMinutes = meditationMinutes
            )
            val profRes = repository.updateProfileFields(
                currentUserId,
                mapOf(
                    "todayNaamJapCount" to newTodayCount,
                    "totalNaamJapCount" to newTotalCount,
                    "todayMalas" to newTodayMalas,
                    "xp" to newXp,
                    "level" to newLevel
                )
            )
            if (logRes.isSuccess && profRes.isSuccess) {
                _sessionJapCount.value = 0
                _snackbarMessage.value = "Saved $count ${profile.activeMantra} beads (+$earnedXp XP)"
            } else {
                _snackbarMessage.value = "Could not save Naam Jap session. Please retry."
            }
        }
    }

    // --- TASKS ACTIONS ---

    fun addTask(title: String, category: TaskCategory, priority: String, dueDate: String) {
        viewModelScope.launch {
            repository.addTask(currentUserId, title, category, priority, dueDate)
                .onSuccess { _snackbarMessage.value = "Task added to your plan!" }
                .onFailure { _snackbarMessage.value = it.localizedMessage ?: "Failed to add task." }
        }
    }

    fun toggleTask(task: LifeTask) {
        val nextCompleted = !task.completed
        viewModelScope.launch {
            repository.toggleTaskCompleted(currentUserId, task.id, nextCompleted)
                .onSuccess {
                    if (nextCompleted) {
                        repository.awardXpAndStudyMinutes(currentUserId, currentProfileOrDefault(), 20L)
                    }
                }
                .onFailure { _snackbarMessage.value = it.localizedMessage ?: "Failed to update task." }
        }
    }

    fun deleteTask(taskId: String) {
        viewModelScope.launch {
            repository.deleteTask(currentUserId, taskId)
                .onFailure { _snackbarMessage.value = it.localizedMessage ?: "Failed to delete task." }
        }
    }

    // --- SUBJECTS & NOTES ACTIONS ---

    fun addSubject(name: String, progress: Long, accent: SubjectAccent) {
        viewModelScope.launch {
            repository.addSubject(currentUserId, name, progress, accent)
                .onSuccess { _snackbarMessage.value = "Subject added!" }
                .onFailure { _snackbarMessage.value = it.localizedMessage ?: "Failed to add subject." }
        }
    }

    fun updateSubjectProgress(subject: StudySubject, newProgress: Long) {
        viewModelScope.launch {
            repository.updateSubjectProgress(
                currentUserId,
                subject.id,
                newProgress,
                subject.studyMinutes
            ).onFailure {
                _snackbarMessage.value = it.localizedMessage ?: "Failed to update subject."
            }
        }
    }

    fun deleteSubject(subjectId: String) {
        viewModelScope.launch {
            repository.deleteSubject(currentUserId, subjectId)
                .onFailure { _snackbarMessage.value = it.localizedMessage ?: "Failed to delete subject." }
        }
    }

    fun addNote(title: String, subjectName: String, content: String) {
        viewModelScope.launch {
            repository.addNote(currentUserId, title, subjectName, content)
                .onSuccess { _snackbarMessage.value = "Study note saved!" }
                .onFailure { _snackbarMessage.value = it.localizedMessage ?: "Failed to save note." }
        }
    }

    fun deleteNote(noteId: String) {
        viewModelScope.launch {
            repository.deleteNote(currentUserId, noteId)
                .onFailure { _snackbarMessage.value = it.localizedMessage ?: "Failed to delete note." }
        }
    }

    // --- WORKOUTS ACTIONS ---

    fun logWorkout(
        title: String,
        type: WorkoutType,
        durationMinutes: Long,
        distanceKm: Double,
        caloriesBurned: Long
    ) {
        viewModelScope.launch {
            repository.addWorkout(
                currentUserId,
                title,
                type,
                durationMinutes,
                distanceKm,
                caloriesBurned
            ).onSuccess {
                val xpGain = (durationMinutes * 2L).coerceIn(15L, 200L)
                repository.awardXpAndStudyMinutes(currentUserId, currentProfileOrDefault(), xpGain)
                _snackbarMessage.value = "Workout logged! +$xpGain XP"
            }.onFailure {
                _snackbarMessage.value = it.localizedMessage ?: "Failed to log workout."
            }
        }
    }

    fun deleteWorkout(workoutId: String) {
        viewModelScope.launch {
            repository.deleteWorkout(currentUserId, workoutId)
                .onFailure { _snackbarMessage.value = it.localizedMessage ?: "Failed to delete workout." }
        }
    }

    // --- STUDY FOCUS TIMER ---

    fun selectFocusDuration(minutes: Int) {
        timerJob?.cancel()
        val secs = minutes.coerceIn(1, 180) * 60
        _focusTimer.update { it.copy(totalSeconds = secs, remainingSeconds = secs, running = false) }
    }

    fun selectFocusSubject(subjectId: String?) {
        _focusTimer.update { it.copy(selectedSubjectId = subjectId) }
    }

    fun toggleFocusTimer() {
        val current = _focusTimer.value
        if (current.running) {
            timerJob?.cancel()
            _focusTimer.update { it.copy(running = false) }
        } else {
            _focusTimer.update { it.copy(running = true) }
            timerJob?.cancel()
            timerJob = viewModelScope.launch {
                while (_focusTimer.value.remainingSeconds > 0 && _focusTimer.value.running) {
                    delay(1000L)
                    _focusTimer.update { state ->
                        val next = (state.remainingSeconds - 1).coerceAtLeast(0)
                        state.copy(remainingSeconds = next)
                    }
                }
                if (_focusTimer.value.remainingSeconds == 0) {
                    _focusTimer.update { it.copy(running = false) }
                    completeFocusSession(_focusTimer.value.totalSeconds / 60)
                }
            }
        }
    }

    fun completeFocusSessionEarly() {
        val state = _focusTimer.value
        timerJob?.cancel()
        val elapsedMinutes = ((state.totalSeconds - state.remainingSeconds) / 60).coerceAtLeast(1)
        _focusTimer.update { it.copy(running = false, remainingSeconds = it.totalSeconds) }
        completeFocusSession(elapsedMinutes)
    }

    private fun completeFocusSession(minutes: Int) {
        val mins = minutes.toLong().coerceAtLeast(1L)
        val xpGain = (mins * 2L).coerceIn(10L, 300L)
        val subjectId = _focusTimer.value.selectedSubjectId
        val subjects = (subjectsState.value as? UiState.Success)?.data.orEmpty()
        val matchedSubject = subjects.firstOrNull { it.id == subjectId }

        viewModelScope.launch {
            repository.awardXpAndStudyMinutes(
                currentUserId,
                currentProfileOrDefault(),
                addedXp = xpGain,
                addedStudyMinutes = mins
            )
            if (matchedSubject != null) {
                val boostedProgress = (matchedSubject.progressPercent + 3L).coerceAtMost(100L)
                repository.updateSubjectProgress(
                    currentUserId,
                    matchedSubject.id,
                    boostedProgress,
                    matchedSubject.studyMinutes + mins
                )
            }
            _snackbarMessage.value = "Focus session logged: ${mins}m (+$xpGain XP)!"
        }
    }

    // --- AI TUTOR STREAMING ---

    fun sendAiMessage(prompt: String) {
        val clean = prompt.trim()
        if (clean.isEmpty() || _aiLoading.value) return
        val priorHistory = _chatMessages.value
        val userMsg = AiChatMessage(id = UUID.randomUUID().toString(), fromUser = true, text = clean)
        val botId = UUID.randomUUID().toString()
        _chatMessages.value = priorHistory + userMsg + AiChatMessage(
            id = botId,
            fromUser = false,
            text = "",
            streaming = true
        )
        _aiLoading.value = true
        _aiError.value = null

        viewModelScope.launch {
            try {
                aiRepository.streamTutorReply(priorHistory, clean).collect { partial ->
                    _chatMessages.update { list ->
                        list.map { if (it.id == botId) it.copy(text = partial, streaming = true) else it }
                    }
                }
                _chatMessages.update { list ->
                    list.map { if (it.id == botId) it.copy(streaming = false) else it }
                }
            } catch (e: TimeoutCancellationException) {
                Log.e(TAG, "AI Tutor timed out", e)
                removeEmptyStreamingBubble(botId)
                _aiError.value = "AI Tutor timed out after 60 seconds. Please try again."
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "AI Tutor failed", e)
                removeEmptyStreamingBubble(botId)
                _aiError.value = e.localizedMessage ?: "AI Tutor request failed."
            } finally {
                _aiLoading.value = false
            }
        }
    }

    private fun removeEmptyStreamingBubble(botId: String) {
        _chatMessages.update { list ->
            list.filterNot { it.id == botId && it.text.isBlank() }
                .map { if (it.id == botId) it.copy(streaming = false) else it }
        }
    }

    // --- AI QUIZ GENERATOR ---

    fun generateQuiz(topic: String) {
        val clean = topic.trim()
        if (clean.isEmpty()) return
        _quizState.value = QuizUiState(topic = clean, loading = true)
        viewModelScope.launch {
            try {
                val questions = aiRepository.generateQuiz(clean)
                _quizState.value = QuizUiState(
                    topic = clean,
                    loading = false,
                    questions = questions
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _quizState.value = QuizUiState(
                    topic = clean,
                    loading = false,
                    error = e.localizedMessage ?: "Could not generate quiz."
                )
            }
        }
    }

    fun answerQuizOption(optionIndex: Int) {
        val state = _quizState.value
        val currentQ = state.questions.getOrNull(state.currentIndex) ?: return
        if (state.selectedOption != null) return
        val isCorrect = optionIndex == currentQ.correctIndex
        _quizState.value = state.copy(
            selectedOption = optionIndex,
            score = if (isCorrect) state.score + 1 else state.score
        )
    }

    fun nextQuizQuestion() {
        val state = _quizState.value
        val nextIdx = state.currentIndex + 1
        if (nextIdx >= state.questions.size) {
            _quizState.value = state.copy(finished = true)
            val xp = (state.score * 15L).coerceAtLeast(10L)
            viewModelScope.launch {
                repository.awardXpAndStudyMinutes(currentUserId, currentProfileOrDefault(), xp)
            }
        } else {
            _quizState.value = state.copy(currentIndex = nextIdx, selectedOption = null)
        }
    }

    fun resetQuiz() {
        _quizState.value = QuizUiState()
    }

    // --- AI PROGRESS COACH ---

    fun requestProgressInsight() {
        val profile = currentProfileOrDefault()
        val tasks = (tasksState.value as? UiState.Success)?.data.orEmpty()
        val subjects = (subjectsState.value as? UiState.Success)?.data.orEmpty()
        val workouts = (workoutsState.value as? UiState.Success)?.data.orEmpty()
        val workoutMinutes = workouts.sumOf { it.durationMinutes }

        _progressInsight.value = UiState.Loading
        viewModelScope.launch {
            try {
                val text = aiRepository.generateProgressInsight(
                    profile = profile,
                    completedTasks = tasks.count { it.completed },
                    totalTasks = tasks.size,
                    subjects = subjects,
                    workoutMinutes = workoutMinutes
                )
                _progressInsight.value = UiState.Success(text)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _progressInsight.value = UiState.Error(
                    e.localizedMessage ?: "Could not analyze progress right now."
                )
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
    }
}
