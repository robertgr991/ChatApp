package com.example.chatapp.ui.chat

import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.text.HtmlCompat
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.chatapp.App
import com.example.chatapp.R
import com.example.chatapp.events.firebase.ChatEventsManager
import com.example.chatapp.models.Message
import com.example.chatapp.models.User
import com.example.chatapp.models.dto.MessageWithPartnerUserDTO
import com.example.chatapp.services.ChatService
import com.example.chatapp.services.UserService
import com.example.chatapp.ui.ActivitiesManager
import com.example.chatapp.ui.notifiers.ToastNotifier
import com.example.chatapp.ui.utils.AlertDialogBuilder
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.database.ChildEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import kotlinx.android.synthetic.main.activity_latest_messages.*
import org.koin.android.ext.android.inject
import java.util.*
import kotlin.collections.HashMap
import kotlin.concurrent.thread

class LatestMessagesActivity : AppCompatActivity() {
    private val toastNotifier: ToastNotifier by inject()
    private val chatService: ChatService by inject()
    private val userService: UserService by inject()
    private val chatEventsManager: ChatEventsManager by inject()
    private val latestMessagesMap: HashMap<String, Message> = HashMap()
    private var latestMessagesAdapter: LatestMessagesAdapter = LatestMessagesAdapter(LinkedList(), this)
    // Listen for new messages in this conversation
    private val latestMessagesListener: ChildEventListener = object: ChildEventListener {
        override fun onCancelled(p0: DatabaseError) {}

        override fun onChildMoved(p0: DataSnapshot, p1: String?) {}

        /**
         * Latest message deleted
         */
        override fun onChildRemoved(snapshot: DataSnapshot) {
            val message = snapshot.getValue(Message::class.java) ?: return

            if (latestMessagesMap[snapshot.key!!] != null) {
                thread(start = true) {
                    latestMessagesMap[snapshot.key!!] = message
                    val newMessages = LinkedList<MessageWithPartnerUserDTO>()
                    var removePosition = 0
                    var index = 0

                    // Find removed latest message position to delete it from
                    // adapter and notify of it's position
                    latestMessagesAdapter.messages.forEach {
                        if (it.message.id != message.id) {
                            newMessages.add(it)
                        } else {
                            removePosition = index
                        }

                        index++
                    }

                    val oldSize = latestMessagesAdapter.itemCount
                    latestMessagesAdapter.messages = newMessages
                    runOnUiThread {
                        latestMessagesAdapter.notifyItemRangeChanged(removePosition, oldSize)
                    }
                }
            }
        }

        /**
         * Existing latest message changed
         */
        override fun onChildChanged(snapshot: DataSnapshot, p1: String?) {
            val message = snapshot.getValue(Message::class.java) ?: return

            if (latestMessagesMap[snapshot.key!!] != null) {
                 thread(start = true) {
                     var detailsChangePosition = 0
                     var detailsChange = false
                     val oldMessage = latestMessagesMap[snapshot.key!!]
                     latestMessagesMap[snapshot.key!!] = message
                     val newMessages = LinkedList<MessageWithPartnerUserDTO>()
                     var partner = User("", "", "")
                     var lastPosition = 0
                     var index = 0

                     // Iterate and find which message changed
                    latestMessagesAdapter.messages.forEach {
                        if (it.user.id != message.fromId && it.user.id != message.toId) {
                            newMessages.add(it)
                        } else {
                            partner = it.user

                            if (message.date == oldMessage?.date) {
                                // There were changes on details(seen/message was deleted)
                                detailsChange = true
                                detailsChangePosition = index
                                newMessages.add(MessageWithPartnerUserDTO(message, partner))
                            } else {
                                lastPosition = index
                            }
                        }

                        index++
                    }

                    if (partner.id == "") {
                        return@thread
                    }

                    if (detailsChange) {
                        // Don't put the message first, just show the changes
                        latestMessagesAdapter.messages = newMessages
                        runOnUiThread {
                            latestMessagesAdapter.notifyItemChanged(detailsChangePosition)
                        }
                    } else {
                        // Put message at the top
                        newMessages.push(MessageWithPartnerUserDTO(message, partner))
                        latestMessagesAdapter.messages = newMessages
                        runOnUiThread {
                            latestMessagesAdapter.notifyItemRangeChanged(0, lastPosition + 1)
                        }
                    }
                 }
            }
        }

        /**
         * New latest message added
         */
        override fun onChildAdded(snapshot: DataSnapshot, p1: String?) {
            val message = snapshot.getValue(Message::class.java) ?: return
            latestMessagesMap[snapshot.key!!] = message
            val partnerId: String

            // Find the partner id
            partnerId = if (message.fromId == App.context.currentUser!!.id) {
                message.toId
            } else {
                message.fromId
            }

            // Retrieve the user from db
            userService.findById(partnerId) {
                if (it != null) {
                    thread(start = true) {
                        // Put the message at the top
                        latestMessagesAdapter.messages.push(MessageWithPartnerUserDTO(message, it))
                        latestMessagesAdapter.messages.sortByDescending { messageWithPartner ->
                            messageWithPartner.message.date
                        }

                        runOnUiThread {
                            latestMessagesAdapter.notifyDataSetChanged()
                        }
                    }
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_latest_messages)

        // Save the device token in database
        if (!App.context.hasSetToken) {
            userService.getCurrentToken {
                if (it != null) {
                    App.context.hasSetToken = true
                    userService.setDeviceToken(it)
                }
            }
        }

        // Set italic title on action bar
        if (supportActionBar != null) {
            supportActionBar?.title = HtmlCompat.fromHtml("<i>${supportActionBar?.title}</i>", HtmlCompat.FROM_HTML_MODE_COMPACT)
        }

        // Set the recycler view
        val layoutManager = LinearLayoutManager(this)
        latest_messages_recyclerview.layoutManager = layoutManager
        // Go to conversation with the user
        latestMessagesAdapter.setOnItemClickListener { partner ->
            ActivitiesManager.redirectToChatWithUser(this, partner)
        }
        // Ask if user wants to remove messages history with this user
        latestMessagesAdapter.setOnItemLongClickListener { partner ->
            onLatestMessageLongClickListener(partner)
        }
        latest_messages_recyclerview.adapter = latestMessagesAdapter
        latest_messages_recyclerview.addItemDecoration(DividerItemDecoration(this, DividerItemDecoration.VERTICAL))
        setBottomNavigation()
    }

    private fun onLatestMessageLongClickListener(partner: User) {
        // Alert dialog for deleting messages with a user
        val alert = AlertDialogBuilder.positiveNegativeDialog(
            this,
            getString(R.string.latest_messages_confirm_delete_title),
            getString(R.string.latest_messages_confirm_delete_text),
            "YES",
            DialogInterface.OnClickListener { dialog, _ ->
                chatService.removeMessagesWithUser(partner) {
                    dialog.dismiss()

                    if (!it) {
                        toastNotifier.notify(this, "There was an error", toastNotifier.lengthLong)
                    }
                }
            },
            "NO",
            DialogInterface.OnClickListener { dialog, _ ->
                // Just dismiss the dialog
                dialog.dismiss()
            }
        )
        alert.show()
    }

    private fun setBottomNavigation() {
        val bottomNavigation = findViewById<BottomNavigationView>(R.id.bottom_navigation)

        // Item selector
        bottomNavigation.setOnNavigationItemSelectedListener {
            when(it.itemId){
                R.id.bottom_navigation_compose -> {
                    ActivitiesManager.redirectToNewMessage(this)
                    return@setOnNavigationItemSelectedListener true
                }

                R.id.bottom_navigation_profile -> {
                    ActivitiesManager.redirectToProfile(this, App.context.currentUser!!)
                    return@setOnNavigationItemSelectedListener true
                }

                R.id.bottom_navigation_signOut -> {
                    userService.signOut {
                        ActivitiesManager.redirectToLogin(this)
                    }
                    return@setOnNavigationItemSelectedListener true
                }
            }
            false
        }
    }

    override fun onStart() {
        super.onStart()
        // Listen for latest messages
        chatEventsManager.onLatestMessages(latestMessagesListener)
    }

    override fun onStop() {
        super.onStop()
        // Clear adapter data
        latestMessagesAdapter.messages = LinkedList()
        latestMessagesAdapter.notifyDataSetChanged()
        chatEventsManager.offLatestMessages(latestMessagesListener)
    }
}
