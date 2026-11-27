package com.example.ui.viewmodel

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AiChatMessage
import com.example.data.AiTutorRepository
import com.example.data.DEFAULT_FOCUS_QUOTE
import com.example.data.LifeRepository
import com.example.data.LifeTask
import com.example.data.QuizQuestion
import com.example.data.SadhanaLog
import com.example.data.StudyNote
import com.example.data.StudySubject
import com.example.data.StudyTimeSlot
import com.example.data.SubjectAccent
import com.example.data.TaskCategory
import com.example.data.UiState
import com.example.data.UserProfile
import com.example.data.WorkoutLog
import com.example.data.WorkoutType
import com.example.focus.FocusPrefsBridge
import com.example.focus.STUDY_MOTIVATIONAL_QUOTES
import com.example.focus.isTimeSlotActiveNow
import com.example.focus.parsePackageCsv
import com.example.focus.remainingSecondsInSlot
import com.example.focus.toPackageCsv
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
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.UUID

private const val TAG = "LifeViewModel"
private const val STOP_TIMEOUT_MS = 5000L
private const val POMODORO_FOCUS_SECONDS = 25 * 60
private const val POMODORO_BREAK_SECONDS = 5 * 60

data class FocusTimerState(
    val totalSeconds: Int = 25 * 60,
    val remainingSeconds: Int = 25 * 60,
    val running: Boolean = false,
    val selectedSubjectId: String? = null
)

data class LiveFocusSessionState(
    val isOverlayOpen: Boolean = false,
    val isActive: Boolean = false,
    val isManualFocus: Boolean = false,
    val activeSlotId: String? = null,
    val activeSlotTitle: String = "Deep Focus Session",
    val pomodoroEnabled: Boolean = true,
    val countdownEnabled: Boolean = true,
    val pomodoroIsBreak: Boolean = false,
    val pomodoroRemainingSeconds: Int = POMODORO_FOCUS_SECONDS,
    val pomodoroRunning: Boolean = false,
    val slotRemainingSeconds: Int = 0,
    val elapsedFocusSeconds: Int = 0,
    val completedPomodoros: Int = 0,
    val blockedPackages: List<String> = emptyList(),
    val motivationalQuote: String = DEFAULT_FOCUS_QUOTE
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
    private val appContext: Context,
    private val repository: LifeRepository,
    private val aiRepository: AiTutorRepository
) : ViewModel() {

    private fun <T> Flow<T>.toSafeUiState(label: String): StateFlow<UiState<T>> =
        this.map<T, UiState<T>> { UiState.Success(it) }
            .catch { error ->
                if (error is CancellationException) throw error
                Log.w(TAG, "Error observing $label", error)
                emit(UiState.Error(error.message ?: "Failed to load $label"))
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
                initialValue = UiState.Loading
            )

    val profileState: StateFlow<UiState<UserProfile?>> =
        repository.observeProfile().toSafeUiState("profile")

    val timeSlotsState: StateFlow<UiState<List<StudyTimeSlot>>> =
        repository.observeTimeSlots().toSafeUiState("time slots")

    val tasksState: StateFlow<UiState<List<LifeTask>>> =
        repository.observeTasks().toSafeUiState("tasks")

    val subjectsState: StateFlow<UiState<List<StudySubject>>> =
        repository.observeSubjects().toSafeUiState("subjects")

    val notesState: StateFlow<UiState<List<StudyNote>>> =
        repository.observeNotes().toSafeUiState("notes")

    val workoutsState: StateFlow<UiState<List<WorkoutLog>>> =
        repository.observeWorkouts().toSafeUiState("workouts")

    val sadhanaLogsState: StateFlow<UiState<List<SadhanaLog>>> =
        repository.observeSadhanaLogs().toSafeUiState("sadhana logs")

    private val _snackbarMessage = MutableStateFlow<String?>(null)
    val snackbarMessage: StateFlow<String?> = _snackbarMessage.asStateFlow()

    // Live Naam Jap session bead counter
    private val _sessionJapCount = MutableStateFlow(0)
    val sessionJapCount: StateFlow<Int> = _sessionJapCount.asStateFlow()

    // Classic Study Timer screen state
    private val _focusTimer = MutableStateFlow(FocusTimerState())
    val focusTimer: StateFlow<FocusTimerState> = _focusTimer.asStateFlow()
    private var timerJob: Job? = null

    // Live Focus Mode + Time Slot Shield state
    private val _liveFocusSession = MutableStateFlow(LiveFocusSessionState())
    val liveFocusSession: StateFlow<LiveFocusSessionState> = _liveFocusSession.asStateFlow()
    private var focusEngineJob: Job? = null
    private val autoOpenedSlotIds = hashSetOf<String>()

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
            repository.ensureUserProfile().onFailure {
                _snackbarMessage.value = it.localizedMessage ?: "Could not initialize local profile."
            }
        }
        observeFocusRulesForServiceSync()
        startLiveFocusEngineLoop()
    }

    private fun observeFocusRulesForServiceSync() {
        viewModelScope.launch {
            combine(
                repository.observeProfile(),
                repository.observeTimeSlots()
            ) { profile, slots ->
                Pair(profile ?: UserProfile(), slots)
            }.collect { (profile, slots) ->
                FocusPrefsBridge.saveSnapshot(
                    context = appContext,
                    manualFocusOn = profile.focusModeEnabled,
                    globalBlockedCsv = profile.globalBlockedPackagesCsv,
                    motivationalQuote = profile.focusMotivationQuote,
                    slots = slots
                )
            }
        }
    }

    private fun startLiveFocusEngineLoop() {
        focusEngineJob?.cancel()
        focusEngineJob = viewModelScope.launch {
            while (isActive) {
                val now = System.currentTimeMillis()
                val profile = currentProfileOrDefault()
                val slots = (timeSlotsState.value as? UiState.Success)?.data.orEmpty()
                val activeSlot = slots.firstOrNull { isTimeSlotActiveNow(it, now) }
                val shouldBeActive = profile.focusModeEnabled || activeSlot != null

                if (shouldBeActive) {
                    val combinedBlocked = buildList {
                        if (profile.focusModeEnabled) {
                            addAll(parsePackageCsv(profile.globalBlockedPackagesCsv))
                        }
                        if (activeSlot != null) {
                            val slotPkgs = parsePackageCsv(activeSlot.blockedPackagesCsv)
                            if (slotPkgs.isNotEmpty()) {
                                addAll(slotPkgs)
                            } else {
                                addAll(parsePackageCsv(profile.globalBlockedPackagesCsv))
                            }
                        }
                    }.distinct()

                    val slotRemaining = activeSlot?.let { remainingSecondsInSlot(it, now) } ?: 0
                    val quote = activeSlot?.motivationalQuote?.takeIf { it.isNotBlank() }
                        ?: profile.focusMotivationQuote.ifBlank { DEFAULT_FOCUS_QUOTE }

                    val wasSlotJustTriggered = activeSlot != null && autoOpenedSlotIds.add(activeSlot.id)
                    var pomodoroJustCompleted = false

                    _liveFocusSession.update { current ->
                        val enteringFresh = !current.isActive
                        val usePomodoro = activeSlot?.pomodoroEnabled ?: true
                        val useCountdown = activeSlot?.countdownEnabled ?: (activeSlot != null)
                        val runningNow = if (enteringFresh || wasSlotJustTriggered) {
                            usePomodoro
                        } else {
                            current.pomodoroRunning && usePomodoro
                        }

                        var nextPomoSec = if (enteringFresh || wasSlotJustTriggered) {
                            POMODORO_FOCUS_SECONDS
                        } else {
                            current.pomodoroRemainingSeconds
                        }
                        var nextIsBreak = if (enteringFresh || wasSlotJustTriggered) {
                            false
                        } else {
                            current.pomodoroIsBreak
                        }
                        var nextCompletedPomos = current.completedPomodoros

                        if (runningNow && !enteringFresh) {
                            if (nextPomoSec > 1) {
                                nextPomoSec -= 1
                            } else {
                                if (!nextIsBreak) {
                                    nextCompletedPomos += 1
                                    pomodoroJustCompleted = true
                                }
                                nextIsBreak = !nextIsBreak
                                nextPomoSec = if (nextIsBreak) POMODORO_BREAK_SECONDS else POMODORO_FOCUS_SECONDS
                            }
                        }

                        current.copy(
                            isOverlayOpen = current.isOverlayOpen || wasSlotJustTriggered,
                            isActive = true,
                            isManualFocus = profile.focusModeEnabled,
                            activeSlotId = activeSlot?.id,
                            activeSlotTitle = activeSlot?.title ?: "Deep Focus Mode",
                            pomodoroEnabled = usePomodoro,
                            countdownEnabled = useCountdown,
                            pomodoroIsBreak = nextIsBreak,
                            pomodoroRemainingSeconds = nextPomoSec,
                            pomodoroRunning = runningNow,
                            slotRemainingSeconds = slotRemaining,
                            elapsedFocusSeconds = if (enteringFresh) 0 else current.elapsedFocusSeconds + 1,
                            completedPomodoros = nextCompletedPomos,
                            blockedPackages = combinedBlocked,
                            motivationalQuote = quote
                        )
                    }

                    if (pomodoroJustCompleted) {
                        repository.awardXpAndStudyMinutes(addedXp = 50L, addedStudyMinutes = 25L)
                        _snackbarMessage.value = "Pomodoro complete! +25 mins & +50 XP logged."
                    }
                } else {
                    autoOpenedSlotIds.clear()
                    if (_liveFocusSession.value.isActive) {
                        val elapsedMins = (_liveFocusSession.value.elapsedFocusSeconds / 60).toLong()
                        if (elapsedMins >= 1L) {
                            repository.awardXpAndStudyMinutes(
                                addedXp = (elapsedMins * 2L).coerceIn(10L, 300L),
                                addedStudyMinutes = elapsedMins
                            )
                        }
                        _liveFocusSession.value = LiveFocusSessionState(
                            motivationalQuote = profile.focusMotivationQuote.ifBlank { DEFAULT_FOCUS_QUOTE }
                        )
                    }
                }

                delay(1000L)
            }
        }
    }

    fun clearSnackbar() {
        _snackbarMessage.value = null
    }

    private fun currentProfileOrDefault(): UserProfile =
        (profileState.value as? UiState.Success)?.data ?: UserProfile()

    // --- FOCUS MODE & TIME SLOT ACTIONS ---

    fun toggleFocusMode(enabled: Boolean) {
        val profile = currentProfileOrDefault()
        val slots = (timeSlotsState.value as? UiState.Success)?.data.orEmpty()
        val activeSlot = slots.firstOrNull { isTimeSlotActiveNow(it) }

        viewModelScope.launch {
            repository.updateProfile { it.copy(focusModeEnabled = enabled) }
                .onSuccess {
                    if (enabled) {
                        val blocked = buildList {
                            addAll(parsePackageCsv(profile.globalBlockedPackagesCsv))
                            activeSlot?.let { addAll(parsePackageCsv(it.blockedPackagesCsv)) }
                        }.distinct()
                        val usePomodoro = activeSlot?.pomodoroEnabled ?: true
                        _liveFocusSession.update {
                            it.copy(
                                isOverlayOpen = true,
                                isActive = true,
                                isManualFocus = true,
                                activeSlotId = activeSlot?.id,
                                activeSlotTitle = activeSlot?.title ?: "Deep Focus Mode",
                                pomodoroEnabled = usePomodoro,
                                countdownEnabled = activeSlot?.countdownEnabled ?: (activeSlot != null),
                                pomodoroIsBreak = false,
                                pomodoroRemainingSeconds = POMODORO_FOCUS_SECONDS,
                                pomodoroRunning = usePomodoro,
                                slotRemainingSeconds = activeSlot?.let { s -> remainingSecondsInSlot(s) } ?: 0,
                                elapsedFocusSeconds = 0,
                                blockedPackages = blocked,
                                motivationalQuote = activeSlot?.motivationalQuote?.takeIf { q -> q.isNotBlank() }
                                    ?: profile.focusMotivationQuote.ifBlank { DEFAULT_FOCUS_QUOTE }
                            )
                        }
                        _snackbarMessage.value = "Focus Mode ON • ${blocked.size} apps shielded"
                    } else {
                        val elapsedMins = (_liveFocusSession.value.elapsedFocusSeconds / 60).toLong()
                        if (elapsedMins >= 1L) {
                            val xpGain = (elapsedMins * 2L).coerceIn(10L, 300L)
                            repository.awardXpAndStudyMinutes(xpGain, elapsedMins)
                            _snackbarMessage.value = "Focus ended • ${elapsedMins}m study saved (+$xpGain XP)"
                        } else {
                            _snackbarMessage.value = "Focus Mode turned OFF"
                        }
                        _liveFocusSession.update { it.copy(isOverlayOpen = false, isManualFocus = false, pomodoroRunning = false) }
                    }
                }
                .onFailure {
                    _snackbarMessage.value = it.localizedMessage ?: "Could not toggle Focus Mode."
                }
        }
    }

    fun openFocusOverlay() {
        val profile = currentProfileOrDefault()
        val slots = (timeSlotsState.value as? UiState.Success)?.data.orEmpty()
        val activeSlot = slots.firstOrNull { isTimeSlotActiveNow(it) }
        val blocked = buildList {
            addAll(parsePackageCsv(profile.globalBlockedPackagesCsv))
            activeSlot?.let { addAll(parsePackageCsv(it.blockedPackagesCsv)) }
        }.distinct()

        _liveFocusSession.update {
            it.copy(
                isOverlayOpen = true,
                isActive = profile.focusModeEnabled || activeSlot != null,
                activeSlotId = activeSlot?.id,
                activeSlotTitle = activeSlot?.title ?: "Deep Focus Mode",
                blockedPackages = blocked,
                motivationalQuote = activeSlot?.motivationalQuote?.takeIf { q -> q.isNotBlank() }
                    ?: profile.focusMotivationQuote.ifBlank { DEFAULT_FOCUS_QUOTE }
            )
        }
    }

    fun minimizeFocusOverlay() {
        _liveFocusSession.update { it.copy(isOverlayOpen = false) }
    }

    fun endFocusAndLogStudy() {
        val elapsedMins = (_liveFocusSession.value.elapsedFocusSeconds / 60).toLong()
        val activeSlotId = _liveFocusSession.value.activeSlotId
        viewModelScope.launch {
            repository.updateProfile { it.copy(focusModeEnabled = false) }
            if (activeSlotId != null) {
                val slots = (timeSlotsState.value as? UiState.Success)?.data.orEmpty()
                val slot = slots.firstOrNull { it.id == activeSlotId }
                if (slot != null && slot.enabled) {
                    repository.upsertTimeSlot(slot.copy(enabled = false))
                }
            }
            if (elapsedMins >= 1L) {
                val xpGain = (elapsedMins * 2L).coerceIn(10L, 300L)
                repository.awardXpAndStudyMinutes(xpGain, elapsedMins)
                _snackbarMessage.value = "Great focus! Logged ${elapsedMins}m (+$xpGain XP)"
            } else {
                _snackbarMessage.value = "Focus session ended."
            }
            _liveFocusSession.value = LiveFocusSessionState(
                motivationalQuote = currentProfileOrDefault().focusMotivationQuote.ifBlank { DEFAULT_FOCUS_QUOTE }
            )
        }
    }

    fun toggleLivePomodoroRunning() {
        _liveFocusSession.update { state ->
            state.copy(pomodoroRunning = !state.pomodoroRunning)
        }
    }

    fun switchLivePomodoroPhase() {
        _liveFocusSession.update { state ->
            val nextBreak = !state.pomodoroIsBreak
            state.copy(
                pomodoroIsBreak = nextBreak,
                pomodoroRemainingSeconds = if (nextBreak) POMODORO_BREAK_SECONDS else POMODORO_FOCUS_SECONDS
            )
        }
    }

    fun cycleMotivationalQuote() {
        val current = _liveFocusSession.value.motivationalQuote
        val idx = STUDY_MOTIVATIONAL_QUOTES.indexOf(current)
        val next = STUDY_MOTIVATIONAL_QUOTES[(idx + 1) % STUDY_MOTIVATIONAL_QUOTES.size]
        updateFocusMotivationQuote(next)
    }

    fun updateFocusMotivationQuote(quote: String) {
        val clean = quote.trim().take(180)
        if (clean.isEmpty()) return
        _liveFocusSession.update { it.copy(motivationalQuote = clean) }
        viewModelScope.launch {
            repository.updateProfile { it.copy(focusMotivationQuote = clean) }
        }
    }

    fun saveTimeSlot(slot: StudyTimeSlot) {
        viewModelScope.launch {
            repository.upsertTimeSlot(slot)
                .onSuccess {
                    _snackbarMessage.value = "Saved time slot: ${slot.title}"
                }
                .onFailure {
                    _snackbarMessage.value = it.localizedMessage ?: "Could not save time slot."
                }
        }
    }

    fun deleteTimeSlot(slotId: String) {
        viewModelScope.launch {
            repository.deleteTimeSlot(slotId)
                .onSuccess { _snackbarMessage.value = "Time slot removed." }
                .onFailure { _snackbarMessage.value = it.localizedMessage ?: "Could not delete time slot." }
        }
    }

    fun toggleTimeSlotEnabled(slot: StudyTimeSlot) {
        val nextEnabled = !slot.enabled
        viewModelScope.launch {
            repository.upsertTimeSlot(slot.copy(enabled = nextEnabled))
                .onSuccess {
                    if (!nextEnabled) {
                        autoOpenedSlotIds.remove(slot.id)
                    }
                    _snackbarMessage.value = if (nextEnabled) {
                        "${slot.title} armed for auto-block"
                    } else {
                        "${slot.title} paused"
                    }
                }
        }
    }

    fun toggleTimeSlotPomodoro(slot: StudyTimeSlot) {
        viewModelScope.launch {
            repository.upsertTimeSlot(slot.copy(pomodoroEnabled = !slot.pomodoroEnabled))
        }
    }

    fun toggleTimeSlotCountdown(slot: StudyTimeSlot) {
        viewModelScope.launch {
            repository.upsertTimeSlot(slot.copy(countdownEnabled = !slot.countdownEnabled))
        }
    }

    fun updateTimeSlotBlockedApps(slot: StudyTimeSlot, selectedPackages: Set<String>) {
        viewModelScope.launch {
            repository.upsertTimeSlot(slot.copy(blockedPackagesCsv = toPackageCsv(selectedPackages)))
                .onSuccess {
                    _snackbarMessage.value = "${selectedPackages.size} apps linked to ${slot.title}"
                }
        }
    }

    fun updateGlobalBlockedApps(selectedPackages: Set<String>) {
        viewModelScope.launch {
            repository.updateProfile { it.copy(globalBlockedPackagesCsv = toPackageCsv(selectedPackages)) }
                .onSuccess {
                    _snackbarMessage.value = "${selectedPackages.size} apps selected for blocking"
                }
        }
    }

    fun removeGlobalBlockedApp(packageName: String) {
        val current = parsePackageCsv(currentProfileOrDefault().globalBlockedPackagesCsv).toMutableSet()
        if (current.remove(packageName)) {
            updateGlobalBlockedApps(current)
        }
    }

    fun updateDailyStudyTargetHours(hours: Int) {
        val boundedMins = (hours.coerceIn(1, 16) * 60).toLong()
        viewModelScope.launch {
            repository.updateProfile { it.copy(dailyStudyTargetMinutes = boundedMins) }
                .onSuccess { _snackbarMessage.value = "Daily study target set to ${hours}h" }
        }
    }

    // --- PROFILE ACTIONS ---

    fun updateDailyGoal(newGoal: String) {
        val clean = newGoal.trim().take(120)
        if (clean.isEmpty()) return
        viewModelScope.launch {
            repository.updateProfile { it.copy(dailyGoalText = clean) }
                .onSuccess { _snackbarMessage.value = "Daily goal updated!" }
                .onFailure { _snackbarMessage.value = it.localizedMessage ?: "Failed to update goal." }
        }
    }

    fun updateProfileIdentity(displayName: String, tagline: String) {
        val cleanName = displayName.trim().take(80)
        val cleanTag = tagline.trim().take(100)
        if (cleanName.isEmpty()) return
        viewModelScope.launch {
            repository.updateProfile { it.copy(displayName = cleanName, tagline = cleanTag) }
                .onSuccess { _snackbarMessage.value = "Profile saved!" }
                .onFailure { _snackbarMessage.value = it.localizedMessage ?: "Failed to save profile." }
        }
    }

    fun setMotivationMode(enabled: Boolean) {
        viewModelScope.launch {
            repository.updateProfile { it.copy(motivationMode = enabled) }
                .onFailure { _snackbarMessage.value = it.localizedMessage ?: "Failed to update setting." }
        }
    }

    fun adjustWaterGlasses(delta: Int) {
        viewModelScope.launch {
            repository.updateProfile { current ->
                current.copy(todayWaterGlasses = (current.todayWaterGlasses + delta).coerceIn(0L, 50L))
            }.onFailure { _snackbarMessage.value = it.localizedMessage ?: "Could not update water." }
        }
    }

    fun updateSleepHours(hours: Double) {
        val bounded = hours.coerceIn(0.0, 24.0)
        viewModelScope.launch {
            repository.updateProfile { it.copy(todaySleepHours = bounded) }
                .onSuccess { _snackbarMessage.value = "Sleep logged: ${"%.1f".format(bounded)} hrs" }
                .onFailure { _snackbarMessage.value = it.localizedMessage ?: "Could not update sleep." }
        }
    }

    fun setActiveMantra(mantra: String) {
        val clean = mantra.trim().take(60)
        if (clean.isEmpty()) return
        viewModelScope.launch {
            repository.updateProfile { it.copy(activeMantra = clean) }
                .onFailure { _snackbarMessage.value = it.localizedMessage ?: "Could not switch mantra." }
        }
    }

    fun resetTodayCounters() {
        viewModelScope.launch {
            _sessionJapCount.value = 0
            repository.resetTodayCounters()
                .onSuccess { _snackbarMessage.value = "Today's counters reset." }
                .onFailure { _snackbarMessage.value = it.localizedMessage ?: "Could not reset counters." }
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
        val earnedXp = (count / 10L).coerceAtLeast(10L)

        viewModelScope.launch {
            val logRes = repository.logSadhanaSession(
                mantra = profile.activeMantra,
                count = count,
                malas = malasEarned,
                meditationMinutes = meditationMinutes
            )
            val profRes = repository.updateProfile { current ->
                val newTodayCount = (current.todayNaamJapCount + count).coerceAtMost(1_000_000L)
                val newTotalCount = (current.totalNaamJapCount + count).coerceAtMost(100_000_000L)
                val newTodayMalas = (newTodayCount / 108L).coerceAtMost(10_000L)
                val newXp = (current.xp + earnedXp).coerceAtMost(10_000_000L)
                val newLevel = (1L + newXp / 250L).coerceIn(1L, 999L)
                current.copy(
                    todayNaamJapCount = newTodayCount,
                    totalNaamJapCount = newTotalCount,
                    todayMalas = newTodayMalas,
                    xp = newXp,
                    level = newLevel
                )
            }
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
            repository.addTask(title, category, priority, dueDate)
                .onSuccess { _snackbarMessage.value = "Task added to your plan!" }
                .onFailure { _snackbarMessage.value = it.localizedMessage ?: "Failed to add task." }
        }
    }

    fun toggleTask(task: LifeTask) {
        val nextCompleted = !task.completed
        viewModelScope.launch {
            repository.toggleTaskCompleted(task.id, nextCompleted)
                .onSuccess {
                    if (nextCompleted) {
                        repository.awardXpAndStudyMinutes(20L)
                    }
                }
                .onFailure { _snackbarMessage.value = it.localizedMessage ?: "Failed to update task." }
        }
    }

    fun deleteTask(taskId: String) {
        viewModelScope.launch {
            repository.deleteTask(taskId)
                .onFailure { _snackbarMessage.value = it.localizedMessage ?: "Failed to delete task." }
        }
    }

    // --- SUBJECTS & NOTES ACTIONS ---

    fun addSubject(name: String, progress: Long, accent: SubjectAccent) {
        viewModelScope.launch {
            repository.addSubject(name, progress, accent)
                .onSuccess { _snackbarMessage.value = "Subject added!" }
                .onFailure { _snackbarMessage.value = it.localizedMessage ?: "Failed to add subject." }
        }
    }

    fun updateSubjectProgress(subject: StudySubject, newProgress: Long) {
        viewModelScope.launch {
            repository.updateSubjectProgress(
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
            repository.deleteSubject(subjectId)
                .onFailure { _snackbarMessage.value = it.localizedMessage ?: "Failed to delete subject." }
        }
    }

    fun addNote(title: String, subjectName: String, content: String) {
        viewModelScope.launch {
            repository.addNote(title, subjectName, content)
                .onSuccess { _snackbarMessage.value = "Study note saved!" }
                .onFailure { _snackbarMessage.value = it.localizedMessage ?: "Failed to save note." }
        }
    }

    fun deleteNote(noteId: String) {
        viewModelScope.launch {
            repository.deleteNote(noteId)
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
                title,
                type,
                durationMinutes,
                distanceKm,
                caloriesBurned
            ).onSuccess {
                val xpGain = (durationMinutes * 2L).coerceIn(15L, 200L)
                repository.awardXpAndStudyMinutes(xpGain)
                _snackbarMessage.value = "Workout logged! +$xpGain XP"
            }.onFailure {
                _snackbarMessage.value = it.localizedMessage ?: "Failed to log workout."
            }
        }
    }

    fun deleteWorkout(workoutId: String) {
        viewModelScope.launch {
            repository.deleteWorkout(workoutId)
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
                addedXp = xpGain,
                addedStudyMinutes = mins
            )
            if (matchedSubject != null) {
                val boostedProgress = (matchedSubject.progressPercent + 3L).coerceAtMost(100L)
                repository.updateSubjectProgress(
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
                repository.awardXpAndStudyMinutes(xp)
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
        focusEngineJob?.cancel()
    }
}
