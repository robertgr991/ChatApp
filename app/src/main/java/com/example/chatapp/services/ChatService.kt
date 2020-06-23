package com.example.chatapp.services

import android.icu.util.TimeUnit
import android.util.Log
import retrofit2.Call
import com.example.chatapp.App
import com.example.chatapp.models.Message
import com.example.chatapp.models.User
import com.example.chatapp.models.dto.CreateMessageDTO
import com.example.chatapp.notifications.*
import com.example.chatapp.repositories.ChatRepository
import com.example.chatapp.repositories.UserRepository
import com.example.chatapp.utils.Utils
import org.koin.core.KoinComponent
import org.koin.core.inject
import retrofit2.Callback
import java.util.*
import kotlin.collections.ArrayList

class ChatService: KoinComponent {
    private val notificationMessageMaxLength = 20
    private val canDeleteMessageFirstHours = 1
    private val chatRepository: ChatRepository by inject()
    private val userRepository: UserRepository by inject()
    // Notifications service
    private val apiService: APIService = Client.getClient("https://fcm.googleapis.com/").create(APIService::class.java)

    fun setTyping(user: User) {
        chatRepository.setTyping(user)
    }

    fun setOffTyping() {
        chatRepository.setOffTyping()
    }

    fun getAllWithUser(user: User, callback: (ArrayList<Message>) -> Unit) {
        chatRepository.getAllWithUser(user, callback)
    }

    fun deleteMessageForBoth(user: User, message: Message) {
        // Can delete a message for both only in the first 'canDeleteMessageFirstHours'
        if (Utils.dateDiffInHours(message.date, Date()) <= canDeleteMessageFirstHours) {
            chatRepository.deleteMessageForBoth(user, message)
        } else {
            chatRepository.deleteMessageForMe(user, message)
        }
    }

    fun deleteMessageForMe(user: User, message: Message) {
        chatRepository.deleteMessageForMe(user, message)
    }

    fun setSeenLastMessage(user: User, message: Message) {
        chatRepository.setSeenLastMessage(user, message)
    }

    fun setSeenMessage(user: User, message: Message) {
        chatRepository.setSeenMessage(user, message)
    }

    fun setOnConversation(user: User) {
        chatRepository.setOnConversation(user)
    }

    fun setOffConversation() {
        chatRepository.setOffConversation()
    }

    fun removeMessagesWithUser(user: User, callback: (Boolean) -> Unit) {
        chatRepository.removeMessagesWithUser(user, callback)
    }

    fun sendMessage(content: Any, toUser: User, callback: ((Boolean, String?) -> Unit)? = null) {
        if (App.context.currentUser == null) {
            return
        }

        // Check if one of the users blocked the other one
        userRepository.isBlockedBy(toUser, App.context.currentUser!!) {
            if (it) {
                if (callback != null) {
                    callback(false, "You blocked this user")
                }
                return@isBlockedBy
            }

            userRepository.isBlockedBy(App.context.currentUser!!, toUser) secondBlockVerify@{ hasBlocked ->
                if (hasBlocked) {
                    if (callback != null) {
                        callback(false, "This user has blocked you")
                    }
                    return@secondBlockVerify
                }

                val message = CreateMessageDTO("", App.context.currentUser!!.id, toUser.id, Date(), content)
                chatRepository.persist(message) { result, errorMessage ->
                    if (callback != null) {
                        callback(result, errorMessage)
                    }

                    // Send notification if the message was successfully sent
                    if (result) {
                        // Send notification to user's device
                        if (toUser.deviceToken != null) {
                            sendNotification(toUser.id, App.context.currentUser!!, message.content, toUser.deviceToken!!)
                        }
                    }
                }
            }
        }
    }

    private fun sendNotification(toId: String, user: User, message: Any, deviceToken: String) {
        if (user.deviceToken == null) {
            return
        }

        val notificationMessage = if (message.toString().length > notificationMessageMaxLength) {
            message.toString().slice(IntRange(0, notificationMessageMaxLength)) + "..."
        } else {
            message.toString()
        }
        val body = "${user.username}: $notificationMessage"
        val date = Utils.formattedDate()
        val data = Data(toId, user.id, body, date)
        val sender = Sender(data, deviceToken)

        apiService
            .sendNotification(sender)
            .enqueue(object: Callback<Response> {
                override fun onFailure(call: Call<Response>, t: Throwable) {}

                override fun onResponse(
                    call: Call<Response>,
                    response: retrofit2.Response<Response>
                ) {}
            })
    }
}