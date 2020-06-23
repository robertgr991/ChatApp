package com.example.chatapp.utils

import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

/**
 * General utilities methods
 */
class Utils {
    companion object {
        fun formattedDate(date: Date = Date()): String {
            val pattern = "dd/MM/yyyy hh:mm"
            val simpleDateFormat = SimpleDateFormat(pattern, Locale.ROOT)
            return simpleDateFormat.format(date)
        }

        /**
         * date1: Smaller date
         * date2: Bigger date
         */
        fun dateDiffInHours(date1: Date, date2: Date): Long = TimeUnit.MILLISECONDS.toHours(date2.time - date1.time)
    }
}