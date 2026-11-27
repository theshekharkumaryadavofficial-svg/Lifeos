package com.example.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Update
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.flow.Flow

@Dao
interface LifeDao {
    // Profile
    @Query("SELECT * FROM user_profile WHERE id = :id LIMIT 1")
    fun observeProfile(id: String = "local_user"): Flow<UserProfile?>

    @Query("SELECT * FROM user_profile WHERE id = :id LIMIT 1")
    suspend fun getProfileOnce(id: String = "local_user"): UserProfile?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertProfile(profile: UserProfile)

    // Money Transactions
    @Query("SELECT * FROM money_transactions ORDER BY timestampMs DESC, createdAt DESC")
    fun observeMoneyTransactions(): Flow<List<MoneyTransaction>>

    @Query("SELECT * FROM money_transactions ORDER BY timestampMs DESC, createdAt DESC")
    suspend fun getMoneyTransactionsOnce(): List<MoneyTransaction>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertMoneyTransactionIgnore(tx: MoneyTransaction): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertMoneyTransactionsIgnore(txList: List<MoneyTransaction>): List<Long>

    @Update
    suspend fun updateMoneyTransaction(tx: MoneyTransaction)

    @Query("DELETE FROM money_transactions WHERE id = :txId")
    suspend fun deleteMoneyTransaction(txId: String)

    @Query("DELETE FROM money_transactions WHERE source = 'SMS'")
    suspend fun deleteAllImportedSmsTransactions()

    @Query("DELETE FROM money_transactions WHERE status = 'QUARANTINED'")
    suspend fun deleteAllQuarantinedTransactions()

    @Query("DELETE FROM money_transactions")
    suspend fun clearAllMoneyTransactions()

    // Study Time Slots
    @Query("SELECT * FROM study_time_slots ORDER BY startHour ASC, startMinute ASC, createdAt ASC")
    fun observeTimeSlots(): Flow<List<StudyTimeSlot>>

    @Query("SELECT * FROM study_time_slots ORDER BY startHour ASC, startMinute ASC, createdAt ASC")
    suspend fun getTimeSlotsOnce(): List<StudyTimeSlot>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertTimeSlot(slot: StudyTimeSlot)

    @Query("DELETE FROM study_time_slots WHERE id = :slotId")
    suspend fun deleteTimeSlot(slotId: String)

    // Tasks
    @Query("SELECT * FROM life_tasks ORDER BY createdAt DESC")
    fun observeTasks(): Flow<List<LifeTask>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: LifeTask)

    @Query("UPDATE life_tasks SET completed = :completed, updatedAt = :updatedAt WHERE id = :taskId")
    suspend fun updateTaskCompleted(taskId: String, completed: Boolean, updatedAt: Long)

    @Query("DELETE FROM life_tasks WHERE id = :taskId")
    suspend fun deleteTask(taskId: String)

    // Subjects
    @Query("SELECT * FROM study_subjects ORDER BY progressPercent DESC, updatedAt DESC")
    fun observeSubjects(): Flow<List<StudySubject>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSubject(subject: StudySubject)

    @Query(
        "UPDATE study_subjects SET progressPercent = :progressPercent, studyMinutes = :studyMinutes, updatedAt = :updatedAt WHERE id = :subjectId"
    )
    suspend fun updateSubjectProgress(
        subjectId: String,
        progressPercent: Long,
        studyMinutes: Long,
        updatedAt: Long
    )

    @Query("DELETE FROM study_subjects WHERE id = :subjectId")
    suspend fun deleteSubject(subjectId: String)

    // Notes
    @Query("SELECT * FROM study_notes ORDER BY updatedAt DESC")
    fun observeNotes(): Flow<List<StudyNote>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNote(note: StudyNote)

    @Query("DELETE FROM study_notes WHERE id = :noteId")
    suspend fun deleteNote(noteId: String)

    // Workouts
    @Query("SELECT * FROM workout_logs ORDER BY createdAt DESC")
    fun observeWorkouts(): Flow<List<WorkoutLog>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWorkout(workout: WorkoutLog)

    @Query("DELETE FROM workout_logs WHERE id = :workoutId")
    suspend fun deleteWorkout(workoutId: String)

    // Sadhana Logs
    @Query("SELECT * FROM sadhana_logs ORDER BY createdAt DESC")
    fun observeSadhanaLogs(): Flow<List<SadhanaLog>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSadhanaLog(log: SadhanaLog)
}

@Database(
    entities = [
        UserProfile::class,
        MoneyTransaction::class,
        StudyTimeSlot::class,
        LifeTask::class,
        StudySubject::class,
        StudyNote::class,
        WorkoutLog::class,
        SadhanaLog::class
    ],
    version = 4,
    exportSchema = false
)
abstract class LifeDatabase : RoomDatabase() {
    abstract fun lifeDao(): LifeDao

    companion object {
        @Volatile
        private var INSTANCE: LifeDatabase? = null

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE user_profile ADD COLUMN todayStudyMinutes INTEGER NOT NULL DEFAULT 0"
                )
                db.execSQL(
                    "ALTER TABLE user_profile ADD COLUMN dailyStudyTargetMinutes INTEGER NOT NULL DEFAULT 480"
                )
                db.execSQL(
                    "ALTER TABLE user_profile ADD COLUMN focusModeEnabled INTEGER NOT NULL DEFAULT 0"
                )
                db.execSQL(
                    "ALTER TABLE user_profile ADD COLUMN globalBlockedPackagesCsv TEXT NOT NULL DEFAULT '$DEFAULT_BLOCKED_PACKAGES_CSV'"
                )
                db.execSQL(
                    "ALTER TABLE user_profile ADD COLUMN focusMotivationQuote TEXT NOT NULL DEFAULT '$DEFAULT_FOCUS_QUOTE'"
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS study_time_slots (
                        id TEXT NOT NULL PRIMARY KEY,
                        userId TEXT NOT NULL,
                        title TEXT NOT NULL,
                        startHour INTEGER NOT NULL,
                        startMinute INTEGER NOT NULL,
                        endHour INTEGER NOT NULL,
                        endMinute INTEGER NOT NULL,
                        enabled INTEGER NOT NULL,
                        pomodoroEnabled INTEGER NOT NULL,
                        countdownEnabled INTEGER NOT NULL,
                        blockedPackagesCsv TEXT NOT NULL,
                        motivationalQuote TEXT NOT NULL,
                        createdAt INTEGER NOT NULL,
                        updatedAt INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
            }
        }

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE user_profile ADD COLUMN moneyOpeningBalance REAL NOT NULL DEFAULT 0.0"
                )
                db.execSQL(
                    "ALTER TABLE user_profile ADD COLUMN moneyMonthlyBudget REAL NOT NULL DEFAULT 10000.0"
                )
                db.execSQL(
                    "ALTER TABLE user_profile ADD COLUMN moneySmsTrackingEnabled INTEGER NOT NULL DEFAULT 1"
                )
                db.execSQL(
                    "ALTER TABLE user_profile ADD COLUMN moneyOnboardingSeen INTEGER NOT NULL DEFAULT 0"
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS money_transactions (
                        id TEXT NOT NULL PRIMARY KEY,
                        userId TEXT NOT NULL,
                        title TEXT NOT NULL,
                        amount REAL NOT NULL,
                        type TEXT NOT NULL,
                        category TEXT NOT NULL,
                        paymentMode TEXT NOT NULL,
                        source TEXT NOT NULL,
                        smsSender TEXT NOT NULL,
                        smsBody TEXT NOT NULL,
                        smsSignature TEXT NOT NULL,
                        detectedBankBalance REAL,
                        timestampMs INTEGER NOT NULL,
                        createdAt INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    "CREATE UNIQUE INDEX IF NOT EXISTS index_money_transactions_smsSignature ON money_transactions (smsSignature)"
                )
            }
        }

        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE user_profile ADD COLUMN trustedSmsSendersCsv TEXT NOT NULL DEFAULT ''"
                )
                db.execSQL(
                    "ALTER TABLE user_profile ADD COLUMN savingsGoalName TEXT NOT NULL DEFAULT 'Save ₹10,000'"
                )
                db.execSQL(
                    "ALTER TABLE user_profile ADD COLUMN savingsGoalTarget REAL NOT NULL DEFAULT 10000.0"
                )
                db.execSQL(
                    "ALTER TABLE user_profile ADD COLUMN savingsGoalSaved REAL NOT NULL DEFAULT 0.0"
                )
                db.execSQL(
                    "ALTER TABLE user_profile ADD COLUMN savingsGoalTargetDate TEXT NOT NULL DEFAULT 'Dec 31, 2026'"
                )
                db.execSQL(
                    "ALTER TABLE user_profile ADD COLUMN smsAuditMigrated INTEGER NOT NULL DEFAULT 0"
                )

                db.execSQL(
                    "ALTER TABLE money_transactions ADD COLUMN status TEXT NOT NULL DEFAULT 'CONFIRMED'"
                )
                db.execSQL(
                    "ALTER TABLE money_transactions ADD COLUMN trustClassification TEXT NOT NULL DEFAULT 'TRUSTED_PATTERN'"
                )
                db.execSQL(
                    "ALTER TABLE money_transactions ADD COLUMN riskScore INTEGER NOT NULL DEFAULT 0"
                )
                db.execSQL(
                    "ALTER TABLE money_transactions ADD COLUMN flagReasonsCsv TEXT NOT NULL DEFAULT ''"
                )
                db.execSQL(
                    "ALTER TABLE money_transactions ADD COLUMN matchedTemplateId TEXT NOT NULL DEFAULT ''"
                )
                db.execSQL(
                    "ALTER TABLE money_transactions ADD COLUMN accountSuffix TEXT NOT NULL DEFAULT ''"
                )
                db.execSQL(
                    "ALTER TABLE money_transactions ADD COLUMN referenceId TEXT NOT NULL DEFAULT ''"
                )
                db.execSQL(
                    "ALTER TABLE money_transactions ADD COLUMN userVerified INTEGER NOT NULL DEFAULT 0"
                )
            }
        }

        fun getInstance(context: Context): LifeDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    LifeDatabase::class.java,
                    "lifeos_local.db"
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
                    .fallbackToDestructiveMigration(dropAllTables = true)
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
