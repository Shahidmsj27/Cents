package com.shahid.cents

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

import java.util.Date

enum class DateFilter(val label: String) {
    Daily("Daily"),
    Weekly("Weekly"),
    Monthly("Monthly"),
    Yearly("Yearly"),
    Custom("Custom")
}

data class DateWindow(
    val filter: DateFilter = DateFilter.Monthly,
    val offset: Int = 0,
    val customStartMillis: Long? = null,
    val customEndMillis: Long? = null
) {
    val startMillis: Long get() = customStartMillis ?: bounds().first
    val endMillis: Long get() = customEndMillis ?: bounds().second

    fun previous(): DateWindow = copy(offset = offset - 1)
    fun next(): DateWindow = copy(offset = offset + 1)
    fun withFilter(nextFilter: DateFilter): DateWindow = copy(filter = nextFilter, offset = 0)

    fun withCustomRange(start: Long, end: Long): DateWindow = copy(
        filter = DateFilter.Custom,
        offset = 0,
        customStartMillis = start,
        customEndMillis = end
    )

    fun label(): String {
        if (filter == DateFilter.Custom && customStartMillis != null && customEndMillis != null) {
            val fmt = SimpleDateFormat("dd MMM yyyy", Locale.ENGLISH)
            return "${fmt.format(Date(customStartMillis))} - ${fmt.format(Date(customEndMillis - 1))}"
        }
        val start = Calendar.getInstance().apply { timeInMillis = startMillis }
        val endInclusive = Calendar.getInstance().apply { timeInMillis = endMillis - 1 }
        return when (filter) {
            DateFilter.Daily -> SimpleDateFormat("dd MMM yyyy", Locale.ENGLISH).format(start.time)
            DateFilter.Weekly -> {
                val formatter = SimpleDateFormat("dd MMM", Locale.ENGLISH)
                "${formatter.format(start.time)} - ${formatter.format(endInclusive.time)}"
            }
            DateFilter.Monthly -> SimpleDateFormat("MMMM yyyy", Locale.ENGLISH).format(start.time)
            DateFilter.Yearly -> SimpleDateFormat("yyyy", Locale.ENGLISH).format(start.time)
            DateFilter.Custom -> "Custom"
        }
    }

    private fun bounds(): Pair<Long, Long> {
        val calendar = Calendar.getInstance().apply {
            firstDayOfWeek = Calendar.MONDAY
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        when (filter) {
            DateFilter.Daily -> calendar.add(Calendar.DATE, offset)
            DateFilter.Weekly -> {
                calendar.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
                calendar.add(Calendar.WEEK_OF_YEAR, offset)
            }
            DateFilter.Monthly -> {
                calendar.set(Calendar.DAY_OF_MONTH, 1)
                calendar.add(Calendar.MONTH, offset)
            }
            DateFilter.Yearly -> {
                calendar.set(Calendar.DAY_OF_YEAR, 1)
                calendar.add(Calendar.YEAR, offset)
            }
            DateFilter.Custom -> {}
        }
        val start = calendar.timeInMillis
        when (filter) {
            DateFilter.Daily -> calendar.add(Calendar.DATE, 1)
            DateFilter.Weekly -> calendar.add(Calendar.WEEK_OF_YEAR, 1)
            DateFilter.Monthly -> calendar.add(Calendar.MONTH, 1)
            DateFilter.Yearly -> calendar.add(Calendar.YEAR, 1)
            DateFilter.Custom -> {}
        }
        return start to calendar.timeInMillis
    }
}

data class CategorySummary(
    val category: String,
    val amountPaise: Long,
    val transactionCount: Int,
    val share: Float
)
