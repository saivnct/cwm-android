package com.lgt.cwm.util

import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import android.text.format.DateFormat
import android.text.format.DateUtils
import android.util.Log
import com.lgt.cwm.R
import java.text.DateFormatSymbols
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

/**
 * Created by giangtpu on 7/25/22.
 */
object DateUtil {
    const val DATE_FORMAT_SEARCH = "dd/MM/yyyy"
    const val TIME_FORMAT_SEARCH = "HH:mm"
    const val DATE_TIME_FORMAT_SEARCH = "dd/MM/yyyy HH:mm"

    const val DefaultDateSearchText = "_ _ _"
    const val DefaultTimeSearchText = "_ _"

    const val TimeBeginOfDayText = "00:00"
    const val TimeEndOfDayText = "23:59"

    fun convertLongToTime(time: Long): String {
        val date = Date(time)
        val format = SimpleDateFormat("yyyy-MM-dd HH:mm")
        return format.format(date)
    }

    fun convertLongToTimeDayChat(time: Long): String {
        val date = Date(time)
        val format = SimpleDateFormat("dd/MM")
        return format.format(date)
    }

    fun convertLongToTimeHourChat(time: Long): String {
        val date = Date(time)
        val format = SimpleDateFormat("HH:mm")
        return format.format(date)
    }

    fun formatDateWithFormat(date: String?, formatDate: String?): Date? {
        val sdf = SimpleDateFormat(formatDate)
        try {
            return sdf.parse(date)
        } catch (var5: ParseException) {
            var5.printStackTrace()
        } catch (var6: Exception) {
            var6.printStackTrace()
        }
        return null
    }

    fun now(): Date{
        val cal = Calendar.getInstance()
        return cal.time
    }

    fun getDateAfter(date: Date, days: Int): Date{
        val cal = GregorianCalendar()
        cal.time = date
        cal.add(Calendar.DAY_OF_YEAR, days)

        return cal.time
    }

    fun getDateAfterHours(date: Date, hours: Int): Date{
        val cal = GregorianCalendar()
        cal.time = date
        cal.add(Calendar.HOUR_OF_DAY, hours)

        return cal.time
    }

    fun formatStringDateWithFormat(date: Date, formatDate: String): String {
        val sdf: SimpleDateFormat
        return if (formatDate.isNullOrBlank()) {
            sdf = SimpleDateFormat(DATE_FORMAT_SEARCH)
            sdf.format(date)
        } else {
            sdf = SimpleDateFormat(formatDate)
            sdf.format(date)
        }
    }

    private fun isYesterday(`when`: Long): Boolean {
        return DateUtils.isToday(`when` + TimeUnit.DAYS.toMillis(1))
    }

    private fun isWithin(millis: Long, span: Long, unit: TimeUnit): Boolean {
        return System.currentTimeMillis() - millis <= unit.toMillis(span)
    }

    private fun formatDateWithDayOfWeek(locale: Locale, timestamp: Long): String {
        return getFormattedDateTime(timestamp, "EEE, MMM d", locale)
    }

    private fun formatDateWithYear(locale: Locale, timestamp: Long): String {
        return getFormattedDateTime(timestamp, "MMM d, yyyy", locale)
    }

    private fun getFormattedDateTime(time: Long, template: String, locale: Locale): String {
        val localizedPattern: String = getLocalizedPattern(template, locale)
        return setLowercaseAmPmStrings(SimpleDateFormat(localizedPattern, locale), locale)
            .format(Date(time))
    }

    private fun getLocalizedPattern(template: String, locale: Locale): String {
        return DateFormat.getBestDateTimePattern(locale, template)
    }

    private fun setLowercaseAmPmStrings(format: SimpleDateFormat, locale: Locale): SimpleDateFormat {
        val symbols = DateFormatSymbols(locale)
        symbols.amPmStrings = arrayOf("am", "pm")
        format.dateFormatSymbols = symbols
        return format
    }

    fun getConversationDateHeaderString(context: Context, locale: Locale, timestamp: Long): String {
        return if (DateUtils.isToday(timestamp)) {
            context.getString(R.string.DateUtils_today)
        } else if (isYesterday(timestamp)) {
            context.getString(R.string.DateUtils_yesterday)
        } else if (isWithin(timestamp, 182, TimeUnit.DAYS)) {
            //within 182 days (6 months) format "Wed, Jul 4"
            formatDateWithDayOfWeek(locale, timestamp)
        } else {
            //format "Jul 4, 2022"
            formatDateWithYear(locale, timestamp)
        }
    }

    @SuppressLint("SimpleDateFormat")
    private fun getDateFormat(): SimpleDateFormat {
        return SimpleDateFormat()
    }

    fun isSameDay(t1: Long, t2: Long): Boolean {
        val d1: String = getDateFormat().format(Date(t1))
        val d2: String = getDateFormat().format(Date(t2))
        return d1 == d2
    }

    /**
     * e.g. 2020-09-04T19:17:51Z
     * https://www.iso.org/iso-8601-date-and-time-format.html
     *
     * Note: SDK_INT == 0 check needed to pass unit tests due to JVM date parser differences.
     *
     * @return The timestamp if able to be parsed, otherwise -1.
     */
    @JvmStatic
    @SuppressLint("ObsoleteSdkInt", "NewApi")
    fun parseIso8601(date: String?): Long {
        val format: SimpleDateFormat =
            if (Build.VERSION.SDK_INT == 0 || Build.VERSION.SDK_INT >= 24) {
                SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssX", Locale.getDefault())
            } else {
                SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ", Locale.getDefault())
            }
        return if (Util.isEmpty(date)) {
            -1
        } else try {
            format.parse(date).time
        } catch (e: ParseException) {
            Log.w("DateUtils", "Failed to parse date.", e)
            -1
        }
    }

}
