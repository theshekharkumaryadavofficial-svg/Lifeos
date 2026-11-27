package com.example.money

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class SmsMoneyParserTest {

    @Test
    fun parsesUpiCreditAndExtractsBankBalanceSeparately() {
        val sms = "Credited Rs.1,000 to your account XXXXX1234 via UPI from Rahul. Avl Bal: Rs.12,850"
        val parsed = SmsMoneyParser.parse(sms, sender = "VK-SBIUPI", timestampMs = 1_700_000_000_000L)
        assertNotNull(parsed)
        assertEquals("INCOME", parsed!!.type)
        assertEquals(1000.0, parsed.amount, 0.001)
        assertEquals("UPI", parsed.paymentMode)
        assertEquals(12850.0, parsed.detectedBankBalance!!, 0.001)
    }

    @Test
    fun classifiesDebitedFromUserCreditedToMerchantAsExpense() {
        val sms = "Rs.1,299.00 debited from your A/c XX4321 and credited to VPA amazon@icici via UPI. Avl Bal Rs.11,551"
        val parsed = SmsMoneyParser.parse(sms, sender = "AD-HDFCBK")
        assertNotNull(parsed)
        assertEquals("EXPENSE", parsed!!.type)
        assertEquals(1299.0, parsed.amount, 0.001)
        assertEquals("Shopping", parsed.category)
        assertEquals("UPI", parsed.paymentMode)
        assertEquals(11551.0, parsed.detectedBankBalance!!, 0.001)
    }

    @Test
    fun parsesSalaryCreditAndBillsAndFoodCorrectly() {
        val salary = SmsMoneyParser.parse("INR 15,000.00 Salary credited to your Bank A/c XX4321 via NEFT.")
        assertNotNull(salary)
        assertEquals("INCOME", salary!!.type)
        assertEquals(15000.0, salary.amount, 0.001)
        assertEquals("Salary", salary.category)

        val food = SmsMoneyParser.parse("Paid Rs.249 to Zomato via PhonePe UPI Ref 123456.")
        assertNotNull(food)
        assertEquals("EXPENSE", food!!.type)
        assertEquals(249.0, food.amount, 0.001)
        assertEquals("Food", food.category)

        val bill = SmsMoneyParser.parse("Rs.870 spent towards Electricity Bill via UPI")
        assertNotNull(bill)
        assertEquals("EXPENSE", bill!!.type)
        assertEquals(870.0, bill.amount, 0.001)
        assertEquals("Bills", bill.category)
    }

    @Test
    fun rejectsOtpsPromotionsFailedTransactionsAndCollectRequests() {
        assertNull(SmsMoneyParser.parse("Your OTP for Rs.5,000 transaction is 482910. Do not share."))
        assertNull(SmsMoneyParser.parse("Congrats! Pre-approved loan up to Rs.5,00,000 credited in 5 mins. Apply now!"))
        assertNull(SmsMoneyParser.parse("Transaction of Rs.500 via UPI failed. Amount not debited."))
        assertNull(SmsMoneyParser.parse("Rahul has requested Rs.500 via UPI. Approve request to pay."))
    }

    @Test
    fun formatsIndianRupeesProperly() {
        assertEquals("₹12,850", formatInr(12850.0))
        assertEquals("₹1,25,000", formatInr(125000.0))
        assertEquals("-₹500", formatInr(-500.0))
    }
}
