package com.example.chatapp.ui.chat

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.ContextThemeWrapper
import android.view.Gravity
import android.view.MenuItem
import android.view.View
import android.widget.PopupMenu
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
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

/**
 * Conversation activity
 */
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
    private var firstStart = true
    private var partnerLatestMessage: Message? = null
    private var previousTypingText = ""
    // Listen for messages in this conversation
    private val conversationListener: ChildEventListener = object: ChildEventListener {
        override fun onCancelled(p0: DatabaseError) {}

        override fun onChildMoved(p0: DataSnapshot, p1: String?) {}

        /**
         * Message deleted
         */
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

                runOnUiThread {
                    if (messagesAdapter.messages[position].id == message.id) {
                        messagesAdapter.messages.removeAt(position)
                        messagesAdapter.notifyItemRemoved(position)
                    }
                }
            }
        }

        /**
         * Message changed(seen/content deleted)
         */
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


                runOnUiThread {
                    if (messagesAdapter.messages[position].id == message.id) {
                        messagesAdapter.messages[position] = message
                        messagesAdapter.notifyItemChanged(position)
                    }
                }
            }
        }

        /**
         * Message added
         */
        override fun onChildAdded(snapshot: DataSnapshot, p1: String?) {
            val message = snapshot.getValue(Message::class.java) ?: return

            thread(start = true) {
                for (msg in messagesAdapter.messages) {
                    if (msg.id == message.id) {
                        return@thread
                    }
                }

                messagesAdapter.messages.add(message)
                runOnUiThread {
                    messagesAdapter.notifyItemInserted(messagesAdapter.itemCount - 1)
                    chat_log_recyclerview.scrollToPosition(messagesAdapter.itemCount - 1)
                }
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
    // Listen for partner image change
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
    // Listen for latest message to set it as seen "true"
    private val latestMessageListener: ValueEventListener = object: ValueEventListener {
        override fun onCancelled(p0: DatabaseError) {

        }
        override fun onDataChange(snapshot: DataSnapshot) {
            val message = snapshot.getValue(Message::class.java) ?: return

            if (message.fromId == partner.id) {
                partnerLatestMessage = message

                thread(start = true) {
                    // Check if the message was already set to seen in messages
                    // and set it in latest messages as well
                    for (msg in messagesAdapter.messages) {
                        if (
                            msg.id == partnerLatestMessage!!.id &&
                            msg.seen == "true" &&
                            partnerLatestMessage!!.seen == "false"
                        ) {
                            chatService.setSeenLastMessage(partner, partnerLatestMessage!!)
                            break
                        }
                    }
                }
            }
        }
    }
    // Listen for partner typing
    private val partnerTypingListener: ValueEventListener = object: ValueEventListener {
        override fun onCancelled(p0: DatabaseError) {}

        override fun onDataChange(snapshot: DataSnapshot) {
            val userId = snapshot.getValue(String::class.java)

            if (userId != null && userId == App.context.currentUser!!.id) {
                // Show typing status
                chat_log_txt_user_typing.visibility = View.VISIBLE
                chat_log_txt_user_status.visibility = View.GONE
            } else if (userId == null) {
                // Show normal status
                chat_log_txt_user_typing.visibility = View.GONE
                chat_log_txt_user_status.visibility = View.VISIBLE
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        if (App.context.currentUser == null) {
            ActivitiesManager.redirectToLogin(this)
        }

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat_log)
        // Hide the original action bar because a custom bar is used
        supportActionBar?.hide()

        // Set status colors
        onlineStatusColor = ContextCompat.getColor(this, R.color.colorOnlineStatus)
        offlineStatusColor = ContextCompat.getColor(this, R.color.colorPrimaryDark)

        // Set other user and it's username && image
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
            clickOnMessage(view, message, true)
        }
        messagesAdapter.setOnMessageClickListener { view, message ->
            clickOnMessage(view, message)
        }
        val layoutManager = LinearLayoutManager(this)
        layoutManager.stackFromEnd = true
        layoutManager.isSmoothScrollbarEnabled = false
        chat_log_recyclerview.layoutManager = layoutManager
        chat_log_recyclerview.adapter = messagesAdapter
        // Listen for recycler view scroll to show seen only when a message is visible at least once
        chat_log_recyclerview.addOnScrollListener(object: RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                val lastVisiblePosition = layoutManager.findLastCompletelyVisibleItemPosition()
                val firstVisiblePosition = layoutManager.findFirstCompletelyVisibleItemPosition()
                setMessagesSeenInRange(firstVisiblePosition, lastVisiblePosition)
            }
        })
        fetchMessages()

        chat_log_btn_back.setOnClickListener {
            ActivitiesManager.redirectToHomepage(this)
        }
        // Send message logic
        chat_log_btn_send.setOnClickListener {
            if (isSendDisabled) {
                return@setOnClickListener
            }

            // Disable the button until the process finishes
            disableSendBtn()
            val content = chat_log_txt_message.text.toString()
            val validation = createMessageValidator.validate(content)

            // Check if input is valid
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

        // Show partner profile
        chat_log_imgview_image.setOnClickListener {
            showUserProfile()
        }
        chat_log_txt_username.setOnClickListener {
            showUserProfile()
        }
        chat_log_txt_user_status.setOnClickListener {
            showUserProfile()
        }

        // When user is typing a message show it to it's partner
        chat_log_txt_message.addTextChangedListener(object: TextWatcher {
            override fun afterTextChanged(editable: Editable?) {
                val text = editable.toString()

                if (text == "" && previousTypingText != "") {
                    chatService.setOffTyping()
                } else if (text != "" && previousTypingText == ""){
                    chatService.setTyping(partner)
                }

                previousTypingText = text
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })
    }

    private fun fetchMessages() {
        chatService.getAllWithUser(partner) { messages ->
            thread(start = true) {
                var stopScroll = false
                var lastScrollPosition = 0
                messages.forEach {  message ->
                    messagesAdapter.messages.add(message)

                    if (!stopScroll) {
                        lastScrollPosition = messagesAdapter.itemCount - 1
                    }

                    if (message.fromId == partner.id && message.seen == "false") {
                        stopScroll = true
                    }
                }
                runOnUiThread {
                    messagesAdapter.notifyDataSetChanged()
                    chat_log_recyclerview.scrollToPosition(lastScrollPosition)
                    chatEventsManager.onUserConversation(partner, conversationListener)
                }
            }
        }
    }

    private fun setMessagesSeenInRange(position1: Int, position2: Int) {
        thread(start = true) {
            messagesAdapter
                .messages
                .slice(IntRange(position1, position2))
                .forEach { message ->
                if (message.fromId == partner.id && message.seen == "false") {
                    chatService.setSeenMessage(partner, message)
                }

                if (
                    partnerLatestMessage != null &&
                    partnerLatestMessage!!.id == message.id &&
                    partnerLatestMessage!!.seen == "false"
                ) {
                    chatService.setSeenLastMessage(partner, message)
                }
            }
        }
    }

    private fun clickOnMessage(view: View, message: Message, own: Boolean = false) {
        val wrapper = ContextThemeWrapper(this, R.style.PopupMenu)
        val popup = PopupMenu(wrapper, view, Gravity.END)
        popup.inflate(R.menu.chat_log_message_popup_menu)

        /**
         * If the message is deleted, disable some menu items
         * that shouldn't be used on a deleted message
         */
        if (message.deleted == "true") {
            popup.menu.findItem(R.id.message_popup_seen).isVisible = false
            popup.menu.findItem(R.id.message_popup_copy).isVisible = false
            popup.menu.findItem(R.id.message_popup_forward).isVisible = false
        }

        popup.setOnMenuItemClickListener { item: MenuItem? ->
            when (item!!.itemId) {
                R.id.message_popup_forward -> {
                    forwardMessage(message.content)
                }
                R.id.message_popup_copy -> {
                    copyMessageToClipboard(message)
                }
                R.id.message_popup_delete -> {
                    if (own) {
                        deleteOwnMessage(partner, message)
                    } else {
                        deletePartnerMessage(partner, message)
                    }
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

    private fun forwardMessage(content: Any) {
        ActivitiesManager.redirectToForward(this, content.toString())
    }

    /**
     * Partner's messages can be deleted only for current user
     */
    private fun deletePartnerMessage(user: User, message: Message) {
        chatService.deleteMessageForMe(user, message)
    }

    /**
     * Delete own message
     * If message is already deleted for both then completely delete it
     * Else first delete it for both if possible
     */
    private fun deleteOwnMessage(user: User, message: Message) {
        if (message.deleted == "true") {
            chatService.deleteMessageForMe(user, message)
        } else {
            chatService.deleteMessageForBoth(user, message)
        }
    }

    // Show "seen" details of a message
    private fun showSeen(message: Message) {
        if (message.deleted == "true") {
            toastNotifier.notify(this, "Can't see details of a deleted message", toastNotifier.lengthShort)
            return
        }

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
            if (!firstStart) {
                chatEventsManager.onUserConversation(partner, conversationListener)
            }
            firstStart = false
            userEventsManager.onUserStatus(partner, partnerStatusListener)
            userEventsManager.onUserImage(partner, partnerImageListener)
            userEventsManager.onUserToken(partner, partnerTokenListener)
            chatEventsManager.onUserTyping(partner, partnerTypingListener)
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
            chatEventsManager.offUserTyping(partner, partnerTypingListener)
            chatService.setOffConversation()
            chatService.setOffTyping()
        }
    }

    override fun onPause() {
        super.onPause()
        chatService.setOffTyping()
    }
}
