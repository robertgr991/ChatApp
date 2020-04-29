package com.example.chatapp.ui.notifiers

import android.content.Context
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import com.example.chatapp.R

class ToastNotifier {
     val lengthShort = Toast.LENGTH_SHORT
     val lengthLong = Toast.LENGTH_LONG

    fun notify(context: Context, message: String, duration: Int) {
        val toastDuration = if (duration == this.lengthLong) lengthLong else this.lengthShort
        val toast = Toast.makeText(
            context,
            message,
            toastDuration
        )
        val toastMessage = toast.view
            .findViewById<View>(android.R.id.message) as TextView
        toastMessage.setTextColor(ContextCompat.getColor(context, R.color.colorAccent))
        toast.show()
    }
}