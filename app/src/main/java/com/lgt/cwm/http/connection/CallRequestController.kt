package com.lgt.cwm.http.connection

import androidx.annotation.WorkerThread
import com.lgt.cwm.util.Util
import okhttp3.Call
import java.io.InputStream
import java.util.concurrent.Executors
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

class CallRequestController(private val call: Call? = null) : RequestController {
    private var stream: InputStream? = null
    private var canceled = false

    private val lock = ReentrantLock()
    private val condition = lock.newCondition()

    override fun cancel() {
        Executors.newSingleThreadExecutor().execute {
            synchronized(this@CallRequestController) {
                if (canceled) return@execute
                call?.cancel()
                canceled = true
            }
        }
    }

    fun setStream(stream: InputStream) {
        lock.withLock {
            this.stream = stream
            condition.signalAll()
        }
    }

    /**
     * Blocks until the stream is available or until the request is canceled.
     */
    @WorkerThread
    fun getStream(): InputStream? {
        lock.withLock {
            while (stream == null && !canceled) {
                Util.wait(this, 0)
            }
            return stream
        }
    }
}