package com.angcyo.acc2.app.component

import android.content.Context
import androidx.appcompat.app.AlertDialog
import com.angcyo.acc2.core.AccPermission
import com.angcyo.library.ex.toApplicationDetailsSettings
import ezy.assist.compat.SettingsCompat

/**
 *
 * Email:angcyo@126.com
 * @author angcyo
 * @date 2021/01/29
 * Copyright (c) 2020 ShenZhen Wayto Ltd. All rights reserved.
 */
object Acc {

    //无障碍弹窗提示
    var isShowAccessibilityDialog = false

    //浮窗弹窗提示
    var isShowOverlaysDialog = false

    /**跳转浮窗权限界面*/
    fun toDrawOverlays(context: Context) {
        try {
            SettingsCompat.manageDrawOverlays(context)
            AccOpenTip.tip("请打开悬浮窗权限")
        } catch (e: Exception) {
            //Tip.tip("没有找到对应的程序.")
            context.toApplicationDetailsSettings()
        }
    }

    /**跳转无障碍权限界面*/
    fun toAccessibilityActivity(context: Context) {
        AccPermission.openAccessibilityActivity(context)
        AccOpenTip.show()
    }

    /**权限通过 返回 true
     * [confirm] 是否需要弹窗手动确认
     * @return [true] 权限通过
     * */
    fun check(context: Context, confirm: Boolean = true): Boolean {
        //优先检查浮窗权限
        if (!SettingsCompat.canDrawOverlays(context)) {
            if (confirm) {
                if (!isShowOverlaysDialog) {
                    AlertDialog.Builder(context)
                        .setCancelable(false)
                        .setTitle("权限提示")
                        .setMessage("请打开\"悬浮窗\"权限.")
                        .setOnDismissListener {
                            isShowOverlaysDialog = false
                        }
                        .setPositiveButton("去打开") { dialog, which ->
                            toDrawOverlays(context)
                        }
                        .show()
                    isShowOverlaysDialog = true
                }
            } else {
                toDrawOverlays(context)
            }
            return false
        }

        //检查无障碍权限
        if (!AccPermission.isServiceEnabled(context)) {
            if (confirm) {
                if (!isShowAccessibilityDialog) {
                    AlertDialog.Builder(context)
                        .setCancelable(false)
                        .setTitle("权限提示")
                        .setMessage("请打开\"无障碍服务\".")
                        .setOnDismissListener {
                            isShowAccessibilityDialog = false
                        }
                        .setPositiveButton("去打开") { dialog, which ->
                            toAccessibilityActivity(context)
                        }
                        .show()
                    isShowAccessibilityDialog = true
                }
            } else {
                toAccessibilityActivity(context)
            }
            return false
        }

        return true
    }
}