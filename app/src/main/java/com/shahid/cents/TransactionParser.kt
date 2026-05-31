package com.shahid.cents

import java.math.BigDecimal
import java.math.RoundingMode
import java.util.Locale

object TransactionParser {
    private val amountRegex = Regex("""(?i)(?:rs\.?|inr|₹)\s*([0-9][0-9,]*(?:\.\d{1,2})?)(?:/-)?|([0-9][0-9,]*(?:\.\d{1,2})?)\s*(?:rs\.?|inr|/-)""")
    private val balanceRegex = Regex("""(?i)(?:avl|available|bal|balance|a/c bal|ac bal)[^0-9₹]*(?:rs\.?|inr|₹)?\s*([0-9][0-9,]*(?:\.\d{1,2})?)(?:/-)?""")
    private val accountRegex = Regex("""(?i)(?:a/c|ac|acct|account|card|xx|x{2,})\s*(?:no\.?|ending|ended|number)?\s*([xX*]*\d{3,6})""")
    private val merchantPatterns = listOf(
        Regex("""(?i)(?:at|to|in favour of|towards|paid to|sent to)\s+([a-z0-9 ._&@-]{3,40})"""),
        Regex("""(?i)(?:info:|remark:|remarks:)\s*([a-z0-9 ._&@-]{3,40})""")
    )

    private val debitWords = listOf("debited", "debit", "spent", "paid", "purchase", "withdrawn", "withdrawal", "deducted", "sent", "dr ", "txn", "transaction", "used", "charged")
    private val creditWords = listOf("credited", "received", "deposited", "salary", "cr ")
    private val billWords = listOf("due", "bill", "statement", "minimum due", "payment due")
    private val reversalWords = listOf("reversed", "reversal", "refund", "failed", "declined", "unsuccessful")
    private val otpWords = listOf("otp", "one time password", "verification code", "login code")
    private val contributionNoticeWords = listOf("epfo", "epf", "uan", "provident fund", "passbook", "pf account")
    private val explicitDebitWords = listOf("debited", "debit", "spent", "paid", "purchase", "withdrawn", "withdrawal", "deducted", "sent", "charged")
    private val bankInstrumentWords = listOf("a/c", "ac ", "acct", "account", "card", "upi", "vpa", "imps", "neft", "rtgs")
    private val creditNoticeWords = listOf("credit card", "credit limit", "available credit", "total credit", "statement", "minimum due", "amount due")
    private val spamKeywords = listOf("lottery", "prize", "congratulations", "won", "gift", "urgent", "click here", "claim", "winning", "reward", "cashback offer", "free")
    private val linkRegex = Regex("""(https?://|www\.)\S+""")


    fun parse(sender: String, body: String): ParsedTransaction? {
        val normalized = normalize(body)
        val hasMoneySignal = amountRegex.containsMatchIn(normalized)
        if (!hasMoneySignal) return null
        if (isSpam(sender, normalized)) return null
        if (isContributionNotice(sender, normalized)) return null
        if (isNonIncomeCreditNotice(normalized)) return null

        if (otpWords.any { normalized.contains(it) } && debitWords.none { normalized.contains(it) } && creditWords.none { normalized.contains(it) }) {
            return null
        }

        val type = detectType(normalized) ?: return null
        val amount = amountRegex.find(normalized)?.groupValues?.drop(1)?.firstOrNull { it.isNotBlank() } ?: return null
        val balance = balanceRegex.find(normalized)?.groupValues?.getOrNull(1)

        return ParsedTransaction(
            amountPaise = amount.toPaise(),
            type = type,
            category = Categorizer.categoryFor(sender, normalized, type),
            merchant = extractMerchant(normalized),
            accountHint = accountRegex.find(normalized)?.groupValues?.getOrNull(1)?.uppercase(Locale.ROOT),
            balancePaise = balance?.toPaise()
        )
    }

    private fun detectType(text: String): TransactionType? = when {
        reversalWords.any { text.contains(it) } -> TransactionType.Reversal
        billWords.any { text.contains(it) } && text.contains("due") -> TransactionType.Bill
        isCredit(text) -> TransactionType.Credit
        explicitDebitWords.any { text.contains(it) } -> TransactionType.Debit
        text.contains("transaction") && bankInstrumentWords.any { text.contains(it) } -> TransactionType.Debit
        text.contains("txn") && bankInstrumentWords.any { text.contains(it) } -> TransactionType.Debit
        else -> null
    }

    private fun isCredit(text: String): Boolean {
        if (isNonIncomeCreditNotice(text)) return false
        return text.contains("credited") ||
            text.contains("deposited") ||
            text.contains("salary") ||
            Regex("""\breceived\b""").containsMatchIn(text) ||
            Regex("""\bcr\b""").containsMatchIn(text)
    }

    private fun isNonIncomeCreditNotice(text: String): Boolean {
        val isCreditNotice = creditNoticeWords.any { text.contains(it) }
        if (!isCreditNotice) return false
        val explicitReceivedSignal = text.contains("credited to your account") ||
            text.contains("credited in your account") ||
            text.contains("credited to a/c") ||
            text.contains("credited to ac")
        return !explicitReceivedSignal
    }

    private fun isContributionNotice(sender: String, text: String): Boolean {
        val combined = "${sender.lowercase(Locale.ROOT)} $text"
        val isProvidentFundMessage = contributionNoticeWords.any { combined.contains(it) }
        if (!isProvidentFundMessage) return false
        val hasBankDebitSignal = explicitDebitWords.any { text.contains(it) } && bankInstrumentWords.any { text.contains(it) }
        return !hasBankDebitSignal
    }

    private fun isSpam(sender: String, text: String): Boolean {
        if (linkRegex.containsMatchIn(text)) return true
        if (spamKeywords.any { text.contains(it) }) return true
        if (sender.length == 10 && sender.all { it.isDigit() }) {
            if (!bankInstrumentWords.any { text.contains(it) }) return true
        }
        return false
    }


    private fun extractMerchant(text: String): String? {
        return merchantPatterns.firstNotNullOfOrNull { pattern ->
            pattern.find(text)?.groupValues?.getOrNull(1)
                ?.replace(Regex("""\s+(on|ref|upi|available|avl|bal).*$"""), "")
                ?.trim(' ', '.', ',', '-')
                ?.takeIf { it.length >= 3 }
                ?.replaceFirstChar { char -> char.titlecase(Locale.ROOT) }
        }
    }

    private fun normalize(value: String): String = value
        .lowercase(Locale.ROOT)
        .replace('\n', ' ')
        .replace(Regex("""\s+"""), " ")
        .trim()

    private fun String.toPaise(): Long = BigDecimal(replace(",", ""))
        .setScale(2, RoundingMode.HALF_UP)
        .multiply(BigDecimal(100))
        .longValueExact()
}
