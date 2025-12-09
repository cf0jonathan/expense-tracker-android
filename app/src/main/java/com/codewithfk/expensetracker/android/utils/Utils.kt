package com.codewithfk.expensetracker.android.utils

import com.codewithfk.expensetracker.android.R
import com.codewithfk.expensetracker.android.data.model.ExpenseEntity
import java.text.NumberFormat
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale


object Utils {

    // Max length for stored/displayed titles to avoid layout breakage
    const val MAX_TITLE_LENGTH = 20

    /**
     * Truncate long titles and append an ellipsis. Returns empty string for null/blank input.
     */
    fun truncateTitle(input: String?, maxLen: Int = MAX_TITLE_LENGTH): String {
        if (input.isNullOrBlank()) return ""
        return if (input.length <= maxLen) input else input.substring(0, maxLen - 1).trimEnd() + "…"
    }

    fun formatDateToHumanReadableForm(dateInMillis: Long): String {
        val dateFormatter = SimpleDateFormat("dd/MM/YYYY", Locale.getDefault())
        return dateFormatter.format(dateInMillis)
    }

    fun formatDateForChart(dateInMillis: Long): String {
        val dateFormatter = SimpleDateFormat("dd-MMM", Locale.getDefault())
        return dateFormatter.format(dateInMillis)
    }

    fun formatCurrency(amount: Double, locale: Locale = Locale.US): String {
        val currencyFormatter = NumberFormat.getCurrencyInstance(locale)
        return currencyFormatter.format(amount)
    }

    fun formatDayMonthYear(dateInMillis: Long): String {
        val dateFormatter = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
        return dateFormatter.format(dateInMillis)
    }

    fun formatDayMonth(dateInMillis: Long): String {
        val dateFormatter = SimpleDateFormat("dd/MMM", Locale.getDefault())
        return dateFormatter.format(dateInMillis)
    }

    fun formatToDecimalValue(d: Double): String {
        return String.format("%.2f", d)
    }

    fun formatStringDateToMonthDayYear(date: String): String {
        val millis = getMillisFromDate(date)
        return formatDayMonthYear(millis)
    }

    fun getMillisFromDate(date: String): Long {
        return getMilliFromDate(date)
    }

    fun getMilliFromDate(dateFormat: String?): Long {
        var date = Date()
        val formatter = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        try {
            date = formatter.parse(dateFormat)
        } catch (e: ParseException) {
            e.printStackTrace()
        }
        println("Today is $date")
        return date.time
    }

    fun getItemIcon(item: ExpenseEntity): Int {
        val title = item.title ?: ""
        val lower = title.lowercase(Locale.getDefault())
        return when {
            lower.contains("paypal") -> R.drawable.ic_paypal
            lower.contains("netflix") -> R.drawable.ic_netflix
            lower.contains("starbuck") -> R.drawable.ic_starbucks
            else -> R.drawable.ic_upwork
        }
    }

}