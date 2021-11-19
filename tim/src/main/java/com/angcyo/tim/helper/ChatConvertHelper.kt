package com.angcyo.tim.helper

import com.angcyo.dsladapter.DslAdapter
import com.angcyo.dsladapter.DslAdapterItem
import com.angcyo.dsladapter.find
import com.angcyo.library.ex.bitmapSize
import com.angcyo.library.ex.nowTime
import com.angcyo.tim.bean.MessageInfoBean
import com.angcyo.tim.bean.isGroup
import com.angcyo.tim.bean.isSelf
import com.angcyo.tim.bean.sender
import com.angcyo.tim.dslitem.BaseChatMsgItem
import com.angcyo.tim.helper.convert.*
import com.angcyo.tim.util.TimConfig
import com.tencent.imsdk.v2.V2TIMManager
import com.tencent.imsdk.v2.V2TIMMessage

/**
 * 聊天转换器
 * Email:angcyo@126.com
 * @author angcyo
 * @date 2021/11/18
 * Copyright (c) 2020 ShenZhen Wayto Ltd. All rights reserved.
 */
object ChatConvertHelper : BaseConvert() {

    val convertList = mutableListOf<BaseConvert>()

    init {
        convertList.add(CustomConvert())
        convertList.add(TextConvert())
        convertList.add(ImageConvert())
        convertList.add(VideoConvert())
        convertList.add(SoundConvert())
        convertList.add(FileConvert())
        convertList.add(LocationConvert())

        //last
        convertList.add(UnknownConvert())
    }

    override fun convertToBean(message: V2TIMMessage): MessageInfoBean? {

        if (message.status == V2TIMMessage.V2TIM_MSG_STATUS_HAS_DELETED /*消息被删除*/ ||
            message.elemType == V2TIMMessage.V2TIM_ELEM_TYPE_NONE /*无元素的消息*/) {
            return null
        }

        var result: MessageInfoBean? = null
        for (convert in convertList) {
            if (convert.handleMessage(message)) {
                result = convert.convertToBean(message)
                if (result != null) {
                    break
                }
            }
        }
        result = result ?: super.convertToBean(message)

        //init
        result?.apply {
            this.message = message
            if (message.status == V2TIMMessage.V2TIM_MSG_STATUS_LOCAL_REVOKED) {
                status = MessageInfoBean.MSG_STATUS_REVOKE
                content = when {
                    isSelf -> "您撤回了一条消息"
                    isGroup -> {
                        val msg: String = TimConfig.covert2HTMLString(sender)
                        msg + "撤回了一条消息"
                    }
                    else -> "对方撤回了一条消息"
                }
            } else {
                if (isSelf) {
                    when (message.status) {
                        V2TIMMessage.V2TIM_MSG_STATUS_SEND_FAIL -> {
                            status = MessageInfoBean.MSG_STATUS_SEND_FAIL
                        }
                        V2TIMMessage.V2TIM_MSG_STATUS_SEND_SUCC -> {
                            status = MessageInfoBean.MSG_STATUS_SEND_SUCCESS
                        }
                        V2TIMMessage.V2TIM_MSG_STATUS_SENDING -> {
                            status = MessageInfoBean.MSG_STATUS_SENDING
                        }
                    }
                } else {
                    status = MessageInfoBean.MSG_STATUS_SEND_SUCCESS
                }
            }
        }

        return result
    }

    override fun convertToItem(bean: MessageInfoBean): BaseChatMsgItem? {

        var result: BaseChatMsgItem? = null
        for (convert in convertList) {
            if (convert.handleBean(bean)) {
                result = convert.convertToItem(bean)
                if (result != null) {
                    break
                }
            }
        }
        result = result ?: super.convertToItem(bean)

        //init
        result?.apply {
            messageInfoBean = bean
        }

        return result
    }
}

fun V2TIMMessage.toMessageInfoBean(): MessageInfoBean? = ChatConvertHelper.convertToBean(this)

fun MessageInfoBean.toDslAdapterItem(): BaseChatMsgItem? = ChatConvertHelper.convertToItem(this)

/**[V2TIMMessage]->[BaseChatMsgItem]
 * [reverse] 将消息进行反序*/
fun List<V2TIMMessage>.toDslAdapterItemList(reverse: Boolean = true): List<BaseChatMsgItem> {
    val result = mutableListOf<BaseChatMsgItem>()
    val list = mutableListOf<MessageInfoBean>()
    forEach {
        it.toMessageInfoBean()?.let { list.add(it) }
    }
    if (reverse) {
        list.reverse()
    }
    list.forEach {
        it.toDslAdapterItem()?.let { item ->
            result.add(item)
        }
    }
    return result
}

/**通过[MessageInfoBean], 查找列表中的[DslAdapterItem]*/
fun MessageInfoBean.findDslAdapterItem(adapter: DslAdapter?): BaseChatMsgItem? {
    val result: BaseChatMsgItem? = adapter?.find {
        it is BaseChatMsgItem && it.messageInfoBean?.messageId == messageId
    }
    return result
}

/**获取第一个[MessageInfoBean]*/
fun DslAdapter.firstMessageInfoBean(): MessageInfoBean? {
    return find<BaseChatMsgItem> { it is BaseChatMsgItem }?.messageInfoBean
}

/**简单消息的包装体*/
fun V2TIMMessage.toMyselfMessageInfoBean(content: String?): MessageInfoBean {
    val bean = MessageInfoBean()

    bean.message = this
    bean.timestamp = nowTime()
    bean.fromUser = V2TIMManager.getInstance().loginUser

    bean.content = content

    bean.downloadStatus = MessageInfoBean.MSG_STATUS_DOWNLOADED
    return bean
}

/**图片消息的包装体*/
fun V2TIMMessage.toMyselfImageMessageInfoBean(
    imagePath: String,
    content: String? = "[图片]",
): MessageInfoBean {
    val bean = toMyselfMessageInfoBean(content)
    bean.dataUri = imagePath
    bean.dataPath = imagePath

    val size = imagePath.bitmapSize()
    bean.imageWidth = size[0]
    bean.imageHeight = size[1]

    return bean
}

/**视频消息的包装体*/
fun V2TIMMessage.toMyselfVideoMessageInfoBean(
    videoFilePath: String,
    snapshotPath: String,
    content: String? = "[视频]",
): MessageInfoBean {
    val bean = toMyselfImageMessageInfoBean(snapshotPath, content)
    bean.dataUri = videoFilePath
    bean.dataPath = videoFilePath
    return bean
}

/**音频消息的包装体*/
fun V2TIMMessage.toMyselfSoundMessageInfoBean(
    soundPath: String,
    content: String? = "[语音]"
): MessageInfoBean {
    val bean = toMyselfMessageInfoBean(content)
    bean.dataUri = soundPath
    bean.dataPath = soundPath
    return bean
}

/**文件消息的包装体*/
fun V2TIMMessage.toMyselfFileMessageInfoBean(
    filePath: String,
    content: String? = "[文件]"
): MessageInfoBean {
    val bean = toMyselfMessageInfoBean(content)
    bean.dataUri = filePath
    bean.dataPath = filePath
    return bean
}

/**位置消息的包装体*/
fun V2TIMMessage.toMyselfLocationMessageInfoBean(content: String? = "[位置]"): MessageInfoBean {
    val bean = toMyselfMessageInfoBean(content)
    return bean
}