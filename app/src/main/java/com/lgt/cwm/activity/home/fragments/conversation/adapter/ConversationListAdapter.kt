package com.lgt.cwm.activity.home.fragments.conversation.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.lgt.cwm.R

import com.lgt.cwm.databinding.ConversationListItemBinding
import com.lgt.cwm.db.entity.SignalMsgStatus
import com.lgt.cwm.models.SignalThreadExt
import com.lgt.cwm.ui.avatar.AvatarConstants
import com.lgt.cwm.ui.avatar.AvatarGenerator
import com.lgt.cwm.ui.avatar.RandomColors
import com.lgt.cwm.util.DebugConfig
import com.lgt.cwm.util.ViewUtil
import dagger.hilt.android.scopes.FragmentScoped
import javax.inject.Inject


@FragmentScoped
class ConversationListAdapter @Inject constructor():  ListAdapter<SignalThreadExt, ConversationListAdapter.ConversationListViewHolder>(
    ConversationListCallback()
){
    private lateinit var context: Context

    @Inject
    lateinit var debugConfig: DebugConfig

    private var listener: OnItemClickListener? = null

    private var selectedThreadIds: Set<String> = emptySet()

    private enum class Payload { TYPING_INDICATOR, SELECTION }

    private class ConversationListCallback : DiffUtil.ItemCallback<SignalThreadExt>() {
        override fun areItemsTheSame(oldItem: SignalThreadExt, newItem: SignalThreadExt): Boolean {
            return oldItem.threadId.equals(newItem.threadId)
        }

        override fun areContentsTheSame(oldItem: SignalThreadExt, newItem: SignalThreadExt): Boolean {
            return oldItem.isContentTheSameWith(newItem)
        }
    }

    fun setOnItemClickListener(listener: OnItemClickListener){
        this.listener = listener
    }

    fun setSelectedThreadIds(threadIds: Set<String>) {
        selectedThreadIds = threadIds
        notifyItemRangeChanged(0, itemCount, Payload.SELECTION)
    }

    class ConversationListViewHolder(val binding: ConversationListItemBinding) : RecyclerView.ViewHolder(binding.root){
        fun bind(item: SignalThreadExt) {
            binding.conversation = item
            binding.executePendingBindings()
        }

        fun setSelectedConversations(item: SignalThreadExt, selectedThreadIds: Set<String>) {
            binding.conversationListItemCheckBox.visibility = if (selectedThreadIds.isEmpty()) View.GONE else View.VISIBLE
            binding.conversationListItemCheckBox.isChecked = selectedThreadIds.contains(item.threadId)
        }
    }

    override fun onCreateViewHolder(viewGroup: ViewGroup, viewType: Int): ConversationListViewHolder {
        context = viewGroup.context
        val binding: ConversationListItemBinding = DataBindingUtil.inflate(LayoutInflater.from(context), R.layout.conversation_list_item, viewGroup, false)
        return ConversationListViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ConversationListViewHolder, position: Int) {
        val item = getItem(position)
        val binding = holder.binding

        val size = ViewUtil.dpToPx(48)
        val avatar = AvatarGenerator.AvatarBuilder(context)
            .setLabel(item.threadName)
            .setAvatarSize(size)
            .setTextSize(24)
            .setBackgroundColor(RandomColors(AvatarConstants.COLOR700).getColor(item.threadName))
            .toCircle()
            .build()


        binding.imageViewAvatar.setImageDrawable(avatar)
        if (item.unreadMsgs > 0){
            binding.conversationListItemUnreadIndicator.visibility = View.VISIBLE
        }else{
            binding.conversationListItemUnreadIndicator.visibility = View.GONE
        }

        when (item.getLastMsgStatus()) {
            SignalMsgStatus.SENT_FAIL.code -> binding.conversationListItemStatus.setFail()
            SignalMsgStatus.SENDING.code -> binding.conversationListItemStatus.setPending()
            SignalMsgStatus.SENT.code -> binding.conversationListItemStatus.setSent()
            SignalMsgStatus.SENT_SEEN.code -> binding.conversationListItemStatus.setSeen()
            SignalMsgStatus.SENT_SEEN_ALL.code -> binding.conversationListItemStatus.setSeen()
            else -> binding.conversationListItemStatus.setNone()
        }

        binding.viewConversationListItem.setOnClickListener {
            listener?.onItemActiveClick(item, position)
        }

        binding.viewConversationListItem.setOnLongClickListener {
            listener?.onItemLongClick(item, position)
            true
        }

        holder.setSelectedConversations(item, selectedThreadIds)

        holder.bind(item);
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    override fun getItemViewType(position: Int): Int {
        return position
    }


    override fun onBindViewHolder(holder: ConversationListViewHolder, position: Int, payloads: MutableList<Any>) {
        if (payloads.isEmpty()) {
            onBindViewHolder(holder, position)
        } else {
            for (payloadObject in payloads) {
                if (payloadObject is Payload) {
                    if (payloadObject == Payload.SELECTION) {
                        holder.setSelectedConversations(getItem(position), selectedThreadIds)
                    } else {

                    }
                }
            }
        }
    }

    interface OnItemClickListener {
        fun onItemActiveClick(item: SignalThreadExt, position: Int)
        fun onItemLongClick(item: SignalThreadExt, position: Int)
    }
}