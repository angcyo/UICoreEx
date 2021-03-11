package com.angcyo.acc2.app.helper

import android.content.Context
import com.angcyo.acc2.app.AppAccLog
import com.angcyo.dialog.itemsDialog
import com.angcyo.library.L
import com.angcyo.library.ex.file
import com.angcyo.library.ex.shareFile
import com.angcyo.library.utils.Constant
import com.angcyo.library.utils.logFilePath

/**
 *
 * Email:angcyo@126.com
 * @author angcyo
 * @date 2021/02/21
 * Copyright (c) 2020 angcyo. All rights reserved.
 */
object LogHelper {

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
        }
    }
}