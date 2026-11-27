package com.example.data

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

class LifeRepository(private val dao: LifeDao) {

    private fun todayIsoDate(): String =
        SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())

    private fun todayDisplayLabel(): String =
        SimpleDateFormat("dd MMM", Locale.US).format(Date())

    // --- PROFILE ---

    suspend fun ensureUserProfile(): Result<Unit> = runSafe {
        val existing = dao.getProfileOnce()
        val today = todayIsoDate()
        val now = System.currentTimeMillis()
        if (existing == null) {
            dao.upsertProfile(
                UserProfile(
                    id = "local_user",
                    userId = "local_user",
                    displayName = "Shekhar",
                    tagline = "Dreamer • Fighter • Achiever",
                    dailyGoalText = "Be 1% Better Than Yesterday",
                    level = 1L,
                    xp = 0L,
                    streakDays = 1L,
                    totalStudyMinutes = 0L,
                    todayStudyMinutes = 0L,
                    dailyStudyTargetMinutes = 480L,
                    focusModeEnabled = false,
                    globalBlockedPackagesCsv = DEFAULT_BLOCKED_PACKAGES_CSV,
                    focusMotivationQuote = DEFAULT_FOCUS_QUOTE,
                    totalNaamJapCount = 0L,
                    todayNaamJapCount = 0L,
                    todayMalas = 0L,
                    activeMantra = "Radha Radha",
                    todayWaterGlasses = 0L,
                    todaySleepHours = 7.0,
                    motivationMode = true,
                    lastActiveDate = today,
                    createdAt = now,
                    updatedAt = now
                )
            )
        } else if (existing.lastActiveDate.isNotEmpty() && existing.lastActiveDate != today) {
            dao.upsertProfile(
                existing.copy(
                    streakDays = (existing.streakDays + 1L).coerceAtMost(10_000L),
                    todayStudyMinutes = 0L,
                    todayNaamJapCount = 0L,
                    todayMalas = 0L,
                    todayWaterGlasses = 0L,
                    lastActiveDate = today,
                    updatedAt = now
                )
            )
        }
    }

    fun observeProfile(): Flow<UserProfile?> = dao.observeProfile()

    suspend fun updateProfile(transform: (UserProfile) -> UserProfile): Result<Unit> = runSafe {
        val current = dao.getProfileOnce() ?: UserProfile(lastActiveDate = todayIsoDate())
        val updated = transform(current).copy(
            id = "local_user",
            userId = "local_user",
            updatedAt = System.currentTimeMillis()
        )
        dao.upsertProfile(updated)
    }

    suspend fun awardXpAndStudyMinutes(
        addedXp: Long,
        addedStudyMinutes: Long = 0L
    ): Result<Unit> = updateProfile { current ->
        val newXp = (current.xp + addedXp).coerceIn(0L, 10_000_000L)
        val newLevel = (1L + (newXp / 250L)).coerceIn(1L, 999L)
        val newStudy = (current.totalStudyMinutes + addedStudyMinutes).coerceIn(0L, 10_000_000L)
        val newTodayStudy = (current.todayStudyMinutes + addedStudyMinutes).coerceIn(0L, 1_440L)
        current.copy(
            xp = newXp,
            level = newLevel,
            totalStudyMinutes = newStudy,
            todayStudyMinutes = newTodayStudy
        )
    }

    suspend fun resetTodayCounters(): Result<Unit> = updateProfile { current ->
        current.copy(
            todayStudyMinutes = 0L,
            todayNaamJapCount = 0L,
            todayMalas = 0L,
            todayWaterGlasses = 0L
        )
    }

    // --- STUDY TIME SLOTS ---

    fun observeTimeSlots(): Flow<List<StudyTimeSlot>> = dao.observeTimeSlots()

    suspend fun getTimeSlotsOnce(): List<StudyTimeSlot> = dao.getTimeSlotsOnce()

    suspend fun upsertTimeSlot(slot: StudyTimeSlot): Result<Unit> = runSafe {
        val cleanTitle = slot.title.trim().take(60).ifEmpty { "Study Session" }
        val cleanQuote = slot.motivationalQuote.trim().take(180).ifEmpty { DEFAULT_FOCUS_QUOTE }
        val now = System.currentTimeMillis()
        val finalId = slot.id.ifBlank { UUID.randomUUID().toString() }
        dao.upsertTimeSlot(
            slot.copy(
                id = finalId,
                userId = "local_user",
                title = cleanTitle,
                startHour = slot.startHour.coerceIn(0, 23),
                startMinute = slot.startMinute.coerceIn(0, 59),
                endHour = slot.endHour.coerceIn(0, 23),
                endMinute = slot.endMinute.coerceIn(0, 59),
                motivationalQuote = cleanQuote,
                createdAt = if (slot.id.isBlank()) now else slot.createdAt,
                updatedAt = now
            )
        )
    }

    suspend fun deleteTimeSlot(slotId: String): Result<Unit> = runSafe {
        require(slotId.isNotBlank())
        dao.deleteTimeSlot(slotId)
    }

    // --- TASKS ---

    fun observeTasks(): Flow<List<LifeTask>> = dao.observeTasks()

    suspend fun addTask(
        title: String,
        category: TaskCategory,
        priority: String = "MEDIUM",
        dueDate: String = "Today"
    ): Result<Unit> = runSafe {
        val cleanTitle = title.trim().take(120)
        require(cleanTitle.isNotEmpty()) { "Task title cannot be empty." }
        val cleanPriority = priority.takeIf { it in listOf("HIGH", "MEDIUM", "LOW") } ?: "MEDIUM"
        val cleanDue = dueDate.trim().take(30)
        val now = System.currentTimeMillis()
        dao.insertTask(
            LifeTask(
                id = UUID.randomUUID().toString(),
                userId = "local_user",
                title = cleanTitle,
                category = category.key,
                completed = false,
                dueDate = cleanDue,
                priority = cleanPriority,
                createdAt = now,
                updatedAt = now
            )
        )
    }

    suspend fun toggleTaskCompleted(taskId: String, completed: Boolean): Result<Unit> = runSafe {
        require(taskId.isNotBlank()) { "Task ID cannot be blank." }
        dao.updateTaskCompleted(taskId, completed, System.currentTimeMillis())
    }

    suspend fun deleteTask(taskId: String): Result<Unit> = runSafe {
        require(taskId.isNotBlank()) { "Task ID cannot be blank." }
        dao.deleteTask(taskId)
    }

    // --- STUDY SUBJECTS ---

    fun observeSubjects(): Flow<List<StudySubject>> = dao.observeSubjects()

    suspend fun addSubject(
        name: String,
        progressPercent: Long,
        accent: SubjectAccent
    ): Result<Unit> = runSafe {
        val cleanName = name.trim().take(80)
        require(cleanName.isNotEmpty()) { "Subject name cannot be empty." }
        val now = System.currentTimeMillis()
        dao.insertSubject(
            StudySubject(
                id = UUID.randomUUID().toString(),
                userId = "local_user",
                name = cleanName,
                progressPercent = progressPercent.coerceIn(0L, 100L),
                studyMinutes = 0L,
                accentKey = accent.key,
                createdAt = now,
                updatedAt = now
            )
        )
    }

    suspend fun updateSubjectProgress(
        subjectId: String,
        newProgressPercent: Long,
        newStudyMinutes: Long
    ): Result<Unit> = runSafe {
        require(subjectId.isNotBlank())
        dao.updateSubjectProgress(
            subjectId = subjectId,
            progressPercent = newProgressPercent.coerceIn(0L, 100L),
            studyMinutes = newStudyMinutes.coerceIn(0L, 1_000_000L),
            updatedAt = System.currentTimeMillis()
        )
    }

    suspend fun deleteSubject(subjectId: String): Result<Unit> = runSafe {
        require(subjectId.isNotBlank())
        dao.deleteSubject(subjectId)
    }

    // --- STUDY NOTES ---

    fun observeNotes(): Flow<List<StudyNote>> = dao.observeNotes()

    suspend fun addNote(
        title: String,
        subjectName: String,
        content: String
    ): Result<Unit> = runSafe {
        val cleanTitle = title.trim().take(120)
        val cleanSubject = subjectName.trim().take(80)
        val cleanContent = content.trim().take(8000)
        require(cleanTitle.isNotEmpty() && cleanContent.isNotEmpty()) {
            "Title and content are required."
        }
        val now = System.currentTimeMillis()
        dao.insertNote(
            StudyNote(
                id = UUID.randomUUID().toString(),
                userId = "local_user",
                title = cleanTitle,
                subjectName = cleanSubject,
                content = cleanContent,
                createdAt = now,
                updatedAt = now
            )
        )
    }

    suspend fun deleteNote(noteId: String): Result<Unit> = runSafe {
        require(noteId.isNotBlank())
        dao.deleteNote(noteId)
    }

    // --- WORKOUTS ---

    fun observeWorkouts(): Flow<List<WorkoutLog>> = dao.observeWorkouts()

    suspend fun addWorkout(
        title: String,
        activityType: WorkoutType,
        durationMinutes: Long,
        distanceKm: Double,
        caloriesBurned: Long
    ): Result<Unit> = runSafe {
        val cleanTitle = title.trim().take(100)
        require(cleanTitle.isNotEmpty()) { "Workout title cannot be empty." }
        val now = System.currentTimeMillis()
        dao.insertWorkout(
            WorkoutLog(
                id = UUID.randomUUID().toString(),
                userId = "local_user",
                title = cleanTitle,
                activityType = activityType.key,
                durationMinutes = durationMinutes.coerceIn(1L, 600L),
                distanceKm = distanceKm.coerceIn(0.0, 500.0),
                caloriesBurned = caloriesBurned.coerceIn(0L, 10_000L),
                dateLabel = todayDisplayLabel(),
                createdAt = now,
                updatedAt = now
            )
        )
    }

    suspend fun deleteWorkout(workoutId: String): Result<Unit> = runSafe {
        require(workoutId.isNotBlank())
        dao.deleteWorkout(workoutId)
    }

    // --- SADHANA / NAAM JAP LOGS ---

    fun observeSadhanaLogs(): Flow<List<SadhanaLog>> = dao.observeSadhanaLogs()

    suspend fun logSadhanaSession(
        mantra: String,
        count: Long,
        malas: Long,
        meditationMinutes: Long
    ): Result<Unit> = runSafe {
        val cleanMantra = mantra.trim().take(60).ifEmpty { "Radha Radha" }
        val now = System.currentTimeMillis()
        dao.insertSadhanaLog(
            SadhanaLog(
                id = UUID.randomUUID().toString(),
                userId = "local_user",
                mantra = cleanMantra,
                count = count.coerceIn(1L, 100_000L),
                malas = malas.coerceIn(0L, 1_000L),
                meditationMinutes = meditationMinutes.coerceIn(0L, 600L),
                dateLabel = todayDisplayLabel(),
                createdAt = now,
                updatedAt = now
            )
        )
    }

    private inline fun runSafe(block: () -> Unit): Result<Unit> = try {
        block()
        Result.success(Unit)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Result.failure(e)
    }
}
