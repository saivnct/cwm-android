package com.lgt.cwm.activity.conversation.fragments.adapter

import android.content.Context
import android.graphics.Color
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.annotation.ColorInt
import androidx.annotation.DrawableRes
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.databinding.ViewDataBinding
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.lgt.cwm.R
import com.lgt.cwm.databinding.*
import com.lgt.cwm.db.entity.SignalMsgDirection
import com.lgt.cwm.db.entity.SignalMsgStatus
import com.lgt.cwm.models.SignalMsgExt
import com.lgt.cwm.models.ThreadParticipantInfo
import com.lgt.cwm.ui.conversation.ConversationItem
import com.lgt.cwm.ui.glide.GlideApp
import com.lgt.cwm.ui.glide.GlideRequests
import com.lgt.cwm.util.*
import cwmSignalMsgPb.CwmSignalMsg
import dagger.hilt.android.scopes.FragmentScoped
import kotlinx.android.synthetic.main.conversation_item_footer_outgoing.view.*
import java.util.*
import javax.inject.Inject

@FragmentScoped
class ConversationAdapter @Inject constructor():
    ListAdapter<SignalMsgExt, RecyclerView.ViewHolder>(MessageCallback()),
    StickyHeaderDecoration.StickyHeaderAdapter<ConversationAdapter.StickyHeaderViewHolder> {
    private val TAG = ConversationAdapter::class.simpleName.toString()

    companion object {
        val HEADER_TYPE_POPOVER_DATE = 1
        val HEADER_TYPE_INLINE_DATE = 2
        private val HEADER_TYPE_LAST_SEEN = 3

        private val MESSAGE_TYPE_OUTGOING_MULTIMEDIA = 0
        private val MESSAGE_TYPE_OUTGOING_TEXT = 1
        private val MESSAGE_TYPE_INCOMING_MULTIMEDIA = 2
        private val MESSAGE_TYPE_INCOMING_TEXT = 3
        private val MESSAGE_TYPE_NOTIFICATION = 4
        private val MESSAGE_TYPE_HEADER = 5
        private val MESSAGE_TYPE_FOOTER = 6
        private val MESSAGE_TYPE_PLACEHOLDER = 7

        private val PAYLOAD_TIMESTAMP = 0
        private val PAYLOAD_NAME_COLORS = 1
        private val PAYLOAD_SELECTED = 2
    }

    private lateinit var context: Context

    private lateinit var glideRequests: GlideRequests

    @Inject
    lateinit var debugConfig: DebugConfig


    private val calendar = Calendar.getInstance()

    private val locale = Locale.getDefault()

    private val isTypingViewEnabled = false

    private var listener: OnItemClickListener? = null
    private var eventListener: ConversationItem.ItemEventListener? = null

    private var selectedMessages: Set<String> = emptySet()

    private var recordToPulse: SignalMsgExt? = null

    private class MessageCallback : DiffUtil.ItemCallback<SignalMsgExt>() {
        override fun areItemsTheSame(oldItem: SignalMsgExt, newItem: SignalMsgExt) = (oldItem.msgId.equals(newItem.msgId))

        override fun areContentsTheSame(oldItem: SignalMsgExt, newItem: SignalMsgExt) = oldItem.isContentTheSameWith(newItem)
    }

    fun setOnItemClickListener(listener: OnItemClickListener){
        this.listener = listener
    }

    fun setItemEventListener(listener: ConversationItem.ItemEventListener){
        this.eventListener = listener
    }

    override fun onCreateViewHolder(viewGroup: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        context = viewGroup.context
        glideRequests = GlideApp.with(context)

        return when (viewType) {
            MESSAGE_TYPE_INCOMING_TEXT -> {
                val binding: ConversationItemReceivedTextOnlyBinding = DataBindingUtil.inflate(LayoutInflater.from(context), R.layout.conversation_item_received_text_only, viewGroup, false)
                ConversationViewHolder(binding)
            }
            MESSAGE_TYPE_INCOMING_MULTIMEDIA -> {
                val binding: ConversationItemReceivedMultimediaBinding = DataBindingUtil.inflate(LayoutInflater.from(context), R.layout.conversation_item_received_multimedia, viewGroup, false)
                ConversationViewHolder(binding)
            }
            MESSAGE_TYPE_OUTGOING_TEXT -> {
                val binding: ConversationItemSentTextOnlyBinding = DataBindingUtil.inflate(LayoutInflater.from(context), R.layout.conversation_item_sent_text_only, viewGroup, false)
                ConversationViewHolder(binding)
            }
            MESSAGE_TYPE_OUTGOING_MULTIMEDIA -> {
                val binding: ConversationItemSentMultimediaBinding = DataBindingUtil.inflate(LayoutInflater.from(context), R.layout.conversation_item_sent_multimedia, viewGroup, false)
                ConversationViewHolder(binding)
            }
            MESSAGE_TYPE_NOTIFICATION -> {
                val binding: ConversationItemUpdateBinding = DataBindingUtil.inflate(LayoutInflater.from(context), R.layout.conversation_item_update, viewGroup, false)
                ConversationViewHolder(binding)
            }

            else -> throw ClassCastException("Unknown viewType ${viewType}")
        }
    }


    override fun getItemViewType(position: Int): Int {
        val message = getItem(position)

        when (message.direction) {
            SignalMsgDirection.INCOMING.code-> {
                when (message.imType) {
                    CwmSignalMsg.SIGNAL_IM_TYPE.IM.number -> {
                        return MESSAGE_TYPE_INCOMING_TEXT
                    }
                    CwmSignalMsg.SIGNAL_IM_TYPE.MULTIMEDIA.number -> {
                        return MESSAGE_TYPE_INCOMING_MULTIMEDIA
                    }
                    CwmSignalMsg.SIGNAL_IM_TYPE.GROUP_THREAD_NOTIFICATION.number -> {
                        return MESSAGE_TYPE_NOTIFICATION
                    }
                    CwmSignalMsg.SIGNAL_IM_TYPE.URL.number -> {
                        return MESSAGE_TYPE_INCOMING_TEXT
                    }
                    CwmSignalMsg.SIGNAL_IM_TYPE.FORWARD.number -> {

                        message.contentSignalForwardMsg?.let { contentSignalForwardMsg ->
                            val originMsgExt = SignalMsgExt(
                                contentSignalForwardMsg = contentSignalForwardMsg,
                                msgDate = message.msgDate,
                                serverDate = message.serverDate)

                            when (originMsgExt.imType) {
                                CwmSignalMsg.SIGNAL_IM_TYPE.IM.number -> {
                                    return MESSAGE_TYPE_INCOMING_TEXT
                                }
                                CwmSignalMsg.SIGNAL_IM_TYPE.MULTIMEDIA.number -> {
                                    return MESSAGE_TYPE_INCOMING_MULTIMEDIA
                                }
                                CwmSignalMsg.SIGNAL_IM_TYPE.URL.number -> {
                                    return MESSAGE_TYPE_INCOMING_TEXT
                                }
                                else -> {
                                    return MESSAGE_TYPE_INCOMING_MULTIMEDIA
                                }
                            }
                        }

                    }
                    else -> {
                        return MESSAGE_TYPE_INCOMING_MULTIMEDIA
                    }
                }
            }
            SignalMsgDirection.OUTGOING.code, SignalMsgDirection.OUTGOING_DIFF_SESSION.code -> {
                when (message.imType) {
                    CwmSignalMsg.SIGNAL_IM_TYPE.IM.number -> {
                        return MESSAGE_TYPE_OUTGOING_TEXT
                    }
                    CwmSignalMsg.SIGNAL_IM_TYPE.MULTIMEDIA.number -> {
                        return MESSAGE_TYPE_OUTGOING_MULTIMEDIA
                    }
                    CwmSignalMsg.SIGNAL_IM_TYPE.GROUP_THREAD_NOTIFICATION.number -> {
                        return MESSAGE_TYPE_NOTIFICATION
                    }
                    CwmSignalMsg.SIGNAL_IM_TYPE.URL.number -> {
                        return MESSAGE_TYPE_OUTGOING_TEXT
                    }
                    CwmSignalMsg.SIGNAL_IM_TYPE.FORWARD.number -> {
                        message.contentSignalForwardMsg?.let { contentSignalForwardMsg ->
                            val originMsgExt = SignalMsgExt(
                                contentSignalForwardMsg = contentSignalForwardMsg,
                                msgDate = message.msgDate,
                                serverDate = message.serverDate)

                            when (originMsgExt.imType) {
                                CwmSignalMsg.SIGNAL_IM_TYPE.IM.number -> {
                                    return MESSAGE_TYPE_OUTGOING_TEXT
                                }
                                CwmSignalMsg.SIGNAL_IM_TYPE.MULTIMEDIA.number -> {
                                    return MESSAGE_TYPE_OUTGOING_MULTIMEDIA
                                }
                                CwmSignalMsg.SIGNAL_IM_TYPE.URL.number -> {
                                    return MESSAGE_TYPE_OUTGOING_TEXT
                                }
                                else -> {
                                    return MESSAGE_TYPE_OUTGOING_MULTIMEDIA
                                }
                            }
                        }

                    }
                    else -> {
                        return MESSAGE_TYPE_OUTGOING_MULTIMEDIA
                    }
                }
            }
        }

        return MESSAGE_TYPE_INCOMING_TEXT

    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val message: SignalMsgExt = getItem(position)

        holder.itemView.setOnClickListener {
            listener?.onItemClick(message, position)
        }
        holder.itemView.setOnLongClickListener {
            listener?.onItemLongClick(message, position, it)

            true
        }

        when (holder) {
            is ConversationViewHolder -> {
                if (selectedMessages.isNotEmpty()) {
                    val isSelected = selectedMessages.contains(message.msgId)
                    if (isSelected) {
                        holder.itemView.setBackgroundColor(ContextCompat.getColor(context, R.color.core_ultramarine_33))
                    } else {
                        holder.itemView.setBackgroundColor(Color.TRANSPARENT)
                    }
                } else {
                    holder.itemView.setBackgroundColor(Color.TRANSPARENT)
                }


                val nextMessage: SignalMsgExt? = if (position > 0) getItem(position - 1) else null
                val previousMessage: SignalMsgExt? = if (position < itemCount - 1) getItem(position + 1) else null

                when (val binding = holder.binding) {
                    is ConversationItemSentTextOnlyBinding -> {
                        if (message.imType == CwmSignalMsg.SIGNAL_IM_TYPE.URL.number) {
                            binding.conversationItemBody.text = message.dataIMSignalURLMessage ?: ""
                        } else {
                            binding.conversationItemBody.text = message.contentIMMessage ?: ""
                        }
                        binding.conversationItemFooter.footer_date.text = DateUtil.convertLongToTimeHourChat(message.msgDate)

                        binding.conversationItem.setItemEventListener(eventListener)
                        binding.conversationItem.bind(message,
                            previousMessage,
                            nextMessage,
                            glideRequests)

                        if (recordToPulse == message) {
                            binding.conversationItem.startPulseOutlinerAnimation()
                            recordToPulse = null
                        }

                        when (message.status) {
                            SignalMsgStatus.SENDING.code ->{
                                binding.conversationItemFooter.footer_delivery_status.setPending()
                            }
                            SignalMsgStatus.SENT.code ->{
                                binding.conversationItemFooter.footer_delivery_status.setSent()
//                                binding.conversationItemFooter.footer_delivery_status.setDelivered()
                            }
                            SignalMsgStatus.SENT_SEEN.code, SignalMsgStatus.SENT_SEEN_ALL.code ->{
                                binding.conversationItemFooter.footer_delivery_status.setSeen()
                            }
                            SignalMsgStatus.SENT_FAIL.code ->{
                                binding.conversationItemFooter.footer_delivery_status.setFail()
                            }

                        }
                    }
                    is ConversationItemReceivedTextOnlyBinding -> {
                        if (message.imType == CwmSignalMsg.SIGNAL_IM_TYPE.URL.number) {
                            binding.conversationItemBody.text = message.dataIMSignalURLMessage ?: ""
                        } else {
                            binding.conversationItemBody.text = message.contentIMMessage ?: ""
                        }

                        binding.conversationItemFooter.footer_date.text = DateUtil.convertLongToTimeHourChat(message.msgDate)

                        binding.conversationItem.setItemEventListener(eventListener)
                        binding.conversationItem.bind(message,
                            previousMessage,
                            nextMessage,
                            glideRequests)

                        if (recordToPulse == message) {
                            binding.conversationItem.startPulseOutlinerAnimation()
                            recordToPulse = null
                        }

                        binding.bodyBubble.background.colorFilter =
                            PorterDuffColorFilter(
                                ContextCompat.getColor(context, R.color.conversation_item_background_secondary),
                                PorterDuff.Mode.SRC_IN
                            )
                        binding.conversationItemFooter.footer_delivery_status.setNone()
                    }
                    is ConversationItemSentMultimediaBinding -> {

                        binding.conversationItem.setItemEventListener(eventListener)
                        binding.conversationItem.bind(message,
                            previousMessage,
                            nextMessage,
                            glideRequests)

                        if (recordToPulse == message) {
                            binding.conversationItem.startPulseOutlinerAnimation()
                            recordToPulse = null
                        }
                    }
                    is ConversationItemReceivedMultimediaBinding -> {
                        binding.conversationItem.bind(message,
                            previousMessage,
                            nextMessage,
                            glideRequests)

                        binding.conversationItem.setItemEventListener(eventListener)
                        binding.conversationItem.bind(message,
                            previousMessage,
                            nextMessage,
                            glideRequests)

                        if (recordToPulse == message) {
                            binding.conversationItem.startPulseOutlinerAnimation()
                            recordToPulse = null
                        }

                        binding.bodyBubble.background.colorFilter =
                            PorterDuffColorFilter(
                                ContextCompat.getColor(context, R.color.conversation_item_background_secondary),
                                PorterDuff.Mode.SRC_IN
                            )
                    }

                    is ConversationItemUpdateBinding -> {
                        binding.conversationUpdateBody.text = message.getSignalGroupThreadNotificationMessageContent(context)
                        binding.conversationUpdateAction.visibility = View.GONE
//                        binding.conversationUpdateAction.text = "Learn more"
//                        binding.conversationUpdateAction.setOnClickListener {
//                            debugConfig.log("Click action")
//                        }
                    }
                }
            }

        }
    }

    fun setSelectedMessages(messages: Set<String>) {
        selectedMessages = messages
        notifyItemRangeChanged(0, itemCount)
    }

    //highlight item
    fun pulseAtPosition(position: Int) {
        if (position in 0 until itemCount) {
//            val correctedPosition = if (isHeaderPosition(position)) position + 1 else position
            recordToPulse = getItem(position)
            notifyItemChanged(position)
        }
    }

    //message item
    inner class ConversationViewHolder(val binding: ViewDataBinding) : RecyclerView.ViewHolder(binding.root){
        fun bind(item: SignalMsgExt) {
//            binding.message = item
            binding.executePendingBindings()
        }

    }

    //Decoration view: sticker header for date and divider unread message
    open class StickyHeaderViewHolder : RecyclerView.ViewHolder {
        lateinit var textView: TextView
        var divider: View? = null

        constructor(binding: ViewDataBinding) : super(binding.root) {
            when (binding) {
                is ConversationItemHeaderBinding -> {
                    textView = binding.text
                }
                is ConversationItemLastSeenBinding -> {
                    divider = binding.lastSeenDivider
                    textView = binding.text
                }
            }
        }

        constructor(textView: TextView) : super(textView) {
            this.textView = textView
        }

        fun setText(text: CharSequence?) {
            textView.text = text
        }

        fun setTextColor(@ColorInt color: Int) {
            textView.setTextColor(color)
        }

        fun setBackgroundRes(@DrawableRes resId: Int) {
            textView.setBackgroundResource(resId)
        }

        fun setDividerColor(@ColorInt color: Int) {
            divider?.setBackgroundColor(color)
        }

        fun clearBackground() {
            textView.background = null
        }
    }

    private fun isHeaderPosition(position: Int): Boolean {
        return isTypingViewEnabled && position == 0
    }

    private fun isFooterPosition(position: Int): Boolean {
        return position == itemCount - 1
    }

    override fun getItemCount(): Int {
        return super.getItemCount()
    }

    fun getPositionByItem(item: SignalMsgExt) = currentList.indexOfFirst { it.msgId == item.msgId }

    override fun getHeaderId(position: Int): Long {
        if (isHeaderPosition(position)) return -1
        //check footer
//        if (isFooterPosition(position)) return -1
        if (position >= itemCount) return -1
        if (position < 0) return -1

        val message: SignalMsgExt = getItem(position) ?: return -1

        calendar.timeInMillis = message.msgDate
        return calendar.get(Calendar.YEAR) * 1000L + calendar.get(Calendar.DAY_OF_YEAR)
    }

    override fun onCreateHeaderViewHolder(parent: ViewGroup, position: Int, type: Int): StickyHeaderViewHolder {
        val binding = ConversationItemHeaderBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return StickyHeaderViewHolder(binding)
    }

    override fun onBindHeaderViewHolder(viewHolder: RecyclerView.ViewHolder, position: Int, type: Int) {
        val context = viewHolder.itemView.context

        val message = Objects.requireNonNull(getItem(position))

        viewHolder as StickyHeaderViewHolder
        viewHolder.setText(DateUtil.getConversationDateHeaderString(context, locale, message.msgDate))

        if (type == HEADER_TYPE_POPOVER_DATE) {
            viewHolder.setBackgroundRes(R.drawable.sticky_date_header_background)
        } else if (type == HEADER_TYPE_INLINE_DATE) {
            viewHolder.clearBackground()
        }

    }

    interface OnItemClickListener {
        fun onItemClick(item: SignalMsgExt, position: Int)
        fun onItemLongClick(item: SignalMsgExt, position: Int, view: View)
    }
}