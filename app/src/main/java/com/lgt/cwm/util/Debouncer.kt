package com.lgt.cwm.util

import android.os.Handler
import android.os.Looper

/**
 * A class that will throttle the number of runnables executed to be at most once every specified
 * interval. However, it could be longer if events are published consistently.
 *
 * Useful for performing actions in response to rapid user input, such as inputting text, where you
 * don't necessarily want to perform an action after *every* input.
 *
 * See http://rxmarbles.com/#debounce
 */
class Debouncer(threshold: Long) {
    private val handler: Handler = Handler(Looper.getMainLooper())
    private val threshold: Long

    /**
     * @param threshold Only one runnable will be executed via [.publish] every
     * `threshold` milliseconds.
     */
    init {
        this.threshold = threshold
    }

    fun publish(runnable: Runnable) {
        handler.removeCallbacksAndMessages(null)
        handler.postDelayed(runnable, threshold)
    }

    fun clear() {
        handler.removeCallbacksAndMessages(null)
    }


}