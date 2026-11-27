package com.example.focus

import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import com.example.data.DEFAULT_BLOCKED_PACKAGES_CSV
import com.example.data.DEFAULT_FOCUS_QUOTE
import com.example.data.StudyTimeSlot
import org.json.JSONArray
import org.json.JSONObject

data class FocusRuntimeSnapshot(
    val manualFocusOn: Boolean,
    val focusStartedAtMs: Long,
    val globalBlockedCsv: String,
    val motivationalQuote: String,
    val slots: List<StudyTimeSlot>
) {
    fun activeSlotNow(nowMillis: Long = System.currentTimeMillis()): StudyTimeSlot? =
        slots.firstOrNull { isTimeSlotActiveNow(it, nowMillis) }

    fun isShieldActivelyEnforcing(nowMillis: Long = System.currentTimeMillis()): Boolean =
        manualFocusOn || activeSlotNow(nowMillis) != null

    fun hasAnyScheduledOrActiveRule(): Boolean =
        manualFocusOn || slots.any { it.enabled }

    fun effectiveBlockedPackages(nowMillis: Long = System.currentTimeMillis()): Set<String> {
        val activeSlot = activeSlotNow(nowMillis)
        val combined = linkedSetOf<String>()
        if (manualFocusOn) {
            combined.addAll(parsePackageCsv(globalBlockedCsv))
        }
        if (activeSlot != null) {
            val slotPkgs = parsePackageCsv(activeSlot.blockedPackagesCsv)
            if (slotPkgs.isNotEmpty()) {
                combined.addAll(slotPkgs)
            } else {
                combined.addAll(parsePackageCsv(globalBlockedCsv))
            }
        }
        return combined
    }

    fun activeQuote(nowMillis: Long = System.currentTimeMillis()): String {
        val slotQuote = activeSlotNow(nowMillis)?.motivationalQuote?.trim().orEmpty()
        return slotQuote.ifEmpty { motivationalQuote.ifBlank { DEFAULT_FOCUS_QUOTE } }
    }
}

object FocusPrefsBridge {
    private const val PREFS_NAME = "lifeos_focus_engine_prefs"
    private const val KEY_MANUAL_FOCUS = "manual_focus_on"
    private const val KEY_FOCUS_STARTED_AT = "focus_started_at_ms"
    private const val KEY_GLOBAL_BLOCKED = "global_blocked_csv"
    private const val KEY_MOTIVATION_QUOTE = "motivation_quote"
    private const val KEY_SLOTS_JSON = "study_slots_json"

    fun saveSnapshot(
        context: Context,
        manualFocusOn: Boolean,
        globalBlockedCsv: String,
        motivationalQuote: String,
        slots: List<StudyTimeSlot>
    ) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val prevManual = prefs.getBoolean(KEY_MANUAL_FOCUS, false)
        val existingStart = prefs.getLong(KEY_FOCUS_STARTED_AT, 0L)
        val newStart = when {
            manualFocusOn && !prevManual -> System.currentTimeMillis()
            manualFocusOn && existingStart > 0L -> existingStart
            manualFocusOn -> System.currentTimeMillis()
            else -> 0L
        }

        val jsonArr = JSONArray()
        for (slot in slots) {
            val obj = JSONObject().apply {
                put("id", slot.id)
                put("title", slot.title)
                put("startHour", slot.startHour)
                put("startMinute", slot.startMinute)
                put("endHour", slot.endHour)
                put("endMinute", slot.endMinute)
                put("enabled", slot.enabled)
                put("pomodoroEnabled", slot.pomodoroEnabled)
                put("countdownEnabled", slot.countdownEnabled)
                put("blockedPackagesCsv", slot.blockedPackagesCsv)
                put("motivationalQuote", slot.motivationalQuote)
            }
            jsonArr.put(obj)
        }

        prefs.edit()
            .putBoolean(KEY_MANUAL_FOCUS, manualFocusOn)
            .putLong(KEY_FOCUS_STARTED_AT, newStart)
            .putString(KEY_GLOBAL_BLOCKED, globalBlockedCsv)
            .putString(KEY_MOTIVATION_QUOTE, motivationalQuote.ifBlank { DEFAULT_FOCUS_QUOTE })
            .putString(KEY_SLOTS_JSON, jsonArr.toString())
            .apply()

        syncServiceLifecycle(context, manualFocusOn || slots.any { it.enabled })
    }

    fun readSnapshot(context: Context): FocusRuntimeSnapshot {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val manual = prefs.getBoolean(KEY_MANUAL_FOCUS, false)
        val startedAt = prefs.getLong(KEY_FOCUS_STARTED_AT, 0L)
        val globalCsv = prefs.getString(KEY_GLOBAL_BLOCKED, DEFAULT_BLOCKED_PACKAGES_CSV)
            ?: DEFAULT_BLOCKED_PACKAGES_CSV
        val quote = prefs.getString(KEY_MOTIVATION_QUOTE, DEFAULT_FOCUS_QUOTE)
            ?: DEFAULT_FOCUS_QUOTE
        val rawJson = prefs.getString(KEY_SLOTS_JSON, "[]") ?: "[]"

        val slots = mutableListOf<StudyTimeSlot>()
        try {
            val arr = JSONArray(rawJson)
            for (i in 0 until arr.length()) {
                val obj = arr.optJSONObject(i) ?: continue
                slots.add(
                    StudyTimeSlot(
                        id = obj.optString("id"),
                        title = obj.optString("title", "Study Session"),
                        startHour = obj.optInt("startHour", 19),
                        startMinute = obj.optInt("startMinute", 0),
                        endHour = obj.optInt("endHour", 22),
                        endMinute = obj.optInt("endMinute", 0),
                        enabled = obj.optBoolean("enabled", true),
                        pomodoroEnabled = obj.optBoolean("pomodoroEnabled", true),
                        countdownEnabled = obj.optBoolean("countdownEnabled", true),
                        blockedPackagesCsv = obj.optString("blockedPackagesCsv", globalCsv),
                        motivationalQuote = obj.optString("motivationalQuote", quote)
                    )
                )
            }
        } catch (_: Exception) {
            // Keep empty list if corrupted
        }

        return FocusRuntimeSnapshot(
            manualFocusOn = manual,
            focusStartedAtMs = startedAt,
            globalBlockedCsv = globalCsv,
            motivationalQuote = quote,
            slots = slots
        )
    }

    fun syncServiceLifecycle(context: Context, shouldRun: Boolean) {
        try {
            val intent = Intent(context, FocusMonitorService::class.java)
            if (shouldRun) {
                ContextCompat.startForegroundService(context, intent)
            } else {
                context.stopService(intent)
            }
        } catch (_: Exception) {
            // Foreground service restrictions on background start handled safely
        }
    }
}
