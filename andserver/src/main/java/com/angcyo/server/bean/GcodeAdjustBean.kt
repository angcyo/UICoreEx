package com.angcyo.server.bean

import com.angcyo.library.extend.IJson

/**
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2022/11/18
 */
data class GcodeAdjustBean(
    /**GCode内容, 如果是图片转GCode, 则内容是图片的base64协议头数据
     * [com.angcyo.canvas.data.CanvasProjectItemBean.imageOriginal]*/
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

    //--图片转GCode相关参数

    /**gcode线距*/
    var gcodeLineSpace: Float = 5f,

    /**gcode角度[0-90]*/
    var gcodeAngle: Float = 0f,

    /**gcode方向 0:0 1:90 2:180 3:270*/
    var gcodeDirection: Int = 0,

    /**gcode是否需要轮廓*/
    var gcodeOutline: Boolean = true,
) : IJson
