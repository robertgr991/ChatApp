package com.example.chatapp.repositories

import org.koin.dsl.module

@JvmField
val repositoriesModule = module {
    single { ChatRepository() }
    single { UserRepository() }
}