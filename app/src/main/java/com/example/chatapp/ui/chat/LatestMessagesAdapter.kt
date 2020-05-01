package com.example.chatapp.ui.chat

import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.chatapp.App
import com.example.chatapp.R
import com.example.chatapp.models.User
import com.example.chatapp.models.dto.MessageWithPartnerUserDTO
import com.squareup.picasso.Picasso
import de.hdodenhof.circleimageview.CircleImageView
import kotlinx.android.synthetic.main.activity_latest_messages_row.view.*
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList

class LatestMessagesAdapter(var messages : ArrayList<MessageWithPartnerUserDTO>, val context: Context) : RecyclerView.Adapter<LatestMessagesAdapter.ViewHolder>() {
    private var onItemClickListener: ((User) -> Unit)? = null

    override fun getItemCount(): Int {
        return messages.size
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(
            LayoutInflater.from(context).inflate(R.layout.activity_latest_messages_row, parent, false)
        )
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val messageWithUser = messages[position]
        val pattern = "dd/MM/yyyy hh:mm"
        val message = messageWithUser.message
        val user = messageWithUser.user
        val simpleDateFormat = SimpleDateFormat(pattern, Locale.ROOT)
        var messageText = message.content.toString()

        holder.date.text = simpleDateFormat.format(message.date)
        holder.username.text = user.username

        Log.d("HOLDER", holder.date.text.toString())
        Log.d("HOLDER", messageText)

        if (messageText.length > 50) {
            messageText = messageText.slice(IntRange(0, 50)) + "..."
        }

        if (message.fromId == App.context.currentUser!!.id) {
            messageText = "You: $messageText"
        }

        holder.content.text = messageText
        if (user.imageName != null) {
            Picasso.get().load(user.imageName).into(holder.image)

        } else {
            Picasso.get().load(R.drawable.default_avatar).into(holder.image)
        }

        if (onItemClickListener != null) {
            holder.itemView.setOnClickListener {
                onItemClickListener!!(user)
            }
        }
    }

    fun setOnItemClickListener(callback: (User) -> Unit) {
        onItemClickListener = callback
    }

    inner class ViewHolder (view: View) : RecyclerView.ViewHolder(view) {
        val username: TextView = view.latest_messages_row_username
        val content: TextView = view.latest_messages_row_content
        val image: CircleImageView = view.latest_messages_row_image
        val date: TextView = view.latest_messages_row_date
    }
}