package com.example.chatapp.ui

import com.example.chatapp.ui.notifiers.ToastNotifier
import org.koin.dsl.module

@JvmField
val uiModule = module {
    single { ToastNotifier() }
}