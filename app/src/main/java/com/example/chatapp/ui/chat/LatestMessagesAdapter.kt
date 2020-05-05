package com.example.chatapp.ui.chat

import android.content.Context
import android.graphics.Typeface
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.chatapp.App
import com.example.chatapp.R
import com.example.chatapp.models.User
import com.example.chatapp.models.dto.MessageWithPartnerUserDTO
import com.example.chatapp.utils.Utils
import de.hdodenhof.circleimageview.CircleImageView
import kotlinx.android.synthetic.main.activity_latest_messages_row.view.*
import java.util.*

class LatestMessagesAdapter(var messages : LinkedList<MessageWithPartnerUserDTO>, val context: Context) : RecyclerView.Adapter<LatestMessagesAdapter.ViewHolder>() {
    private var onItemClickListener: ((User) -> Unit)? = null
    private var onItemLongClickListener: ((User) -> Unit)? = null

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
        val message = messageWithUser.message
        val user = messageWithUser.user
        var messageText = if (message.deleted == "false") message.content.toString() else context.getString(R.string.message_deleted_text)

        holder.date.text = Utils.formattedDate(message.date)
        holder.username.text = user.username

        if (messageText.length > 50) {
            messageText = messageText.slice(IntRange(0, 50)) + "..."
        }

        if (message.fromId == App.context.currentUser!!.id) {
            messageText = "You: $messageText"
            holder.row.setBackgroundColor(ContextCompat.getColor(context, R.color.colorPrimary))

            if (message.seen == "true" && message.deleted == "false") {
                holder.seen.visibility = View.VISIBLE
            } else {
                holder.seen.visibility = View.GONE
            }
        } else {
            holder.seen.visibility = View.GONE

            if (message.seen == "false") {
                holder.row.setBackgroundColor(ContextCompat.getColor(context, R.color.colorNewLatestMessageRow))
            } else {
                holder.row.setBackgroundColor(ContextCompat.getColor(context, R.color.colorPrimary))
            }
        }

        holder.content.text = messageText

        if (message.deleted == "true") {
            holder.content.setTypeface(holder.content.typeface, Typeface.ITALIC)
        }

        if (user.imageName != null) {
            Glide.with(context).load(user.imageName).into(holder.image)

        } else {
            Glide.with(context).load(R.drawable.default_avatar).into(holder.image)
        }

        if (onItemClickListener != null) {
            holder.itemView.setOnClickListener {
                onItemClickListener!!(user)
            }
            holder.itemView.setOnLongClickListener {
                onItemLongClickListener!!(user)
                true
            }
        }
    }

    fun setOnItemClickListener(callback: (User) -> Unit) {
        onItemClickListener = callback
    }

    fun setOnItemLongClickListener(callback: (User) -> Unit) {
        onItemLongClickListener = callback
    }

    inner class ViewHolder (view: View) : RecyclerView.ViewHolder(view) {
        val username: TextView = view.latest_messages_row_username
        val content: TextView = view.latest_messages_row_content
        val image: CircleImageView = view.latest_messages_row_image
        val date: TextView = view.latest_messages_row_date
        val seen: ImageView = view.latest_messages_row_seen
        val row: ConstraintLayout = view.latest_messages_row
    }
}