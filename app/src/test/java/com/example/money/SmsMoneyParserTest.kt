package com.example.money

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SmsMoneyParserTest {

    @Test
    fun screenshotScamSms_isHighRiskQuarantinedAndExcludedFromBalance() {
        val scamSms = "A/C829****404, Welcometoezooperstoreurl- Rs.5912 has been credited, " +
            "Bal Rs.8935 UserName-829****404 Password- check here: bit.ly/4jDeQvh - TBTSMS"

        val parsed = SmsMoneyParser.parse(
            body = scamSms,
            sender = "AX-TBTACC-S",
            timestampMs = 1_764_250_000_000L
        )

        assertNotNull("Scam SMS should be captured for quarantine warning, not silently confirmed", parsed)
        val result = parsed!!
        assertEquals(TrustClassification.HIGH_RISK, result.trustClassification)
        assertEquals(TransactionStatus.QUARANTINED, result.status)
        assertEquals(5912.0, result.amount, 0.01)
        assertEquals(8935.0, result.detectedBankBalance ?: -1.0, 0.01)
        assertTrue("Risk score must be high", result.riskScore >= 80)
        assertTrue(
            "Should flag suspicious link",
            result.flagReasons.any { it.contains("link", ignoreCase = true) }
        )
        assertTrue(
            "Should flag password/credential request",
            result.flagReasons.any { it.contains("credential", ignoreCase = true) || it.contains("Password", ignoreCase = true) }
        )
        assertTrue(
            "Should flag malformed format",
            result.flagReasons.any { it.contains("malformed", ignoreCase = true) }
        )
    }

    @Test
    fun genuineSbiUpiCredit_isTrustedAndConfirmed() {
        val body = "Dear UPI user A/C X4321 credited by Rs 1,500.00 on date 27Nov26 " +
            "trf from RAHUL Refno 623489012345. Avl Bal Rs 14,350.00 -SBI"

        val parsed = SmsMoneyParser.parse(body = body, sender = "VK-SBIUPI-T")

        assertNotNull(parsed)
        val result = parsed!!
        assertEquals("INCOME", result.type)
        assertEquals(1500.0, result.amount, 0.01)
        assertEquals(14350.0, result.detectedBankBalance ?: -1.0, 0.01)
        assertEquals("4321", result.accountSuffix)
        assertEquals("623489012345", result.referenceId)
        assertEquals(TrustClassification.TRUSTED_PATTERN, result.trustClassification)
        assertEquals(TransactionStatus.CONFIRMED, result.status)
        assertTrue(result.flagReasons.isEmpty())
    }

    @Test
    fun genuineHdfcDebitWithCreditedRecipientVpa_isExpenseConfirmed() {
        val body = "Rs.249.00 debited from your HDFC Bank A/c XX9012 on 27-11-26 and " +
            "credited to VPA swiggy@axisbank UPI Ref 512390812345. Avl Bal: Rs.11,200.50"

        val parsed = SmsMoneyParser.parse(body = body, sender = "AD-HDFCBK-T")

        assertNotNull(parsed)
        val result = parsed!!
        assertEquals("EXPENSE", result.type)
        assertEquals(249.0, result.amount, 0.01)
        assertEquals(11200.50, result.detectedBankBalance ?: -1.0, 0.01)
        assertEquals("Food", result.category)
        assertEquals(TrustClassification.TRUSTED_PATTERN, result.trustClassification)
        assertEquals(TransactionStatus.CONFIRMED, result.status)
    }

    @Test
    fun genuineIciciRefund_isIncomeConfirmed() {
        val body = "ICICI Bank Acct XX1122 credited with Rs 799.00 refund from Amazon on 27-Nov-26. " +
            "UPI Ref 409988776655. Avl Bal Rs 9,800.00"

        val parsed = SmsMoneyParser.parse(body = body, sender = "AX-ICICIB-T")

        assertNotNull(parsed)
        val result = parsed!!
        assertEquals("INCOME", result.type)
        assertEquals(799.0, result.amount, 0.01)
        assertEquals(TrustClassification.TRUSTED_PATTERN, result.trustClassification)
        assertEquals(TransactionStatus.CONFIRMED, result.status)
    }

    @Test
    fun spoofedKnownBankSenderWithPhishingLink_isStillQuarantined() {
        val body = "Dear Customer A/c XX4321 credited Rs.5,000.00. Verify KYC Password here: https://bit.ly/fakebank"

        val parsed = SmsMoneyParser.parse(body = body, sender = "VK-SBIINB-T")

        assertNotNull(parsed)
        val result = parsed!!
        assertEquals(TrustClassification.HIGH_RISK, result.trustClassification)
        assertEquals(TransactionStatus.QUARANTINED, result.status)
    }

    @Test
    fun unknownSenderWithoutFraudSignals_requiresReview() {
        val body = "Rs.750.00 credited to your A/c XX4012 on 27-Nov Ref 99182736"

        val parsed = SmsMoneyParser.parse(body = body, sender = "VM-MYSHOP-S")

        assertNotNull(parsed)
        val result = parsed!!
        assertEquals(TrustClassification.REVIEW_REQUIRED, result.trustClassification)
        assertEquals(TransactionStatus.PENDING_REVIEW, result.status)
    }

    @Test
    fun personalPhoneNumberSender_isHighRiskQuarantined() {
        val body = "Rs.2,500 credited to your A/c XX1234 via UPI Ref 8822113344"

        val parsed = SmsMoneyParser.parse(body = body, sender = "+919876543210")

        assertNotNull(parsed)
        val result = parsed!!
        assertEquals(TrustClassification.HIGH_RISK, result.trustClassification)
        assertEquals(TransactionStatus.QUARANTINED, result.status)
    }

    @Test
    fun promotionalDltSuffixSender_isHighRiskQuarantined() {
        val body = "Rs.1,000 credited to your HDFC Bank A/c XX1234 Ref 12345678"

        val parsed = SmsMoneyParser.parse(body = body, sender = "AD-HDFCBK-P")

        assertNotNull(parsed)
        val result = parsed!!
        assertEquals(TrustClassification.HIGH_RISK, result.trustClassification)
        assertEquals(TransactionStatus.QUARANTINED, result.status)
    }

    @Test
    fun customTrustedSender_confirmsCleanSmsButBlocksPhishing() {
        val cleanBody = "A/c XX7788 debited Rs.350.00 via UPI Ref 1122334455"
        val phishingBody = "A/c XX7788 credited Rs.3500.00 check here: bit.ly/scam99 Password-1234"
        val customSenders = setOf("MAHBNK")

        val cleanResult = SmsMoneyParser.parse(cleanBody, sender = "AX-MAHBNK-T", customTrustedSenders = customSenders)
        val phishResult = SmsMoneyParser.parse(phishingBody, sender = "AX-MAHBNK-T", customTrustedSenders = customSenders)

        assertNotNull(cleanResult)
        assertEquals(TrustClassification.TRUSTED_PATTERN, cleanResult!!.trustClassification)
        assertEquals(TransactionStatus.CONFIRMED, cleanResult.status)

        assertNotNull(phishResult)
        assertEquals(TrustClassification.HIGH_RISK, phishResult!!.trustClassification)
        assertEquals(TransactionStatus.QUARANTINED, phishResult.status)
    }

    @Test
    fun duplicateFingerprints_matchForSameRefAndDifferForDifferentRef() {
        val body1 = "Dear UPI user A/C X4321 credited by Rs 500.00 Refno 778899001122 -SBI"
        val body2 = "Dear UPI user A/C X4321 credited by Rs 500.00 Refno 778899001122 -SBI"
        val body3 = "Dear UPI user A/C X4321 credited by Rs 500.00 Refno 778899009999 -SBI"

        val p1 = SmsMoneyParser.parse(body1, sender = "VK-SBIUPI-T", timestampMs = 1_700_000_000_000L)!!
        val p2 = SmsMoneyParser.parse(body2, sender = "VK-SBIUPI-T", timestampMs = 1_700_000_050_000L)!!
        val p3 = SmsMoneyParser.parse(body3, sender = "VK-SBIUPI-T", timestampMs = 1_700_000_050_000L)!!

        assertEquals(p1.smsSignature, p2.smsSignature)
        assertNotEquals(p1.smsSignature, p3.smsSignature)
    }

    @Test
    fun balanceOnlyOtpFailedAndMalformedSms_areIgnored() {
        val balanceOnly = "Your SBI A/c XX4321 available balance as on 27Nov26 is Rs.12,850.00"
        val otpSms = "Your OTP for Rs.1,500 transaction on HDFC Card is 492810. Do not share."
        val failedSms = "UPI transaction of Rs.500.00 from A/c XX4321 has failed. Amount not debited."
        val collectReq = "Rahul has requested Rs.400 via UPI. Approve collect request in app."
        val malformed = "Hello bro how are you?? 12345"

        assertNull(SmsMoneyParser.parse(balanceOnly, sender = "VK-SBIINB-T"))
        assertNull(SmsMoneyParser.parse(otpSms, sender = "AD-HDFCBK-T"))
        assertNull(SmsMoneyParser.parse(failedSms, sender = "VK-SBIUPI-T"))
        assertNull(SmsMoneyParser.parse(collectReq, sender = "VK-SBIUPI-T"))
        assertNull(SmsMoneyParser.parse(malformed, sender = "FRIEND"))
    }
}
