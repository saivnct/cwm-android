package com.lgt.cwm.ui.conversation

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import com.lgt.cwm.databinding.ConversationScrollToBinding

class ConversationScrollToView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : FrameLayout(context, attrs, defStyle) {

    private var unreadCount: TextView
    private var scrollButton: ImageView

    init {
        val binding = ConversationScrollToBinding.inflate(LayoutInflater.from(context), this, true);

        unreadCount = binding.conversationScrollToCount
        scrollButton = binding.conversationScrollToButton

        if (attrs != null) {
            //apply style
        }
    }

    override fun setOnClickListener(listener: OnClickListener?) {
        scrollButton.setOnClickListener(listener)
    }

    fun setUnreadCount(unreadCount: Int) {
        this.unreadCount.text = formatUnreadCount(unreadCount)
        this.unreadCount.visibility = if (unreadCount > 0) VISIBLE else GONE
    }

    private fun formatUnreadCount(unreadCount: Int): CharSequence {
        return if (unreadCount > 999) "999+" else unreadCount.toString()
    }

}