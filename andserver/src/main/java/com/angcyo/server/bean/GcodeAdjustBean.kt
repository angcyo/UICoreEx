package com.angcyo.server.bean

import com.angcyo.library.extend.IJson

/**
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2022/11/18
 */
data class GcodeAdjustBean(
    /**GCode内容*/
    var content: String? = null,
    /**需要调整到的坐标*/
    var left: Double = 0.0,
    var top: Double = 0.0,
    /**需要调整的宽高*/
    var width: Double = 0.0,
    var height: Double = 0.0,
    /**旋转的角度*/
    var rotate: Float = 0f,
    var autoCnc: Boolean = false,
    var isFinish: Boolean = false,
) : IJson
