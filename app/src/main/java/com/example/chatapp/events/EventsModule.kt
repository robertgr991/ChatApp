package com.example.chatapp.events

import com.example.chatapp.events.firebase.ChatEventsManager
import org.koin.dsl.module

@JvmField
val eventsModule = module {
    single { ChatEventsManager() }
}