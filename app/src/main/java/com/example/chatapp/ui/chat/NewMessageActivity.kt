package com.example.chatapp.ui.chat

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.chatapp.R
import com.example.chatapp.models.User
import com.example.chatapp.services.UserService
import kotlinx.android.synthetic.main.activity_new_message.*
import org.koin.android.ext.android.inject
import java.util.*
import kotlin.collections.ArrayList

class NewMessageActivity : AppCompatActivity() {
    private val userService: UserService by inject()
    private lateinit var userListAdapter: UserListAdapter
    private var users: ArrayList<User> = ArrayList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_new_message)
        supportActionBar?.title = "Select User"

        // Fetch users
        userService.getAll {fetchedUsers ->
            users = fetchedUsers
            userListAdapter.users = users
            userListAdapter.notifyDataSetChanged()
        }

        // Set the recycler view
        new_message_recycler_view.layoutManager = LinearLayoutManager(this)
        userListAdapter = UserListAdapter(users, this)

        userListAdapter.setOnItemClickListener { user ->
            val intent = Intent(this, ChatLogActivity::class.java)
            intent.putExtra("user", user)
            startActivity(intent)
            finish()
        }

        new_message_recycler_view.adapter = userListAdapter

        new_message_search_text.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(view: Editable?) {}

            override fun beforeTextChanged(text: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun onTextChanged(text: CharSequence?, start: Int, before: Int, count: Int) {
                if (users.size == 0) {
                    return
                }

                if (text == null) {
                    userListAdapter.users = users
                    return
                }

                val lowerText = text.toString().toLowerCase(Locale.ROOT)

                userListAdapter.users = users.filter { user ->
                    user.username
                        .toLowerCase(Locale.ROOT)
                        .startsWith(lowerText)
                } as ArrayList<User>
                userListAdapter.notifyDataSetChanged()
            }
        })
    }
}
