package com.angcyo.game.core

import android.graphics.Canvas
import android.os.Handler
import android.os.HandlerThread
import android.os.SystemClock
import android.view.Choreographer
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

/**
 *
 * Email:angcyo@126.com
 * @author angcyo
 * @date 2020/03/09
 * Copyright (c) 2019 ShenZhen O&M Cloud Co., Ltd. All rights reserved.
 */
class GameRenderEngine : Runnable, Choreographer.FrameCallback {

    companion object {
        //状态 初始化
        const val RENDER_STATUS_INIT = 0

        //状态 开始
        const val RENDER_STATUS_START = 1

        //1471086336
        fun engineTime(): Long = SystemClock.uptimeMillis()
    }

    //当前引擎状态
    var _renderStatus: Int = RENDER_STATUS_INIT
    val _lock = ReentrantLock()

    //触发器
    val _choreographer: Choreographer = Choreographer.getInstance()
    var _renderThread: HandlerThread? = null
    var _renderHandler: Handler? = null
    val _engineParams = EngineParams()

    //渲染开始的时间, 用于计算fps
    var _frame = 0L

    var gameLayerManager = GameLayerManager(this)

    val fps: Float
        get() {
            val nowTime = engineTime()
            return _frame * 1000 * 1f / (nowTime - _engineParams.engineStartTime)
        }

    /**帧绘制*/
    var onFrameDraw: () -> Unit = {}

    /**开始渲染*/
    fun startRender() {
        _lock.withLock {
            if (_renderStatus == RENDER_STATUS_START) {
                //已经开始了
                return
            }
            _engineParams.engineStartTime = engineTime()
            _renderThread =
                HandlerThread("GameRenderEngine_${_engineParams.engineStartTime}").apply {
                    start()
                    _renderHandler = Handler(looper)
                    _renderHandler?.post(this@GameRenderEngine)
                }

            _renderStatus = RENDER_STATUS_START
            _choreographer.postFrameCallback(this)
        }
    }

    /**结束渲染*/
    fun endRender() {
        _lock.withLock {
            _renderStatus = RENDER_STATUS_INIT
            _renderThread?.quit()
            _renderThread = null
            _renderHandler = null
        }
    }

    /**初始化引擎参数*/
    fun initEngine(params: EngineParams) {
        _engineParams.engineWidth = params.engineWidth
        _engineParams.engineHeight = params.engineHeight
        gameLayerManager.onEngineUpdate(params)
    }

    /**绘制驱动*/
    override fun doFrame(frameTimeNanos: Long) {
        _renderHandler?.post(this)

        _frame++
        onFrameDraw()

        if (_renderStatus == RENDER_STATUS_START) {
            _choreographer.postFrameCallback(this)
        }
    }

    /**绘制执行*/
    fun draw(canvas: Canvas) {
        gameLayerManager.draw(canvas)
    }

    /**数据更新驱动*/
    override fun run() {
        gameLayerManager.update()
    }
}

/**获取引擎统一计算使用时间*/
fun engineTime() = GameRenderEngine.engineTime()