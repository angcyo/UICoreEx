package com.angcyo.github.core

import android.graphics.Bitmap
import androidx.palette.graphics.Palette

/**
 *
 * Email:angcyo@126.com
 * @author angcyo
 * @date 2020/07/02
 * Copyright (c) 2020 ShenZhen Wayto Ltd. All rights reserved.
 */

/**调色板*/
fun Bitmap.palette(): Palette {
    // 同步方式获取
    return Palette.from(this).generate().apply {
        vibrantSwatch//有活力的
        darkVibrantSwatch//有活力的暗色
        lightVibrantSwatch//有活力的亮色

        mutedSwatch//柔和的
        darkMutedSwatch//柔和的暗色
        lightMutedSwatch//柔和的亮色

        vibrantSwatch?.population //样本中的像素数量
        vibrantSwatch?.rgb //颜色的RBG值
        vibrantSwatch?.hsl //颜色的HSL值
        vibrantSwatch?.bodyTextColor //主体文字的颜色值
        vibrantSwatch?.titleTextColor //标题文字的颜色值
    }
}

fun Bitmap.paletteAsync(action: (Palette?) -> Unit) {
    // 异步方式获取
    Palette.from(this).generate(action)
}