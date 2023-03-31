package com.angcyo.laserpacker.device

import com.angcyo.library.component.*
import com.angcyo.library.ex._string

/**
 * 雕刻通知管理
 * Email:angcyo@126.com
 * @author angcyo
 * @date 2022/12/09
 * Copyright (c) 2020 angcyo. All rights reserved.
 */
object EngraveNotifyHelper {

    /**通知的id*/
    const val ENGRAVE_NOTIFY_ID = 0x99

    /**通知通道*/
    const val ENGRAVE_NOTIFY_CHANNEL = "Engrave"

    /**创建雕刻通知通道*/
    fun createEngraveChannel() {
        DslNotify().apply {
            channelId = ENGRAVE_NOTIFY_CHANNEL
            channelName = _string(R.string.engrave_channel)
            _createNotifyChannel()
        }
    }

    /**显示雕刻通知*/
    fun showEngraveNotify(progress: Int = 0) {
        dslNotify {
            notifyId = ENGRAVE_NOTIFY_ID
            notifyProgress = progress
            channelId = ENGRAVE_NOTIFY_CHANNEL
            channelName = _string(R.string.engrave_channel)
            notifyOngoing = true
            low()
            single("${_string(R.string.engraving)}...${progress}%")
        }
    }

    /**取消通知*/
    fun hideEngraveNotify() {
        ENGRAVE_NOTIFY_ID.cancelNotify()
    }

    /**通知通道是否激活*/
    fun isChannelEnable() = ENGRAVE_NOTIFY_CHANNEL.isChannelEnable()

    /**打开通知开关设置界面*/
    fun openChannelSetting() = ENGRAVE_NOTIFY_CHANNEL.openNotificationChannelSetting()
}