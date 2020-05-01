package com.example.chatapp.ui.chat

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.chatapp.App
import com.example.chatapp.R
import com.example.chatapp.models.Message
import kotlinx.android.synthetic.main.activity_chat_log_message_from.view.*
import kotlinx.android.synthetic.main.activity_chat_log_message_own.view.*
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList

abstract class BaseViewHolder<T>(itemView: View) : RecyclerView.ViewHolder(itemView) {
    abstract fun bind(item: T)
}

class MessagesAdapter(var messages : ArrayList<Message>, val context: Context) : RecyclerView.Adapter<BaseViewHolder<*>>() {
    companion object {
        private const val TYPE_FROM = 0
        private const val TYPE_OWN = 1
    }

    override fun getItemCount(): Int {
        return messages.size
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BaseViewHolder<*> {
        return when (viewType) {
            TYPE_FROM -> {
                val view = LayoutInflater.from(context).inflate(R.layout.activity_chat_log_message_from, parent, false)
                FromMessageViewHolder(view)
            }
            else -> {
                val view = LayoutInflater.from(context).inflate(R.layout.activity_chat_log_message_own, parent, false)
                OwnMessageViewHolder(view)
            }
        }
    }

    override fun onBindViewHolder(holder: BaseViewHolder<*>, position: Int) {
        val message = messages[position]

        when (holder) {
            is FromMessageViewHolder -> holder.bind(message)
            is OwnMessageViewHolder -> holder.bind(message)
            else -> throw IllegalArgumentException()
        }
    }

    override fun getItemViewType(position: Int): Int {
        return if (messages[position].fromId == App.context.currentUser?.id) {
            TYPE_OWN
        } else {
            TYPE_FROM
        }
    }

    inner class FromMessageViewHolder(itemView: View) : BaseViewHolder<Message>(itemView) {
        override fun bind(item: Message) {
            val pattern = "dd/MM/yyyy hh:mm"
            val simpleDateFormat = SimpleDateFormat(pattern, Locale.ROOT)

            itemView.chat_log_message_from_date.text = simpleDateFormat.format(item.date)
            itemView.chat_log_message_from_content.text = item.content.toString()
        }
    }

    inner class OwnMessageViewHolder(itemView: View) : BaseViewHolder<Message>(itemView) {
        override fun bind(item: Message) {
            val pattern = "dd/MM/yyyy hh:mm"
            val simpleDateFormat = SimpleDateFormat(pattern, Locale.ROOT)

            itemView.chat_log_message_own_date.text = simpleDateFormat.format(item.date)
            itemView.chat_log_message_own_content.text = item.content.toString()
        }
    }
}