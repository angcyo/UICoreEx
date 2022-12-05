package com.angcyo.engrave.data

import android.widget.LinearLayout

/**
 * (x,y)为线段起始点坐标，len为线段长度,数据类型为U16。
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2022/08/26
 */
data class BitmapPath(
    /**起始点坐标*/
    val x: Int,
    val y: Int,
    /**总的像素长度*/
    var len: Int,
    /**从左到右, 还是从右到左*/
    var ltr: Boolean,
    /**扫描方向*/
    val orientation: Int = LinearLayout.HORIZONTAL,
    /**一行是否结束*/
    var lineEnd: Boolean = false,
)
