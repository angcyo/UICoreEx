package com.angcyo.engrave.auto

import android.graphics.RectF
import com.angcyo.canvas.utils.CanvasDataHandleOperate
import com.angcyo.library.app
import com.angcyo.library.ex.*
import com.angcyo.library.unit.MmValueUnit
import com.angcyo.library.unit.toMm
import com.angcyo.opencv.OpenCV
import com.angcyo.server.bean.GcodeAdjustBean
import com.yanzhenjie.andserver.annotation.*
import kotlin.io.readText

/**
 * 雕刻相关的接口
 *
 * [com.yanzhenjie.andserver.framework.handler.MappingAdapter]
 * [com.angcyo.server.def.DeviceControllerAdapter]
 *
 * [com.angcyo.engrave.auto.AutoEngraveController]
 * [com.angcyo.server.def.DeviceController]
 *
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2022/07/22
 */

@RestController
@CrossOrigin
class AutoEngraveController {

    /**GCode数据调整
     *
     * [com.yanzhenjie.andserver.framework.handler.MappingHandler]
     * [com.angcyo.server.def.DeviceControllerGcodeAdjustHandler]
     * */
    @PostMapping("/gcodeAdjust")
    fun gcodeAdjust(@RequestBody bean: GcodeAdjustBean): String {
        val gcode = bean.content
        return if (gcode.isNullOrBlank()) {
            "${nowTimeString()}\n无效的GCode内容"
        } else {
            val rect = RectF()
            val mmValueUnit = MmValueUnit()
            val left = mmValueUnit.convertValueToPixel(bean.left)
            val top = mmValueUnit.convertValueToPixel(bean.top)
            val width = mmValueUnit.convertValueToPixel(bean.width)
            val height = mmValueUnit.convertValueToPixel(bean.height)
            rect.set(
                left.toFloat(), top.toFloat(),
                (left + width).toFloat(),
                (top + height).toFloat()
            )
            val gCodeFile = CanvasDataHandleOperate.gCodeAdjust(
                gcode,
                rect,
                bean.rotate,
                bean.autoCnc,
                bean.isFinish
            )
            gCodeFile.readText()
        }
    }

    /**图片转GCode
     *
     * [com.yanzhenjie.andserver.framework.handler.MappingHandler]
     * [com.angcyo.server.def.DeviceControllerGcodeAdjustHandler]
     * */
    @PostMapping("/bitmapToGCode")
    fun bitmapToGCode(@RequestBody bean: GcodeAdjustBean): String {
        val originBitmap = bean.content?.toBitmapOfBase64()
        return if (originBitmap == null) {
            "${nowTimeString()}\n无效的图片!"
        } else {
            val width = originBitmap.width.toMm()
            bean.content = OpenCV.bitmapToGCode(
                app(),
                originBitmap,
                (width / 2).toDouble(),
                lineSpace = bean.gcodeLineSpace.toDouble(),
                direction = bean.gcodeDirection,
                angle = bean.gcodeAngle.toDouble(),
                type = if (bean.gcodeOutline) 1 else 3,
                isLast = bean.isFinish
            ).readText()
            gcodeAdjust(bean)
        }
    }
}