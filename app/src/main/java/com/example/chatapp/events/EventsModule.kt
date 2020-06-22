package com.example.chatapp.events

import com.example.chatapp.events.firebase.ChatEventsManager
import com.example.chatapp.events.firebase.UserEventsManager
import org.koin.dsl.module

@JvmField
val eventsModule = module {
    single { ChatEventsManager() }
    single { UserEventsManager() }
}