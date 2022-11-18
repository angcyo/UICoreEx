package com.angcyo.server.def

import android.content.ComponentName
import android.content.Intent
import android.graphics.RectF
import android.net.Uri
import com.angcyo.base.dslAHelper
import com.angcyo.canvas.data.toMm
import com.angcyo.canvas.utils.CanvasDataHandleOperate
import com.angcyo.core.dslitem.DslLastDeviceInfoItem
import com.angcyo.library.app
import com.angcyo.library.component.appBean
import com.angcyo.library.component.dslIntentQuery
import com.angcyo.library.component.lastContext
import com.angcyo.library.ex.*
import com.angcyo.library.unit.MmValueUnit
import com.angcyo.library.utils.SysIntent
import com.angcyo.library.utils.SystemBatchBean
import com.angcyo.library.utils.getLongNumStringList
import com.angcyo.opencv.OpenCV
import com.angcyo.server.bean.GcodeAdjustBean
import com.angcyo.server.bean.SchemeReqBean
import com.yanzhenjie.andserver.annotation.*
import kotlin.io.readText

/**
 * 提供一些设备的基础信息
 *
 * [com.yanzhenjie.andserver.framework.handler.MappingAdapter]
 * [com.angcyo.server.def.DeviceControllerAdapter]
 *
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2022/07/22
 */

@RestController
@CrossOrigin
class DeviceController {

    /**获取设备的基础信息*/
    @GetMapping("/device")
    fun device(): String {
        return DslLastDeviceInfoItem.deviceInfo {
            appendln()
            append(app().packageName)
            appendln()
        }.toString()
    }

    /**查看和打开对应的Scheme*/
    @PostMapping("/scheme")
    fun scheme(@RequestBody bean: SchemeReqBean): String {
        val scheme = bean.scheme
        if (scheme.isNullOrBlank()) {
            return "scheme 不正确"
        } else {
            dslIntentQuery {
                queryData = Uri.parse(scheme)
                queryCategory = listOf(Intent.CATEGORY_BROWSABLE)
            }.apply {
                if (isNotEmpty()) {
                    //找到了
                    val actionInfo = first().activityInfo
                    lastContext.dslAHelper {
                        val componentName =
                            ComponentName(actionInfo.packageName, actionInfo.name)
                        start(scheme.decode().urlIntent(componentName))
                    }
                    return "正在使用${actionInfo.packageName.appBean()?.appName}打开"
                } else {
                    return "未找到对应的应用"
                }
            }
        }
    }

    /**GCode数据调整
     *
     * [com.yanzhenjie.andserver.framework.handler.MappingHandler]
     * [com.angcyo.server.def.DeviceControllerGcodeAdjustHandler]
     * */
    @PostMapping("/gcodeAdjust")
    fun gcodeAdjust(@RequestBody bean: GcodeAdjustBean): String {
        val gcode = bean.content
        return if (gcode.isNullOrBlank()) {
            "无效的GCode内容"
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
            "无效的图片!"
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

    /**批量添加通讯记录*/
    @PostMapping("/batchAddCallLogs")
    fun batchAddCallLogs(@RequestBody body: String): String {
        val batchBeanList = getSystemBatchBean(body)
        if (batchBeanList.isEmpty()) {
            return "无数据需要添加!"
        }
        val array = SysIntent.batchAddCallLogs(batchBeanList)
        return buildString {
            appendLine("${nowTimeString()} 成功:${array.size()}")
            batchBeanList.forEach {
                appendLine("${it.name} ${it.number}")
            }
        }
    }

    /**批量添加通讯录*/
    @PostMapping("/batchAddContacts")
    fun batchAddContacts(@RequestBody body: String): String {
        val batchBeanList = getSystemBatchBean(body)
        if (batchBeanList.isEmpty()) {
            return "无数据需要添加!"
        }
        val array = SysIntent.batchAddContacts(batchBeanList)
        return buildString {
            appendLine("${nowTimeString()} 成功:${array.size()}")
            batchBeanList.forEach {
                appendLine("${it.name} ${it.number}")
            }
        }
    }

    private fun getSystemBatchBean(body: String): List<SystemBatchBean> {
        val result = mutableListOf<SystemBatchBean>()
        val list = body.split("\\n")
        list.forEach { line ->
            var name: String? = null
            var number: String? = null
            line.split(" ").forEach {
                if (it.isBlank()) {
                    //empty
                } else if (name == null) {
                    name = it
                } else if (number == null) {
                    it.getLongNumStringList()?.firstOrNull()?.let {
                        number = it
                    }
                }
            }
            if (!name.isNullOrBlank() && !number.isNullOrBlank()) {
                result.add(SystemBatchBean(name!!, number!!))
            }
        }
        return result
    }
}