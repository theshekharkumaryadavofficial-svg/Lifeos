package com.example.data

import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withTimeout
import org.json.JSONObject

private const val TAG = "AiTutorRepository"

class AiTutorRepository {

    private val chatModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            systemInstruction = content {
                text(
                    "You are LifeOS AI Tutor & Personal Discipline Coach. " +
                        "Help the user excel in Study (Maths, Physics, Electricity, NDA/competitive exams, summaries, study plans), " +
                        "Fitness & Running, Health & Nutrition (neutral, encouraging, never extreme), and Self-Discipline / Naam Jap / Meditation. " +
                        "Respond warmly, clearly, and concisely in English or Hinglish matching the user's language."
                )
            }
        )
    }

    private val quizSchema by lazy {
        Schema.obj(
            properties = mapOf(
                "questions" to Schema.array(
                    items = Schema.obj(
                        properties = mapOf(
                            "question" to Schema.string(description = "Clear multiple choice question"),
                            "options" to Schema.array(
                                items = Schema.string(),
                                description = "Exactly 4 short answer choices"
                            ),
                            "correctIndex" to Schema.integer(description = "Zero-based index (0 to 3) of the correct option"),
                            "explanation" to Schema.string(description = "Concise 1-2 sentence explanation of why the answer is right")
                        )
                    ),
                    description = "List of 5 multiple-choice questions"
                )
            )
        )
    }

    private val quizModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = quizSchema
            }
        )
    }

    fun streamTutorReply(
        history: List<AiChatMessage>,
        userPrompt: String
    ): Flow<String> = flow {
        val conversation = history.takeLast(8).map { msg ->
            content(if (msg.fromUser) "user" else "model") { text(msg.text) }
        }
        val chat = chatModel.startChat(history = conversation)
        val accumulated = StringBuilder()
        withTimeout(60_000L) {
            chat.sendMessageStream(userPrompt).collect { chunk ->
                val part = chunk.text.orEmpty()
                if (part.isNotEmpty()) {
                    accumulated.append(part)
                    emit(accumulated.toString())
                }
            }
        }
        if (accumulated.isBlank()) {
            throw IllegalStateException("AI Tutor returned an empty response.")
        }
    }

    suspend fun generateQuiz(topic: String): List<QuizQuestion> {
        return try {
            val prompt =
                "Create a 5-question multiple choice quiz on the topic: '$topic'. " +
                    "Make questions clear, educational, and exam-ready with exactly 4 options each."
            val response = withTimeout(60_000L) {
                quizModel.generateContent(prompt)
            }
            val jsonString = response.text
                ?: throw IllegalStateException("Quiz model returned an empty response.")
            val root = JSONObject(jsonString)
            val arr = root.getJSONArray("questions")
            val result = mutableListOf<QuizQuestion>()
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                val optsArr = obj.getJSONArray("options")
                val options = (0 until optsArr.length()).map { optsArr.getString(it) }
                if (options.size >= 2) {
                    result.add(
                        QuizQuestion(
                            question = obj.getString("question"),
                            options = options,
                            correctIndex = obj.optInt("correctIndex", 0).coerceIn(0, options.lastIndex),
                            explanation = obj.optString("explanation", "")
                        )
                    )
                }
            }
            if (result.isEmpty()) {
                throw IllegalStateException("No quiz questions could be parsed.")
            }
            result
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Quiz generation timed out", e)
            throw IllegalStateException("Quiz generation timed out after 60 seconds. Please try again.", e)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Quiz generation failed", e)
            throw e
        }
    }

    suspend fun generateProgressInsight(
        profile: UserProfile,
        completedTasks: Int,
        totalTasks: Int,
        subjects: List<StudySubject>,
        workoutMinutes: Long
    ): String {
        return try {
            val subjectSummary = if (subjects.isEmpty()) {
                "No subjects added yet"
            } else {
                subjects.joinToString { "${it.name}: ${it.progressPercent}%" }
            }
            val prompt = """
                Analyze this user's LifeOS progress and write a short, energizing 3-bullet coaching message (under 90 words total):
                - Name: ${profile.displayName}, Level ${profile.level}, Streak: ${profile.streakDays} days
                - Tasks done: $completedTasks / $totalTasks
                - Total Study: ${profile.totalStudyMinutes} min | Subjects: $subjectSummary
                - Today's Naam Jap: ${profile.todayNaamJapCount} beads (${profile.todayMalas} malas) | Lifetime: ${profile.totalNaamJapCount}
                - Fitness workouts logged: $workoutMinutes min | Today Water: ${profile.todayWaterGlasses}/8 glasses
                Keep it encouraging, balanced, and actionable for tomorrow.
            """.trimIndent()
            val response = withTimeout(60_000L) {
                chatModel.generateContent(prompt)
            }
            response.text?.takeIf { it.isNotBlank() }
                ?: throw IllegalStateException("AI Coach returned an empty insight.")
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Progress insight timed out", e)
            throw IllegalStateException("Progress analysis timed out. Please try again.", e)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Progress insight failed", e)
            throw e
        }
    }
}
