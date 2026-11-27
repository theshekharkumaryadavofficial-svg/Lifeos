package com.example.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
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
        LifeTask::class,
        StudySubject::class,
        StudyNote::class,
        WorkoutLog::class,
        SadhanaLog::class
    ],
    version = 1,
    exportSchema = false
)
abstract class LifeDatabase : RoomDatabase() {
    abstract fun lifeDao(): LifeDao

    companion object {
        @Volatile
        private var INSTANCE: LifeDatabase? = null

        fun getInstance(context: Context): LifeDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    LifeDatabase::class.java,
                    "lifeos_local.db"
                ).fallbackToDestructiveMigration(dropAllTables = false)
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
