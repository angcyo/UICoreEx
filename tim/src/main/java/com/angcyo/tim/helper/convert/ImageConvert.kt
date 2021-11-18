package com.angcyo.tim.helper.convert

import com.angcyo.library.ex.bitmapSize
import com.angcyo.library.ex.isFileExist
import com.angcyo.tim.bean.MessageInfoBean
import com.angcyo.tim.bean.isSelf
import com.angcyo.tim.dslitem.BaseChatMsgItem
import com.angcyo.tim.dslitem.MsgImageItem
import com.angcyo.tim.util.TimConfig
import com.tencent.imsdk.v2.V2TIMImageElem
import com.tencent.imsdk.v2.V2TIMMessage
import java.io.File

/**
 * 图片消息的转换
 * Email:angcyo@126.com
 * @author angcyo
 * @date 2021/11/18
 * Copyright (c) 2020 ShenZhen Wayto Ltd. All rights reserved.
 */
class ImageConvert : BaseConvert() {

    override fun handleMessage(message: V2TIMMessage): Boolean {
        return message.elemType == V2TIMMessage.V2TIM_ELEM_TYPE_IMAGE
    }

    override fun handleBean(bean: MessageInfoBean): Boolean {
        return bean.message?.elemType == V2TIMMessage.V2TIM_ELEM_TYPE_IMAGE
    }

    override fun convertToBean(message: V2TIMMessage): MessageInfoBean {
        return baseMessageInfoBean(message).apply {
            content = "[图片]"

            val imageEle: V2TIMImageElem = message.imageElem
            val localPath = imageEle.path //获取原图本地文件路径，只对消息发送方有效
            if (isSelf && localPath.isFileExist()) {
                //自己的消息优先使用本地图片
                dataPath = localPath
                dataUri = localPath
                val size: IntArray = localPath.bitmapSize()
                imageWidth = size[0]
                imageHeight = size[1]
                downloadStatus = MessageInfoBean.MSG_STATUS_DOWNLOADED
            } else {
                //远程消息使用缩略图
                val imageList = imageEle.imageList
                for (i in imageList.indices) {
                    val img = imageList[i]
                    if (img.type == V2TIMImageElem.V2TIM_IMAGE_TYPE_THUMB) {
                        //缩略图
                        val path: String = TimConfig.generateImagePath(
                            img.uuid,
                            V2TIMImageElem.V2TIM_IMAGE_TYPE_THUMB
                        )
                        imageWidth = img.width
                        imageHeight = img.height
                        val file = File(path)
                        if (file.exists()) {
                            dataPath = path
                        }
                        if (dataUri.isNullOrEmpty()) {
                            dataUri = path
                        }
                    } else if (img.type == V2TIMImageElem.V2TIM_IMAGE_TYPE_ORIGIN) {
                        //原图
                        val path: String = TimConfig.generateImagePath(
                            img.uuid,
                            V2TIMImageElem.V2TIM_IMAGE_TYPE_ORIGIN
                        )
                        dataUri = path

                        downloadStatus = if (path.isFileExist()) {
                            MessageInfoBean.MSG_STATUS_DOWNLOADED
                        } else {
                            MessageInfoBean.MSG_STATUS_UN_DOWNLOAD
                        }
                    } else if (img.type == V2TIMImageElem.V2TIM_IMAGE_TYPE_LARGE) {
                        //长图
                    }
                }
            }
        }
    }

    override fun convertToItem(bean: MessageInfoBean): BaseChatMsgItem {
        return MsgImageItem()
    }
}