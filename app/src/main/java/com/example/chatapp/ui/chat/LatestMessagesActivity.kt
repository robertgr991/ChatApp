package com.example.chatapp.ui.chat

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.chatapp.App
import com.example.chatapp.R
import com.example.chatapp.events.firebase.ChatEventsManager
import com.example.chatapp.models.Message
import com.example.chatapp.models.User
import com.example.chatapp.models.dto.MessageWithPartnerUserDTO
import com.example.chatapp.services.UserService
import com.example.chatapp.ui.ActivitiesManager
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.database.ChildEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import kotlinx.android.synthetic.main.activity_latest_messages.*
import org.koin.android.ext.android.inject
import kotlin.collections.ArrayList
import kotlin.collections.HashMap

class LatestMessagesActivity : AppCompatActivity() {
    private val userService: UserService by inject()
    private val chatEventsManager: ChatEventsManager by inject()
    private val latestMessagesMap: HashMap<String, Message> = HashMap()
    private lateinit var latestMessagesAdapter: LatestMessagesAdapter
    // Listen for new messages in this conversation
    private val latestMessagesListener: ChildEventListener = object: ChildEventListener {
        override fun onCancelled(p0: DatabaseError) {}

        override fun onChildMoved(p0: DataSnapshot, p1: String?) {}

        override fun onChildRemoved(snapshot: DataSnapshot) {
            val message = snapshot.getValue(Message::class.java) ?: return

            if (latestMessagesMap[snapshot.key!!] != null) {
                latestMessagesMap[snapshot.key!!] = message
                val newMessages = ArrayList<MessageWithPartnerUserDTO>()

                latestMessagesAdapter.messages.forEach {
                    if (it.message.id != message.id) {
                        newMessages.add(it)
                    }
                }
                latestMessagesAdapter.messages = newMessages
                latestMessagesAdapter.notifyDataSetChanged()
            }
        }

        override fun onChildChanged(snapshot: DataSnapshot, p1: String?) {
            val message = snapshot.getValue(Message::class.java) ?: return
            Log.d("LATEST SNAPSHOT", snapshot.key)

            if (latestMessagesMap[snapshot.key!!] != null) {
                latestMessagesMap[snapshot.key!!] = message
                val newMessages = ArrayList<MessageWithPartnerUserDTO>()
                var partner = User("", "", "")

                latestMessagesAdapter.messages.forEach {
                    if (it.message.id != message.id) {
                        newMessages.add(it)
                    } else {
                        partner = it.user
                    }
                }

                if (partner.id == "") {
                    return
                }

                newMessages.add(MessageWithPartnerUserDTO(message, partner))
                latestMessagesAdapter.messages = newMessages
                latestMessagesAdapter.notifyDataSetChanged()
            }
        }

        override fun onChildAdded(snapshot: DataSnapshot, p1: String?) {
            val message = snapshot.getValue(Message::class.java) ?: return
            Log.d("LATEST SNAPSHOT", snapshot.key)
            Log.d("LATEST MESSAGE", message.toString())
            latestMessagesMap[snapshot.key!!] = message
            val partnerId: String

            partnerId = if (message.fromId == App.context.currentUser!!.id) {
                message.toId
            } else {
                message.fromId
            }

            userService.findById(partnerId) {
                Log.d("FIND BY ID", it.toString())
                if (it != null) {
                    latestMessagesAdapter.messages.add(MessageWithPartnerUserDTO(message, it))
                    latestMessagesAdapter.notifyDataSetChanged()
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_latest_messages)
        Log.d("LATEST CREATED", "LATEST CREATED")
        // Set the recycler view
        val layoutManager = LinearLayoutManager(this)
        latest_messages_recyclerview.layoutManager = layoutManager
        latestMessagesAdapter = LatestMessagesAdapter(ArrayList(), this)
        latestMessagesAdapter.setOnItemClickListener {
            val intent = Intent(this, ChatLogActivity::class.java)
            intent.putExtra("user", it)
            startActivity(intent)
        }
        latest_messages_recyclerview.adapter = latestMessagesAdapter
        latest_messages_recyclerview.addItemDecoration(DividerItemDecoration(this, DividerItemDecoration.VERTICAL))

        // Set bottomNavigation
        val bottomNavigation = findViewById<BottomNavigationView>(R.id.bottom_navigation)

        // Set items
        bottomNavigation.setSelectedItemId(R.id.dashboard);

        // Item selector
        bottomNavigation.setOnNavigationItemSelectedListener {
            when(it.itemId){
                R.id.dashboard-> {
                    ActivitiesManager.redirectToHomepage(this)
                    return@setOnNavigationItemSelectedListener true
                }

                R.id.profile_edit-> {
                    // To add edit profile
                    return@setOnNavigationItemSelectedListener true
                }

                R.id.signout-> {
                    userService.signOut()
                    ActivitiesManager.redirectToLogin(this)
                    return@setOnNavigationItemSelectedListener true
                }

            }
            false
        }

    }

    override fun onStart() {
        super.onStart()
        Log.d("LATEST STARTED", "LATEST STARTED")
        // Listen for latest messages
        chatEventsManager.onLatestMessages(latestMessagesListener)
    }

    override fun onStop() {
        super.onStop()
        Log.d("LATEST", "ON STOP")
        latestMessagesAdapter.messages = ArrayList()
        chatEventsManager.offLatestMessages(latestMessagesListener)
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        when (item?.itemId) {
            R.id.menu_new_message -> {
                ActivitiesManager.redirectToNewMessage(this)
            }
            R.id.menu_sign_out -> {
                userService.signOut()
                ActivitiesManager.redirectToLogin(this)
            }
        }

        return super.onOptionsItemSelected(item)
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.nav_menu, menu)
        return super.onCreateOptionsMenu(menu)
    }
}
