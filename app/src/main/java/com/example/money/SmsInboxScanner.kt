package com.example.money

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.provider.Telephony
import androidx.core.content.ContextCompat
import com.example.data.MoneyTransaction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object SmsInboxScanner {

    fun hasSmsPermissions(context: Context): Boolean {
        val readGranted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_SMS
        ) == PackageManager.PERMISSION_GRANTED
        val receiveGranted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECEIVE_SMS
        ) == PackageManager.PERMISSION_GRANTED
        return readGranted && receiveGranted
    }

    fun hasReadSmsPermission(context: Context): Boolean =
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_SMS
        ) == PackageManager.PERMISSION_GRANTED

    suspend fun scanRecentBankSms(
        context: Context,
        daysBack: Int = 90,
        maxMessages: Int = 600
    ): List<MoneyTransaction> = withContext(Dispatchers.IO) {
        if (!hasReadSmsPermission(context)) return@withContext emptyList()

        val cutoffMs = System.currentTimeMillis() - (daysBack.toLong() * 24L * 60L * 60L * 1000L)
        val parsedList = mutableListOf<MoneyTransaction>()

        val projection = arrayOf(
            Telephony.Sms.ADDRESS,
            Telephony.Sms.BODY,
            Telephony.Sms.DATE
        )
        val selection = "${Telephony.Sms.DATE} >= ?"
        val selectionArgs = arrayOf(cutoffMs.toString())
        val sortOrder = "${Telephony.Sms.DATE} DESC"

        try {
            context.contentResolver.query(
                Telephony.Sms.Inbox.CONTENT_URI,
                projection,
                selection,
                selectionArgs,
                sortOrder
            )?.use { cursor ->
                val addrIdx = cursor.getColumnIndex(Telephony.Sms.ADDRESS)
                val bodyIdx = cursor.getColumnIndex(Telephony.Sms.BODY)
                val dateIdx = cursor.getColumnIndex(Telephony.Sms.DATE)
                var scanned = 0

                while (cursor.moveToNext() && scanned < maxMessages) {
                    scanned++
                    val sender = if (addrIdx >= 0) cursor.getString(addrIdx).orEmpty() else "BANK"
                    val body = if (bodyIdx >= 0) cursor.getString(bodyIdx).orEmpty() else ""
                    val dateMs = if (dateIdx >= 0) cursor.getLong(dateIdx) else System.currentTimeMillis()
                    if (body.isBlank()) continue

                    val parsed = SmsMoneyParser.parse(
                        body = body,
                        sender = sender,
                        timestampMs = dateMs
                    )
                    if (parsed != null) {
                        parsedList.add(parsed.toEntity())
                    }
                }
            }
        } catch (_: SecurityException) {
            // Permission revoked mid-scan
        } catch (_: Exception) {
            // Ignore malformed rows safely
        }

        parsedList
    }
}
