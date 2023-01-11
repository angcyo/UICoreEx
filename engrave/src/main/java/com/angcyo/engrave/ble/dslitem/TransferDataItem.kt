package com.angcyo.engrave.ble.dslitem

import com.angcyo.dsladapter.DslAdapterItem
import com.angcyo.engrave.EngraveFlowDataHelper
import com.angcyo.engrave.R
import com.angcyo.engrave.transition.IEngraveTransition
import com.angcyo.http.rx.runRx
import com.angcyo.item.style.addGridMedia
import com.angcyo.item.style.clearGridMedia
import com.angcyo.item.style.gridMediaSpanCount
import com.angcyo.library.Library
import com.angcyo.library.ex.*
import com.angcyo.library.libCacheFile
import com.angcyo.library.toastQQ
import com.angcyo.library.utils.logPath
import com.angcyo.pager.dslitem.DslNineMediaItem
import com.angcyo.widget.DslViewHolder
import com.angcyo.widget.span.span
import java.io.File

/**
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2023/01/11
 */
class TransferDataItem : DslNineMediaItem() {

    private var logFilePathList: List<String>? = null

    /**传输的文件*/
    var itemTransferDataFile: File? = null
        set(value) {
            field = value
            itemTransferDataIndex = value?.name
        }

    /**数据索引*/
    private var itemTransferDataIndex: Any? = null
        set(value) {
            field = value
            val list = EngraveFlowDataHelper.getIndexLogFilePath(value)
            logFilePathList = list
            clearGridMedia()
            list.forEach {
                if (it.endsWith(IEngraveTransition.EXT_PREVIEW)) {
                    addGridMedia(it)
                }
            }
        }

    init {
        itemLayoutId = R.layout.item_transfer_data_index_layout
        gridMediaSpanCount = 2
    }

    override fun onItemBind(
        itemHolder: DslViewHolder,
        itemPosition: Int,
        adapterItem: DslAdapterItem,
        payloads: List<Any>
    ) {
        super.onItemBind(itemHolder, itemPosition, adapterItem, payloads)
        itemHolder.tv(R.id.lib_label_view)?.text = span {
            append(itemTransferDataIndex?.toStr())
            append("/")
            append(itemTransferDataFile?.lastModified()?.toFullTime()) {
                foregroundColor = _color(R.color.text_sub_color)
            }
        }

        itemHolder.click(R.id.lib_share_view) {
            shareTransferData()
        }
    }

    /**分享传输日志*/
    fun shareTransferData() {
        val list = logFilePathList
        if (list.isNullOrEmpty()) {
            return
        }
        toastQQ(_string(R.string.create_log_tip))
        runRx({
            val logList = mutableListOf(logPath())
            Library.hawkPath?.let { logList.add(it) } //xml
            logList.addAll(list)

            logList.zip(libCacheFile("TransferData-log-${nowTimeString("yyyy-MM-dd")}.zip").absolutePath)
                ?.shareFile()
        })
    }

}