package com.angcyo.game.spirit

import android.graphics.Canvas
import com.angcyo.game.core.DrawParams
import com.angcyo.game.core.UpdateParams
import com.angcyo.library.ex.randomColorIn

/**
 * 绘制颜色的精灵
 * Email:angcyo@126.com
 * @author angcyo
 * @date 2020/03/09
 * Copyright (c) 2019 ShenZhen O&M Cloud Co., Ltd. All rights reserved.
 */
open class ColorSpirit : BaseSpirit() {

    var roundX = 0f
    var roundY = 0f

    override fun draw(canvas: Canvas, drawParams: DrawParams) {
        spiritPaint.color = getSpiritColor()
        canvas.drawRoundRect(spiritRectF, roundX, roundY, spiritPaint)
    }

    open fun getSpiritColor(): Int {
        return randomColorIn()
    }

    override fun update(updateParams: UpdateParams) {

    }

}