package com.example.money

enum class TrustClassification(val key: String, val label: String) {
    TRUSTED_PATTERN("TRUSTED_PATTERN", "Trusted Pattern"),
    REVIEW_REQUIRED("REVIEW_REQUIRED", "Review Required"),
    HIGH_RISK("HIGH_RISK", "High Risk Fraud");

    companion object {
        fun fromKey(raw: String): TrustClassification =
            entries.firstOrNull { it.key == raw } ?: REVIEW_REQUIRED
    }
}

enum class TransactionStatus(val key: String, val label: String) {
    CONFIRMED("CONFIRMED", "Confirmed"),
    PENDING_REVIEW("PENDING_REVIEW", "Pending Review"),
    QUARANTINED("QUARANTINED", "Quarantined");

    companion object {
        fun fromKey(raw: String): TransactionStatus =
            entries.firstOrNull { it.key == raw } ?: PENDING_REVIEW
    }
}

data class BankSmsTemplate(
    val id: String,
    val bankName: String,
    val senderHeaders: Set<String>,
    val expectedBodyHints: List<String>,
    val description: String
)

object BankTemplateRegistry {

    val DEFAULT_TEMPLATES: List<BankSmsTemplate> = listOf(
        BankSmsTemplate(
            id = "tpl_sbi",
            bankName = "State Bank of India (SBI)",
            senderHeaders = setOf("SBIINB", "SBIPSG", "SBIUPI", "CBSSBI", "ATMSBI", "SBICRD", "SBIYNO"),
            expectedBodyHints = listOf("a/c", "acct", "upi", "sbi", "debited", "credited", "ref"),
            description = "SBI Debit, Credit, UPI & ATM transactional alerts"
        ),
        BankSmsTemplate(
            id = "tpl_hdfc",
            bankName = "HDFC Bank",
            senderHeaders = setOf("HDFCBK", "HDFCCC", "HDFCBN"),
            expectedBodyHints = listOf("a/c", "account", "upi", "hdfc", "card", "debited", "credited", "spent"),
            description = "HDFC Bank Account, UPI & Card alerts"
        ),
        BankSmsTemplate(
            id = "tpl_icici",
            bankName = "ICICI Bank",
            senderHeaders = setOf("ICICIB", "ICICIT", "ICICIC"),
            expectedBodyHints = listOf("acct", "a/c", "icici", "upi", "debited", "credited", "card"),
            description = "ICICI Bank Account, UPI & Card alerts"
        ),
        BankSmsTemplate(
            id = "tpl_axis",
            bankName = "Axis Bank",
            senderHeaders = setOf("AXISBK", "AXISBN", "AXISCC"),
            expectedBodyHints = listOf("a/c", "axis", "upi", "debited", "credited", "spent"),
            description = "Axis Bank Account & UPI alerts"
        ),
        BankSmsTemplate(
            id = "tpl_kotak",
            bankName = "Kotak Mahindra Bank",
            senderHeaders = setOf("KOTAKB", "KOTAKM", "KMBANK"),
            expectedBodyHints = listOf("a/c", "kotak", "upi", "debited", "credited", "spent"),
            description = "Kotak Bank Account & UPI alerts"
        ),
        BankSmsTemplate(
            id = "tpl_pnb",
            bankName = "Punjab National Bank (PNB)",
            senderHeaders = setOf("PNBSMS", "PNBBNK", "PNBUPI"),
            expectedBodyHints = listOf("a/c", "pnb", "upi", "debited", "credited"),
            description = "PNB Account & UPI alerts"
        ),
        BankSmsTemplate(
            id = "tpl_bob",
            bankName = "Bank of Baroda",
            senderHeaders = setOf("BOBTXN", "BOBSMS", "BARODA", "BOBUPI"),
            expectedBodyHints = listOf("a/c", "bob", "upi", "debited", "credited"),
            description = "Bank of Baroda Account & UPI alerts"
        ),
        BankSmsTemplate(
            id = "tpl_canara",
            bankName = "Canara Bank",
            senderHeaders = setOf("CANBNK", "CANARA"),
            expectedBodyHints = listOf("a/c", "canara", "upi", "debited", "credited"),
            description = "Canara Bank Account & UPI alerts"
        ),
        BankSmsTemplate(
            id = "tpl_union_boi",
            bankName = "Union / BOI / Indian Bank",
            senderHeaders = setOf("UNIONB", "UBIN", "BOIIND", "INDBNK", "IDBIBK", "CENTBK", "UCOBNK"),
            expectedBodyHints = listOf("a/c", "acct", "upi", "debited", "credited"),
            description = "PSU Bank transactional alerts"
        ),
        BankSmsTemplate(
            id = "tpl_private",
            bankName = "IndusInd / Yes / IDFC / Federal",
            senderHeaders = setOf("INDUSB", "YESBNK", "IDFCFB", "FEDBNK", "AUBANK", "RBLBNK"),
            expectedBodyHints = listOf("a/c", "acct", "upi", "debited", "credited", "card"),
            description = "Private Bank Account & Card alerts"
        ),
        BankSmsTemplate(
            id = "tpl_payments_bank",
            bankName = "Paytm / Airtel / IPPB / Jio / Fino",
            senderHeaders = setOf("PYTM", "PAYTMB", "PAYTM", "AIRBNK", "AIRTEL", "IPPBNK", "IPPB", "JIOPAY", "FINOPB"),
            expectedBodyHints = listOf("a/c", "upi", "wallet", "debited", "credited", "paid", "received"),
            description = "Payments Bank & UPI transactional alerts"
        )
    )

    fun extractCoreSenderHeader(rawSender: String): String {
        val upper = rawSender.trim().uppercase()
        if (upper.isBlank()) return ""
        // Strip Indian DLT prefix (e.g. AX-, VK-, JD-, AD-, BZ-, VM-, VM-) and trailing category suffix (-T, -S, -P, -G)
        var core = upper.replace(Regex("^[A-Z]{2}[- ]"), "")
        core = core.replace(Regex("-[TSPG]$"), "")
        return core.replace(Regex("[^A-Z0-9]"), "")
    }

    fun extractDltSuffix(rawSender: String): String? {
        val upper = rawSender.trim().uppercase()
        val match = Regex("-([TSPG])$").find(upper)
        return match?.groupValues?.getOrNull(1)
    }

    fun isPersonalPhoneNumber(rawSender: String): Boolean {
        val digits = rawSender.filter { it.isDigit() }
        return digits.length >= 10 && rawSender.none { it.isLetter() }
    }

    fun matchTemplate(
        rawSender: String,
        customTrustedSenders: Set<String>
    ): BankSmsTemplate? {
        val core = extractCoreSenderHeader(rawSender)
        if (core.isBlank()) return null

        DEFAULT_TEMPLATES.firstOrNull { tpl ->
            tpl.senderHeaders.any { hdr -> core == hdr || core.startsWith(hdr) }
        }?.let { return it }

        val customMatch = customTrustedSenders
            .map { extractCoreSenderHeader(it) }
            .firstOrNull { it.isNotBlank() && (core == it || core.startsWith(it)) }
        if (customMatch != null) {
            return BankSmsTemplate(
                id = "tpl_custom_$customMatch",
                bankName = "Trusted Sender ($customMatch)",
                senderHeaders = setOf(customMatch),
                expectedBodyHints = listOf("a/c", "upi", "debited", "credited", "paid", "received"),
                description = "User-approved trusted sender template"
            )
        }
        return null
    }

    fun parseCustomSendersCsv(csv: String): Set<String> =
        csv.split(",")
            .map { extractCoreSenderHeader(it) }
            .filter { it.length >= 3 }
            .toSet()

    fun toCustomSendersCsv(senders: Collection<String>): String =
        senders.map { extractCoreSenderHeader(it) }
            .filter { it.length >= 3 }
            .distinct()
            .sorted()
            .joinToString(",")
}
