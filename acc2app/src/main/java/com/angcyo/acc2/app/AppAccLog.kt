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
import com.angcyo.library.utils.logFilePath

/**
 *
 * Email:angcyo@126.com
 * @author angcyo
 * @date 2021/02/01
 * Copyright (c) 2020 ShenZhen Wayto Ltd. All rights reserved.
 */

class AppAccLog(accControl: AccControl) : AccPrint(accControl) {

    companion object {

        var accLogFolderName = "acc"
        var accLogFileName = "acc.log"

        /**catch的日志文件路径*/
        fun logCatchPath() = accLogFolderName.logFilePath()

        /**acc的日志文件路径*/
        fun logPath() = FileUtils.appRootExternalFolderFile(
            app(),
            accLogFolderName,
            accLogFileName
        )?.absolutePath
    }

    override fun log(log: String?) {
        super.log(log)
        log?.wrapLog()?.saveAccRunLog()
    }

    override fun next(actionBean: ActionBean, time: Long) {
        super.next(actionBean, time)
        "next[$time]->${actionBean.title}${(actionBean.summary ?: actionBean.des).des()}".wrapLog()
            .saveAccRunLog()

        doMain {
            val title = if (AccWindow.fullscreenLayer) {
                actionBean.title
            } else {
                actionBean.summary ?: actionBean.title
            }
            AccWindow.showProgress(accControl?.accSchedule?.indexTip(), title, time) {
                textColor =
                    if (accControl?.accSchedule?._isLeaveWindow == true) _color(R.color.warning) else Color.WHITE
            }
        }
    }

    override fun handleNode(nodeList: List<AccessibilityNodeInfoCompat>?) {
        super.handleNode(nodeList)

        if (isDebugType() ||
            (accControl?._taskBean?.showNodeTip ?: app().memoryConfigBean.showNodeTip)
        ) {
            doMain {
                AccTouchTipLayer().apply {
                    lineColor = cColor
                    showRect(nodeList?.toRect())
                }
            }
        }
    }

    override fun touch(x1: Float, y1: Float, x2: Float?, y2: Float?) {
        super.touch(x1, y1, x2, y2)
        if (isDebugType() ||
            (accControl?._taskBean?.showTouchTip ?: app().memoryConfigBean.showTouchTip)
        ) {
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

/**写入acc log 到文件*/
fun Any.saveAccLog() = DslFileHelper.write(AppAccLog.accLogFolderName, data = this.toString())

fun Any.saveAccRunLog() =
    DslFileHelper.write(
        AppAccLog.accLogFolderName,
        AppAccLog.accLogFileName,
        data = this.toString()
    )