package com.example.chatapp.notifications

import com.example.chatapp.BuildConfig
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.Headers
import retrofit2.http.POST

interface APIService {
    @Headers(
            "Content-Type: application/json",
            "Authorization: key=${BuildConfig.API_KEY}"
    )
    @POST("fcm/send")
    fun sendNotification(@Body body: Sender): Call<Response>
}