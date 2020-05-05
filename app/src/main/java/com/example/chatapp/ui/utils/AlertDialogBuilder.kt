package com.example.chatapp.ui.utils

import android.content.Context
import android.content.DialogInterface
import androidx.appcompat.app.AlertDialog

class AlertDialogBuilder {
    companion object {
        fun positiveNegativeDialog(
            context: Context,
            title: String,
            message: String,
            positiveText: String,
            positiveListener: DialogInterface.OnClickListener,
            negativeText: String,
            negativeListener: DialogInterface.OnClickListener
        ): AlertDialog {
            val builder = AlertDialog.Builder(context)
            builder.setTitle(title)
            builder.setMessage(message)
            builder.setPositiveButton(positiveText, positiveListener)
            builder.setNegativeButton(negativeText, negativeListener)

            return builder.create()
        }
    }
}