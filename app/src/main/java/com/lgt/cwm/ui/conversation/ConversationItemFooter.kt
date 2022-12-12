package com.lgt.cwm.ui.conversation

import android.content.Context
import android.content.res.TypedArray
import android.graphics.PorterDuff
import android.graphics.Rect
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.annotation.DrawableRes
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import com.lgt.cwm.R
import com.lgt.cwm.databinding.ConversationItemFooterIncomingBinding
import com.lgt.cwm.databinding.ConversationItemFooterOutgoingBinding
import com.lgt.cwm.db.entity.SignalMsg
import com.lgt.cwm.ui.DeliveryStatusView
import com.lgt.cwm.ui.components.Projection
import com.lgt.cwm.util.ViewUtil
import java.util.*

class ConversationItemFooter : ConstraintLayout {
    private lateinit var dateView: TextView
    private lateinit var deliveryStatusView: DeliveryStatusView
    private var isOutgoing = false
    private var onTouchDelegateChangedListener: OnTouchDelegateChangedListener? = null

    constructor(context: Context) : super(context) {
        init(null)
    }

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        init(attrs)
    }

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        init(attrs)
    }

    private fun init(attrs: AttributeSet?) {
        val typedArray: TypedArray? = if (attrs != null) {
            context.theme.obtainStyledAttributes(attrs, R.styleable.ConversationItemFooter, 0, 0)
        } else {
            null
        }

        if (typedArray != null) {
            val mode = typedArray.getInt(R.styleable.ConversationItemFooter_footer_mode, 0)
            isOutgoing = mode == 0

            if (isOutgoing) {
                val binding = ConversationItemFooterOutgoingBinding.inflate(LayoutInflater.from(context), this, true)
                dateView = binding.footerDate
                deliveryStatusView = binding.footerDeliveryStatus
            } else {
                val binding = ConversationItemFooterIncomingBinding.inflate(LayoutInflater.from(context), this, true)
                dateView = binding.footerDate
                deliveryStatusView = binding.footerDeliveryStatus
            }
        } else {
            val binding = ConversationItemFooterOutgoingBinding.inflate(LayoutInflater.from(context), this, true)
            isOutgoing = true

            dateView = binding.footerDate
            deliveryStatusView = binding.footerDeliveryStatus
        }

        if (typedArray != null) {
//            setTextColor(typedArray.getInt(R.styleable.ConversationItemFooter_footer_text_color, ContextCompat.getColor(context, R.color.core_white)))
//            setIconColor(typedArray.getInt(R.styleable.ConversationItemFooter_footer_icon_color, ContextCompat.getColor(context, R.color.core_white)))
            typedArray.recycle()
        }
    }

    fun setOnTouchDelegateChangedListener(onTouchDelegateChangedListener: OnTouchDelegateChangedListener?) {
        this.onTouchDelegateChangedListener = onTouchDelegateChangedListener
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
    }

    fun setMessage(message: SignalMsg, locale: Locale) {
        presentDate(message, locale)
        presentDeliveryStatus(message)
    }

    fun setTextColor(color: Int) {
        dateView.setTextColor(color)
    }

    fun setIconColor(color: Int) {
        deliveryStatusView.setTint(color)
    }

    fun enableBubbleBackground(@DrawableRes drawableRes: Int, tint: Int?) {
        setBackgroundResource(drawableRes)
        if (tint != null) {
            background.setColorFilter(tint, PorterDuff.Mode.MULTIPLY)
        } else {
            background.clearColorFilter()
        }
    }

    fun disableBubbleBackground() {
        background = null
    }

    fun getProjection(coordinateRoot: ViewGroup): Projection? {
        return if (visibility == VISIBLE) {
            Projection.relativeToParent(coordinateRoot, this, Projection.Corners(ViewUtil.dpToPx(11).toFloat()))
        } else {
            null
        }
    }

    private fun notifyTouchDelegateChanged(rect: Rect, touchDelegate: View) {
        onTouchDelegateChangedListener?.onTouchDelegateChanged(rect, touchDelegate)
    }

    private fun presentDate(message: SignalMsg, locale: Locale) {
        dateView.forceLayout()

    }

    private fun presentDeliveryStatus(message: SignalMsg) {

    }

    interface OnTouchDelegateChangedListener {
        fun onTouchDelegateChanged(delegateRect: Rect, delegateView: View)
    }
}