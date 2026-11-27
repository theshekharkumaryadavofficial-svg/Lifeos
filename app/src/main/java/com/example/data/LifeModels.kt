package com.example.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

const val DEFAULT_BLOCKED_PACKAGES_CSV =
    "com.google.android.youtube,com.instagram.android,com.whatsapp,com.android.chrome,com.zhiliaoapp.musically,com.facebook.katana,com.twitter.android"

const val DEFAULT_FOCUS_QUOTE =
    "Discipline beats motivation. Stay focused now and build your future."

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

@Entity(tableName = "user_profile")
data class UserProfile(
    @PrimaryKey val id: String = "local_user",
    val userId: String = "local_user",
    val displayName: String = "Shekhar",
    val tagline: String = "Dreamer • Fighter • Achiever",
    val dailyGoalText: String = "Be 1% Better Than Yesterday",
    val level: Long = 1L,
    val xp: Long = 0L,
    val streakDays: Long = 1L,
    val totalStudyMinutes: Long = 0L,
    val todayStudyMinutes: Long = 0L,
    val dailyStudyTargetMinutes: Long = 480L,
    val focusModeEnabled: Boolean = false,
    val globalBlockedPackagesCsv: String = DEFAULT_BLOCKED_PACKAGES_CSV,
    val focusMotivationQuote: String = DEFAULT_FOCUS_QUOTE,
    val moneyOpeningBalance: Double = 0.0,
    val moneyMonthlyBudget: Double = 10000.0,
    val moneySmsTrackingEnabled: Boolean = true,
    val moneyOnboardingSeen: Boolean = false,
    val trustedSmsSendersCsv: String = "",
    val savingsGoalName: String = "Save ₹10,000",
    val savingsGoalTarget: Double = 10000.0,
    val savingsGoalSaved: Double = 0.0,
    val savingsGoalTargetDate: String = "Dec 31, 2026",
    val smsAuditMigrated: Boolean = false,
    val totalNaamJapCount: Long = 0L,
    val todayNaamJapCount: Long = 0L,
    val todayMalas: Long = 0L,
    val activeMantra: String = "Radha Radha",
    val todayWaterGlasses: Long = 0L,
    val todaySleepHours: Double = 7.0,
    val motivationMode: Boolean = true,
    val lastActiveDate: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "money_transactions",
    indices = [Index(value = ["smsSignature"], unique = true)]
)
data class MoneyTransaction(
    @PrimaryKey val id: String = "",
    val userId: String = "local_user",
    val title: String = "",
    val amount: Double = 0.0,
    val type: String = "EXPENSE", // "INCOME" or "EXPENSE"
    val category: String = "Others",
    val paymentMode: String = "UPI",
    val source: String = "MANUAL", // "SMS" or "MANUAL"
    val status: String = "CONFIRMED", // "CONFIRMED", "PENDING_REVIEW", "QUARANTINED"
    val trustClassification: String = "TRUSTED_PATTERN", // "TRUSTED_PATTERN", "REVIEW_REQUIRED", "HIGH_RISK"
    val riskScore: Int = 0,
    val flagReasonsCsv: String = "", // "||"-separated human-readable explanations
    val matchedTemplateId: String = "",
    val accountSuffix: String = "",
    val referenceId: String = "",
    val userVerified: Boolean = false,
    val smsSender: String = "",
    val smsBody: String = "",
    val smsSignature: String = "",
    val detectedBankBalance: Double? = null,
    val timestampMs: Long = System.currentTimeMillis(),
    val createdAt: Long = System.currentTimeMillis()
) {
    fun flagReasonsList(): List<String> =
        flagReasonsCsv.split("||").map { it.trim() }.filter { it.isNotEmpty() }
}

@Entity(tableName = "study_time_slots")
data class StudyTimeSlot(
    @PrimaryKey val id: String = "",
    val userId: String = "local_user",
    val title: String = "Study Session 1",
    val startHour: Int = 19,
    val startMinute: Int = 0,
    val endHour: Int = 22,
    val endMinute: Int = 0,
    val enabled: Boolean = true,
    val pomodoroEnabled: Boolean = true,
    val countdownEnabled: Boolean = true,
    val blockedPackagesCsv: String = DEFAULT_BLOCKED_PACKAGES_CSV,
    val motivationalQuote: String = DEFAULT_FOCUS_QUOTE,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "life_tasks")
data class LifeTask(
    @PrimaryKey val id: String = "",
    val userId: String = "local_user",
    val title: String = "",
    val category: String = "STUDY",
    val completed: Boolean = false,
    val dueDate: String = "Today",
    val priority: String = "MEDIUM",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "study_subjects")
data class StudySubject(
    @PrimaryKey val id: String = "",
    val userId: String = "local_user",
    val name: String = "",
    val progressPercent: Long = 0L,
    val studyMinutes: Long = 0L,
    val accentKey: String = "CRIMSON",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "study_notes")
data class StudyNote(
    @PrimaryKey val id: String = "",
    val userId: String = "local_user",
    val title: String = "",
    val subjectName: String = "General",
    val content: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "workout_logs")
data class WorkoutLog(
    @PrimaryKey val id: String = "",
    val userId: String = "local_user",
    val title: String = "",
    val activityType: String = "WORKOUT",
    val durationMinutes: Long = 30L,
    val distanceKm: Double = 0.0,
    val caloriesBurned: Long = 180L,
    val dateLabel: String = "Today",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "sadhana_logs")
data class SadhanaLog(
    @PrimaryKey val id: String = "",
    val userId: String = "local_user",
    val mantra: String = "Radha Radha",
    val count: Long = 108L,
    val malas: Long = 1L,
    val meditationMinutes: Long = 10L,
    val dateLabel: String = "Today",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

data class AiChatMessage(
    val id: String,
    val fromUser: Boolean,
    val text: String,
    val streaming: Boolean = false
)

data class QuizQuestion(
    val question: String,
    val options: List<String>,
    val correctIndex: Int,
    val explanation: String
)

enum class TaskCategory(val key: String, val label: String) {
    STUDY("STUDY", "Study"),
    FITNESS("FITNESS", "Fitness"),
    DISCIPLINE("DISCIPLINE", "Discipline"),
    WORK("WORK", "Work");

    companion object {
        fun fromKey(raw: String): TaskCategory =
            entries.firstOrNull { it.key == raw } ?: STUDY
    }
}

enum class WorkoutType(val key: String, val label: String) {
    RUNNING("RUNNING", "Running"),
    WALKING("WALKING", "Walking"),
    WORKOUT("WORKOUT", "Workout"),
    YOGA("YOGA", "Yoga"),
    CYCLING("CYCLING", "Cycling");

    companion object {
        fun fromKey(raw: String): WorkoutType =
            entries.firstOrNull { it.key == raw } ?: WORKOUT
    }
}

enum class SubjectAccent(val key: String) {
    CRIMSON("CRIMSON"),
    GOLD("GOLD"),
    CYAN("CYAN"),
    PURPLE("PURPLE"),
    GREEN("GREEN");

    companion object {
        fun fromKey(raw: String): SubjectAccent =
            entries.firstOrNull { it.key == raw } ?: CRIMSON
    }
}
