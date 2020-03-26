package com.angcyo.game

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import com.angcyo.game.core.EngineParams
import com.angcyo.game.core.GameRenderEngine
import com.angcyo.library.ex.dp
import com.angcyo.library.ex.isDebug

/**
 * 游戏渲染,按照60帧的速度一直绘制的游戏渲染View
 * Email:angcyo@126.com
 * @author angcyo
 * @date 2020/03/09
 * Copyright (c) 2019 ShenZhen O&M Cloud Co., Ltd. All rights reserved.
 */
class GameRenderView(context: Context, attributeSet: AttributeSet? = null) :
    View(context, attributeSet) {

    val gameRenderEngine = GameRenderEngine().apply {
        onFrameDraw = {
            invalidate()
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        startRenderInner()
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        endRender()
    }

    override fun onVisibilityChanged(changedView: View, visibility: Int) {
        super.onVisibilityChanged(changedView, visibility)
        if (visibility == VISIBLE) {
            startRenderInner()
        } else {
            endRender()
        }
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        gameRenderEngine.initEngine(EngineParams(w.toFloat(), h.toFloat()))
    }

    private fun startRenderInner() {
        startRender()
    }

    /*开始渲染界面*/
    fun startRender() {
        gameRenderEngine.startRender()
    }

    /**结束渲染*/
    fun endRender() {
        gameRenderEngine.endRender()
    }

    var showFps: Boolean = isDebug()

    private val fpsPaint: Paint by lazy {
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = 14 * dp
            color = Color.RED
            strokeWidth = 1 * dp
            style = Paint.Style.FILL_AND_STROKE
            setShadowLayer(1 * dp, 1 * dp, 1 * dp, Color.BLACK)
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        gameRenderEngine.draw(canvas)

        if (showFps) {
            val text = "${gameRenderEngine.fps}"
            canvas.drawText(text, 10 * dp, 20 * dp, fpsPaint)
        }
    }
}