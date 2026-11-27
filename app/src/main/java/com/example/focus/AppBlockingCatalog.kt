package com.example.focus

import android.app.AppOpsManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Process
import android.provider.Settings
import androidx.compose.ui.graphics.Color
import com.example.data.StudyTimeSlot
import java.util.Calendar
import java.util.Locale

data class BlockableAppInfo(
    val packageName: String,
    val displayName: String,
    val shortBadge: String,
    val brandColor: Color,
    val accentColor: Color = brandColor,
    val isInstalled: Boolean = false
)

val POPULAR_DISTRACTING_APPS: List<BlockableAppInfo> = listOf(
    BlockableAppInfo(
        packageName = "com.google.android.youtube",
        displayName = "YouTube",
        shortBadge = "YT",
        brandColor = Color(0xFFFF0000),
        accentColor = Color(0xFFCC0000)
    ),
    BlockableAppInfo(
        packageName = "com.instagram.android",
        displayName = "Instagram",
        shortBadge = "IG",
        brandColor = Color(0xFFE1306C),
        accentColor = Color(0xFFF77737)
    ),
    BlockableAppInfo(
        packageName = "com.whatsapp",
        displayName = "WhatsApp",
        shortBadge = "WA",
        brandColor = Color(0xFF25D366),
        accentColor = Color(0xFF128C7E)
    ),
    BlockableAppInfo(
        packageName = "com.android.chrome",
        displayName = "Chrome",
        shortBadge = "CR",
        brandColor = Color(0xFF4285F4),
        accentColor = Color(0xFF34A853)
    ),
    BlockableAppInfo(
        packageName = "com.zhiliaoapp.musically",
        displayName = "TikTok",
        shortBadge = "TT",
        brandColor = Color(0xFF111111),
        accentColor = Color(0xFFEE1D52)
    ),
    BlockableAppInfo(
        packageName = "com.facebook.katana",
        displayName = "Facebook",
        shortBadge = "f",
        brandColor = Color(0xFF1877F2),
        accentColor = Color(0xFF0C5DC7)
    ),
    BlockableAppInfo(
        packageName = "com.twitter.android",
        displayName = "X",
        shortBadge = "X",
        brandColor = Color(0xFF14171A),
        accentColor = Color(0xFF334155)
    ),
    BlockableAppInfo(
        packageName = "com.snapchat.android",
        displayName = "Snapchat",
        shortBadge = "SC",
        brandColor = Color(0xFFFACC15),
        accentColor = Color(0xFFEAB308)
    ),
    BlockableAppInfo(
        packageName = "org.telegram.messenger",
        displayName = "Telegram",
        shortBadge = "TG",
        brandColor = Color(0xFF26A5E4),
        accentColor = Color(0xFF0088CC)
    ),
    BlockableAppInfo(
        packageName = "com.reddit.frontpage",
        displayName = "Reddit",
        shortBadge = "RD",
        brandColor = Color(0xFFFF4500),
        accentColor = Color(0xFFD93A00)
    ),
    BlockableAppInfo(
        packageName = "com.netflix.mediaclient",
        displayName = "Netflix",
        shortBadge = "N",
        brandColor = Color(0xFFE50914),
        accentColor = Color(0xFFB20710)
    )
)

val STUDY_MOTIVATIONAL_QUOTES: List<String> = listOf(
    "Discipline beats motivation. Stay focused now and build your future.",
    "Your future self is watching you right now. Keep going, don't quit.",
    "Block the distractions. Master your craft. Own your destiny.",
    "Small focused hours every single day create unstoppable results.",
    "Success is earned in silence when nobody is watching.",
    "Pain of discipline weighs ounces; regret weighs tons. Study deep!"
)

private val SAFE_SYSTEM_PACKAGES = setOf(
    "com.android.settings",
    "com.android.systemui",
    "com.google.android.dialer",
    "com.android.phone",
    "com.android.emergency"
)

fun parsePackageCsv(csv: String): List<String> =
    csv.split(",")
        .map { it.trim() }
        .filter { it.isNotEmpty() }
        .distinct()

fun toPackageCsv(packages: Collection<String>): String =
    packages.map { it.trim() }
        .filter { it.isNotEmpty() }
        .distinct()
        .joinToString(",")

fun findKnownAppInfo(packageName: String): BlockableAppInfo {
    POPULAR_DISTRACTING_APPS.firstOrNull { it.packageName == packageName }?.let { return it }
    val rawTail = packageName.substringAfterLast('.').ifBlank { packageName }
    val cleanName = rawTail.replaceFirstChar { it.uppercase(Locale.getDefault()) }
    val badge = cleanName.take(2).uppercase(Locale.getDefault())
    val palette = listOf(
        Color(0xFF6366F1),
        Color(0xFF06B6D4),
        Color(0xFFEC4899),
        Color(0xFF10B981),
        Color(0xFFF59E0B),
        Color(0xFF8B5CF6)
    )
    val idx = (packageName.hashCode() and Int.MAX_VALUE) % palette.size
    return BlockableAppInfo(
        packageName = packageName,
        displayName = cleanName,
        shortBadge = badge,
        brandColor = palette[idx],
        isInstalled = true
    )
}

fun loadBlockableApps(context: Context): List<BlockableAppInfo> {
    val pm = context.packageManager
    val ownPkg = context.packageName
    val installedMap = linkedMapOf<String, String>()

    try {
        val launcherIntent = Intent(Intent.ACTION_MAIN, null).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }
        val resolved = pm.queryIntentActivities(launcherIntent, 0)
        for (ri in resolved) {
            val pkg = ri.activityInfo?.packageName ?: continue
            if (pkg == ownPkg || pkg in SAFE_SYSTEM_PACKAGES) continue
            val label = ri.loadLabel(pm)?.toString()?.trim().orEmpty()
            if (label.isNotEmpty()) {
                installedMap[pkg] = label
            }
        }
    } catch (_: Exception) {
        // Fallback to popular catalog if launcher query is restricted
    }

    val result = mutableListOf<BlockableAppInfo>()
    val seen = hashSetOf<String>()

    for (popular in POPULAR_DISTRACTING_APPS) {
        val installedLabel = installedMap[popular.packageName]
        result.add(
            popular.copy(
                displayName = installedLabel ?: popular.displayName,
                isInstalled = installedLabel != null
            )
        )
        seen.add(popular.packageName)
    }

    installedMap.entries
        .filter { it.key !in seen }
        .sortedBy { it.value.lowercase(Locale.getDefault()) }
        .forEach { (pkg, label) ->
            val info = findKnownAppInfo(pkg)
            result.add(info.copy(displayName = label, shortBadge = label.take(2).uppercase(Locale.getDefault()), isInstalled = true))
        }

    return result
}

fun formatHourMinute12h(hour24: Int, minute: Int): String {
    val h = hour24.coerceIn(0, 23)
    val m = minute.coerceIn(0, 59)
    val suffix = if (h < 12) "AM" else "PM"
    val displayHour = when {
        h == 0 -> 12
        h > 12 -> h - 12
        else -> h
    }
    return String.format(Locale.US, "%d:%02d %s", displayHour, m, suffix)
}

fun slotDurationMinutes(startHour: Int, startMinute: Int, endHour: Int, endMinute: Int): Int {
    val startTotal = startHour.coerceIn(0, 23) * 60 + startMinute.coerceIn(0, 59)
    val endTotal = endHour.coerceIn(0, 23) * 60 + endMinute.coerceIn(0, 59)
    val diff = if (endTotal > startTotal) {
        endTotal - startTotal
    } else {
        (24 * 60 - startTotal) + endTotal
    }
    return diff.coerceAtLeast(1)
}

fun slotDurationLabel(startHour: Int, startMinute: Int, endHour: Int, endMinute: Int): String {
    val mins = slotDurationMinutes(startHour, startMinute, endHour, endMinute)
    val hours = mins / 60
    val remMins = mins % 60
    return when {
        hours > 0 && remMins == 0 -> if (hours == 1) "1 hr" else "$hours hrs"
        hours > 0 -> "${hours}h ${remMins}m"
        else -> "${remMins} mins"
    }
}

fun isTimeSlotActiveNow(slot: StudyTimeSlot, nowMillis: Long = System.currentTimeMillis()): Boolean {
    if (!slot.enabled) return false
    val cal = Calendar.getInstance().apply { timeInMillis = nowMillis }
    val nowMin = cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)
    val startMin = slot.startHour * 60 + slot.startMinute
    val endMin = slot.endHour * 60 + slot.endMinute
    return if (startMin == endMin) {
        false
    } else if (startMin < endMin) {
        nowMin in startMin until endMin
    } else {
        // Crosses midnight, e.g., 10:00 PM (1320) to 12:00 AM (0) or 1:00 AM (60)
        nowMin >= startMin || nowMin < endMin
    }
}

fun remainingSecondsInSlot(slot: StudyTimeSlot, nowMillis: Long = System.currentTimeMillis()): Int {
    val cal = Calendar.getInstance().apply { timeInMillis = nowMillis }
    val nowSec = cal.get(Calendar.HOUR_OF_DAY) * 3600 +
        cal.get(Calendar.MINUTE) * 60 +
        cal.get(Calendar.SECOND)
    val endSec = slot.endHour * 3600 + slot.endMinute * 60
    return if (endSec > nowSec) {
        endSec - nowSec
    } else {
        (24 * 3600 - nowSec) + endSec
    }.coerceAtLeast(0)
}

fun formatTimerMmSs(totalSeconds: Int): String {
    val safe = totalSeconds.coerceAtLeast(0)
    val mins = safe / 60
    val secs = safe % 60
    return String.format(Locale.US, "%02d:%02d", mins, secs)
}

fun formatTimerHhMmSs(totalSeconds: Int): String {
    val safe = totalSeconds.coerceAtLeast(0)
    val hrs = safe / 3600
    val mins = (safe % 3600) / 60
    val secs = safe % 60
    return if (hrs > 0) {
        String.format(Locale.US, "%02d:%02d:%02d", hrs, mins, secs)
    } else {
        String.format(Locale.US, "%02d:%02d", mins, secs)
    }
}

fun hasUsageStatsPermission(context: Context): Boolean {
    return try {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as? AppOpsManager
            ?: return false
        val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            appOps.unsafeCheckOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                context.packageName
            )
        } else {
            @Suppress("DEPRECATION")
            appOps.checkOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                context.packageName
            )
        }
        mode == AppOpsManager.MODE_ALLOWED
    } catch (_: Exception) {
        false
    }
}

fun canDrawOverlays(context: Context): Boolean {
    return try {
        Settings.canDrawOverlays(context)
    } catch (_: Exception) {
        false
    }
}

fun openUsageAccessSettings(context: Context) {
    try {
        val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    } catch (_: Exception) {
        val fallback = Intent(Settings.ACTION_SETTINGS).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(fallback)
    }
}

fun openOverlaySettings(context: Context) {
    try {
        val intent = Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            Uri.parse("package:${context.packageName}")
        ).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    } catch (_: Exception) {
        val fallback = Intent(Settings.ACTION_SETTINGS).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(fallback)
    }
}
