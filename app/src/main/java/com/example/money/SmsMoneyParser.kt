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
    val category: String, // "Food", "Shopping", "Bills", "Travel", "Study", "Health", "Entertainment", "Salary", "Income", "Others"
    val paymentMode: String, // "UPI", "Bank", "Wallet", "Card"
    val detectedBankBalance: Double?,
    val sender: String,
    val normalizedSender: String,
    val rawBody: String,
    val timestampMs: Long,
    val smsSignature: String,
    val accountSuffix: String = "",
    val referenceId: String = "",
    val matchedTemplateId: String = "",
    val matchedBankName: String = "",
    val trustClassification: TrustClassification = TrustClassification.REVIEW_REQUIRED,
    val status: TransactionStatus = TransactionStatus.PENDING_REVIEW,
    val riskScore: Int = 0,
    val flagReasons: List<String> = emptyList()
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
        status = status.key,
        trustClassification = trustClassification.key,
        riskScore = riskScore,
        flagReasonsCsv = flagReasons.joinToString("||"),
        matchedTemplateId = matchedTemplateId,
        accountSuffix = accountSuffix,
        referenceId = referenceId,
        userVerified = false,
        smsSender = sender.take(40),
        smsBody = rawBody.take(500),
        smsSignature = smsSignature,
        detectedBankBalance = detectedBankBalance,
        timestampMs = timestampMs,
        createdAt = System.currentTimeMillis()
    )
}

object SmsMoneyParser {

    // Non-transaction messages that should be silently ignored (never create any record)
    private val SILENT_IGNORE_PATTERNS = listOf(
        Regex("\\b(otp|one\\s*time\\s*password|verification\\s*code|login\\s*code)\\b", RegexOption.IGNORE_CASE),
        Regex("\\b(failed|declined|unsuccessful|cancelled|canceled|rejected|bounced|not\\s+debited|not\\s+credited)\\b", RegexOption.IGNORE_CASE),
        Regex("has\\s+requested|collect\\s+request|approve\\s+request|payment\\s+request|mandate\\s+created|standing\\s+instruction", RegexOption.IGNORE_CASE),
        Regex("due\\s+on|bill\\s+is\\s+due|reminder\\s+to\\s+pay|will\\s+be\\s+debited|upcoming\\s+emi", RegexOption.IGNORE_CASE)
    )

    // HIGH-RISK Fraud & Phishing Detectors (force QUARANTINED even if credit/debit words appear!)
    private val SUSPICIOUS_LINK_REGEX = Regex(
        "(?:https?://|www\\.|bit\\.ly|tinyurl\\.com|t\\.co/|goo\\.gl/|rb\\.gy/|cutt\\.ly/|is\\.gd/|ow\\.ly/|shorturl|tiny\\.cc|rebrand\\.ly|storeurl|[a-z0-9-]+\\.(?:xyz|top|club|vip|site|online|win|bet|click|link|buzz|tk|ml|ga)/?\\S*)",
        RegexOption.IGNORE_CASE
    )
    private val CREDENTIAL_LURE_REGEX = Regex(
        "\\b(password|passcode|user\\s*name|username|user\\s*id|upi\\s*pin|mpin|atm\\s*pin|cvv|check\\s+here|login\\s+here|verify\\s+kyc|update\\s+kyc|share\\s+otp|enter\\s+otp|click\\s+here)\\b",
        RegexOption.IGNORE_CASE
    )
    private val URGENCY_SCAM_LURE_REGEX = Regex(
        "\\b(account\\s+(?:will\\s+be\\s+)?(?:blocked|suspended|frozen|closed)|kyc\\s+expired|urgent\\s+action|claim\\s+now|congratulations|lottery|jackpot|rummy|casino|betting|welcome\\s+bonus|free\\s+bonus|pre-?approved\\s+loan|loan\\s+up\\s*to|win\\s+up\\s*to|apply\\s+now|play\\s+now|withdraw\\s+now)\\b",
        RegexOption.IGNORE_CASE
    )
    private val MALFORMED_SCAM_TOKEN_REGEX = Regex(
        "(?:welcometo[a-z]{4,}|[a-z]{18,}(?:url|store|win|bonus|cash|pay)|tbtsms)",
        RegexOption.IGNORE_CASE
    )

    // Strong DEBIT (Expense) phrases
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
        "(?:avl\\.?\\s*bal(?:ance)?|available\\s*bal(?:ance)?|a/c\\s*bal(?:ance)?|updated\\s*bal(?:ance)?|closing\\s*bal(?:ance)?|\\bbal(?:ance)?\\s*(?:is|:)?)\\s*[:\\-]?\\s*(?:rs\\.?|inr|₹)\\s*([0-9,]+(?:\\.[0-9]{1,2})?)",
        RegexOption.IGNORE_CASE
    )

    // Primary currency amount pattern
    private val CURRENCY_AMOUNT_REGEX = Regex(
        "(?:rs\\.?|inr|₹)\\s*([0-9][0-9,]*(?:\\.[0-9]{1,2})?)",
        RegexOption.IGNORE_CASE
    )

    // Fallback amount after verb
    private val VERB_AMOUNT_REGEX = Regex(
        "(?:debited|credited|paid|received|spent|sent|withdrawn)\\s+(?:by|of|for|with)?\\s*(?:rs\\.?|inr|₹)?\\s*([0-9][0-9,]*(?:\\.[0-9]{1,2})?)",
        RegexOption.IGNORE_CASE
    )

    // Account suffix extraction (e.g. A/c XX1234, Acct *4321, A/C829****404, ending 9876)
    private val ACCOUNT_SUFFIX_REGEX = Regex(
        "(?:a/c|acct|account|card)\\s*(?:no\\.?|number|ending|xx|\\*)*\\s*[:\\-]?\\s*[xX*0-9]*?([0-9]{4}|[0-9]{3})\\b",
        RegexOption.IGNORE_CASE
    )

    // Reference / UTR / UPI Ref extraction
    private val REFERENCE_ID_REGEX = Regex(
        "\\b(?:upi\\s*ref(?:\\s*no\\.?)?|ref(?:\\s*no\\.?)?|utr(?:\\s*no\\.?)?|txn\\s*id|imps/?)\\s*[:#\\-]?\\s*([A-Za-z0-9]{6,22})",
        RegexOption.IGNORE_CASE
    )

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
        sender: String = "",
        timestampMs: Long = System.currentTimeMillis(),
        customTrustedSenders: Set<String> = emptySet()
    ): ParsedSmsMoney? {
        // Step 2: Normalize whitespace without destroying rawBody
        val rawPreserved = body.trim()
        val normalizedBody = rawPreserved.replace("\n", " ").replace(Regex("\\s+"), " ").trim()
        if (normalizedBody.length < 12) return null

        val lower = normalizedBody.lowercase(Locale.US)

        // Check if this is purely an OTP/failed/reminder message WITHOUT a scam credit/debit claim
        val hasDebit = DEBIT_PATTERNS.any { it.containsMatchIn(normalizedBody) }
        val hasCredit = CREDIT_PATTERNS.any { it.containsMatchIn(normalizedBody) }

        // If message is purely OTP, failed payment, or collect request -> ignore completely
        if (SILENT_IGNORE_PATTERNS.any { it.containsMatchIn(normalizedBody) }) {
            // Exception: if it's an obvious phishing credential lure claiming money was credited (not a genuine OTP), quarantine it
            val hasScamLinkAndClaim = (hasCredit || hasDebit) &&
                SUSPICIOUS_LINK_REGEX.containsMatchIn(normalizedBody) &&
                !lower.contains("do not share")
            if (!hasScamLinkAndClaim) return null
        }

        // Balance-only SMS (no credit/debit verb) -> ignore
        if (!hasDebit && !hasCredit) return null

        // Step 3: Extract transaction type (strict debit precedence)
        val type = when {
            hasDebit && !hasCredit -> "EXPENSE"
            hasCredit && !hasDebit -> "INCOME"
            else -> {
                val userAccountCredited =
                    lower.contains("credited to your") ||
                        lower.contains("credited in your") ||
                        (lower.contains("your a/c") && lower.indexOf("credited") in 0 until (lower.indexOf("debited").takeIf { it >= 0 } ?: Int.MAX_VALUE)) ||
                        lower.contains("received from") ||
                        lower.contains("refund")
                if (userAccountCredited && !lower.contains("debited from your")) "INCOME" else "EXPENSE"
            }
        }

        // Extract available balance separately before transaction amount
        val balanceMatch = BALANCE_REGEX.find(normalizedBody)
        val detectedBalance = balanceMatch?.groupValues?.getOrNull(1)?.toCleanDouble()
        val bodyWithoutBalance = if (balanceMatch != null) {
            normalizedBody.removeRange(balanceMatch.range)
        } else {
            normalizedBody
        }

        val amount = CURRENCY_AMOUNT_REGEX.find(bodyWithoutBalance)
            ?.groupValues?.getOrNull(1)?.toCleanDouble()
            ?: VERB_AMOUNT_REGEX.find(bodyWithoutBalance)
                ?.groupValues?.getOrNull(1)?.toCleanDouble()
            ?: return null

        if (amount <= 0.0 || amount > 50_000_000.0) return null

        val normalizedSender = BankTemplateRegistry.extractCoreSenderHeader(sender)
        val dltSuffix = BankTemplateRegistry.extractDltSuffix(sender)
        val accountSuffix = ACCOUNT_SUFFIX_REGEX.find(normalizedBody)?.groupValues?.getOrNull(1).orEmpty()
        val referenceId = REFERENCE_ID_REGEX.find(normalizedBody)?.groupValues?.getOrNull(1).orEmpty()

        val paymentMode = when {
            lower.contains("upi") || lower.contains("vpa") || lower.contains("@") ||
                lower.contains("gpay") || lower.contains("phonepe") || lower.contains("bhim") -> "UPI"
            lower.contains("wallet") || lower.contains("amazon pay") || lower.contains("mobikwik") -> "Wallet"
            lower.contains("card") || lower.contains("atm") || lower.contains("pos ") -> "Card"
            else -> "Bank"
        }

        val title = extractReadableTitle(normalizedBody, lower, type, paymentMode)
        val category = detectCategory(lower, title, type)

        // Step 4: Match against configurable Bank-Specific Message Templates
        val matchedTemplate = BankTemplateRegistry.matchTemplate(sender, customTrustedSenders)

        // Step 5: Comprehensive Fraud & Trust Evaluation
        val highRiskReasons = mutableListOf<String>()
        val reviewReasons = mutableListOf<String>()
        var riskScore = 0

        val hasSuspiciousLink = SUSPICIOUS_LINK_REGEX.containsMatchIn(normalizedBody)
        val hasCredentialRequest = CREDENTIAL_LURE_REGEX.containsMatchIn(normalizedBody)
        val hasUrgencyOrScamLure = URGENCY_SCAM_LURE_REGEX.containsMatchIn(normalizedBody)
        val hasMalformedFormat = MALFORMED_SCAM_TOKEN_REGEX.containsMatchIn(normalizedBody)
        val isPersonalNumber = BankTemplateRegistry.isPersonalPhoneNumber(sender)

        if (hasSuspiciousLink) {
            highRiskReasons.add("Contains suspicious external or shortened link (e.g. bit.ly / URL)")
            riskScore += 45
        }
        if (hasCredentialRequest) {
            highRiskReasons.add("Requests or references sensitive credentials (Password / Username / PIN / KYC)")
            riskScore += 50
        }
        if (hasUrgencyOrScamLure) {
            highRiskReasons.add("Contains scam urgency threat, bonus lure, or promotional loan claim")
            riskScore += 45
        }
        if (hasMalformedFormat) {
            highRiskReasons.add("Unusual or malformed merchant/SMS format inconsistent with genuine banks")
            riskScore += 35
        }
        if (isPersonalNumber) {
            highRiskReasons.add("Sent from a personal phone number instead of an official bank header")
            riskScore += 45
        }
        if (dltSuffix == "P") {
            highRiskReasons.add("Promotional DLT sender suffix (-P) cannot issue genuine bank transactions")
            riskScore += 40
        }

        // Check template / sender authenticity
        if (matchedTemplate == null) {
            val senderLabel = sender.ifBlank { "Unknown" }
            reviewReasons.add("Sender '$senderLabel' does not match any verified bank or custom trusted template")
            riskScore += 25
        } else {
            val hintsMatched = matchedTemplate.expectedBodyHints.count { lower.contains(it) }
            if (hintsMatched < 2) {
                reviewReasons.add("Message structure does not match standard ${matchedTemplate.bankName} template")
                riskScore += 20
            }
        }

        // Check cross-bank spoofing mismatch (e.g., sender says SBI but text claims HDFC)
        if (matchedTemplate != null && !matchedTemplate.id.startsWith("tpl_custom_")) {
            val otherBankMentioned = detectConflictingBankName(lower, matchedTemplate.id)
            if (otherBankMentioned != null) {
                highRiskReasons.add("Sender header (${matchedTemplate.bankName}) conflicts with bank name in SMS ($otherBankMentioned)")
                riskScore += 40
            }
        }

        // Missing standard bank proof fields (no account suffix AND no UPI/Ref ID)
        val hasBankStructure = accountSuffix.isNotEmpty() || referenceId.isNotEmpty() || paymentMode == "UPI"
        if (!hasBankStructure) {
            reviewReasons.add("Missing standard masked account number and transaction reference ID")
            riskScore += 15
        }

        // DLT '-S' (Service) suffix from an unknown sender claiming credit is suspicious
        if (dltSuffix == "S" && matchedTemplate == null) {
            reviewReasons.add("Unverified Service (-S) header outside trusted bank registry")
            riskScore += 20
        }

        val allFlags = (highRiskReasons + reviewReasons).distinct()
        val boundedScore = riskScore.coerceIn(0, 100)

        // Step 7: Assign strict Trust Classification & Storage Status
        val classification = when {
            highRiskReasons.isNotEmpty() || boundedScore >= 60 -> TrustClassification.HIGH_RISK
            reviewReasons.isNotEmpty() || matchedTemplate == null -> TrustClassification.REVIEW_REQUIRED
            else -> TrustClassification.TRUSTED_PATTERN
        }

        val status = when (classification) {
            TrustClassification.TRUSTED_PATTERN -> TransactionStatus.CONFIRMED
            TrustClassification.REVIEW_REQUIRED -> TransactionStatus.PENDING_REVIEW
            TrustClassification.HIGH_RISK -> TransactionStatus.QUARANTINED
        }

        // Step 6: Deterministic Duplicate Fingerprint
        val signature = buildTransactionFingerprint(
            normalizedSender = normalizedSender,
            amount = amount,
            type = type,
            accountSuffix = accountSuffix,
            referenceId = referenceId,
            timestampMs = timestampMs,
            normalizedBody = normalizedBody
        )

        return ParsedSmsMoney(
            title = title,
            amount = amount,
            type = type,
            category = category,
            paymentMode = paymentMode,
            detectedBankBalance = detectedBalance,
            sender = sender.ifBlank { "UNKNOWN" },
            normalizedSender = normalizedSender,
            rawBody = rawPreserved,
            timestampMs = timestampMs,
            smsSignature = signature,
            accountSuffix = accountSuffix,
            referenceId = referenceId,
            matchedTemplateId = matchedTemplate?.id.orEmpty(),
            matchedBankName = matchedTemplate?.bankName.orEmpty(),
            trustClassification = classification,
            status = status,
            riskScore = boundedScore,
            flagReasons = allFlags
        )
    }

    private fun detectConflictingBankName(lowerBody: String, matchedTemplateId: String): String? {
        val bankKeywords = mapOf(
            "tpl_sbi" to listOf("state bank of india", "sbi "),
            "tpl_hdfc" to listOf("hdfc bank"),
            "tpl_icici" to listOf("icici bank"),
            "tpl_axis" to listOf("axis bank"),
            "tpl_kotak" to listOf("kotak bank", "kotak mahindra"),
            "tpl_pnb" to listOf("punjab national bank")
        )
        for ((tplId, keywords) in bankKeywords) {
            if (tplId != matchedTemplateId && keywords.any { lowerBody.contains(it) }) {
                return keywords.first().replaceFirstChar { it.uppercase(Locale.US) }
            }
        }
        return null
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
        if (lower.contains("refund")) return "Refund Received"

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
            type == "INCOME" && paymentMode == "UPI" -> "UPI Credit"
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
                text.contains("gift") -> "Gift"
                else -> "Income"
            }
        }
        return when {
            listOf("swiggy", "zomato", "food", "restaurant", "cafe", "domino", "pizza", "burger", "kfc", "blinkit", "zepto", "instamart", "bakery", "chai", "coffee", "tea", "biryani", "hotel", "mess")
                .any { text.contains(it) } -> "Food"
            listOf("book", "course", "udemy", "coursera", "unacademy", "physics", "pw ", "tuition", "college", "school", "exam", "stationery", "study")
                .any { text.contains(it) } -> "Study"
            listOf("pharmacy", "medical", "hospital", "doctor", "apollo", "1mg", "pharmeasy", "clinic", "health", "gym")
                .any { text.contains(it) } -> "Health"
            listOf("netflix", "spotify", "hotstar", "prime video", "bookmyshow", "cinema", "movie", "game", "steam", "youtube")
                .any { text.contains(it) } -> "Entertainment"
            listOf("amazon", "flipkart", "myntra", "ajio", "meesho", "nykaa", "dmart", "mall", "mart", "store", "shop", "reliance", "croma")
                .any { text.contains(it) } -> "Shopping"
            listOf("electricity", "bill", "recharge", "jio", "airtel", "bsnl", "vi ", "wifi", "broadband", "gas", "water", "dth", "rent", "emi", "insurance", "fastag", "postpaid", "prepaid")
                .any { text.contains(it) } -> "Bills"
            listOf("uber", "ola", "rapido", "irctc", "metro", "petrol", "diesel", "fuel", "hpcl", "iocl", "bpcl", "flight", "indigo", "train", "bus", "redbus", "makemytrip", "toll", "cab", "auto")
                .any { text.contains(it) } -> "Travel"
            else -> "Others"
        }
    }

    fun buildTransactionFingerprint(
        normalizedSender: String,
        amount: Double,
        type: String,
        accountSuffix: String,
        referenceId: String,
        timestampMs: Long,
        normalizedBody: String
    ): String {
        val amountPaise = (amount * 100.0).roundToLong()
        val timeBucket5Min = timestampMs / 300_000L
        val key = if (referenceId.isNotBlank()) {
            "$normalizedSender|$amountPaise|$type|$accountSuffix|ref:${referenceId.uppercase(Locale.US)}"
        } else {
            "$normalizedSender|$amountPaise|$type|$accountSuffix|t:$timeBucket5Min|${normalizedBody.lowercase(Locale.US).take(90)}"
        }
        val digest = MessageDigest.getInstance("SHA-256").digest(key.toByteArray())
        return "fp_" + digest.take(14).joinToString("") { "%02x".format(it) }
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
        SimpleDateFormat("MMM d, yyyy", Locale.US).format(Date(timestampMs))
    }
}
