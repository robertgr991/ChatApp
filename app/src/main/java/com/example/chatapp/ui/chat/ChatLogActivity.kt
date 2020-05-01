package com.example.chatapp.ui.chat

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.chatapp.App
import com.example.chatapp.R
import com.example.chatapp.events.firebase.ChatEventsManager
import com.example.chatapp.models.Message
import com.example.chatapp.models.User
import com.example.chatapp.services.ChatService
import com.example.chatapp.ui.ActivitiesManager
import com.example.chatapp.ui.notifiers.ToastNotifier
import com.example.chatapp.validators.CreateMessageValidator
import com.google.firebase.database.ChildEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.squareup.picasso.Picasso
import kotlinx.android.synthetic.main.activity_chat_log.*
import org.koin.android.ext.android.inject
import kotlin.collections.ArrayList

class ChatLogActivity : AppCompatActivity() {
    private val chatEventsManager: ChatEventsManager by inject()
    private val chatService: ChatService by inject()
    private val toastNotifier: ToastNotifier by inject()
    private val createMessageValidator: CreateMessageValidator by inject()
    private lateinit var messagesAdapter: MessagesAdapter
    private lateinit var toUser: User
    private var isSendDisabled = false
    // Listen for new messages in this conversation
    private val conversationListener: ChildEventListener = object: ChildEventListener {
        override fun onCancelled(p0: DatabaseError) {}

        override fun onChildMoved(p0: DataSnapshot, p1: String?) {}

        override fun onChildRemoved(p0: DataSnapshot) {}

        override fun onChildChanged(p0: DataSnapshot, p1: String?) {}

        override fun onChildAdded(snapshot: DataSnapshot, p1: String?) {
            val message = snapshot.getValue(Message::class.java) ?: return
            messagesAdapter.messages.add(message)
            messagesAdapter.notifyDataSetChanged()

            if (message.fromId == App.context.currentUser!!.id) {
                chat_log_recyclerview.smoothScrollToPosition(messagesAdapter.itemCount - 1)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        if (App.context.currentUser == null) {
            ActivitiesManager.redirectToLogin(this)
        }

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat_log)
        supportActionBar?.hide()

        // Set other user and it's username/image
        val user = intent.getParcelableExtra<User>("user")
        toUser = user
        chat_log_txt_username.text = toUser.username

        if (toUser.imageName != null) {
            Picasso.get().load(toUser.imageName).into(chat_log_imgview_image)

        } else {
            Picasso.get().load(R.drawable.default_avatar).into(chat_log_imgview_image)
        }

        // Create the recycler view and the adapter
        messagesAdapter = MessagesAdapter(ArrayList(), this)
        val layoutManager = LinearLayoutManager(this)
        layoutManager.stackFromEnd = true
        chat_log_recyclerview.layoutManager = layoutManager
        chat_log_recyclerview.adapter = messagesAdapter

        chat_log_btn_back.setOnClickListener {
            finish()
        }

        // Send message logic
        chat_log_btn_send.setOnClickListener {
            if (isSendDisabled) {
                return@setOnClickListener
            }

            disableSendBtn()
            val content = chat_log_txt_message.text.toString()
            val validation = createMessageValidator.validate(content)

            if (!validation.status) {
                toastNotifier.notify(this, validation.messages[0], toastNotifier.lengthLong)
                return@setOnClickListener
            }

            chatService.sendMessage(content, toUser) { result, message ->
                if (result) {
                    chat_log_txt_message.setText("")
                } else if (message != null) {
                    toastNotifier.notify(this, message, toastNotifier.lengthLong)
                }

                enableSendBtn()
            }
        }
    }

    private fun disableSendBtn() {
        chat_log_btn_send.background = this.getDrawable(R.drawable.rounded_btn_disabled)
        isSendDisabled = true
    }

    private fun enableSendBtn() {
        chat_log_btn_send.background = this.getDrawable(R.drawable.rounded_btn_accent)
        isSendDisabled = false
    }

    override fun onStart() {
        super.onStart()
        chatEventsManager.onUserConversation(toUser, conversationListener)
    }

    override fun onStop() {
        super.onStop()
        chatEventsManager.offUserConversation(toUser, conversationListener)
    }
}
