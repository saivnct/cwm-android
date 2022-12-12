package com.lgt.cwm.ui.conversation

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.LinearLayout
import com.lgt.cwm.databinding.ConversationItemForwardBinding
import kotlinx.android.synthetic.main.conversation_item_forward.view.*

class ForwardView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : LinearLayout(context, attrs, defStyle) {

    init {
        val binding: ConversationItemForwardBinding =
            ConversationItemForwardBinding.inflate(LayoutInflater.from(context), this, true);

        if (attrs != null) {
            //apply style
        }

        setNone()
    }

    fun setNone() {
        this.visibility = GONE
    }

    fun setForwardFrom(from: String) {
        this.visibility = VISIBLE
        tv_forward_from.text = from
    }

}