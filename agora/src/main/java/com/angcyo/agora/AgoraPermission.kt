package com.angcyo.agora

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

/**
 * 声网必要权限检查权限
 * Email:angcyo@126.com
 * @author angcyo
 * @date 2020/02/06
 */
object AgoraPermission {

    const val PERMISSION_REQ_ID = 22

    val REQUESTED_PERMISSIONS = arrayOf(
        Manifest.permission.RECORD_AUDIO,
        Manifest.permission.CAMERA,
        Manifest.permission.WRITE_EXTERNAL_STORAGE
    )

    /**检查是否具备权限, 没权限则请求*/
    fun requestCheckPermission(context: Activity): Boolean {
        return if (havePermission(context)) {
            true
        } else {
            ActivityCompat.requestPermissions(
                context,
                REQUESTED_PERMISSIONS,
                PERMISSION_REQ_ID
            )
            false
        }
    }

    fun havePermission(context: Context): Boolean {
        if (ContextCompat.checkSelfPermission(
                context,
                REQUESTED_PERMISSIONS[0]
            ) != PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(
                context,
                REQUESTED_PERMISSIONS[1]
            ) != PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(
                context,
                REQUESTED_PERMISSIONS[2]
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return false
        }
        return true
    }
}