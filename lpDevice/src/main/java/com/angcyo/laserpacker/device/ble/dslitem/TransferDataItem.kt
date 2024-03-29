package com.angcyo.laserpacker.device.ble.dslitem

import com.angcyo.core.component.ScreenShotModel
import com.angcyo.dsladapter.DslAdapterItem
import com.angcyo.http.rx.runRx
import com.angcyo.item.style.addGridMedia
import com.angcyo.item.style.clearGridMedia
import com.angcyo.item.style.gridMediaSpanCount
import com.angcyo.laserpacker.LPDataConstant
import com.angcyo.laserpacker.device.DeviceHelper
import com.angcyo.laserpacker.device.R
import com.angcyo.library.ex._color
import com.angcyo.library.ex._string
import com.angcyo.library.ex.nowTimeString
import com.angcyo.library.ex.shareFile
import com.angcyo.library.ex.toFullTime
import com.angcyo.library.ex.toStr
import com.angcyo.library.ex.zip
import com.angcyo.library.libCacheFile
import com.angcyo.library.toastQQ
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
            val list = DeviceHelper.getIndexLogFilePath(value)
            logFilePathList = list
            clearGridMedia()
            list.forEach {
                if (it.endsWith(LPDataConstant.EXT_PREVIEW)) {
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
            val logList = mutableListOf<String>()
            logList.addAll(ScreenShotModel.getBaseLogShareList())
            logList.addAll(list)

            logList.zip(libCacheFile("TransferData-log-${nowTimeString("yyyy-MM-dd")}.zip").absolutePath)
                ?.shareFile()
        })
    }

}