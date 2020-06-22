package com.example.chatapp.ui.chat

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.chatapp.R
import com.example.chatapp.models.User
import com.example.chatapp.services.UserService
import com.example.chatapp.ui.ActivitiesManager
import com.example.chatapp.ui.utils.ProgressDialog
import kotlinx.android.synthetic.main.activity_new_message.*
import org.koin.android.ext.android.inject
import java.util.*
import kotlin.collections.ArrayList
import kotlin.concurrent.thread

/**
 * Activity for starting/resuming a conversation with a user
 */
class NewMessageActivity : AppCompatActivity() {
    private val userService: UserService by inject()
    private lateinit var progressDialog: ProgressDialog
    private lateinit var userListAdapter: UserListAdapter
    private var users: ArrayList<User> = ArrayList()

    companion object {
        private const val SEARCH_KEY = "SEARCH_KEY"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_new_message)

        // Set progress dialog
        progressDialog = ProgressDialog(this)

        supportActionBar?.title = "Select User"
        var filterUsername = ""

        // Retrieve searched username saved before orientation change
        if (savedInstanceState != null) {
            val searchedUsername = savedInstanceState.getString(SEARCH_KEY)

            if (searchedUsername != null) {
                new_message_search_text.setText(searchedUsername)
                filterUsername = searchedUsername
            }
        }

        fetchUsers(filterUsername)
        // Set the recycler view and adapter
        new_message_recycler_view.layoutManager = LinearLayoutManager(this)
        userListAdapter = UserListAdapter(users, this)
        userListAdapter.setOnItemClickListener { user ->
            ActivitiesManager.redirectToChatWithUser(this, user)
            finish()
        }
        new_message_recycler_view.adapter = userListAdapter

        // Filtering users by username
        new_message_search_text.addTextChangedListener(object : TextWatcher {
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

                    userListAdapter.users = users.filter { user ->
                        user.username
                            .toLowerCase(Locale.ROOT)
                            .startsWith(lowerText)
                    } as ArrayList<User>
                    runOnUiThread {
                        userListAdapter.notifyDataSetChanged()
                    }
                }
            }
        })
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        // Save searched username to restore filtered users after orientation change
        outState.putString(SEARCH_KEY, new_message_search_text.text.toString())
    }

    private fun fetchUsers(searchedUsername: String = "") {
        progressDialog.show()
        userService.getAll {fetchedUsers ->
            progressDialog.cancel()
            users = fetchedUsers
            userListAdapter.users = users

            if (searchedUsername != "") {
                userListAdapter.users = users.filter { user ->
                    user.username
                        .toLowerCase(Locale.ROOT)
                        .startsWith(searchedUsername)
                } as ArrayList<User>
            }

            userListAdapter.notifyDataSetChanged()
        }
    }

    override fun onBackPressed() {
        super.onBackPressed()
        ActivitiesManager.redirectToHomepage(this)
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        when (item?.itemId) {
            R.id.menu_refresh_users -> {
                new_message_search_text.setText("")
                // Fetch users again for changes
                fetchUsers()
            }
        }

        return super.onOptionsItemSelected(item)
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.new_message_menu, menu)
        return super.onCreateOptionsMenu(menu)
    }
}
