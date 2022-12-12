package com.lgt.cwm.activity.home.fragments.account.register

import android.content.Context
import android.util.AttributeSet
import com.google.android.material.button.MaterialButton
import com.lgt.cwm.R
import java.util.concurrent.TimeUnit

class CodeCountDownView : MaterialButton {
    private var countDownToTime: Long = 0
    private var listener: Listener? = null

    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    /**
     * Starts a count down to the specified {@param time}.
     */
    fun startCountDownTo(time: Long) {
        if (time > 0) {
            countDownToTime = time
            updateCountDown()
        }
    }

    fun setCallEnabled() {
        setText(R.string.RegisterFragment_call)
        isEnabled = true
        alpha = 1.0f
    }

    private fun updateCountDown() {
        val remainingMillis = countDownToTime - System.currentTimeMillis()
        if (remainingMillis > 0) {
            isEnabled = false
            alpha = 0.5f
            val totalRemainingSeconds = TimeUnit.MILLISECONDS.toSeconds(remainingMillis).toInt()
            val minutesRemaining = totalRemainingSeconds / 60
            val secondsRemaining = totalRemainingSeconds % 60
            text = resources.getString(R.string.RegisterFragment_call_me_instead_available_in,
                minutesRemaining, secondsRemaining)

            listener?.onRemaining(this, totalRemainingSeconds)

            postDelayed({ updateCountDown() }, 250)
        } else {
            setCallEnabled()
        }
    }

    fun setListener(listener: Listener?) {
        this.listener = listener
    }

    interface Listener {
        fun onRemaining(view: CodeCountDownView, secondsRemaining: Int)
    }
}