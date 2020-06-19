package com.mama.views.utils

import android.os.Handler
import android.os.HandlerThread

object HandlerThreadUtils {
    private var handlerThread: HandlerThread? = null
    private const val THREAD_NAME = "Smart_Handler_Thread"
    private var handler: Handler? = null
    fun init() {
        if (handlerThread == null) {
            synchronized(HandlerThreadUtils::class.java) {
                if (handlerThread == null) {
                    handlerThread = HandlerThread(THREAD_NAME)
                    handlerThread!!.start()
                    handler =
                        Handler(handlerThread!!.looper)
                }
            }
        }
    }

    fun post(runnable: Runnable?) {
        if (handler == null) return
        handler!!.post(runnable)
    }

    fun postDelay(runnable: Runnable?, delay: Long) {
        if (handler == null) return
        handler!!.postDelayed(runnable, delay)
    }

    fun quit() {
        if (handlerThread != null) {
            if (handler != null) {
                handler!!.removeCallbacks(null)
            }
            handlerThread!!.quitSafely()
        }
    }
}