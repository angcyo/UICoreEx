package com.angcyo.acc2.app

import android.graphics.Color
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat
import com.angcyo.acc2.app.component.AccTouchTipLayer
import com.angcyo.acc2.app.component.AccWindow
import com.angcyo.acc2.bean.ActionBean
import com.angcyo.acc2.control.AccControl
import com.angcyo.acc2.control.AccPrint
import com.angcyo.acc2.parse.toRect
import com.angcyo.core.R
import com.angcyo.core.component.file.DslFileHelper
import com.angcyo.http.rx.doMain
import com.angcyo.library.ex._color
import com.angcyo.library.ex.des
import com.angcyo.library.ex.isDebugType
import com.angcyo.library.ex.wrapLog
import com.angcyo.library.utils.FileUtils
import com.angcyo.library.utils.logFileName
import com.angcyo.library.utils.toLogFilePath

/**
 *
 * Email:angcyo@126.com
 * @author angcyo
 * @date 2021/02/01
 * Copyright (c) 2020 ShenZhen Wayto Ltd. All rights reserved.
 */

class AppAccPrint(accControl: AccControl) : AccPrint(accControl) {

    companion object {

        var accLogFolderName = "acc"
        var accLogFileName = "acc.log"

        /**catch的日志文件路径*/
        fun logCatchPath() = logFileName().toLogFilePath(accLogFolderName)

        /**acc的日志文件路径*/
        fun logPath() =
            FileUtils.appRootExternalFolderFile(accLogFolderName, accLogFileName).absolutePath
    }

    override fun log(log: String?) {
        super.log(log)
        log?.wrapLog()?.saveAccRunLog()
    }

    override fun next(actionBean: ActionBean, time: Long) {
        super.next(actionBean, time)
        "next[$time]->${actionBean.title}${(actionBean.summary ?: actionBean.des).des()}".wrapLog()
            .saveAccRunLog()

        doMain(false) {
            val title = if (AccWindow.fullscreenLayer) {
                actionBean.title
            } else {
                actionBean.summary ?: actionBean.title
            }

            val color =
                if (accControl?.accSchedule?._isLeaveWindow == true) _color(R.color.warning) else Color.WHITE

            if (time <= 0) {
                AccWindow.apply {
                    text = accControl?.accSchedule?.indexTip()
                    textColor = color

                    summary = title

                    progressFlicker = true
                    updateProgress = false
                    update()
                }
            } else {
                AccWindow.showProgress(accControl?.accSchedule?.indexTip(), title, time) {
                    textColor = color
                }
            }
        }
    }

    override fun sleep(time: Long) {
        super.sleep(time)
        doMain(false) {
            AccWindow.progressFlicker = true
            AccWindow.update()
        }
    }

    override fun handleNode(nodeList: List<AccessibilityNodeInfoCompat>?) {
        super.handleNode(nodeList)
        val showNodeTip = accControl?._taskBean?.showNodeTip
        if (isDebugType() || (showNodeTip ?: app().memoryConfigBean.showNodeTip)) {
            showNodeRect(nodeList)
        }
    }

    override fun touch(x1: Float, y1: Float, x2: Float?, y2: Float?) {
        super.touch(x1, y1, x2, y2)
        val showTouchTip = accControl?._taskBean?.showTouchTip
        if (isDebugType() || (showTouchTip ?: app().memoryConfigBean.showTouchTip)) {
            doMain {
                AccTouchTipLayer().apply {
                    if (x2 == null || y2 == null) {
                        showTouch(x1, y1)
                    } else {
                        showMove(x1, y1, x2, y2)
                    }
                }
            }
        }
    }
}

/**显示节点矩形提示*/
fun showNodeRect(nodeList: List<AccessibilityNodeInfoCompat>?) {
    doMain {
        AccTouchTipLayer().apply {
            lineColor = cColor
            showRect(nodeList?.toRect())
        }
    }
}

/**写入acc log 到文件*/
fun Any.saveAccLog() = DslFileHelper.write(AppAccPrint.accLogFolderName, data = this.toString())

fun Any.saveAccRunLog() = DslFileHelper.write(
    AppAccPrint.accLogFolderName,
    AppAccPrint.accLogFileName,
    data = this.toString()
)