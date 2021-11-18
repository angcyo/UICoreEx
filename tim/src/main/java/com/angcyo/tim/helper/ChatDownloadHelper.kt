package com.angcyo.tim.helper

import com.angcyo.dsladapter.DslAdapterItem
import com.angcyo.dsladapter.mediaPayload
import com.angcyo.library.L
import com.angcyo.library.ex.isFileExist
import com.angcyo.tim.TimSdkException
import com.angcyo.tim.bean.MessageInfoBean
import com.angcyo.tim.bean.MessageInfoBean.Companion.MSG_STATUS_DOWNLOADED
import com.angcyo.tim.bean.MessageInfoBean.Companion.MSG_STATUS_DOWNLOADING
import com.angcyo.tim.bean.MessageInfoBean.Companion.MSG_STATUS_DOWNLOAD_FAIL
import com.angcyo.tim.bean.MessageInfoBean.Companion.MSG_STATUS_UN_DOWNLOAD
import com.angcyo.tim.dslitem.BaseChatMsgItem
import com.angcyo.tim.helper.ChatDownloadHelper.DOWNLOAD_SUCCESS
import com.angcyo.tim.util.TimConfig
import com.tencent.imsdk.v2.*

/**
 * 下载助手
 * Email:angcyo@126.com
 * @author angcyo
 * @date 2021/11/18
 * Copyright (c) 2020 ShenZhen Wayto Ltd. All rights reserved.
 */

/**下载回调
 * [progress]进度为-100时, 表示下载完成*/
typealias DownloadAction = (progress: Int, error: TimSdkException?) -> Unit

object ChatDownloadHelper {

    /**下载成功*/
    const val DOWNLOAD_SUCCESS = -100

    val downloadElementList = mutableListOf<String>()

    //<editor-fold desc="下载">

    /**下载语音文件*/
    fun downloadSound(
        element: V2TIMSoundElem?,
        bean: MessageInfoBean?,
        item: BaseChatMsgItem?,
        action: DownloadAction? = null
    ) {
        if (element == null) {
            action?.invoke(-1, TimSdkException(-1, "无效的数据"))
            return
        }

        val uuid = element.uuid
        if (uuid.isNullOrEmpty()) {
            action?.invoke(-1, TimSdkException(-1, "无效的数据"))
            return
        }
        if (downloadElementList.contains(uuid)) {
            return
        }

        val path: String = TimConfig.getRecordDownloadDir(uuid)
        if (path.isFileExist()) {
            notifyDownloadSuccess(bean, item, path)
            action?.invoke(DOWNLOAD_SUCCESS, null)
        } else {
            downloadElementList.add(uuid)
            element.downloadSound(path, DownloadListener(uuid, path, bean, item, action))
        }
    }

    /**下载指定类型的图片
     * [type] <0 下载所有类型*/
    fun downloadImage(
        element: V2TIMImageElem?,
        type: Int,
        bean: MessageInfoBean?,
        item: BaseChatMsgItem?,
        action: DownloadAction? = null
    ) {
        if (element == null) {
            action?.invoke(-1, TimSdkException(-1, "无效的数据"))
            return
        }
        val images = element.imageList

        //下载图片
        for (image in images) {
            if (type < 0) {
                //下载所有
                downloadImage(image, bean, item, action)
            } else if (type == image.type) {
                //需要下载的图片类型
                downloadImage(image, bean, item, action)
            }
        }
    }

    fun downloadImage(
        image: V2TIMImageElem.V2TIMImage,
        bean: MessageInfoBean?,
        item: BaseChatMsgItem?,
        action: DownloadAction? = null
    ) {
        val uuid = image.uuid
        if (uuid.isNullOrEmpty()) {
            action?.invoke(-1, TimSdkException(-1, "无效的数据"))
            return
        }
        if (downloadElementList.contains(uuid)) {
            return
        }

        val path: String = TimConfig.generateImagePath(uuid, image.type)
        if (path.isFileExist()) {
            if (image.type == V2TIMImageElem.V2TIM_IMAGE_TYPE_ORIGIN) {
                bean?.dataUri = path
            }
            if (image.type == V2TIMImageElem.V2TIM_IMAGE_TYPE_THUMB) {
                notifyImageDownloadSuccess(bean, item, path)
            }
            action?.invoke(DOWNLOAD_SUCCESS, null)
        } else {
            downloadElementList.add(uuid)
            image.downloadImage(path, object : DownloadListener(uuid, path, bean, item, action) {
                override fun onSuccess() {
                    //super.onSuccess()
                    if (image.type == V2TIMImageElem.V2TIM_IMAGE_TYPE_ORIGIN) {
                        bean?.dataUri = path
                    }
                    if (image.type == V2TIMImageElem.V2TIM_IMAGE_TYPE_THUMB) {
                        notifyImageDownloadSuccess(bean, item, path)
                    }
                    action?.invoke(DOWNLOAD_SUCCESS, null)
                }
            })
        }
    }

    /**下载视频缩略图*/
    fun downloadSnapshot(
        element: V2TIMVideoElem?,
        bean: MessageInfoBean?,
        item: BaseChatMsgItem?,
        action: DownloadAction? = null
    ) {
        if (element == null) {
            action?.invoke(-1, TimSdkException(-1, "无效的数据"))
            return
        }

        val uuid = element.snapshotUUID
        if (uuid.isNullOrEmpty()) {
            action?.invoke(-1, TimSdkException(-1, "无效的数据"))
            return
        }
        if (downloadElementList.contains(uuid)) {
            return
        }

        val path: String = TimConfig.getImageDownloadDir(uuid)
        if (path.isFileExist()) {
            notifyImageDownloadSuccess(bean, item, path)
            action?.invoke(DOWNLOAD_SUCCESS, null)
        } else {
            downloadElementList.add(uuid)
            element.downloadSnapshot(
                path,
                object : DownloadListener(uuid, path, bean, item, action) {
                    override fun onSuccess() {
                        //super.onSuccess()
                        notifyImageDownloadSuccess(bean, item, path)
                        //缩略图不更改视频下载状态
                        bean?.downloadStatus = MSG_STATUS_UN_DOWNLOAD
                        action?.invoke(DOWNLOAD_SUCCESS, null)
                    }
                })
        }
    }

    /**下载视频*/
    fun downloadVideo(
        element: V2TIMVideoElem?,
        bean: MessageInfoBean?,
        item: BaseChatMsgItem?,
        action: DownloadAction? = null
    ) {
        if (element == null) {
            action?.invoke(-1, TimSdkException(-1, "无效的数据"))
            return
        }

        val uuid = element.videoUUID
        if (uuid.isNullOrEmpty()) {
            action?.invoke(-1, TimSdkException(-1, "无效的数据"))
            return
        }
        if (downloadElementList.contains(uuid)) {
            return
        }

        val path: String = TimConfig.getVideoDownloadDir(uuid)
        if (path.isFileExist()) {
            notifyDownloadSuccess(bean, item, path)
            action?.invoke(DOWNLOAD_SUCCESS, null)
        } else {
            downloadElementList.add(uuid)
            element.downloadVideo(path, DownloadListener(uuid, path, bean, item, action))
        }
    }

    /**下载文件*/
    fun downloadFile(
        element: V2TIMFileElem?,
        bean: MessageInfoBean?,
        item: BaseChatMsgItem?,
        action: DownloadAction? = null
    ) {
        if (element == null) {
            action?.invoke(-1, TimSdkException(-1, "无效的数据"))
            return
        }

        val uuid = element.uuid
        if (uuid.isNullOrEmpty()) {
            action?.invoke(-1, TimSdkException(-1, "无效的数据"))
            return
        }
        if (downloadElementList.contains(uuid)) {
            return
        }

        val path: String = TimConfig.getFileDownloadDir(uuid)
        if (path.isFileExist()) {
            notifyDownloadSuccess(bean, item, path)
            action?.invoke(DOWNLOAD_SUCCESS, null)
        } else {
            downloadElementList.add(uuid)
            element.downloadFile(path, DownloadListener(uuid, path, bean, item, action))
        }
    }

    //</editor-fold desc="下载">

    //<editor-fold desc="通知">

    fun notifyDownloadSuccess(
        bean: MessageInfoBean?,
        item: BaseChatMsgItem?,
        path: String,
        payload: Any? = DslAdapterItem.PAYLOAD_UPDATE_PART
    ) {
        bean?.apply {
            dataUri = path
            if (downloadStatus != MSG_STATUS_DOWNLOADED) {
                //下载完成
                downloadStatus = MSG_STATUS_DOWNLOADED
                item?.updateAdapterItem(payload)
            }
        }
    }

    /**通知图片下载成功*/
    fun notifyImageDownloadSuccess(
        bean: MessageInfoBean?,
        item: BaseChatMsgItem?,
        path: String,
        payload: Any? = mediaPayload()
    ) {
        bean?.apply {
            dataPath = path
            if (downloadStatus != MSG_STATUS_DOWNLOADED) {
                //下载完成
                downloadStatus = MSG_STATUS_DOWNLOADED
                item?.updateAdapterItem(payload)
            }
        }
    }

    fun notifyDownloadError(bean: MessageInfoBean?, item: BaseChatMsgItem?) {
        bean?.apply {
            if (downloadStatus != MSG_STATUS_DOWNLOAD_FAIL) {
                //下载完成
                downloadStatus = MSG_STATUS_DOWNLOAD_FAIL
                item?.updateAdapterItem()
            }
        }
    }

    fun notifyDownloadProgress(
        bean: MessageInfoBean?,
        item: BaseChatMsgItem?,
        progressInfo: V2TIMElem.V2ProgressInfo
    ) {
        bean?.apply {
            //下载进度
            downloadStatus = MSG_STATUS_DOWNLOADING
            downloadProgress = progressInfo.progress
            item?.updateAdapterItem()
        }
    }

    //</editor-fold desc="通知">
}

open class DownloadListener(
    val uuid: String,
    val path: String,
    val bean: MessageInfoBean?,
    val item: BaseChatMsgItem?,
    val action: DownloadAction? = null
) : V2TIMDownloadCallback {

    override fun onSuccess() {
        L.i("下载成功:$path")
        ChatDownloadHelper.downloadElementList.remove(uuid)
        ChatDownloadHelper.notifyDownloadSuccess(bean, item, path)
        action?.invoke(DOWNLOAD_SUCCESS, null)
    }

    override fun onError(code: Int, desc: String?) {
        L.w("下载失败:$path $code:$desc")
        ChatDownloadHelper.downloadElementList.remove(uuid)
        ChatDownloadHelper.notifyDownloadError(bean, item)
        action?.invoke(-1, TimSdkException(code, desc))
    }

    override fun onProgress(progressInfo: V2TIMElem.V2ProgressInfo) {
        L.d("下载中:$path ${progressInfo.currentSize}/${progressInfo.totalSize}")
        ChatDownloadHelper.notifyDownloadProgress(bean, item, progressInfo)
        action?.invoke(progressInfo.progress, null)
    }
}

/**进度[0-100]*/
val V2TIMElem.V2ProgressInfo.progress: Int
    get() {
        val currentSize = currentSize
        val totalSize = totalSize

        var progress = 0
        if (totalSize > 0) {
            progress = (100 * currentSize / totalSize).toInt()
        }
        if (progress > 100) {
            progress = 100
        }
        return progress
    }