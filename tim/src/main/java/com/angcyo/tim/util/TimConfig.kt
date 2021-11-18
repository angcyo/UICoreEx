package com.angcyo.tim.util

import com.angcyo.library._screenWidth
import com.angcyo.library.ex.dpi
import com.angcyo.library.utils.folderPath

/**
 *
 * Email:angcyo@126.com
 * @author angcyo
 * @date 2021/11/10
 * Copyright (c) 2020 ShenZhen Wayto Ltd. All rights reserved.
 */
object TimConfig {
    const val RECORD_DIR_SUFFIX = "/tim/record/"
    const val RECORD_DOWNLOAD_DIR_SUFFIX = "/tim/record/download/"
    const val VIDEO_DOWNLOAD_DIR_SUFFIX = "/tim/video/download/"
    const val IMAGE_BASE_DIR_SUFFIX = "/tim/image/"
    const val IMAGE_DOWNLOAD_DIR_SUFFIX = "/tim/image/download/"
    const val MEDIA_DIR_SUFFIX = "/tim/media/"
    const val FILE_DOWNLOAD_DIR_SUFFIX = "/tim/file/download/"
    const val CRASH_LOG_DIR_SUFFIX = "/tim/crash/"

    /**显示消息时间的时间间隔 5分钟
     * 距离上一条消息5分钟时, 就显示消息时间*/
    const val SHOW_MESSAGE_TIME_INTERVAL = 5 * 60 * 1000

    /**图片消息显示的最大大小*/
    val IMAGE_MESSAGE_DEFAULT_MAX_SIZE = _screenWidth * 2 / 3

    /**音频消息, 最小/最大宽度*/
    val AUDIO_MESSAGE_MIN_WIDTH: Int = 60 * dpi
    val AUDIO_MESSAGE_MAX_WIDTH: Int = 250 * dpi

    /**视频录制/选择最大时长, 毫秒*/
    val MAX_VIDEO_DURATION = 30 * 1000L
    val MIN_VIDEO_DURATION = 3 * 1000L

    /**音频录制/选择最大时长, 秒*/
    val MAX_AUDIO_DURATION = 60L
    val MIN_AUDIO_DURATION = 3L

    /**拉取消息的数量*/
    const val MSG_PAGE_COUNT = 20

    // 消息已读上报时间间隔
    val READ_REPORT_INTERVAL = 1000 // 单位： 毫秒

    /**
     * 根据图片 UUID 和 类型得到图片文件路径
     * @param uuid 图片 UUID
     * @param imageType 图片类型 V2TIMImageElem.V2TIM_IMAGE_TYPE_THUMB , V2TIMImageElem.V2TIM_IMAGE_TYPE_ORIGIN ,
     * V2TIMImageElem.V2TIM_IMAGE_TYPE_LARGE
     * @return 图片文件路径
     */
    fun generateImagePath(uuid: String?, imageType: Int): String {
        return folderPath(IMAGE_DOWNLOAD_DIR_SUFFIX) + uuid + "_" + imageType
    }

    fun getRecordDownloadDir(uuid: String): String {
        return folderPath(RECORD_DOWNLOAD_DIR_SUFFIX) + uuid
    }

    fun getVideoDownloadDir(uuid: String): String {
        return folderPath(VIDEO_DOWNLOAD_DIR_SUFFIX) + uuid
    }

    fun getImageDownloadDir(uuid: String): String {
        return folderPath(IMAGE_DOWNLOAD_DIR_SUFFIX) + uuid
    }

    fun getFileDownloadDir(uuid: String): String {
        return folderPath(FILE_DOWNLOAD_DIR_SUFFIX) + uuid
    }

    fun covert2HTMLString(original: String): String {
        return "\"<font color=\"#5B6B92\">$original</font>\""
    }

}