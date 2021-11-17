package com.angcyo.tim.dslitem

import android.view.ViewGroup
import com.angcyo.dsladapter.isUpdateMedia
import com.angcyo.dsladapter.mediaPayload
import com.angcyo.glide.clear
import com.angcyo.glide.giv
import com.angcyo.library.L
import com.angcyo.library.ex.isFileExist
import com.angcyo.library.ex.toElapsedTime
import com.angcyo.library.ex.toMillisecond
import com.angcyo.pager.dslSinglePager
import com.angcyo.tim.R
import com.angcyo.tim.bean.*
import com.angcyo.tim.util.TimConfig.IMAGE_MESSAGE_DEFAULT_MAX_SIZE
import com.angcyo.tim.util.TimConfig.generateImagePath
import com.angcyo.tim.util.TimConfig.getImageDownloadDir
import com.angcyo.tim.util.TimConfig.getVideoDownloadDir
import com.angcyo.widget.DslViewHolder
import com.tencent.imsdk.v2.*

/**
 * 图片/视频 消息item
 * Email:angcyo@126.com
 * @author angcyo
 * @date 2021/11/16
 * Copyright (c) 2020 ShenZhen Wayto Ltd. All rights reserved.
 */
class MsgImageItem : BaseChatMsgItem() {

    init {
        msgContentLayoutId = R.layout.msg_iamge_layout
    }

    override fun bindMsgStyle(itemHolder: DslViewHolder, itemPosition: Int, payloads: List<Any>) {
        super.bindMsgStyle(itemHolder, itemPosition, payloads)
        //取消消息的背景
        itemHolder.view(R.id.msg_content_layout)?.background = null
    }

    override fun bindMsgContent(itemHolder: DslViewHolder, itemPosition: Int, payloads: List<Any>) {
        super.bindMsgContent(itemHolder, itemPosition, payloads)
        val mediaUpdate = payloads.isUpdateMedia()
        messageInfoBean?.let { bean ->
            val isVideoMessage = bean.msgType == V2TIMMessage.V2TIM_ELEM_TYPE_VIDEO

            //size
            itemHolder.view(R.id.msg_image_view)?.apply {
                layoutParams = getImageParams(layoutParams, bean)
            }

            if (mediaUpdate) {
                //图片
                val imagePath = bean.imagePath
                if (imagePath.isNullOrEmpty()) {
                    itemHolder.giv(R.id.msg_image_view)?.clear()
                } else {
                    itemHolder.giv(R.id.msg_image_view)?.load(imagePath)
                }
            }

            //bind
            if (isVideoMessage) {
                bindVideoContent(itemHolder, itemPosition, payloads, bean)
            } else {
                bindImageContent(itemHolder, itemPosition, payloads, bean)
            }
        }
    }

    /**绑定图片消息内容*/
    open fun bindImageContent(itemHolder: DslViewHolder, itemPosition: Int, payloads: List<Any>, bean: MessageInfoBean) {
        //image
        itemHolder.gone(R.id.msg_video_play_view)
        itemHolder.gone(R.id.msg_video_duration_view)

        downloadImage(bean)

        itemHolder.click(R.id.msg_content_layout) {
            //消息内容点击
            itemFragment?.dslSinglePager(itemHolder.view(R.id.msg_image_view), bean.dataUri)
        }
    }

    /**绑定视频消息内容*/
    open fun bindVideoContent(itemHolder: DslViewHolder, itemPosition: Int, payloads: List<Any>, bean: MessageInfoBean) {
        //video
        itemHolder.visible(R.id.msg_video_play_view)
        itemHolder.visible(R.id.msg_video_duration_view)

        val videoElem = bean.message?.videoElem
        itemHolder.tv(R.id.msg_video_duration_view)?.text =
                videoElem?.duration?.toMillisecond()
                        ?.toElapsedTime(units = arrayOf("", "", ":", ":", ":"))

        val imagePath = bean.imagePath
        if (imagePath.isNullOrEmpty()) {
            downloadVideoSnapshot(bean)
        }

        //下载视频
        downloadVideo(bean)

        itemHolder.click(R.id.msg_content_layout) {
            //消息内容点击
            itemFragment?.dslSinglePager(itemHolder.view(R.id.msg_image_view), bean.dataUri)
        }
    }

    fun getImageParams(
            params: ViewGroup.LayoutParams,
            msg: MessageInfoBean
    ): ViewGroup.LayoutParams {
        if (msg.imageWidth <= 0 || msg.imageHeight <= 0) {
            return params
        }
        if (msg.imageWidth > msg.imageHeight) {
            params.width = IMAGE_MESSAGE_DEFAULT_MAX_SIZE
            params.height = IMAGE_MESSAGE_DEFAULT_MAX_SIZE * msg.imageHeight / msg.imageWidth
        } else {
            params.width = IMAGE_MESSAGE_DEFAULT_MAX_SIZE * msg.imageWidth / msg.imageHeight
            params.height = IMAGE_MESSAGE_DEFAULT_MAX_SIZE
        }
        return params
    }

    val downloadElementList = mutableListOf<String>()

    /**下载图片原图和缩略图*/
    fun downloadImage(msg: MessageInfoBean) {
        val images = msg.imageList

        //下载图片
        for (image in images) {
            val uuid = image.uuid
            if (uuid.isNullOrEmpty() || downloadElementList.contains(uuid)) {
                continue
            }
            downloadElementList.add(uuid)
            val path: String = generateImagePath(uuid, image.type)
            if (!path.isFileExist()) {
                image.downloadImage(path, object : V2TIMDownloadCallback {
                    override fun onSuccess() {
                        //downloadElementList.remove(uuid)
                        if (image.type == V2TIMImageElem.V2TIM_IMAGE_TYPE_ORIGIN) {
                            msg.dataUri = path
                        } else {
                            msg.dataPath = path
                            updateAdapterItem(mediaPayload())
                        }
                    }

                    override fun onError(code: Int, desc: String?) {
                        L.w("下载失败:$code:$desc")
                        downloadElementList.remove(uuid)
                    }

                    override fun onProgress(progressInfo: V2TIMElem.V2ProgressInfo) {
                        L.i("图片下载中:${progressInfo.currentSize}/${progressInfo.totalSize}")
                    }
                })
            }
        }
    }

    /**下载视频快照*/
    fun downloadVideoSnapshot(msg: MessageInfoBean) {
        val videoElem: V2TIMVideoElem = msg.videoElem ?: return

        //下载视频快照图片
        val uuid = videoElem.snapshotUUID
        if (uuid.isNullOrEmpty()) {
            return
        }
        if (downloadElementList.contains(uuid)) {
            return
        }
        downloadElementList.add(uuid)
        val path: String = getImageDownloadDir(uuid)
        if (!path.isFileExist()) {
            videoElem.downloadSnapshot(path, object : V2TIMDownloadCallback {
                override fun onSuccess() {
                    //downloadElementList.remove(uuid)
                    msg.dataPath = path
                    updateAdapterItem(mediaPayload())
                }

                override fun onError(code: Int, desc: String?) {
                    L.w("下载失败:$code:$desc")
                    downloadElementList.remove(uuid)
                }

                override fun onProgress(progressInfo: V2TIMElem.V2ProgressInfo) {
                    L.i("图片下载中:${progressInfo.currentSize}/${progressInfo.totalSize}")
                }
            })
        }
    }

    /**下载视频*/
    fun downloadVideo(msg: MessageInfoBean) {
        val videoElem: V2TIMVideoElem = msg.videoElem ?: return

        //下载视频快照图片
        val uuid = videoElem.videoUUID
        if (uuid.isNullOrEmpty()) {
            return
        }
        if (downloadElementList.contains(uuid)) {
            return
        }
        downloadElementList.add(uuid)
        val path: String = getVideoDownloadDir(uuid)
        if (!path.isFileExist()) {
            videoElem.downloadVideo(path, object : V2TIMDownloadCallback {
                override fun onSuccess() {
                    //downloadElementList.remove(uuid)
                    msg.dataUri = path
                    updateAdapterItem(mediaPayload())
                }

                override fun onError(code: Int, desc: String?) {
                    L.w("下载失败:$code:$desc")
                    downloadElementList.remove(uuid)
                }

                override fun onProgress(progressInfo: V2TIMElem.V2ProgressInfo) {
                    L.i("图片下载中:${progressInfo.currentSize}/${progressInfo.totalSize}")
                }
            })
        }
    }
}