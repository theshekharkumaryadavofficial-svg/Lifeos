package com.example.data

import android.content.Context
import com.example.R
import com.example.firebase.OperationType
import com.example.firebase.handleFirestoreError
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class LifeRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private fun requireUserId(expectedUid: String? = null): String {
        val uid = auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
        if (expectedUid != null && uid != expectedUid) {
            throw IllegalStateException("Signed-in user changed; refusing cross-user operation.")
        }
        return uid
    }

    private fun todayIsoDate(): String =
        SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())

    private fun todayDisplayLabel(): String =
        SimpleDateFormat("dd MMM", Locale.US).format(Date())

    private fun userDoc(uid: String) = db.collection("users").document(uid)

    // --- PROFILE ---

    suspend fun ensureUserProfile(userId: String, googleDisplayName: String?): Result<Unit> {
        val path = "users/$userId"
        return try {
            val uid = requireUserId(userId)
            val docRef = userDoc(uid)
            val snap = docRef.get().await()
            val today = todayIsoDate()
            if (!snap.exists()) {
                val safeName = googleDisplayName?.trim()
                    ?.takeIf { it.isNotEmpty() }
                    ?.take(80)
                    ?: "Achiever"
                val payload = mapOf(
                    "userId" to uid,
                    "displayName" to safeName,
                    "tagline" to "Dreamer • Fighter • Achiever",
                    "dailyGoalText" to "Be 1% Better Than Yesterday",
                    "level" to 1L,
                    "xp" to 0L,
                    "streakDays" to 1L,
                    "totalStudyMinutes" to 0L,
                    "totalNaamJapCount" to 0L,
                    "todayNaamJapCount" to 0L,
                    "todayMalas" to 0L,
                    "activeMantra" to "Radha Radha",
                    "todayWaterGlasses" to 0L,
                    "todaySleepHours" to 7.0,
                    "motivationMode" to true,
                    "lastActiveDate" to today,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                docRef.set(payload).await()
            } else {
                val lastActive = snap.getString("lastActiveDate") ?: ""
                if (lastActive.isNotEmpty() && lastActive != today) {
                    val currentStreak = (snap.getLong("streakDays") ?: 1L).coerceAtMost(9999L)
                    docRef.update(
                        mapOf(
                            "streakDays" to (currentStreak + 1L),
                            "todayNaamJapCount" to 0L,
                            "todayMalas" to 0L,
                            "todayWaterGlasses" to 0L,
                            "lastActiveDate" to today,
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    ).await()
                }
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    fun observeProfile(userId: String): Flow<UserProfile?> = flow {
        val uid = requireUserId(userId)
        val path = "users/$uid"
        emitAll(
            userDoc(uid)
                .snapshots()
                .map { snapshot ->
                    if (snapshot.exists()) {
                        snapshot.toObject(
                            UserProfile::class.java,
                            DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                        )
                    } else {
                        null
                    }
                }
                .catch { error ->
                    handleFlowError(error, OperationType.GET, path, uid)
                    throw error
                }
        )
    }

    suspend fun updateProfileFields(userId: String, updates: Map<String, Any>): Result<Unit> {
        val path = "users/$userId"
        return try {
            val uid = requireUserId(userId)
            val safeUpdates = updates.toMutableMap()
            safeUpdates["updatedAt"] = FieldValue.serverTimestamp()
            userDoc(uid).update(safeUpdates).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun awardXpAndStudyMinutes(
        userId: String,
        currentProfile: UserProfile,
        addedXp: Long,
        addedStudyMinutes: Long = 0L
    ): Result<Unit> {
        val newXp = (currentProfile.xp + addedXp).coerceIn(0L, 10_000_000L)
        val newLevel = (1L + (newXp / 250L)).coerceIn(1L, 999L)
        val newStudy = (currentProfile.totalStudyMinutes + addedStudyMinutes).coerceIn(0L, 10_000_000L)
        return updateProfileFields(
            userId,
            mapOf(
                "xp" to newXp,
                "level" to newLevel,
                "totalStudyMinutes" to newStudy
            )
        )
    }

    // --- TASKS ---

    fun observeTasks(userId: String): Flow<List<LifeTask>> = flow {
        val uid = requireUserId(userId)
        val path = "users/$uid/tasks"
        emitAll(
            userDoc(uid).collection("tasks")
                .snapshots()
                .map { snap ->
                    snap.toObjects(
                        LifeTask::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedByDescending { it.createdAt?.seconds ?: 0L }
                }
                .catch { error ->
                    handleFlowError(error, OperationType.LIST, path, uid)
                    throw error
                }
        )
    }

    suspend fun addTask(
        userId: String,
        title: String,
        category: TaskCategory,
        priority: String = "MEDIUM",
        dueDate: String = "Today"
    ): Result<Unit> {
        val cleanTitle = title.trim().take(120)
        if (cleanTitle.isEmpty()) {
            return Result.failure(IllegalArgumentException("Task title cannot be empty."))
        }
        val cleanPriority = priority.takeIf { it in listOf("HIGH", "MEDIUM", "LOW") } ?: "MEDIUM"
        val cleanDue = dueDate.trim().take(30)
        var path = "users/$userId/tasks"
        return try {
            val uid = requireUserId(userId)
            val docRef = userDoc(uid).collection("tasks").document()
            path = docRef.path
            val payload = mapOf(
                "userId" to uid,
                "title" to cleanTitle,
                "category" to category.key,
                "completed" to false,
                "dueDate" to cleanDue,
                "priority" to cleanPriority,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            docRef.set(payload).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun toggleTaskCompleted(
        userId: String,
        taskId: String,
        completed: Boolean
    ): Result<Unit> {
        val path = "users/$userId/tasks/$taskId"
        return try {
            val uid = requireUserId(userId)
            require(taskId.isNotBlank()) { "Task ID cannot be blank." }
            userDoc(uid).collection("tasks").document(taskId)
                .update(
                    mapOf(
                        "completed" to completed,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun deleteTask(userId: String, taskId: String): Result<Unit> {
        val path = "users/$userId/tasks/$taskId"
        return try {
            val uid = requireUserId(userId)
            require(taskId.isNotBlank()) { "Task ID cannot be blank." }
            userDoc(uid).collection("tasks").document(taskId).delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    // --- STUDY SUBJECTS ---

    fun observeSubjects(userId: String): Flow<List<StudySubject>> = flow {
        val uid = requireUserId(userId)
        val path = "users/$uid/subjects"
        emitAll(
            userDoc(uid).collection("subjects")
                .snapshots()
                .map { snap ->
                    snap.toObjects(
                        StudySubject::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedByDescending { it.progressPercent }
                }
                .catch { error ->
                    handleFlowError(error, OperationType.LIST, path, uid)
                    throw error
                }
        )
    }

    suspend fun addSubject(
        userId: String,
        name: String,
        progressPercent: Long,
        accent: SubjectAccent
    ): Result<Unit> {
        val cleanName = name.trim().take(80)
        if (cleanName.isEmpty()) {
            return Result.failure(IllegalArgumentException("Subject name cannot be empty."))
        }
        val boundedProgress = progressPercent.coerceIn(0L, 100L)
        var path = "users/$userId/subjects"
        return try {
            val uid = requireUserId(userId)
            val docRef = userDoc(uid).collection("subjects").document()
            path = docRef.path
            docRef.set(
                mapOf(
                    "userId" to uid,
                    "name" to cleanName,
                    "progressPercent" to boundedProgress,
                    "studyMinutes" to 0L,
                    "accentKey" to accent.key,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun updateSubjectProgress(
        userId: String,
        subjectId: String,
        newProgressPercent: Long,
        newStudyMinutes: Long
    ): Result<Unit> {
        val path = "users/$userId/subjects/$subjectId"
        return try {
            val uid = requireUserId(userId)
            require(subjectId.isNotBlank())
            userDoc(uid).collection("subjects").document(subjectId)
                .update(
                    mapOf(
                        "progressPercent" to newProgressPercent.coerceIn(0L, 100L),
                        "studyMinutes" to newStudyMinutes.coerceIn(0L, 1_000_000L),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun deleteSubject(userId: String, subjectId: String): Result<Unit> {
        val path = "users/$userId/subjects/$subjectId"
        return try {
            val uid = requireUserId(userId)
            require(subjectId.isNotBlank())
            userDoc(uid).collection("subjects").document(subjectId).delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    // --- STUDY NOTES ---

    fun observeNotes(userId: String): Flow<List<StudyNote>> = flow {
        val uid = requireUserId(userId)
        val path = "users/$uid/notes"
        emitAll(
            userDoc(uid).collection("notes")
                .snapshots()
                .map { snap ->
                    snap.toObjects(
                        StudyNote::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedByDescending { it.updatedAt?.seconds ?: 0L }
                }
                .catch { error ->
                    handleFlowError(error, OperationType.LIST, path, uid)
                    throw error
                }
        )
    }

    suspend fun addNote(
        userId: String,
        title: String,
        subjectName: String,
        content: String
    ): Result<Unit> {
        val cleanTitle = title.trim().take(120)
        val cleanSubject = subjectName.trim().take(80)
        val cleanContent = content.trim().take(8000)
        if (cleanTitle.isEmpty() || cleanContent.isEmpty()) {
            return Result.failure(IllegalArgumentException("Title and content are required."))
        }
        var path = "users/$userId/notes"
        return try {
            val uid = requireUserId(userId)
            val docRef = userDoc(uid).collection("notes").document()
            path = docRef.path
            docRef.set(
                mapOf(
                    "userId" to uid,
                    "title" to cleanTitle,
                    "subjectName" to cleanSubject,
                    "content" to cleanContent,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun deleteNote(userId: String, noteId: String): Result<Unit> {
        val path = "users/$userId/notes/$noteId"
        return try {
            val uid = requireUserId(userId)
            require(noteId.isNotBlank())
            userDoc(uid).collection("notes").document(noteId).delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    // --- WORKOUTS ---

    fun observeWorkouts(userId: String): Flow<List<WorkoutLog>> = flow {
        val uid = requireUserId(userId)
        val path = "users/$uid/workouts"
        emitAll(
            userDoc(uid).collection("workouts")
                .snapshots()
                .map { snap ->
                    snap.toObjects(
                        WorkoutLog::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedByDescending { it.createdAt?.seconds ?: 0L }
                }
                .catch { error ->
                    handleFlowError(error, OperationType.LIST, path, uid)
                    throw error
                }
        )
    }

    suspend fun addWorkout(
        userId: String,
        title: String,
        activityType: WorkoutType,
        durationMinutes: Long,
        distanceKm: Double,
        caloriesBurned: Long
    ): Result<Unit> {
        val cleanTitle = title.trim().take(100)
        if (cleanTitle.isEmpty()) {
            return Result.failure(IllegalArgumentException("Workout title cannot be empty."))
        }
        var path = "users/$userId/workouts"
        return try {
            val uid = requireUserId(userId)
            val docRef = userDoc(uid).collection("workouts").document()
            path = docRef.path
            docRef.set(
                mapOf(
                    "userId" to uid,
                    "title" to cleanTitle,
                    "activityType" to activityType.key,
                    "durationMinutes" to durationMinutes.coerceIn(1L, 600L),
                    "distanceKm" to distanceKm.coerceIn(0.0, 500.0),
                    "caloriesBurned" to caloriesBurned.coerceIn(0L, 10_000L),
                    "dateLabel" to todayDisplayLabel(),
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun deleteWorkout(userId: String, workoutId: String): Result<Unit> {
        val path = "users/$userId/workouts/$workoutId"
        return try {
            val uid = requireUserId(userId)
            require(workoutId.isNotBlank())
            userDoc(uid).collection("workouts").document(workoutId).delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    // --- SADHANA / NAAM JAP LOGS ---

    fun observeSadhanaLogs(userId: String): Flow<List<SadhanaLog>> = flow {
        val uid = requireUserId(userId)
        val path = "users/$uid/sadhanaLogs"
        emitAll(
            userDoc(uid).collection("sadhanaLogs")
                .snapshots()
                .map { snap ->
                    snap.toObjects(
                        SadhanaLog::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedByDescending { it.createdAt?.seconds ?: 0L }
                }
                .catch { error ->
                    handleFlowError(error, OperationType.LIST, path, uid)
                    throw error
                }
        )
    }

    suspend fun logSadhanaSession(
        userId: String,
        mantra: String,
        count: Long,
        malas: Long,
        meditationMinutes: Long
    ): Result<Unit> {
        val cleanMantra = mantra.trim().take(60).ifEmpty { "Radha Radha" }
        val safeCount = count.coerceIn(1L, 100_000L)
        val safeMalas = malas.coerceIn(0L, 1_000L)
        val safeMeditation = meditationMinutes.coerceIn(0L, 600L)
        var path = "users/$userId/sadhanaLogs"
        return try {
            val uid = requireUserId(userId)
            val docRef = userDoc(uid).collection("sadhanaLogs").document()
            path = docRef.path
            docRef.set(
                mapOf(
                    "userId" to uid,
                    "mantra" to cleanMantra,
                    "count" to safeCount,
                    "malas" to safeMalas,
                    "meditationMinutes" to safeMeditation,
                    "dateLabel" to todayDisplayLabel(),
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    // --- FIREBASE APP UPDATE CONFIG ---

    private fun appUpdateDoc() = db.collection("app_config").document("update")

    fun observeAppUpdateConfig(userId: String): Flow<AppUpdateConfig?> = flow {
        val uid = requireUserId(userId)
        val path = "app_config/update"
        emitAll(
            appUpdateDoc()
                .snapshots()
                .map { snapshot ->
                    if (snapshot.exists()) {
                        snapshot.toObject(
                            AppUpdateConfig::class.java,
                            DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                        )
                    } else {
                        null
                    }
                }
                .catch { error ->
                    handleFlowError(error, OperationType.GET, path, uid)
                    emit(null)
                }
        )
    }

    suspend fun fetchAppUpdateConfig(userId: String): Result<AppUpdateConfig?> {
        val path = "app_config/update"
        return try {
            requireUserId(userId)
            val snap = appUpdateDoc().get().await()
            val config = if (snap.exists()) {
                snap.toObject(
                    AppUpdateConfig::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                )
            } else {
                null
            }
            Result.success(config)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, path)
            Result.failure(e)
        }
    }

    suspend fun publishAppUpdateConfig(
        userId: String,
        latestVersionCode: Long,
        latestVersionName: String,
        releaseNotes: String,
        downloadUrl: String,
        forceUpdate: Boolean
    ): Result<Unit> {
        val cleanVersionName = latestVersionName.trim().take(30)
        val cleanNotes = releaseNotes.trim().take(1000)
        val cleanUrl = downloadUrl.trim().take(500)
        if (latestVersionCode !in 1L..100_000L) {
            return Result.failure(IllegalArgumentException("Version code must be between 1 and 100000."))
        }
        if (cleanVersionName.isEmpty()) {
            return Result.failure(IllegalArgumentException("Version name cannot be empty."))
        }
        if (cleanUrl.isNotEmpty() && !cleanUrl.startsWith("https://")) {
            return Result.failure(IllegalArgumentException("Download URL must start with https://"))
        }
        val path = "app_config/update"
        return try {
            requireUserId(userId)
            val docRef = appUpdateDoc()
            val existingSnap = docRef.get().await()
            val payload = mapOf(
                "latestVersionCode" to latestVersionCode,
                "latestVersionName" to cleanVersionName,
                "releaseNotes" to cleanNotes,
                "downloadUrl" to cleanUrl,
                "forceUpdate" to forceUpdate,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            if (existingSnap.exists()) {
                docRef.update(payload).await()
            } else {
                docRef.set(payload).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    private fun handleFlowError(
        error: Throwable,
        operationType: OperationType,
        path: String,
        uid: String
    ) {
        val firestoreError = generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
        if (firestoreError == null && error is CancellationException) {
            throw error
        }
        val exceptionToLog = firestoreError ?: (error as? Exception)
        if (exceptionToLog != null && auth.currentUser?.uid == uid) {
            handleFirestoreError(exceptionToLog, operationType, path)
        }
    }
}
