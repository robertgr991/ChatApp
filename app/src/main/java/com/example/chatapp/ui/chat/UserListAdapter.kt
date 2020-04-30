package com.example.chatapp.ui.chat

import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.chatapp.R
import com.example.chatapp.models.User
import com.squareup.picasso.Picasso
import de.hdodenhof.circleimageview.CircleImageView
import kotlinx.android.synthetic.main.activity_new_message_user.view.*

class UserListAdapter(var users : ArrayList<User>, val context: Context) : RecyclerView.Adapter<UserListAdapter.ViewHolder>() {
    override fun getItemCount(): Int {
        return users.size
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(
            LayoutInflater.from(context).inflate(R.layout.activity_new_message_user, parent, false)
        )
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val user = users[position]
        holder.username.text = user.username

        if (user.imageName != null) {
            Picasso.get().load(user.imageName).into(holder.image)

        } else {
            Picasso.get().load(R.drawable.default_avatar).into(holder.image)
        }
    }

    inner class ViewHolder (view: View) : RecyclerView.ViewHolder(view) {
        val username: TextView = view.new_message_user_username
        val image: CircleImageView = view.new_message_user_image
    }
}

