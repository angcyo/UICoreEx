package com.angcyo.acc2.app

import android.widget.EditText
import com.angcyo.acc2.app.dslitem.shareApk
import com.angcyo.acc2.app.helper.LogHelper
import com.angcyo.library.ex.file
import com.angcyo.library.ex.shareFile

/**
 *
 * Email:angcyo@126.com
 * @author angcyo
 * @date 2021/12/20
 * Copyright (c) 2020 ShenZhen Wayto Ltd. All rights reserved.
 */
object AccDebug {

    /**调试:文本输入框文本改变时
     *
     * [com.angcyo.core.Debug.onDebugTextChanged]
     * */
    fun onAccDebugTextChanged(
        editText: EditText?,
        text: CharSequence?,
        start: Int,
        lengthBefore: Int,
        lengthAfter: Int
    ) {
        when (text?.toString()?.lowercase()) {
            //分享acc日志文件
            "cmd:share:acc", "9._1" -> {
                val file = AppAccPrint.logPath().file()
                file.shareFile()
            }
            //分享catch日志文件
            "cmd:share:catch", "9._2" -> {
                val file = AppAccPrint.logCatchPath().file()
                file.shareFile()
            }
            //分享Apk对话框
            "cmd:share:apk", "9._8" -> {
                editText?.context?.shareApk()
            }
            //分享日志对话框
            "cmd:share:dialog", "9._9" -> {
                editText?.context?.let {
                    LogHelper.showLogShareDialog(it)
                }
            }
        }
    }
}