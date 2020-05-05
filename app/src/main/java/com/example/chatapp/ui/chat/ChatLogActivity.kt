package com.example.chatapp.ui.chat

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.ContextThemeWrapper
import android.view.Gravity
import android.view.MenuItem
import android.widget.PopupMenu
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.example.chatapp.App
import com.example.chatapp.R
import com.example.chatapp.events.firebase.ChatEventsManager
import com.example.chatapp.events.firebase.UserEventsManager
import com.example.chatapp.models.Message
import com.example.chatapp.models.User
import com.example.chatapp.services.ChatService
import com.example.chatapp.services.NotificationsService
import com.example.chatapp.ui.ActivitiesManager
import com.example.chatapp.ui.notifiers.ToastNotifier
import com.example.chatapp.utils.Utils
import com.example.chatapp.validators.CreateMessageValidator
import com.google.firebase.database.ChildEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import kotlinx.android.synthetic.main.activity_chat_log.*
import org.koin.android.ext.android.inject
import java.util.*
import kotlin.collections.ArrayList
import kotlin.concurrent.thread
import kotlin.properties.Delegates


class ChatLogActivity : AppCompatActivity() {
    private val chatEventsManager: ChatEventsManager by inject()
    private val userEventsManager: UserEventsManager by inject()
    private val chatService: ChatService by inject()
    private val toastNotifier: ToastNotifier by inject()
    private val createMessageValidator: CreateMessageValidator by inject()
    private val notificationsService: NotificationsService by inject()
    private lateinit var messagesAdapter: MessagesAdapter
    private lateinit var partner: User
    private var onlineStatusColor by Delegates.notNull<Int>()
    private var offlineStatusColor by Delegates.notNull<Int>()
    private var isSendDisabled = false
    // Listen for messages in this conversation
    private val conversationListener: ChildEventListener = object: ChildEventListener {
        override fun onCancelled(p0: DatabaseError) {}

        override fun onChildMoved(p0: DataSnapshot, p1: String?) {}

        override fun onChildRemoved(snapshot: DataSnapshot) {
            val message = snapshot.getValue(Message::class.java) ?: return
            thread(start = true) {
                var position = -1

                for ((index, msg) in messagesAdapter.messages.withIndex()) {
                    if (msg.id == message.id) {
                        position = index
                        break
                    }
                }

                if (position == -1) {
                    return@thread
                }

                messagesAdapter.messages.removeAt(position)
                runOnUiThread {
                    messagesAdapter.notifyItemRemoved(position)
                }
            }
        }

        override fun onChildChanged(snapshot: DataSnapshot, p1: String?) {
            val message = snapshot.getValue(Message::class.java) ?: return
            thread(start = true) {
                var position = -1

                for ((index, msg) in messagesAdapter.messages.withIndex()) {
                    if (msg.id == message.id) {
                        position = index
                        break
                    }
                }

                if (position == -1) {
                    return@thread
                }

                messagesAdapter.messages[position] = message
                runOnUiThread {
                    messagesAdapter.notifyItemChanged(position)
                }
            }
        }

        override fun onChildAdded(snapshot: DataSnapshot, p1: String?) {
            val message = snapshot.getValue(Message::class.java) ?: return
            messagesAdapter.messages.add(message)
            messagesAdapter.notifyItemInserted(messagesAdapter.itemCount - 1)
            chat_log_recyclerview.smoothScrollToPosition(messagesAdapter.itemCount - 1)

            if (message.fromId == partner.id && message.seen == "false") {
                chatService.setSeenMessage(partner, message)
            }
        }
    }
    // Listen for partner status change
    @ExperimentalStdlibApi
    private val partnerStatusListener: ValueEventListener = object: ValueEventListener {
        override fun onCancelled(p0: DatabaseError) {}

        override fun onDataChange(snapshot: DataSnapshot) {
            val status = snapshot.getValue(String::class.java) ?: return

            partner.status = status
            chat_log_txt_user_status.text = status.capitalize(Locale.ROOT)

            chat_log_txt_user_status.setTextColor(if (status == "online") {
                onlineStatusColor
            } else {
                offlineStatusColor
            })
        }
    }
    // Listen for image change
    private val partnerImageListener: ValueEventListener = object: ValueEventListener {
        override fun onCancelled(p0: DatabaseError) {}

        override fun onDataChange(snapshot: DataSnapshot) {
            val newImage = snapshot.getValue(String::class.java)
            partner.imageName = newImage

            if (newImage == null) {
                Glide.with(applicationContext).load(R.drawable.default_avatar).into(chat_log_imgview_image)
            } else {
                Glide.with(applicationContext).load(newImage).into(chat_log_imgview_image)
            }
        }
    }
    // Listen for partner device token change(for sending notifications)
    private val partnerTokenListener: ValueEventListener = object: ValueEventListener {
        override fun onCancelled(p0: DatabaseError) {
        }

        override fun onDataChange(snapshot: DataSnapshot) {
            val newToken = snapshot.getValue(String::class.java)
            partner.deviceToken = newToken
        }
    }
    // Listen for latest message to show "Seen" to the other user
    // when current user reads the last message
    private val latestMessageListener: ValueEventListener = object: ValueEventListener {
        override fun onCancelled(p0: DatabaseError) {

        }
        override fun onDataChange(snapshot: DataSnapshot) {
            val message = snapshot.getValue(Message::class.java) ?: return

            if (message.fromId == partner.id && message.seen == "false") {
                chatService.setSeenLastMessage(partner, message)
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

        // Set status colors
        onlineStatusColor = ContextCompat.getColor(this, R.color.colorOnlineStatus)
        offlineStatusColor = ContextCompat.getColor(this, R.color.colorPrimaryDark)

        // Set other user and it's username/image
        val user = intent.getParcelableExtra<User>("user")
        partner = user
        chat_log_txt_username.text = partner.username

        if (partner.imageName != null) {
            Glide.with(this).load(partner.imageName).into(chat_log_imgview_image)

        } else {
            Glide.with(this).load(R.drawable.default_avatar).into(chat_log_imgview_image)
        }

        // Create the recycler view and the adapter
        messagesAdapter = MessagesAdapter(ArrayList(), this)
        messagesAdapter.setOnOwnMessageClickListener { view, message ->
            Log.d("MESSAGE OWN", message.toString())
            val wrapper = ContextThemeWrapper(this, R.style.PopupMenu)
            val popup = PopupMenu(wrapper, view, Gravity.END)
            popup.inflate(R.menu.chat_log_message_popup_menu)
            popup.setOnMenuItemClickListener { item: MenuItem? ->
                when (item!!.itemId) {
                    R.id.message_popup_forward -> {
                        Log.d("ITEM 1", item.title.toString())
                    }
                    R.id.message_popup_copy -> {
                        copyMessageToClipboard(message)
                    }
                    R.id.message_popup_delete -> {
                        chatService.deleteMessageForBoth(partner, message)
                    }
                    R.id.message_popup_seen -> {
                        showSeen(message)
                    }
                }
                true
            }
            popup.gravity = Gravity.CENTER_HORIZONTAL
            popup.show()
        }
        messagesAdapter.setOnMessageClickListener { view, message ->
            Log.d("MESSAGE", message.toString())
        }
        val layoutManager = LinearLayoutManager(this)
        layoutManager.stackFromEnd = true
        chat_log_recyclerview.layoutManager = layoutManager
        chat_log_recyclerview.adapter = messagesAdapter

        chat_log_btn_back.setOnClickListener {
            ActivitiesManager.redirectToHomepage(this)
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

            chatService.sendMessage(content, partner) { result, message ->
                if (result) {
                    chat_log_txt_message.setText("")
                } else if (message != null) {
                    toastNotifier.notify(this, message, toastNotifier.lengthLong)
                }

                enableSendBtn()
            }
        }

        // Show partner user profile
        chat_log_imgview_image.setOnClickListener {
            showUserProfile()
        }
        chat_log_txt_username.setOnClickListener {
            showUserProfile()
        }
        chat_log_txt_user_status.setOnClickListener {
            showUserProfile()
        }
    }

    private fun showSeen(message: Message) {
        if (message.seen != "false") {
            if (message.seenAt != null) {
                toastNotifier.notify(this, "Seen at: ${Utils.formattedDate(message.seenAt)}", toastNotifier.lengthShort)
            }
        } else {
            toastNotifier.notify(this, "Not yet seen", toastNotifier.lengthShort)
        }
    }

    private fun copyMessageToClipboard(message: Message) {
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val text = message.content.toString()
        val clip = ClipData.newPlainText("text", text)
        clipboard.primaryClip = clip
        toastNotifier.notify(this, "Message copied", toastNotifier.lengthShort)
    }

    private fun showUserProfile() {
        ActivitiesManager.redirectToProfile(this, partner)
        finish()
    }

    private fun disableSendBtn() {
        chat_log_btn_send.background = this.getDrawable(R.drawable.rounded_btn_disabled)
        isSendDisabled = true
    }

    private fun enableSendBtn() {
        chat_log_btn_send.background = this.getDrawable(R.drawable.rounded_btn_accent)
        isSendDisabled = false
    }

    override fun onBackPressed() {
        super.onBackPressed()
        ActivitiesManager.redirectToHomepage(this)
    }

    @ExperimentalStdlibApi
    override fun onStart() {
        super.onStart()
        thread(start = true) {
            // Cancel notifications from this user, if there are any
            notificationsService.cancelNotificationFromUser(partner)
            chatEventsManager.onLatestMessageWithUser(partner, latestMessageListener)
            chatEventsManager.onUserConversation(partner, conversationListener)
            userEventsManager.onUserStatus(partner, partnerStatusListener)
            userEventsManager.onUserImage(partner, partnerImageListener)
            userEventsManager.onUserToken(partner, partnerTokenListener)
            chatService.setOnConversation(partner)
        }
    }

    @ExperimentalStdlibApi
    override fun onStop() {
        super.onStop()
        thread(start = true) {
            chatEventsManager.offLatestMessageWithUser(partner, latestMessageListener)
            chatEventsManager.offUserConversation(partner, conversationListener)
            userEventsManager.offUserStatus(partner, partnerStatusListener)
            userEventsManager.offUserImage(partner, partnerImageListener)
            userEventsManager.offUserToken(partner, partnerTokenListener)
            chatService.setOffConversation()
        }
    }
}
