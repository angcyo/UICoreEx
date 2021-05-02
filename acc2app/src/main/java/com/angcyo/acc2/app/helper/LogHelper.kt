package com.angcyo.acc2.app.helper

import android.content.Context
import com.angcyo.acc2.app.AppAccLog
import com.angcyo.core.component.file.DslFileHelper
import com.angcyo.core.component.file.wrapData
import com.angcyo.dialog.itemsDialog
import com.angcyo.library.L
import com.angcyo.library.ex.file
import com.angcyo.library.ex.readText
import com.angcyo.library.ex.shareFile
import com.angcyo.library.utils.Constant
import com.angcyo.library.utils.FileUtils
import com.angcyo.library.utils.logFilePath

/**
 *
 * Email:angcyo@126.com
 * @author angcyo
 * @date 2021/02/21
 * Copyright (c) 2020 angcyo. All rights reserved.
 */
object LogHelper {

    var LOG_TASK_FILE_NAME = "task.log"

    /**执行任务时的日志输出*/
    fun taskLog(data: CharSequence?) {
        if (!data.isNullOrEmpty()) {
            DslFileHelper.write(
                AppAccLog.accLogFolderName,
                LOG_TASK_FILE_NAME,
                data.wrapData()
            )
        }
    }

    fun taskLogPath() = FileUtils.appRootExternalFolderFile(
        folder = AppAccLog.accLogFolderName,
        name = LOG_TASK_FILE_NAME
    )?.absolutePath

    /**读取日志文本*/
    fun readTaskLog(): String? {
        return taskLogPath()?.file()?.readText()
    }

    /**显示日志分享对话框*/
    fun showLogShareDialog(context: Context) {
        //日志菜单
        context.itemsDialog {
            canceledOnTouchOutside = true
            //dialogBottomCancelItem = null

            addDialogItem {
                //itemTextGravity = Gravity.LEFT or Gravity.CENTER_VERTICAL
                itemText = "分享catch日志"
                itemClick = {
                    AppAccLog.logCatchPath()?.file()?.shareFile(it.context, toast = true)
                    _dialog?.dismiss()
                }
            }

            addDialogItem {
                //itemTextGravity = Gravity.LEFT or Gravity.CENTER_VERTICAL
                itemText = "分享acc日志"
                itemClick = {
                    AppAccLog.logPath()?.file()?.shareFile(it.context, toast = true)
                    _dialog?.dismiss()
                }
            }

            addDialogItem {
                //itemTextGravity = Gravity.LEFT or Gravity.CENTER_VERTICAL
                itemText = "分享http日志"
                itemClick = {
                    Constant.HTTP_FOLDER_NAME.logFilePath()?.file()
                        ?.shareFile(it.context, toast = true)
                    _dialog?.dismiss()
                }
            }

            addDialogItem {
                //itemTextGravity = Gravity.LEFT or Gravity.CENTER_VERTICAL
                itemText = "分享log日志"
                itemClick = {
                    L.DEFAULT_FILE_PRINT_PATH?.file()?.shareFile(it.context, toast = true)
                    _dialog?.dismiss()
                }
            }

            if (LOG_TASK_FILE_NAME.isNotEmpty()) {
                addDialogItem {
                    //itemTextGravity = Gravity.LEFT or Gravity.CENTER_VERTICAL
                    itemText = "分享task日志"
                    itemClick = {
                        taskLogPath()?.file()?.shareFile(it.context, toast = true)
                        _dialog?.dismiss()
                    }
                }
            }
        }
    }
}