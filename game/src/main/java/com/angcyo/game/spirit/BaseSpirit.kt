package com.angcyo.game.spirit

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import com.angcyo.game.core.DrawParams
import com.angcyo.game.core.SpiritParams
import com.angcyo.game.core.UpdateParams
import com.angcyo.game.layer.BaseLayer
import com.angcyo.library.ex.dp

/**
 * 绘制的最小单位精灵
 * Email:angcyo@126.com
 * @author angcyo
 * @date 2020/03/09
 * Copyright (c) 2019 ShenZhen O&M Cloud Co., Ltd. All rights reserved.
 */

abstract class BaseSpirit {
    companion object {
        const val SPIRIT_STATUS_NORMAL = 0
        const val SPIRIT_STATUS_PAUSE_DRAW = 0x1
        const val SPIRIT_STATUS_PAUSE_UPDATE = 0x2
    }

    /**精灵的状态*/
    var spiritStatus: Int = SPIRIT_STATUS_NORMAL

    /**画笔*/
    val spiritPaint: Paint by lazy {
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            strokeJoin = Paint.Join.ROUND
            strokeCap = Paint.Cap.ROUND
            style = Paint.Style.STROKE
            isFilterBitmap = true
            textSize = 12 * dp
            strokeWidth = 1 * dp
        }
    }

    /**精灵在[layer]中的矩形坐标*/
    val spiritRectF = RectF()

    /**精灵参数*/
    val spiritParams = SpiritParams()

    var _layer: BaseLayer? = null

    open fun attachToLayer(layer: BaseLayer) {
        _layer = layer
    }

    open fun detachFromLayer(layer: BaseLayer) {
        _layer = null
    }

    abstract fun draw(canvas: Canvas, drawParams: DrawParams)

    abstract fun update(updateParams: UpdateParams)
}