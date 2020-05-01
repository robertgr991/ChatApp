package com.example.chatapp.services

import org.koin.dsl.module

@JvmField
val servicesModule = module {
    single { UserService() }
    single { ChatService() }
}