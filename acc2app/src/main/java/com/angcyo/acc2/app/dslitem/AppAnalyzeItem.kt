package com.angcyo.acc2.app.dslitem

import android.content.Context
import android.view.Gravity
import android.widget.TextView
import com.angcyo.acc2.app.Acc2App
import com.angcyo.acc2.app.AppAccLog
import com.angcyo.acc2.app.R
import com.angcyo.acc2.app.app
import com.angcyo.acc2.app.model.AdaptiveModel
import com.angcyo.acc2.app.model.allApp
import com.angcyo.acc2.app.model.isAdaptive
import com.angcyo.core.component.HttpConfigDialog
import com.angcyo.core.vmApp
import com.angcyo.dialog.itemsDialog
import com.angcyo.dsladapter.DslAdapterItem
import com.angcyo.http.rx.runRx
import com.angcyo.library.component.appBean
import com.angcyo.library.ex.*
import com.angcyo.library.utils.Constant
import com.angcyo.library.utils.Device
import com.angcyo.library.utils.logFilePath
import com.angcyo.widget.DslViewHolder
import com.angcyo.widget.span.span
import kotlin.math.max

/**
 * 应用分析item
 * Email:angcyo@126.com
 * @author angcyo
 * @date 2020/07/30
 * Copyright (c) 2020 ShenZhen Wayto Ltd. All rights reserved.
 */

class AppAnalyzeItem : DslAdapterItem() {

    companion object {
        val MAX_TEXT_LENGTH: Int
            get() = 30 * 1024

        val MAX_TEXT_LINES: Int
            get() = 800
    }

    var taskLog: CharSequence? = null
    var httpLog: CharSequence? = null

    init {
        itemLayoutId = R.layout.app_analyze_item
    }

    var _loadFilePath: String? = null

    override fun onItemBind(
        itemHolder: DslViewHolder,
        itemPosition: Int,
        adapterItem: DslAdapterItem,
        payloads: List<Any>
    ) {
        super.onItemBind(itemHolder, itemPosition, adapterItem, payloads)

        //任务日志
        itemHolder.throttleClick(R.id.task_log_button) {
            _loadFilePath = AppAccLog.logPath()

            if (taskLog == null) {
                runRx({
                    taskLog = loadLog(_loadFilePath)
                }) {
                    itemHolder.tv(R.id.lib_text_view)?.text = taskLog.or()
                }
            }
            itemHolder.tv(R.id.lib_text_view)?.text = taskLog.or("loading...")
        }

        //网络日志
        itemHolder.throttleClick(R.id.http_log_button) {
            _loadFilePath = Constant.HTTP_FOLDER_NAME.logFilePath()

            if (httpLog == null) {
                runRx({
                    httpLog = loadLog(_loadFilePath)
                }) {
                    itemHolder.tv(R.id.lib_text_view)?.text = httpLog.or()
                }
            }
            itemHolder.tv(R.id.lib_text_view)?.text = httpLog.or("loading...")
        }

        //日志菜单
        itemHolder.throttleClick(R.id.lib_text_view) { textView ->
            textView.context.itemsDialog {
                canceledOnTouchOutside = true
                dialogBottomCancelItem = null

                addDialogItem {
                    itemTextGravity = Gravity.LEFT or Gravity.CENTER_VERTICAL
                    itemText = span {
                        drawable {
                            backgroundDrawable =
                                _drawable(R.drawable.ic_file_share)
                        }
                        append(" 分享日志")
                    }
                    itemClick = {
                        _loadFilePath?.file()?.shareFile(it.context)
                        _dialog?.dismiss()
                    }
                }

                addDialogItem {
                    itemTextGravity = Gravity.LEFT or Gravity.CENTER_VERTICAL
                    itemText = span {
                        drawable {
                            backgroundDrawable =
                                _drawable(R.drawable.lib_ic_copy)
                        }
                        append(" 复制日志")
                    }
                    itemClick = {
                        (textView as TextView?)?.text?.copy()
                        _dialog?.dismiss()
                    }
                }
            }
        }

        //默认点击
        if (_loadFilePath == null) {
            itemHolder.clickView(R.id.task_log_button)
        }

        //分享
        itemHolder.throttleClick(R.id.share_app_button) {
            itemHolder.context.shareApk()
        }
    }

    fun loadLog(filePath: String?): CharSequence {
        return span {
            appendLine(HttpConfigDialog.appBaseUrl)
            Device.buildString(this)
            appendLine("appAndroidId:${Acc2App.appAndroidId}")
            appendLine("androidId:${Device.androidId}")

            appendln()

            logApp(app().packageName)
            logAllApp()

            append(
                filePath?.file()?.readText()
                    ?.run {
                        if (length > MAX_TEXT_LENGTH) {
                            "...${substring(max(0, length - MAX_TEXT_LENGTH) /*3073760*/, length)}"
                        } else {
                            this
                        }
                    }
                    ?: filePath)
        }
    }
}

/**打印所有app信息*/
fun Appendable.logAllApp(logAdaptive: Boolean = true) {
    allApp.forEach {
        logApp(it.packageName, it.label, logAdaptive)
    }
}

/**打印app的一下信息*/
fun Appendable.logApp(packageName: String?, label: String? = "--", logAdaptive: Boolean = true) {
    packageName?.appBean()?.apply {
        append(appName)
        append(": ")
        append(versionName)
        append("(${versionCode})")

        if (logAdaptive) {

            //适配信息
            vmApp<AdaptiveModel>().adaptiveData.value?.it {
                it.data?.find { it.packageName == packageName }?.it {
                    appendLine()
                    append("适配版本:${it.versionNameList}")
                    appendLine()
                }
            }
        }
    }.elseNull {
        append("$label: 未安装")
    }
    appendLine()
}

fun Context.shareApk() {
    itemsDialog {
        canceledOnTouchOutside = true
        dialogTitle = "分享应用APK文件"

        val adaptiveModel = vmApp<AdaptiveModel>()
        allApp.sortedBy {
            if (it.packageName.isAdaptive()) {
                1
            } else {
                -1
            }
        }.forEach {
            it.packageName?.appBean()?.also { appBean ->
                addDialogItem {
                    itemText = "${appBean.appName}${appBean.versionName}(${appBean.versionCode})${
                        if (adaptiveModel.getAdaptiveInfo(appBean.packageName) == null) " ×" else ""
                    }"
                    itemClick = {
                        appBean.packageInfo.applicationInfo.sourceDir.file()
                            ?.shareFile(this@shareApk)

                        itemText?.copy()
                    }
                }
            }
        }
    }
}