package com.angcyo.acc2.app.helper

import android.content.Context
import com.angcyo.acc2.app.AppAccPrint
import com.angcyo.acc2.app.app
import com.angcyo.core.CoreApplication.Companion.DEFAULT_FILE_PRINT_PATH
import com.angcyo.core.component.GitModel
import com.angcyo.core.component.addGistFile
import com.angcyo.core.component.file.DslFileHelper
import com.angcyo.core.component.file.wrapData
import com.angcyo.core.component.gistBodyBuilder
import com.angcyo.core.vmApp
import com.angcyo.dialog.itemsDialog
import com.angcyo.library.ex.*
import com.angcyo.library.utils.*
import java.io.File

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
                AppAccPrint.accLogFolderName,
                LOG_TASK_FILE_NAME,
                data.wrapData()
            )
        }
    }

    fun taskLogPath() = FileUtils.appRootExternalFolderFile(
        folder = AppAccPrint.accLogFolderName,
        name = LOG_TASK_FILE_NAME
    ).absolutePath

    /**读取日志文本*/
    fun readTaskLog(): String? {
        return taskLogPath().file().readText()
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
                    AppAccPrint.logCatchPath().file().apply {
                        uploadFileToGist("catch.log", this)
                        shareFile(it.context, toast = true)
                    }
                    _dialog?.dismiss()
                }
            }

            addDialogItem {
                //itemTextGravity = Gravity.LEFT or Gravity.CENTER_VERTICAL
                itemText = "分享acc日志"
                itemClick = {
                    AppAccPrint.logPath().file().apply {
                        uploadFileToGist("acc.log", this)
                        shareFile(it.context, toast = true)
                    }
                    _dialog?.dismiss()
                }
            }

            addDialogItem {
                //itemTextGravity = Gravity.LEFT or Gravity.CENTER_VERTICAL
                itemText = "分享http日志"
                itemClick = {
                    logFileName().toLogFilePath(Constant.HTTP_FOLDER_NAME).file().apply {
                        uploadFileToGist("http.log", this)
                        shareFile(it.context, toast = true)
                    }
                    _dialog?.dismiss()
                }
            }

            addDialogItem {
                //itemTextGravity = Gravity.LEFT or Gravity.CENTER_VERTICAL
                itemText = "分享log日志"
                itemClick = {
                    DEFAULT_FILE_PRINT_PATH?.file()?.apply {
                        uploadFileToGist(LogFile.l, this)
                        shareFile(it.context, toast = true)
                    }
                    _dialog?.dismiss()
                }
            }

            if (LOG_TASK_FILE_NAME.isNotEmpty()) {
                addDialogItem {
                    //itemTextGravity = Gravity.LEFT or Gravity.CENTER_VERTICAL
                    itemText = "分享task日志"
                    itemClick = {
                        taskLogPath()?.file()?.apply {
                            uploadFileToGist("task.log", this)
                            shareFile(it.context, toast = true)
                        }
                        _dialog?.dismiss()
                    }
                }
            }
        }
    }

    /**上传数据到gist*/
    fun uploadFileToGist(des: String, file: File) {
        val limit = app().memoryConfigBean.uploadLogLineLimit
        val log = file.readTextLastLines(limit)?.ifEmpty { "no data!" } ?: "no data!"

        val info = buildString {
            //屏幕信息, 设备信息
            app().let {
                Device.screenInfo(it, this)
                appendln()
                Device.deviceInfo(it, this)
            }
        }

        vmApp<GitModel>().postGist(gistBodyBuilder("${nowTimeString()}/${Device.androidId}/$des") {
            addGistFile("device info", info)
            addGistFile(des, log)
        })
    }
}