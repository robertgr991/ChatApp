package com.example.chatapp.ui.utils

import android.app.AlertDialog
import android.content.Context
import com.example.chatapp.R
import dmax.dialog.SpotsDialog

class ProgressDialog(context: Context) {
    private var progressDialog: AlertDialog = SpotsDialog.Builder()
        .setContext(context)
        .setMessage(R.string.progress_dialog_text)
        .setTheme(R.style.ProgressDialogCustom)
        .setCancelable(false)
        .build()

    fun show() {
        progressDialog.show()
    }

    fun cancel() {
        progressDialog.dismiss()
    }
}