package com.example.chatapp

import com.example.chatapp.firebase_events.ChatEventsManager
import com.example.chatapp.repositories.ChatRepository
import com.example.chatapp.repositories.UserRepository
import com.example.chatapp.services.ChatService
import com.example.chatapp.services.UserService
import com.example.chatapp.ui.notifiers.ToastNotifier
import com.example.chatapp.validators.CreateMessageValidator
import com.example.chatapp.validators.CreateUserValidator
import org.koin.dsl.module

@JvmField
val appModule = module {
    single { UserService() }
    single { ChatService() }
    single { ChatRepository() }
    single { ToastNotifier() }
    single { CreateUserValidator() }
    single { CreateMessageValidator() }
    single { UserRepository() }
    single { ChatEventsManager() }
}