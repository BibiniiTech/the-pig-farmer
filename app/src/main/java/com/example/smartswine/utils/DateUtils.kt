package com.example.smartswine.utils

import java.text.SimpleDateFormat
import java.util.*

object DateUtils {
    private fun getTaskDateFormat(locale: Locale = Locale.getDefault()) = SimpleDateFormat("MMM d", locale)
    private fun getDisplayDateFormat(locale: Locale = Locale.getDefault()) = SimpleDateFormat("EEEE, MMMM d, yyyy", locale)
    private fun getInternalDateFormat() = SimpleDateFormat("dd/MM/yyyy", Locale.ENGLISH)
    private fun getProductionDateFormat() = SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH)

    fun formatToInternal(date: Date): String = getInternalDateFormat().format(date)
    fun formatToProduction(date: Date): String = getProductionDateFormat().format(date)
    
    fun isFutureDate(dateStr: String, locale: Locale = Locale.getDefault()): Boolean {
        return try {
            val date = parseDisplay(dateStr, locale) ?: parseTask(dateStr, locale) ?: return false
            
            val now = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            val target = Calendar.getInstance().apply {
                time = date
                if (get(Calendar.YEAR) == 1970) {
                    set(Calendar.YEAR, now[Calendar.YEAR])
                    
                    // Smart year matching
                    val diffDays = (timeInMillis - now.timeInMillis) / (1000 * 60 * 60 * 24)
                    if (diffDays > 180) {
                        add(Calendar.YEAR, -1)
                    } else if (diffDays < -180) {
                        add(Calendar.YEAR, 1)
                    }
                }
            }
            target.after(now)
        } catch (_: Exception) {
            false
        }
    }

    fun parseInternal(dateStr: String): Date? = try { getInternalDateFormat().parse(dateStr) } catch (_: Exception) { null }
    fun parseProduction(dateStr: String): Date? = try { getProductionDateFormat().parse(dateStr) } catch (_: Exception) { null }
    
    fun parseDisplay(dateStr: String, locale: Locale = Locale.getDefault()): Date? {
        // Try current locale
        try { return getDisplayDateFormat(locale).parse(dateStr) } catch (_: Exception) {}
        
        // Try all supported locales as fallbacks
        for (lang in AppLanguage.entries) {
            try { return getDisplayDateFormat(lang.toLocale()).parse(dateStr) } catch (_: Exception) {}
        }
        return null
    }
    
    fun parseTask(dateStr: String, locale: Locale = Locale.getDefault()): Date? {
        try { return getTaskDateFormat(locale).parse(dateStr) } catch (_: Exception) {}
        for (lang in AppLanguage.entries) {
            try { return getTaskDateFormat(lang.toLocale()).parse(dateStr) } catch (_: Exception) {}
        }
        return null
    }

    fun isTaskOverdue(dateStr: String, locale: Locale = Locale.getDefault()): Boolean {
        if ((dateStr == "Today") || (dateStr == "Tomorrow")) return false
        return try {
            val taskDate = parseTask(dateStr, locale) ?: return false
            val now = Calendar.getInstance()
            val taskCal = Calendar.getInstance().apply {
                time = taskDate
                set(Calendar.YEAR, now[Calendar.YEAR])
                
                // Smart year matching: Find if this month/day is closer to last year, this year, or next year
                val diffDays = (timeInMillis - now.timeInMillis) / (1000 * 60 * 60 * 24)
                if (diffDays > 180) {
                    add(Calendar.YEAR, -1)
                } else if (diffDays < -180) {
                    add(Calendar.YEAR, 1)
                }
                // End of the day check
                set(Calendar.HOUR_OF_DAY, 23)
                set(Calendar.MINUTE, 59)
                set(Calendar.SECOND, 59)
            }
            taskCal.before(now)
        } catch (_: Exception) {
            false
        }
    }

    fun getCurrentDateDisplay(locale: Locale = Locale.getDefault()): String {
        return getDisplayDateFormat(locale).format(Calendar.getInstance().time)
    }

    fun getGreeting(): String {
        return when (Calendar.getInstance()[Calendar.HOUR_OF_DAY]) {
            in 0..11 -> "good_morning"
            in 12..16 -> "good_afternoon"
            else -> "good_evening"
        }
    }
    
    fun formatDateToDisplay(millis: Long, locale: Locale = Locale.getDefault()): String {
        return getDisplayDateFormat(locale).format(Date(millis))
    }

    fun convertToTaskDate(dateStr: String, locale: Locale = Locale.getDefault()): String {
        return try {
            val date = parseDisplay(dateStr, locale) ?: parseInternal(dateStr) ?: return dateStr
            getTaskDateFormat(locale).format(date)
        } catch (_: Exception) {
            dateStr
        }
    }

    fun convertToDisplayDate(dateStr: String, locale: Locale = Locale.getDefault()): String {
        return try {
            val date = parseTask(dateStr, locale) ?: parseInternal(dateStr) ?: return dateStr
            
            val now = Calendar.getInstance()
            val target = Calendar.getInstance().apply {
                time = date
                if (get(Calendar.YEAR) == 1970) {
                    set(Calendar.YEAR, now[Calendar.YEAR])
                }
            }
            getDisplayDateFormat(locale).format(target.time)
        } catch (_: Exception) {
            dateStr
        }
    }

    fun addDaysToDate(dateStr: String, days: Int, locale: Locale = Locale.getDefault()): String {
        return try {
            val date = parseDisplay(dateStr, locale) ?: parseTask(dateStr, locale) ?: return dateStr
            
            val cal = Calendar.getInstance().apply {
                time = date
                add(Calendar.DAY_OF_YEAR, days)
            }
            // If it was a display date (has year usually), return display date, otherwise task date
            if (dateStr.contains(",")) {
                 getDisplayDateFormat(locale).format(cal.time)
            } else {
                 getTaskDateFormat(locale).format(cal.time)
            }
        } catch (_: Exception) {
            dateStr
        }
    }

    fun calculateAgeMonths(birthDateStr: String): Int {
        return try {
            val birthDate = getInternalDateFormat().parse(birthDateStr) ?: return 0
            val today = Calendar.getInstance()
            val birth = Calendar.getInstance().apply { time = birthDate }
            
            var months = (today[Calendar.YEAR] - birth[Calendar.YEAR]) * 12
            months += today[Calendar.MONTH] - birth[Calendar.MONTH]
            
            if (today[Calendar.DAY_OF_MONTH] < birth[Calendar.DAY_OF_MONTH]) {
                months--
            }
            months
        } catch (_: Exception) {
            0
        }
    }
}
