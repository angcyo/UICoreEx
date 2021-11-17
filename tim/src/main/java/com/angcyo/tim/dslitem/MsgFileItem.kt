package com.angcyo.tim.dslitem

import com.angcyo.library.L
import com.angcyo.library.app
import com.angcyo.library.component.DslIntent
import com.angcyo.library.ex.fileSize
import com.angcyo.library.ex.formatFileSize
import com.angcyo.library.ex.isFileExist
import com.angcyo.library.ex.toUri
import com.angcyo.tim.R
import com.angcyo.tim.bean.MessageInfoBean
import com.angcyo.tim.bean.fileElem
import com.angcyo.tim.util.TimConfig
import com.angcyo.widget.DslViewHolder
import com.tencent.imsdk.v2.V2TIMDownloadCallback
import com.tencent.imsdk.v2.V2TIMElem
import com.tencent.imsdk.v2.V2TIMFileElem

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
                itemHolder.tv(R.id.msg_file_size_view)?.text =
                    formatFileSize(app(), path.fileSize(element.fileSize.toLong()))

                downloadFile(bean, element)
            }

            itemHolder.click(R.id.msg_content_layout) {
                //消息内容点击
                val path = bean.dataUri
                if (path.isFileExist()) {
                    DslIntent.openFile(itemHolder.context, path.toUri()!!)
                }
            }
        }
    }

    fun downloadFile(bean: MessageInfoBean, element: V2TIMFileElem) {
        if (element.uuid.isNullOrEmpty()) {
            return
        }
        val path: String = TimConfig.getFileDownloadDir(element.uuid)
        if (!path.isFileExist()) {
            element.downloadFile(path, object : V2TIMDownloadCallback {
                override fun onProgress(progressInfo: V2TIMElem.V2ProgressInfo) {
                    val currentSize = progressInfo.currentSize
                    val totalSize = progressInfo.totalSize
                    var progress = 0
                    if (totalSize > 0) {
                        progress = (100 * currentSize / totalSize).toInt()
                    }
                    if (progress > 100) {
                        progress = 100
                    }
                    L.i("下载文件progress:$progress")
                }

                override fun onError(code: Int, desc: String) {
                    L.e("下载文件失败:$code:$desc")
                }

                override fun onSuccess() {
                    bean.dataPath = path
                    bean.dataUri = path
                }
            })
        } else {
            bean.dataPath = path
            bean.dataUri = path
        }
    }
}