package com.angcyo.tim.dslitem

import com.angcyo.dsladapter.isItemDetached
import com.angcyo.library.app
import com.angcyo.library.component.DslIntent
import com.angcyo.library.ex.fileSize
import com.angcyo.library.ex.formatFileSize
import com.angcyo.library.ex.toUri
import com.angcyo.tim.R
import com.angcyo.tim.bean.MessageInfoBean
import com.angcyo.tim.bean.fileElem
import com.angcyo.tim.helper.ChatDownloadHelper
import com.angcyo.widget.DslViewHolder

/**
 * 文件消息item
 * Email:angcyo@126.com
 * @author angcyo
 * @date 2021/11/17
 * Copyright (c) 2020 ShenZhen Wayto Ltd. All rights reserved.
 */
class MsgFileItem : BaseChatMsgItem() {

    init {
        msgContentLayoutId = R.layout.msg_file_layout
    }

    override fun bindMsgContent(itemHolder: DslViewHolder, itemPosition: Int, payloads: List<Any>) {
        super.bindMsgContent(itemHolder, itemPosition, payloads)

        messageInfoBean?.let { bean ->
            bean.fileElem?.let { element ->
                itemHolder.tv(R.id.msg_file_name_view)?.text = element.fileName

                val path = bean.dataUri
                itemHolder.tv(R.id.msg_file_size_view)?.text = if (element.fileSize.toLong() <= 0) {
                    formatFileSize(app(), path.fileSize())
                } else {
                    formatFileSize(app(), element.fileSize.toLong())
                }
            }

            //下载中提示
            itemHolder.visible(
                R.id.msg_sending_view,
                bean.status == MessageInfoBean.MSG_STATUS_SENDING ||
                        bean.downloadStatus == MessageInfoBean.MSG_STATUS_DOWNLOADING
            )

            //下载状态提示
            itemHolder.tv(R.id.msg_file_status_view)?.text = when (bean.downloadStatus) {
                MessageInfoBean.MSG_STATUS_DOWNLOADING -> "下载中...${bean.downloadProgress}%"
                MessageInfoBean.MSG_STATUS_DOWNLOADED -> "已下载"
                else -> "未下载"
            }

            itemHolder.click(R.id.msg_content_layout) {
                //消息内容点击
                if (bean.downloadStatus == MessageInfoBean.MSG_STATUS_DOWNLOADED) {
                    //文本下载完成
                    DslIntent.openFile(itemHolder.context, bean.dataUri.toUri()!!)
                } else if (bean.downloadStatus == MessageInfoBean.MSG_STATUS_DOWNLOADING) {
                    //下载中
                } else {
                    //开始下载
                    ChatDownloadHelper.downloadFile(
                        bean.fileElem,
                        bean,
                        this
                    ) { progress, error ->
                        if (progress == ChatDownloadHelper.DOWNLOAD_SUCCESS && !isItemDetached()) {
                            DslIntent.openFile(itemHolder.context, bean.dataUri.toUri()!!)
                        }
                    }
                }
            }
        }
    }
}