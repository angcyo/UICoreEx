package com.angcyo.tim.dslitem

import android.view.ViewGroup
import com.angcyo.dsladapter.isItemDetached
import com.angcyo.dsladapter.isUpdateMedia
import com.angcyo.glide.clear
import com.angcyo.glide.giv
import com.angcyo.library.ex.toElapsedTime
import com.angcyo.library.ex.toMillisecond
import com.angcyo.pager.dslSinglePager
import com.angcyo.tim.R
import com.angcyo.tim.bean.MessageInfoBean
import com.angcyo.tim.bean.imagePath
import com.angcyo.tim.bean.msgType
import com.angcyo.tim.bean.videoElem
import com.angcyo.tim.helper.ChatDownloadHelper
import com.angcyo.tim.util.TimConfig.IMAGE_MESSAGE_DEFAULT_MAX_SIZE
import com.angcyo.widget.DslViewHolder
import com.tencent.imsdk.v2.V2TIMImageElem
import com.tencent.imsdk.v2.V2TIMMessage

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
        clearMsgBackground(itemHolder)
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
    open fun bindImageContent(
        itemHolder: DslViewHolder,
        itemPosition: Int,
        payloads: List<Any>,
        bean: MessageInfoBean
    ) {
        //image
        itemHolder.gone(R.id.msg_video_play_view)
        itemHolder.gone(R.id.msg_video_duration_view)

        //下载缩略图
        ChatDownloadHelper.downloadImage(
            bean.message?.imageElem,
            V2TIMImageElem.V2TIM_IMAGE_TYPE_THUMB,
            bean,
            this,
            null
        )

        itemHolder.click(R.id.msg_content_layout) {
            //消息内容点击
            ChatDownloadHelper.downloadImage(
                bean.message?.imageElem,
                V2TIMImageElem.V2TIM_IMAGE_TYPE_ORIGIN,
                bean,
                this,
                null
            )
            itemFragment?.dslSinglePager(itemHolder.view(R.id.msg_image_view), bean.dataUri)
        }
    }

    /**绑定视频消息内容*/
    open fun bindVideoContent(
        itemHolder: DslViewHolder,
        itemPosition: Int,
        payloads: List<Any>,
        bean: MessageInfoBean
    ) {
        //video
        itemHolder.visible(R.id.msg_video_play_view)
        itemHolder.visible(R.id.msg_video_duration_view)

        val videoElem = bean.message?.videoElem
        itemHolder.tv(R.id.msg_video_duration_view)?.text =
            videoElem?.duration?.toMillisecond()
                ?.toElapsedTime(units = arrayOf("", "", ":", ":", ":"))

        //下载视频快照
        ChatDownloadHelper.downloadSnapshot(bean.videoElem, bean, this)

        //下载中提示
        itemHolder.visible(
            R.id.msg_sending_view,
            bean.status == MessageInfoBean.MSG_STATUS_SENDING ||
                    bean.downloadStatus == MessageInfoBean.MSG_STATUS_DOWNLOADING
        )

        itemHolder.click(R.id.msg_content_layout) {
            //消息内容点击, 先下载视频, 再播放
            if (bean.downloadStatus == MessageInfoBean.MSG_STATUS_DOWNLOADED) {
                //文本下载完成
                itemFragment?.dslSinglePager(itemHolder.view(R.id.msg_image_view), bean.dataUri)
            } else if (bean.downloadStatus == MessageInfoBean.MSG_STATUS_DOWNLOADING) {
                //下载中
            } else {
                //开始下载
                ChatDownloadHelper.downloadVideo(videoElem, bean, this) { progress, error ->
                    if (progress == ChatDownloadHelper.DOWNLOAD_SUCCESS && !isItemDetached()) {
                        itemFragment?.dslSinglePager(
                            itemHolder.view(R.id.msg_image_view),
                            bean.dataUri
                        )
                    }
                }
            }
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
}