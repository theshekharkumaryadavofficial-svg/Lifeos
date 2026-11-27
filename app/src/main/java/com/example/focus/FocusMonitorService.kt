package com.example.focus

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class FocusMonitorService : Service() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var monitorJob: Job? = null
    private var lastTriggeredSlotId: String? = null
    private var lastBlockedLaunchTimeMs: Long = 0L
    private var lastNotificationText: String = ""

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val notification = buildNotification("Monitoring active study schedule & blocked apps")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }

        startMonitoringLoop()
        return START_STICKY
    }

    private fun startMonitoringLoop() {
        if (monitorJob?.isActive == true) return
        monitorJob = serviceScope.launch {
            while (isActive) {
                val now = System.currentTimeMillis()
                val snapshot = FocusPrefsBridge.readSnapshot(applicationContext)

                if (!snapshot.hasAnyScheduledOrActiveRule()) {
                    stopSelf()
                    break
                }

                val activeSlot = snapshot.activeSlotNow(now)
                val enforcing = snapshot.isShieldActivelyEnforcing(now)

                if (enforcing) {
                    val blockedPkgs = snapshot.effectiveBlockedPackages(now)
                    val title = activeSlot?.title ?: "Focus Mode Active"
                    val countdownSec = activeSlot?.let { remainingSecondsInSlot(it, now) } ?: 0
                    val statusLine = if (activeSlot != null) {
                        "$title • ${formatTimerHhMmSs(countdownSec)} left • ${blockedPkgs.size} apps blocked"
                    } else {
                        "Focus Mode ON • ${blockedPkgs.size} distracting apps blocked"
                    }
                    updateNotificationIfChanged(statusLine)

                    // Auto-open animated Focus Shield when a scheduled Study Time Slot starts
                    if (activeSlot != null && activeSlot.id != lastTriggeredSlotId) {
                        lastTriggeredSlotId = activeSlot.id
                        if (canDrawOverlays(applicationContext)) {
                            launchBlockerScreen(
                                blockedAppName = "",
                                slotTitle = activeSlot.title,
                                quote = snapshot.activeQuote(now),
                                pomodoroEnabled = activeSlot.pomodoroEnabled,
                                countdownEnabled = activeSlot.countdownEnabled,
                                slotRemainingSeconds = countdownSec,
                                blockedCsv = toPackageCsv(blockedPkgs),
                                reason = FocusBlockerActivity.REASON_SLOT_STARTED
                            )
                        }
                    }

                    // Actively detect & block restricted apps in foreground
                    if (blockedPkgs.isNotEmpty() && hasUsageStatsPermission(applicationContext)) {
                        val fgPkg = queryForegroundPackage(now)
                        if (fgPkg != null &&
                            fgPkg != packageName &&
                            fgPkg in blockedPkgs &&
                            (now - lastBlockedLaunchTimeMs) > 1200L
                        ) {
                            lastBlockedLaunchTimeMs = now
                            val appInfo = findKnownAppInfo(fgPkg)
                            launchBlockerScreen(
                                blockedAppName = appInfo.displayName,
                                slotTitle = activeSlot?.title ?: "Focus Mode",
                                quote = snapshot.activeQuote(now),
                                pomodoroEnabled = activeSlot?.pomodoroEnabled ?: true,
                                countdownEnabled = activeSlot?.countdownEnabled ?: (activeSlot != null),
                                slotRemainingSeconds = countdownSec,
                                blockedCsv = toPackageCsv(blockedPkgs),
                                reason = FocusBlockerActivity.REASON_APP_BLOCKED
                            )
                        }
                    }
                } else {
                    lastTriggeredSlotId = null
                    val enabledCount = snapshot.slots.count { it.enabled }
                    updateNotificationIfChanged(
                        "Scheduled: $enabledCount study time slot(s) armed for auto-block"
                    )
                }

                delay(1000L)
            }
        }
    }

    private fun queryForegroundPackage(nowMillis: Long): String? {
        return try {
            val usm = getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager
                ?: return null
            val events = usm.queryEvents(nowMillis - 6_000L, nowMillis)
            val event = UsageEvents.Event()
            var latestPkg: String? = null
            var latestTime = 0L
            while (events.hasNextEvent()) {
                events.getNextEvent(event)
                val isForeground = event.eventType == UsageEvents.Event.ACTIVITY_RESUMED ||
                    @Suppress("DEPRECATION")
                    (event.eventType == UsageEvents.Event.MOVE_TO_FOREGROUND)
                if (isForeground && event.timeStamp >= latestTime) {
                    latestTime = event.timeStamp
                    latestPkg = event.packageName
                }
            }
            latestPkg
        } catch (_: Exception) {
            null
        }
    }

    private fun launchBlockerScreen(
        blockedAppName: String,
        slotTitle: String,
        quote: String,
        pomodoroEnabled: Boolean,
        countdownEnabled: Boolean,
        slotRemainingSeconds: Int,
        blockedCsv: String,
        reason: String
    ) {
        try {
            val intent = Intent(applicationContext, FocusBlockerActivity::class.java).apply {
                addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_SINGLE_TOP or
                        Intent.FLAG_ACTIVITY_CLEAR_TOP
                )
                putExtra(FocusBlockerActivity.EXTRA_REASON, reason)
                putExtra(FocusBlockerActivity.EXTRA_BLOCKED_APP_NAME, blockedAppName)
                putExtra(FocusBlockerActivity.EXTRA_SLOT_TITLE, slotTitle)
                putExtra(FocusBlockerActivity.EXTRA_QUOTE, quote)
                putExtra(FocusBlockerActivity.EXTRA_POMODORO_ENABLED, pomodoroEnabled)
                putExtra(FocusBlockerActivity.EXTRA_COUNTDOWN_ENABLED, countdownEnabled)
                putExtra(FocusBlockerActivity.EXTRA_SLOT_REMAINING_SECONDS, slotRemainingSeconds)
                putExtra(FocusBlockerActivity.EXTRA_BLOCKED_CSV, blockedCsv)
            }
            startActivity(intent)
        } catch (_: Exception) {
            // Ignored safely if background activity launch is restricted by OS without overlay permission
        }
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            getString(R.string.focus_shield_notification_channel),
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Keeps LifeOS Focus Mode & Time Slot app blocker active during study sessions"
            setShowBadge(false)
        }
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        nm?.createNotificationChannel(channel)
    }

    private fun buildNotification(contentText: String): Notification {
        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(EXTRA_OPEN_FOCUS_SCREEN, true)
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            101,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(getString(R.string.focus_shield_notification_title))
            .setContentText(contentText)
            .setOngoing(true)
            .setSilent(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun updateNotificationIfChanged(newText: String) {
        if (newText == lastNotificationText) return
        lastNotificationText = newText
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        nm?.notify(NOTIFICATION_ID, buildNotification(newText))
    }

    override fun onDestroy() {
        monitorJob?.cancel()
        serviceScope.cancel()
        super.onDestroy()
    }

    companion object {
        private const val CHANNEL_ID = "lifeos_focus_shield"
        private const val NOTIFICATION_ID = 4102
        const val EXTRA_OPEN_FOCUS_SCREEN = "extra_open_focus_screen"
    }
}
