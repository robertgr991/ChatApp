package com.example.chatapp.utils

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.provider.MediaStore
import java.io.ByteArrayOutputStream
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

        fun getImageUriFromBitmap(context: Context, bitmap: Bitmap): Uri {
            val bytes = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, bytes)
            val path = MediaStore.Images.Media.insertImage(context.contentResolver, bitmap, "Title", null)
            return Uri.parse(path.toString())
        }

        /**
         * date1: Smaller date
         * date2: Bigger date
         */
        fun dateDiffInHours(date1: Date, date2: Date): Long = TimeUnit.MILLISECONDS.toHours(date2.time - date1.time)
    }
}