package com.angcyo.github.widget

import android.content.Context
import android.util.AttributeSet
import com.angcyo.github.R
import com.angcyo.library.ex.getColor
import com.scwang.wave.MultiWaveHeader

/**
 *
 * Email:angcyo@126.com
 * @author angcyo
 * @date 2020/05/28
 * Copyright (c) 2020 ShenZhen Wayto Ltd. All rights reserved.
 */
class WaveView(context: Context, attributeSet: AttributeSet? = null) :
    MultiWaveHeader(context, attributeSet) {

    init {
        val typedArray = context.obtainStyledAttributes(attributeSet, R.styleable.MultiWaveHeader)
        velocity = typedArray.getFloat(R.styleable.MultiWaveHeader_mwhVelocity, 2f)

        startColor = typedArray.getColor(
            R.styleable.MultiWaveHeader_mwhStartColor,
            getColor(R.color.colorPrimary)
        )
        closeColor = typedArray.getColor(
            R.styleable.MultiWaveHeader_mwhCloseColor,
            getColor(R.color.colorPrimaryDark)
        )

        tag = if (typedArray.hasValue(R.styleable.MultiWaveHeader_mwhWaves)) {
            typedArray.getString(R.styleable.MultiWaveHeader_mwhWaves)
        } else {
            /**
             * 格式-format
             * offsetX offsetY scaleX scaleY velocity（dp/s）
             * 水平偏移量 竖直偏移量 水平拉伸比例 竖直拉伸比例 速度
             */
            val waves =
                "0,20,1,1,12\n120,5,1,1,33\n200,0,1,1,18\n70,25,1.4,1.4,-26\n100,5,1.4,1.2,15\n420,0,1.15,1,-10\n520,10,1.7,1.5,20\n220,0,1,1,-15".split(
                    "\n"
                )
            waves.slice(0..2).joinToString("\n")
        }

        typedArray.recycle()
    }

}