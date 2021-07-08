package com.angcyo.acc2.app.server

import android.graphics.*
import com.angcyo.acc2.app.AppAccLog
import com.angcyo.acc2.app.R
import com.angcyo.acc2.app.component.Task
import com.angcyo.acc2.core.AccNodeLog
import com.angcyo.acc2.eachChildDepth
import com.angcyo.drawable.drawTextByLT
import com.angcyo.drawable.paint
import com.angcyo.drawable.textHeight
import com.angcyo.http.base.toJson
import com.angcyo.library._screenHeight
import com.angcyo.library._screenWidth
import com.angcyo.library.ex.*
import com.yanzhenjie.andserver.annotation.GetMapping
import com.yanzhenjie.andserver.annotation.PathVariable
import com.yanzhenjie.andserver.annotation.RestController
import kotlin.math.max

/**
 * https://yanzhenjie.com/AndServer/annotation/RestController.html
 *
 * Email:angcyo@126.com
 * @author angcyo
 * @date 2021/07/06
 * Copyright (c) 2020 angcyo. All rights reserved.
 */

@RestController
class AccController {

    /**捕抓界面*/
    @GetMapping("/catch")
    fun catchLog(): String {
        return AccNodeLog().getAccessibilityWindowLog().toString()
    }

    /**清理日志, 只会清理acc目录下的日志*/
    @GetMapping("/clear")
    fun clearLog(): String {
        val path = AppAccLog.logPath()
        val result = path?.file()?.deleteRecursively() == true
        return "清理日志[$result]:$path"
    }

    /**返回控制器的状态日志*/
    @GetMapping("/controlLog")
    fun controlLog(): String {
        return Task.control.controlLog()
    }

    /**控制器正在进行的任务*/
    @GetMapping("/task")
    fun task(): String {
        return Task.control._taskBean?.toJson() ?: "无任务在运行"
    }

    /**使用acc抓取界面, 然后还原成蓝图
     * [type] 需要不抓的窗口类型, active:表示当前活跃的窗口, 默认表示所有窗口
     * */
    @GetMapping("/shot/{type}")
    fun shot(@PathVariable("type", required = false) type: String? = null): Bitmap {
        val rootWindowList = if (type?.lowercase() == "active") {
            Task.control.accService()?.windows?.filter {
                it.root == Task.control.accService()?.rootInActiveWindow
            }
        } else {
            Task.control.accService()?.windows
        }

        var w = _screenWidth
        var h = _screenHeight

        //画布填充
        val paddingLeft = 2 * dpi
        val paddingTop = 2 * dpi
        val paddingRight = 2 * dpi
        val paddingBottom = 2 * dpi

        //画笔
        val paint = paint()

        //窗口标题文本绘制高度
        val windowTitleHeight = paint.textHeight()
        //窗口之间间隙
        val windowSpace = 10 * dpi

        //窗口的bounds
        val windowOutBounds = Rect()
        //节点的bounds
        val nodeOutBounds = Rect()

        //初始化画布大小
        if (rootWindowList.isNullOrEmpty()) {
            //无窗口
            h = 20 * dpi
        } else {
            w = 0
            rootWindowList.forEachIndexed { index, windowInfo ->
                windowInfo.getBoundsInScreen(windowOutBounds)

                w = max(w, windowOutBounds.width())

                if (index == 0) {
                    h = windowOutBounds.height()
                } else {
                    h += windowOutBounds.height()
                }
                h += windowTitleHeight.toInt()
                if (index > 0) {
                    h += windowSpace
                }
            }
            w += paddingLeft + paddingRight
            h += paddingTop + paddingBottom
        }

        //画布
        val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        //画布配置
        val backgroundColor = Color.WHITE //画布背景
        val textColor = _color(R.color.text_primary_color) //文本颜色
        val textSize = 9 * dp
        val subTextSize = 3 * dp
        val subTextColor = _color(R.color.text_place_color)
        val rectStrokeWidth = 1f //矩形边框粗细
        val rectStrokeColor = _color(R.color.colorPrimaryDark) //矩形边框颜色

        canvas.drawColor(backgroundColor)

        //开始绘制
        val left = 0f + paddingLeft
        var top = 0f + paddingTop

        if (rootWindowList.isNullOrEmpty()) {
            //无窗口
            canvas.drawTextByLT("无窗口找到", left, top, paint)
        } else {

            fun configTextPaint(color: Int = textColor, size: Float = textSize) {
                paint.style = Paint.Style.FILL
                paint.color = color
                paint.textSize = size
            }

            fun configRectPaint(color: Int = rectStrokeColor) {
                paint.style = Paint.Style.STROKE
                paint.color = color
                paint.strokeWidth = rectStrokeWidth
            }

            rootWindowList.forEach { windowInfo ->
                windowInfo.getBoundsInScreen(windowOutBounds)

                //绘制窗口标题和包名
                configTextPaint()
                canvas.drawTextByLT(
                    "${windowInfo.title} ${windowInfo.root.packageName}",
                    left,
                    top,
                    paint
                )
                top += windowTitleHeight

                //绘制窗口rect
                configRectPaint()
                windowOutBounds.offsetTo(left.toInt(), top.toInt())
                canvas.drawRect(windowOutBounds, paint)

                //绘制节点
                windowInfo.root.wrap().eachChildDepth { node, depth ->
                    node.getBoundsInScreen(nodeOutBounds)

                    val text = node.text ?: node.contentDescription

                    if (!nodeOutBounds.isEmpty) {

                        if (nodeOutBounds.top > windowOutBounds.height()) {
                            //节点在绘制的窗口外
                            nodeOutBounds.offset(0, -nodeOutBounds.top)
                        }

                        nodeOutBounds.offset(left.toInt(), top.toInt())

                        if (!text.isNullOrEmpty()) {
                            //绘制node rect
                            configRectPaint()
                            canvas.drawRect(nodeOutBounds, paint)

                            //绘制文本
                            configTextPaint()
                            canvas.drawTextByLT(
                                text,
                                nodeOutBounds.left.toFloat(),
                                nodeOutBounds.top.toFloat(),
                                paint
                            )
                        } else if (node.childCount <= 0) {
                            configRectPaint(subTextColor)
                            canvas.drawRect(nodeOutBounds, paint)

                            //绘制文本
                            configTextPaint(subTextColor, subTextSize)
                            canvas.drawTextByLT(
                                node.className.toString().subEnd(".", true) ?: "",
                                nodeOutBounds.left.toFloat(),
                                nodeOutBounds.top.toFloat(),
                                paint
                            )
                        }
                    }

                    false
                }

                top += windowOutBounds.height()
                top += windowSpace
            }
        }

        return bitmap
    }
}