package com.example.chatapp.ui.chat

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.chatapp.App
import com.example.chatapp.R
import com.example.chatapp.models.User
import com.example.chatapp.services.ChatService
import com.example.chatapp.services.UserService
import com.example.chatapp.ui.notifiers.ToastNotifier
import com.example.chatapp.ui.utils.ProgressDialog
import kotlinx.android.synthetic.main.activity_chat_log.*
import kotlinx.android.synthetic.main.activity_forward_message.*
import org.koin.android.ext.android.inject
import java.util.*
import kotlin.collections.ArrayList
import kotlin.concurrent.schedule
import kotlin.concurrent.thread

class ForwardMessageActivity : AppCompatActivity() {
    private val userService: UserService by inject()
    private val chatService: ChatService by inject()
    private val toastNotifier: ToastNotifier by inject()
    private var users: ArrayList<User> = ArrayList()
    private var usersToForward: HashMap<String, User> = HashMap()
    private lateinit var progressDialog: ProgressDialog
    private lateinit var userListAdapter: UserListAdapter
    private var message: String? = null

    companion object {
        private const val SEARCH_KEY = "SEARCH_KEY"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_forward_message)

        // Hide the original action bar because a custom bar is used
        supportActionBar?.hide()

        // Set progress dialog
        progressDialog = ProgressDialog(this)

        // Retrieve searched username saved before orientation change
        var filterUsername = ""

        if (savedInstanceState != null) {
            val searchedUsername = savedInstanceState.getString(SEARCH_KEY)

            if (searchedUsername != null) {
                forward_message_search_txt.setText(searchedUsername)
                filterUsername = searchedUsername
            }
        }

        if (intent.hasExtra("message")) {
            message = intent.getStringExtra("message")
        }

        // Filtering users by username
        forward_message_search_txt.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(view: Editable?) {}

            override fun beforeTextChanged(text: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun onTextChanged(text: CharSequence?, start: Int, before: Int, count: Int) {
                thread(start = true) {
                    if (users.size == 0) {
                        return@thread
                    }

                    if (text == null || text == "") {
                        userListAdapter.users = users
                        runOnUiThread {
                            userListAdapter.notifyDataSetChanged()
                        }
                        return@thread
                    }

                    val lowerText = text.toString().toLowerCase(Locale.ROOT)
                    userListAdapter.users = filterUsersByUsername(lowerText, users)

                    runOnUiThread {
                        userListAdapter.notifyDataSetChanged()
                    }
                }
            }
        })

        fetchUsers(filterUsername)
        // Set the recycler view and adapter
        forward_message_recycler_view.layoutManager = LinearLayoutManager(this)
        userListAdapter = UserListAdapter(users, this)
        userListAdapter.setOnBindViewHolderExtra { viewHolder, user ->
            if (usersToForward.contains(user.id)) {
                viewHolder.itemView.setBackgroundColor(ContextCompat.getColor(this, R.color.colorNewLatestMessageRow))
            } else {
                viewHolder.itemView.setBackgroundColor(ContextCompat.getColor(this, R.color.colorPrimary))
            }
        }
        userListAdapter.setOnItemClickListener { user ->
            if (usersToForward.containsKey(user.id)) {
                usersToForward.remove(user.id)
            } else {
                usersToForward[user.id] = user
            }

            userListAdapter.notifyDataSetChanged()
        }
        forward_message_recycler_view.adapter = userListAdapter

        forward_message_btn.setOnClickListener {
            if (message == null) {
                onBackPressed()
                return@setOnClickListener
            }

            forward_message_btn.isEnabled = false
            forward_message_btn.background = this.getDrawable(R.drawable.rounded_btn_disabled)

            if (usersToForward.size == 0) {
                toastNotifier.notify(this, "Select a user to forward the message", toastNotifier.lengthLong)
                forward_message_btn.isEnabled = false
                forward_message_btn.background = this.getDrawable(R.drawable.rounded_btn_accent)
            } else if (usersToForward.size > 0) {
                thread(start = true) {
                    usersToForward.forEach { userEntry ->
                        chatService.sendMessage(message!!, userEntry.value)
                    }

                    runOnUiThread {
                        toastNotifier.notify(this, "Message forwarded", toastNotifier.lengthShort)
                    }

                    Timer().schedule(2000) {
                        runOnUiThread {
                            onBackPressed()
                        }
                    }
                }
            }
        }

        forward_message_cancel.setOnClickListener {
            onBackPressed()
        }
    }

    private fun filterUsersByUsername(searchedUsername: String, users: ArrayList<User>): ArrayList<User> {
        return users.filter { user ->
            user.username
                .toLowerCase(Locale.ROOT)
                .startsWith(searchedUsername)
        } as ArrayList<User>
    }

    private fun fetchUsers(searchedUsername: String = "") {
        progressDialog.show()
        userService.getAll {fetchedUsers ->
            progressDialog.cancel()
            users = fetchedUsers
            userListAdapter.users = users

            thread(start = true) {
                if (searchedUsername != "") {
                    userListAdapter.users = filterUsersByUsername(searchedUsername, users)
                }

                runOnUiThread {
                    userListAdapter.notifyDataSetChanged()
                }
            }
        }
    }
}