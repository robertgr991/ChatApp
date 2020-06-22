package com.example.chatapp.validators

import org.koin.dsl.module

@JvmField
val validatorsModule = module {
    single { CreateUserValidator() }
    single { CreateMessageValidator() }
    single { UpdateUserValidator() }
}