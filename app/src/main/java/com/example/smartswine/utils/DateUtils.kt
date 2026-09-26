package com.example.smartswine.utils

import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

object DateUtils {
    private fun getTaskDateFormat(locale: Locale = Locale.getDefault()) = SimpleDateFormat("MMM d", locale)
    private fun getDisplayDateFormat(locale: Locale = Locale.getDefault()) = SimpleDateFormat("EEEE, MMMM d, yyyy", locale)
    private fun getInternalDateFormat() = SimpleDateFormat("dd/MM/yyyy", Locale.ENGLISH)
    private fun getProductionDateFormat() = SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH)

    fun formatToInternal(date: Date): String = getInternalDateFormat().format(date)
    fun formatToProduction(date: Date): String = getProductionDateFormat().format(date)
    
    fun parseInternal(dateStr: String): Date? = try { getInternalDateFormat().parse(dateStr) } catch (_: Exception) { null }
    fun parseProduction(dateStr: String): Date? = try { getProductionDateFormat().parse(dateStr) } catch (_: Exception) { null }

    fun parseAnyDate(dateStr: String, locale: Locale = Locale.getDefault()): Date? {
        val trimmed = dateStr.trim()
        if (trimmed.isEmpty()) return null
        return parseProduction(trimmed)
            ?: parseInternal(trimmed)
            ?: parseSwineDate(trimmed)
            ?: parseDisplay(trimmed, locale)
            ?: parseTask(trimmed, locale)
    }

    fun parseAnyDateNonNull(dateStr: String, locale: Locale = Locale.getDefault()): Date {
        return parseAnyDate(dateStr, locale) ?: Date(0)
    }

    fun isFutureDate(dateStr: String, locale: Locale = Locale.getDefault()): Boolean {
        return try {
            val date = parseAnyDate(dateStr, locale) ?: return false
            
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

    fun isWithdrawalActive(dateStr: String, locale: Locale = Locale.getDefault()): Boolean {
        if (dateStr.isBlank()) return false
        return try {
            val date = parseAnyDate(dateStr, locale) ?: return false
            val today = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            val target = Calendar.getInstance().apply {
                time = date
                if (get(Calendar.YEAR) == 1970) {
                    set(Calendar.YEAR, today[Calendar.YEAR])
                }
                set(Calendar.HOUR_OF_DAY, 23)
                set(Calendar.MINUTE, 59)
                set(Calendar.SECOND, 59)
                set(Calendar.MILLISECOND, 999)
            }
            target.timeInMillis >= today.timeInMillis
        } catch (_: Exception) {
            false
        }
    }

    fun parseSwineDate(dateStr: String): Date? {
        val trimmed = dateStr.trim()
        if (trimmed.isEmpty()) return null

        // 1. yyyy-MM-dd or yyyy/MM/dd (Year first)
        if (Regex("^\\d{4}[-/]\\d{1,2}[-/]\\d{1,2}.*").matches(trimmed)) {
            val datePart = trimmed.substringBefore("T").substringBefore(" ")
            val sep = if (datePart.contains("-")) "-" else "/"
            val parts = datePart.split(sep)
            if (parts.size >= 3) {
                val year = parts[0].toIntOrNull() ?: return null
                val month = parts[1].toIntOrNull() ?: return null
                val day = parts[2].toIntOrNull() ?: return null
                if (month in 1..12 && day in 1..31) {
                    val cal = Calendar.getInstance().apply {
                        set(Calendar.YEAR, year)
                        set(Calendar.MONTH, month - 1)
                        set(Calendar.DAY_OF_MONTH, day)
                        set(Calendar.HOUR_OF_DAY, 0)
                        set(Calendar.MINUTE, 0)
                        set(Calendar.SECOND, 0)
                        set(Calendar.MILLISECOND, 0)
                    }
                    return cal.time
                }
            }
        }

        // 2. dd/MM/yyyy or dd-MM-yyyy (Day first - e.g. 15/10/2024 or 5-3-2024)
        if (Regex("^\\d{1,2}[-/]\\d{1,2}[-/]\\d{4}$").matches(trimmed)) {
            val sep = if (trimmed.contains("/")) "/" else "-"
            val parts = trimmed.split(sep)
            if (parts.size == 3) {
                val p0 = parts[0].toIntOrNull() ?: return null
                val p1 = parts[1].toIntOrNull() ?: return null
                val year = parts[2].toIntOrNull() ?: return null

                val day: Int
                val month: Int
                if (p0 > 12 && p1 <= 12) {
                    day = p0
                    month = p1
                } else if (p1 > 12 && p0 <= 12) {
                    month = p0
                    day = p1
                } else {
                    day = p0
                    month = p1
                }

                if (month in 1..12 && day in 1..31) {
                    val cal = Calendar.getInstance().apply {
                        set(Calendar.YEAR, year)
                        set(Calendar.MONTH, month - 1)
                        set(Calendar.DAY_OF_MONTH, day)
                        set(Calendar.HOUR_OF_DAY, 0)
                        set(Calendar.MINUTE, 0)
                        set(Calendar.SECOND, 0)
                        set(Calendar.MILLISECOND, 0)
                    }
                    return cal.time
                }
            }
        }

        // 3. Epoch milliseconds if numeric
        val millis = trimmed.toLongOrNull()
        if (millis != null && millis > 1000000000L) {
            return Date(millis)
        }

        // 4. Strict fallbacks checking year boundary
        try {
            val d = SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH).apply { isLenient = false }.parse(trimmed)
            if (d != null) {
                val cal = Calendar.getInstance().apply { time = d }
                if (cal[Calendar.YEAR] in 1990..2100) return d
            }
        } catch (_: Exception) {}

        try {
            val d = SimpleDateFormat("dd/MM/yyyy", Locale.ENGLISH).apply { isLenient = false }.parse(trimmed)
            if (d != null) {
                val cal = Calendar.getInstance().apply { time = d }
                if (cal[Calendar.YEAR] in 1990..2100) return d
            }
        } catch (_: Exception) {}

        return parseDisplay(trimmed)
    }

    fun normalizeToStandardSwineDate(dateStr: String): String {
        val date = parseSwineDate(dateStr) ?: return dateStr
        return formatToInternal(date)
    }
    
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
            val taskDate = parseAnyDate(dateStr, locale) ?: return false
            val now = Calendar.getInstance()
            val taskCal = Calendar.getInstance().apply {
                time = taskDate
                if (get(Calendar.YEAR) == 1970) {
                    set(Calendar.YEAR, now[Calendar.YEAR])
                    val diffDays = (timeInMillis - now.timeInMillis) / (1000 * 60 * 60 * 24)
                    if (diffDays > 180) {
                        add(Calendar.YEAR, -1)
                    } else if (diffDays < -180) {
                        add(Calendar.YEAR, 1)
                    }
                }
                // End of the day check
                set(Calendar.HOUR_OF_DAY, 23)
                set(Calendar.MINUTE, 59)
                set(Calendar.SECOND, 59)
                set(Calendar.MILLISECOND, 999)
            }
            taskCal.before(now)
        } catch (_: Exception) {
            false
        }
    }

    fun isTaskDueOrUpcoming(
        dateStr: String, 
        isWithdrawal: Boolean = false, 
        maxUpcomingDays: Int = 2, 
        locale: Locale = Locale.getDefault()
    ): Boolean {
        if (dateStr.isBlank()) return false
        if (dateStr.equals("Today", ignoreCase = true)) return true
        if (dateStr.equals("Tomorrow", ignoreCase = true)) return !isWithdrawal // Withdrawal clearance is only when due today or overdue
        return try {
            val date = parseAnyDate(dateStr, locale) ?: return false
            val today = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            val target = Calendar.getInstance().apply {
                time = date
                if (get(Calendar.YEAR) == 1970) {
                    set(Calendar.YEAR, today[Calendar.YEAR])
                    val diffDays = (timeInMillis - today.timeInMillis) / (1000 * 60 * 60 * 24)
                    if (diffDays > 180) {
                        add(Calendar.YEAR, -1)
                    } else if (diffDays < -180) {
                        add(Calendar.YEAR, 1)
                    }
                }
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            val diffInMillis = target.timeInMillis - today.timeInMillis
            val diffInDays = TimeUnit.MILLISECONDS.toDays(diffInMillis)

            if (isWithdrawal) {
                // Meat withdrawal clearance only alerts on or after the clearance date
                diffInDays <= 0
            } else {
                // Regular tasks alert if overdue, due today, or upcoming within maxUpcomingDays
                diffInDays <= maxUpcomingDays
            }
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

    fun formatDateToDisplay(date: Date, locale: Locale = Locale.getDefault()): String {
        return formatDateToDisplay(date.time, locale)
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
            val date = parseAnyDate(dateStr, locale) ?: return dateStr
            
            val cal = Calendar.getInstance().apply {
                time = date
                add(Calendar.DAY_OF_YEAR, days)
            }
            // If it was a display date (has year usually), return display date, otherwise appropriate format
            if (dateStr.contains(",")) {
                 getDisplayDateFormat(locale).format(cal.time)
            } else if (dateStr.contains("-")) {
                 getProductionDateFormat().format(cal.time)
            } else if (dateStr.contains("/")) {
                 getInternalDateFormat().format(cal.time)
            } else {
                 getTaskDateFormat(locale).format(cal.time)
            }
        } catch (_: Exception) {
            dateStr
        }
    }

    fun calculateAgeMonths(birthDateStr: String): Int {
        return try {
            val birthDate = parseSwineDate(birthDateStr) ?: return 0
            val today = Calendar.getInstance()
            val birth = Calendar.getInstance().apply { time = birthDate }
            
            var months = (today[Calendar.YEAR] - birth[Calendar.YEAR]) * 12
            months += today[Calendar.MONTH] - birth[Calendar.MONTH]
            
            val maxDayThisMonth = today.getActualMaximum(Calendar.DAY_OF_MONTH)
            val effectiveBirthDay = Math.min(birth[Calendar.DAY_OF_MONTH], maxDayThisMonth)
            if (today[Calendar.DAY_OF_MONTH] < effectiveBirthDay) {
                months--
            }
            Math.max(0, months)
        } catch (_: Exception) {
            0
        }
    }

    fun calculateAgeDays(birthDateStr: String): Int {
        return try {
            val birthDate = parseSwineDate(birthDateStr) ?: return 0
            val today = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            val birth = Calendar.getInstance().apply {
                time = birthDate
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            val diffMs = today.timeInMillis - birth.timeInMillis
            Math.round(diffMs.toDouble() / (1000.0 * 60 * 60 * 24)).toInt()
        } catch (_: Exception) {
            0
        }
    }

    fun calculateAgeWeeks(birthDateStr: String): Int {
        val days = calculateAgeDays(birthDateStr)
        return if (days > 0) days / 7 else 0
    }

    fun formatSwineAge(birthDateStr: String, compact: Boolean = false, lang: String? = null): String {
        val languageCode = lang ?: Translator.currentLanguageCode
        val days = calculateAgeDays(birthDateStr)
        if (days < 0) return Translator.getString("age_future_birth", languageCode)
        if (days == 0) return Translator.getString("age_today", languageCode)
        if (days == 1) return if (compact) Translator.getString("age_day_compact", languageCode, 1) else Translator.getString("age_day_single", languageCode, 1)
        if (days < 7) return if (compact) Translator.getString("age_days_compact", languageCode, days) else Translator.getString("age_days_plural", languageCode, days)
        
        if (days < 30) {
            val weeks = days / 7
            val remDays = days % 7
            val wkStr = if (compact) {
                Translator.getString(if (weeks == 1) "age_week_compact" else "age_weeks_compact", languageCode, weeks)
            } else {
                Translator.getString(if (weeks == 1) "age_week_single" else "age_weeks_plural", languageCode, weeks)
            }
            return if (remDays == 0) {
                wkStr
            } else {
                val dStr = if (compact) {
                    Translator.getString(if (remDays == 1) "age_day_compact" else "age_days_compact", languageCode, remDays)
                } else {
                    Translator.getString(if (remDays == 1) "age_day_single" else "age_days_plural", languageCode, remDays)
                }
                "$wkStr, $dStr"
            }
        }
        
        val months = calculateAgeMonths(birthDateStr)
        val birthDate = parseSwineDate(birthDateStr)
        val remDays = if (birthDate != null && months > 0) {
            val today = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            val lastMonthMilestone = Calendar.getInstance().apply {
                time = birthDate
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
                add(Calendar.MONTH, months)
            }
            java.util.concurrent.TimeUnit.MILLISECONDS.toDays(today.timeInMillis - lastMonthMilestone.timeInMillis).toInt().coerceAtLeast(0)
        } else {
            0
        }

        val moStr = if (compact) {
            Translator.getString(if (months == 1) "age_month_compact" else "age_months_compact", languageCode, months)
        } else {
            Translator.getString(if (months == 1) "age_month_single" else "age_months_plural", languageCode, months)
        }
        val remDaysStr = if (compact) {
            Translator.getString(if (remDays == 1) "age_day_compact" else "age_days_compact", languageCode, remDays)
        } else {
            Translator.getString(if (remDays == 1) "age_day_single" else "age_days_plural", languageCode, remDays)
        }

        if (months < 12) {
            return if (remDays == 0) moStr else "$moStr, $remDaysStr"
        }
        
        val years = months / 12
        val remMonths = months % 12
        val yrStr = if (compact) {
            Translator.getString(if (years == 1) "age_year_compact" else "age_years_compact", languageCode, years)
        } else {
            Translator.getString(if (years == 1) "age_year_single" else "age_years_plural", languageCode, years)
        }
        val remMoStr = if (compact) {
            Translator.getString(if (remMonths == 1) "age_month_compact" else "age_months_compact", languageCode, remMonths)
        } else {
            Translator.getString(if (remMonths == 1) "age_month_single" else "age_months_plural", languageCode, remMonths)
        }

        return when {
            remMonths == 0 && remDays == 0 -> yrStr
            remMonths == 0 -> "$yrStr, $remDaysStr"
            remDays == 0 -> "$yrStr, $remMoStr"
            else -> "$yrStr, $remMoStr, $remDaysStr"
        }
    }
}
