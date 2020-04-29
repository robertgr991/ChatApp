package com.example.chatapp

import com.example.chatapp.repositories.UserRepository
import com.example.chatapp.services.UserService
import com.example.chatapp.ui.notifiers.ToastNotifier
import com.example.chatapp.validators.CreateUserValidator
import org.koin.dsl.module

@JvmField
val appModule = module {
    single { UserService() }
    single { ToastNotifier() }
    single { CreateUserValidator() }
    single { UserRepository() }
}