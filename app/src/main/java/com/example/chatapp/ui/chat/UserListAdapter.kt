package com.example.chatapp.ui.chat

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.chatapp.R
import com.example.chatapp.models.User
import de.hdodenhof.circleimageview.CircleImageView
import kotlinx.android.synthetic.main.activity_new_message_user.view.*

class UserListAdapter(var users : ArrayList<User>, val context: Context) : RecyclerView.Adapter<UserListAdapter.ViewHolder>() {
    private var onItemClickListener: ((User) -> Unit)? = null
    private var onBindViewHolderExtra: ((ViewHolder, User) -> Unit)? = null

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
            Glide.with(context).load(user.imageName).into(holder.image)

        } else {
            Glide.with(context).load(R.drawable.default_avatar).into(holder.image)
        }

        if (onBindViewHolderExtra != null) {
            onBindViewHolderExtra!!(holder, user)
        }

        if (onItemClickListener != null) {
            holder.itemView.setOnClickListener {
                onItemClickListener!!(user)
            }
        }
    }

    fun setOnBindViewHolderExtra(callback: (ViewHolder, User) -> Unit) {
        onBindViewHolderExtra = callback
    }

    fun setOnItemClickListener(callback: (User) -> Unit) {
        onItemClickListener = callback
    }

    inner class ViewHolder (view: View) : RecyclerView.ViewHolder(view) {
        val username: TextView = view.new_message_user_username
        val image: CircleImageView = view.new_message_user_image
    }
}

