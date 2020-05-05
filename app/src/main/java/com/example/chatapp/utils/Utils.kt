package com.example.chatapp.utils

import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

class Utils {
    companion object {
        fun formattedDate(date: Date = Date()): String {
            val pattern = "dd/MM/yyyy hh:mm"
            val simpleDateFormat = SimpleDateFormat(pattern, Locale.ROOT)
            return simpleDateFormat.format(date)
        }

        fun dateDiffInHours(date1: Date, date2: Date): Long = TimeUnit.MILLISECONDS.toHours(date1.time - date2.time)
    }
}