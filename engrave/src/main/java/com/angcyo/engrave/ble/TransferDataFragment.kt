package com.angcyo.engrave.ble

import android.os.Bundle
import com.angcyo.core.fragment.BaseDslFragment
import com.angcyo.engrave.ble.dslitem.TransferDataItem
import com.angcyo.engrave.transition.EngraveTransitionManager
import com.angcyo.library.ex.file
import com.angcyo.library.ex.page
import com.angcyo.library.utils.folderPath

/**
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2023/01/11
 */
class TransferDataFragment : BaseDslFragment() {

    init {
        fragmentTitle = "数据记录"
        enableAdapterRefresh = true
    }

    override fun initBaseView(savedInstanceState: Bundle?) {
        super.initBaseView(savedInstanceState)
    }

    override fun onLoadData() {
        super.onLoadData()

        //传输的数据所在文件夹
        val transferFolder = folderPath(EngraveTransitionManager.ENGRAVE_TRANSFER_FILE_FOLDER)
        val list = transferFolder.file().page(page)

        loadDataEnd(TransferDataItem::class, list) { file ->
            itemTransferDataFile = file
        }
    }
}