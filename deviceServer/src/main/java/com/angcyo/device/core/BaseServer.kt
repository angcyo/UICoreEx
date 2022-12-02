package com.angcyo.device.core

import com.angcyo.library.annotation.CallPoint
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.concurrent.thread

/**
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2022/12/02
 */
abstract class BaseServer(val freq: Long /*休眠频率, 毫秒*/) : Runnable {

    /**是否正在运行*/
    val isRunning: Boolean
        get() = _isRunning.get()

    //---

    /**是否在运行*/
    val _isRunning = AtomicBoolean(false)

    var _thread: Thread? = null

    /**开始广播*/
    @CallPoint
    fun start() {
        if (isRunning) {
            return
        }

        _thread = thread {
            _isRunning.set(true)
            run()
        }
    }

    /**停止广播*/
    @CallPoint
    fun stop() {
        _isRunning.set(false)
        _thread?.interrupt()
        _thread = null
    }

    override fun run() {
        while (isRunning) {
            try {
                runInner()
                //---
                Thread.yield()
                Thread.sleep(freq)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    /**运行的代码*/
    abstract fun runInner()

}