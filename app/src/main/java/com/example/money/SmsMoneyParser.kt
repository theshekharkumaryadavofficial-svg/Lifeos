package com.example.money

import com.example.data.MoneyTransaction
import java.security.MessageDigest
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.UUID
import kotlin.math.abs
import kotlin.math.roundToLong

data class ParsedSmsMoney(
    val title: String,
    val amount: Double,
    val type: String, // "INCOME" or "EXPENSE"
    val category: String, // "Food", "Shopping", "Bills", "Travel", "Salary", "Others"
    val paymentMode: String, // "UPI", "Bank", "Wallet", "Card"
    val detectedBankBalance: Double?,
    val sender: String,
    val rawBody: String,
    val timestampMs: Long,
    val smsSignature: String
) {
    fun toEntity(): MoneyTransaction = MoneyTransaction(
        id = UUID.randomUUID().toString(),
        userId = "local_user",
        title = title,
        amount = amount,
        type = type,
        category = category,
        paymentMode = paymentMode,
        source = "SMS",
        smsSender = sender.take(40),
        smsBody = rawBody.take(500),
        smsSignature = smsSignature,
        detectedBankBalance = detectedBankBalance,
        timestampMs = timestampMs,
        createdAt = System.currentTimeMillis()
    )
}

object SmsMoneyParser {

    // Reject OTPs, promotional offers, failed attempts, collect requests, and non-transaction alerts
    private val REJECT_PATTERNS = listOf(
        Regex("\\botp\\b", RegexOption.IGNORE_CASE),
        Regex("one\\s*time\\s*password", RegexOption.IGNORE_CASE),
        Regex("verification\\s*code", RegexOption.IGNORE_CASE),
        Regex("do\\s*not\\s*share", RegexOption.IGNORE_CASE),
        Regex("pre-?approved|loan\\s+up\\s*to|win\\s+up\\s*to|get\\s+up\\s*to|cashback\\s+up\\s*to", RegexOption.IGNORE_CASE),
        Regex("apply\\s+now|click\\s+here|avail\\s+now|limited\\s+offer|flat\\s+\\d+%", RegexOption.IGNORE_CASE),
        Regex("\\b(failed|declined|unsuccessful|cancelled|canceled|rejected|bounced)\\b", RegexOption.IGNORE_CASE),
        Regex("has\\s+requested|collect\\s+request|approve\\s+request|payment\\s+request|mandate\\s+created", RegexOption.IGNORE_CASE),
        Regex("due\\s+on|bill\\s+is\\s+due|reminder\\s+to\\s+pay|will\\s+be\\s+debited", RegexOption.IGNORE_CASE)
    )

    // Strong DEBIT (Expense) phrases — checked with higher precedence when user's own account is debited
    private val DEBIT_PATTERNS = listOf(
        Regex("\\b(debited|deducted|withdrawn|spent|paid|sent\\s+to|transferred\\s+to|purchase|bought|autopay|charged)\\b", RegexOption.IGNORE_CASE),
        Regex("debited\\s+(by|for|with|from)", RegexOption.IGNORE_CASE),
        Regex("payment\\s+of\\s+(rs\\.?|inr|₹)", RegexOption.IGNORE_CASE),
        Regex("sent\\s+(rs\\.?|inr|₹)", RegexOption.IGNORE_CASE),
        Regex("used\\s+(at|for)\\b", RegexOption.IGNORE_CASE),
        Regex("\\bdr\\.?\\s*(rs\\.?|inr|₹)", RegexOption.IGNORE_CASE)
    )

    // Strong CREDIT (Income) phrases
    private val CREDIT_PATTERNS = listOf(
        Regex("\\b(credited|received|deposited|refund|refunded|salary|cashback\\s+credited|reversed)\\b", RegexOption.IGNORE_CASE),
        Regex("added\\s+to\\s+(your\\s+)?(a/c|acct|account|wallet)", RegexOption.IGNORE_CASE),
        Regex("paid\\s+you|sent\\s+you|money\\s+received", RegexOption.IGNORE_CASE),
        Regex("\\bcr\\.?\\s*(rs\\.?|inr|₹)", RegexOption.IGNORE_CASE)
    )

    // Extract available balance separately so we never confuse Avl Bal with transaction amount
    private val BALANCE_REGEX = Regex(
        "(?:avl\\.?\\s*bal(?:ance)?|available\\s*bal(?:ance)?|a/c\\s*bal(?:ance)?|updated\\s*bal(?:ance)?|bal(?:ance)?\\s*(?:is|:)?)\\s*[:\\-]?\\s*(?:rs\\.?|inr|₹)\\s*([0-9,]+(?:\\.[0-9]{1,2})?)",
        RegexOption.IGNORE_CASE
    )

    // Primary currency amount pattern: Rs.1,000 / Rs 500.50 / INR 15,000 / ₹249
    private val CURRENCY_AMOUNT_REGEX = Regex(
        "(?:rs\\.?|inr|₹)\\s*([0-9][0-9,]*(?:\\.[0-9]{1,2})?)",
        RegexOption.IGNORE_CASE
    )

    // Fallback amount after verb like "debited by 500.00" or "credited with 1200"
    private val VERB_AMOUNT_REGEX = Regex(
        "(?:debited|credited|paid|received|spent|sent|withdrawn)\\s+(?:by|of|for|with)?\\s*(?:rs\\.?|inr|₹)?\\s*([0-9][0-9,]*(?:\\.[0-9]{1,2})?)",
        RegexOption.IGNORE_CASE
    )

    // Merchant/party extraction patterns
    private val TO_MERCHANT_REGEX = Regex(
        "(?:\\bto\\b|\\bat\\b|\\btowards\\b)\\s+([A-Za-z0-9][A-Za-z0-9 &._@-]{1,34}?)(?:\\s+(?:on|via|ref|upi|from|avl|bal|using|through|id|dt|dated)|[.,;]|$)",
        RegexOption.IGNORE_CASE
    )
    private val FROM_SENDER_REGEX = Regex(
        "(?:\\bfrom\\b|\\bby\\b)\\s+([A-Za-z0-9][A-Za-z0-9 &._@-]{1,34}?)(?:\\s+(?:on|via|ref|upi|to|avl|bal|in|into|a/c|acct|account)|[.,;]|$)",
        RegexOption.IGNORE_CASE
    )
    private val VPA_REGEX = Regex(
        "(?:vpa|upi\\s*id)[:\\s]+([A-Za-z0-9._-]+@[A-Za-z0-9]+)",
        RegexOption.IGNORE_CASE
    )

    fun parse(
        body: String,
        sender: String = "BANK",
        timestampMs: Long = System.currentTimeMillis()
    ): ParsedSmsMoney? {
        val cleanBody = body.replace("\n", " ").replace(Regex("\\s+"), " ").trim()
        if (cleanBody.length < 12) return null

        // 1. Reject OTPs, offers, failed payments, and collect requests
        if (REJECT_PATTERNS.any { it.containsMatchIn(cleanBody) }) return null

        // 2. Determine whether it's a valid transaction (INCOME vs EXPENSE)
        val hasDebit = DEBIT_PATTERNS.any { it.containsMatchIn(cleanBody) }
        val hasCredit = CREDIT_PATTERNS.any { it.containsMatchIn(cleanBody) }
        if (!hasDebit && !hasCredit) return null

        // Crucial disambiguation:
        // "A/c XX1234 debited by Rs.500 and credited to VPA zomato@axis" -> USER's account was DEBITED (EXPENSE!)
        val type = when {
            hasDebit && !hasCredit -> "EXPENSE"
            hasCredit && !hasDebit -> "INCOME"
            else -> {
                val lower = cleanBody.lowercase(Locale.US)
                val userAccountCredited =
                    lower.contains("credited to your") ||
                        lower.contains("credited in your") ||
                        lower.contains("your a/c") && lower.indexOf("credited") in 0 until (lower.indexOf("debited").takeIf { it >= 0 } ?: Int.MAX_VALUE) ||
                        lower.contains("received from") ||
                        lower.contains("refund")
                if (userAccountCredited && !lower.contains("debited from your")) "INCOME" else "EXPENSE"
            }
        }

        // 3. Extract optional available balance, then strip balance phrase before extracting transaction amount
        val balanceMatch = BALANCE_REGEX.find(cleanBody)
        val detectedBalance = balanceMatch?.groupValues?.getOrNull(1)?.toCleanDouble()
        val bodyWithoutBalance = if (balanceMatch != null) {
            cleanBody.removeRange(balanceMatch.range)
        } else {
            cleanBody
        }

        // 4. Extract transaction amount (prefer explicit currency symbol Rs/INR/₹)
        val amount = CURRENCY_AMOUNT_REGEX.find(bodyWithoutBalance)
            ?.groupValues?.getOrNull(1)?.toCleanDouble()
            ?: VERB_AMOUNT_REGEX.find(bodyWithoutBalance)
                ?.groupValues?.getOrNull(1)?.toCleanDouble()
            ?: return null

        if (amount <= 0.0 || amount > 50_000_000.0) return null

        // 5. Detect Payment Mode
        val lower = cleanBody.lowercase(Locale.US)
        val paymentMode = when {
            lower.contains("upi") || lower.contains("vpa") || lower.contains("@") ||
                lower.contains("gpay") || lower.contains("phonepe") || lower.contains("bhim") -> "UPI"
            lower.contains("wallet") || lower.contains("amazon pay") || lower.contains("mobikwik") -> "Wallet"
            lower.contains("card") || lower.contains("atm") || lower.contains("pos ") -> "Card"
            else -> "Bank"
        }

        // 6. Extract clean Title / Merchant / Counterparty
        val title = extractReadableTitle(cleanBody, lower, type, paymentMode)

        // 7. Auto-categorize
        val category = detectCategory(lower, title, type)

        // 8. Build deterministic signature so repeated scans never create duplicates
        val signature = buildSmsSignature(timestampMs, amount, type, cleanBody)

        return ParsedSmsMoney(
            title = title,
            amount = amount,
            type = type,
            category = category,
            paymentMode = paymentMode,
            detectedBankBalance = detectedBalance,
            sender = sender,
            rawBody = cleanBody,
            timestampMs = timestampMs,
            smsSignature = signature
        )
    }

    private fun extractReadableTitle(
        cleanBody: String,
        lower: String,
        type: String,
        paymentMode: String
    ): String {
        if (lower.contains("salary")) return "Salary Credit"
        if (lower.contains("electricity") || lower.contains("bescom") || lower.contains("power bill")) {
            return "Electricity Bill"
        }
        if (lower.contains("recharge") || lower.contains("jio") || lower.contains("airtel") || lower.contains("vi prepaid")) {
            return "Mobile Recharge"
        }
        if (lower.contains("atm") && lower.contains("withdrawn")) return "ATM Withdrawal"

        val candidate = if (type == "INCOME") {
            FROM_SENDER_REGEX.find(cleanBody)?.groupValues?.getOrNull(1)
                ?: VPA_REGEX.find(cleanBody)?.groupValues?.getOrNull(1)?.substringBefore("@")
        } else {
            TO_MERCHANT_REGEX.find(cleanBody)?.groupValues?.getOrNull(1)
                ?: VPA_REGEX.find(cleanBody)?.groupValues?.getOrNull(1)?.substringBefore("@")
        }

        val sanitized = candidate
            ?.replace(Regex("^(your\\s+)?(a/c|acct|account|bank|vpa|upi)\\s*[xX*0-9-]*", RegexOption.IGNORE_CASE), "")
            ?.replace(Regex("[^A-Za-z0-9 &._-]"), "")
            ?.trim()
            .orEmpty()

        val looksLikeAccountMask = sanitized.matches(Regex("^[xX*]{2,}[0-9]+$")) || sanitized.length < 2

        if (!looksLikeAccountMask && sanitized.isNotBlank()) {
            val formatted = sanitized
                .split(" ")
                .filter { it.isNotBlank() }
                .take(4)
                .joinToString(" ") { word ->
                    if (word.length <= 3 && word.all { it.isUpperCase() }) word
                    else word.lowercase(Locale.US).replaceFirstChar { it.uppercase(Locale.US) }
                }
            return if (type == "INCOME" && !formatted.startsWith("Received", ignoreCase = true)) {
                "Received from $formatted"
            } else {
                formatted
            }
        }

        return when {
            type == "INCOME" && paymentMode == "UPI" -> "UPI Credit Received"
            type == "INCOME" -> "Bank Credit"
            paymentMode == "UPI" -> "UPI Payment"
            paymentMode == "Card" -> "Card Spend"
            paymentMode == "Wallet" -> "Wallet Payment"
            else -> "Bank Debit"
        }
    }

    private fun detectCategory(lower: String, title: String, type: String): String {
        val text = "$lower ${title.lowercase(Locale.US)}"
        if (type == "INCOME") {
            return when {
                text.contains("salary") || text.contains("payroll") || text.contains("stipend") -> "Salary"
                else -> "Income"
            }
        }
        return when {
            listOf("swiggy", "zomato", "food", "restaurant", "cafe", "domino", "pizza", "burger", "kfc", "blinkit", "zepto", "instamart", "bakery", "chai", "coffee", "tea", "biryani", "hotel", "mess")
                .any { text.contains(it) } -> "Food"
            listOf("amazon", "flipkart", "myntra", "ajio", "meesho", "nykaa", "dmart", "mall", "mart", "store", "shop", "reliance", "croma", "decathlon")
                .any { text.contains(it) } -> "Shopping"
            listOf("electricity", "bill", "recharge", "jio", "airtel", "bsnl", "vi ", "wifi", "broadband", "gas", "water", "dth", "rent", "emi", "insurance", "fastag", "postpaid", "prepaid")
                .any { text.contains(it) } -> "Bills"
            listOf("uber", "ola", "rapido", "irctc", "metro", "petrol", "diesel", "fuel", "hpcl", "iocl", "bpcl", "flight", "indigo", "train", "bus", "redbus", "makemytrip", "toll", "cab", "auto")
                .any { text.contains(it) } -> "Travel"
            else -> "Others"
        }
    }

    fun buildSmsSignature(timestampMs: Long, amount: Double, type: String, cleanBody: String): String {
        val minuteBucket = timestampMs / 60_000L
        val amountPaise = (amount * 100.0).roundToLong()
        val rawKey = "$minuteBucket|$amountPaise|$type|${cleanBody.lowercase(Locale.US).take(120)}"
        val digest = MessageDigest.getInstance("SHA-256").digest(rawKey.toByteArray())
        return "sms_" + digest.take(12).joinToString("") { "%02x".format(it) }
    }

    private fun String.toCleanDouble(): Double? =
        replace(",", "").trim().toDoubleOrNull()
}

// --- INDIAN RUPEE & DATE FORMATTING HELPERS ---

fun formatInr(amount: Double): String {
    val isNegative = amount < 0.0
    val absVal = abs(amount)
    val whole = absVal.toLong()
    val hasDecimals = abs(absVal - whole) >= 0.005
    val formattedInt = formatIndianInteger(whole)
    val decimalPart = if (hasDecimals) {
        val paise = ((absVal - whole) * 100.0).roundToLong().coerceIn(0L, 99L)
        if (paise == 0L) "" else String.format(Locale.US, ".%02d", paise)
    } else ""
    return (if (isNegative) "-₹" else "₹") + formattedInt + decimalPart
}

fun formatInrCompact(amount: Double): String {
    val isNeg = amount < 0.0
    val absVal = abs(amount)
    val sign = if (isNeg) "-" else ""
    return when {
        absVal >= 10_000_000.0 -> "${sign}₹${DecimalFormat("0.#", DecimalFormatSymbols(Locale.US)).format(absVal / 10_000_000.0)}Cr"
        absVal >= 100_000.0 -> "${sign}₹${DecimalFormat("0.#", DecimalFormatSymbols(Locale.US)).format(absVal / 100_000.0)}L"
        else -> formatInr(amount)
    }
}

private fun formatIndianInteger(value: Long): String {
    val str = value.toString()
    if (str.length <= 3) return str
    val last3 = str.takeLast(3)
    val remaining = str.dropLast(3)
    val grouped = StringBuilder()
    var i = remaining.length
    while (i > 0) {
        val start = (i - 2).coerceAtLeast(0)
        if (grouped.isNotEmpty()) grouped.insert(0, ',')
        grouped.insert(0, remaining.substring(start, i))
        i = start
    }
    return "$grouped,$last3"
}

fun formatTransactionDateLabel(timestampMs: Long): String {
    val nowCal = Calendar.getInstance()
    val txCal = Calendar.getInstance().apply { timeInMillis = timestampMs }
    val sameDay = nowCal.get(Calendar.YEAR) == txCal.get(Calendar.YEAR) &&
        nowCal.get(Calendar.DAY_OF_YEAR) == txCal.get(Calendar.DAY_OF_YEAR)
    return if (sameDay) {
        SimpleDateFormat("h:mm a", Locale.US).format(Date(timestampMs))
    } else {
        SimpleDateFormat("d MMM", Locale.US).format(Date(timestampMs))
    }
}
