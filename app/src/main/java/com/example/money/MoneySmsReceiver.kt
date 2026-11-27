package com.example.money

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import com.example.data.LifeDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MoneySmsReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return

        val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
        if (messages.isNullOrEmpty()) return

        // Group multipart SMS chunks from the same sender into one complete message body
        val groupedBySender = linkedMapOf<String, StringBuilder>()
        var latestTimestamp = System.currentTimeMillis()
        for (sms in messages) {
            val sender = sms.originatingAddress.orEmpty().ifBlank { "BANK" }
            val bodyPart = sms.messageBody.orEmpty()
            if (sms.timestampMillis > 0L) {
                latestTimestamp = sms.timestampMillis
            }
            groupedBySender.getOrPut(sender) { StringBuilder() }.append(bodyPart)
        }

        val pendingResult = goAsync()
        val appContext = context.applicationContext
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val dao = LifeDatabase.getInstance(appContext).lifeDao()
                val profile = dao.getProfileOnce()
                if (profile != null && !profile.moneySmsTrackingEnabled) {
                    return@launch
                }

                for ((sender, bodyBuilder) in groupedBySender) {
                    val parsed = SmsMoneyParser.parse(
                        body = bodyBuilder.toString(),
                        sender = sender,
                        timestampMs = latestTimestamp
                    )
                    if (parsed != null) {
                        dao.insertMoneyTransactionIgnore(parsed.toEntity())
                    }
                }
            } catch (_: Exception) {
                // Keep receiver resilient on background delivery
            } finally {
                pendingResult.finish()
            }
        }
    }
}
