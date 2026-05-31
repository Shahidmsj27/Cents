package com.shahid.cents

object Categorizer {
    private val rules = listOf(
        "swiggy|zomato|restaurant|cafe|pizza|food" to "Food",
        "blinkit|zepto|bigbasket|grocery|dmart|reliance fresh" to "Groceries",
        "uber|ola|rapido|metro|irctc|railway|bus" to "Transport",
        "amazon|flipkart|myntra|ajio|nykaa" to "Shopping",
        "petrol|fuel|hpcl|iocl|bpcl" to "Fuel",
        "airtel|jio|vi |vodafone|electricity|broadband|wifi|gas bill" to "Bills",
        "salary|payroll" to "Salary",
        "atm|cash withdrawal" to "ATM",
        "sip|mutual fund|zerodha|groww|upstox" to "Investment",
        "hospital|clinic|pharmacy|medical|apollo" to "Healthcare",
        "netflix|prime video|hotstar|bookmyshow|spotify" to "Entertainment",
        "rent" to "Rent",
        "emi|loan" to "EMI"
    )

    fun categoryFor(sender: String, text: String, type: TransactionType): String {
        if (type == TransactionType.Credit && text.contains("salary")) return "Salary"
        if (type == TransactionType.Bill) return "Bills"
        val haystack = "$sender $text"
        return rules.firstOrNull { Regex(it.first, RegexOption.IGNORE_CASE).containsMatchIn(haystack) }?.second
            ?: if (type == TransactionType.Credit) "Income" else "Unknown"
    }
}
