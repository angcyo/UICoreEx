package com.angcyo.engrave.ble

import androidx.recyclerview.widget.RecyclerView
import com.angcyo.core.fragment.BaseDslFragment
import com.angcyo.dsladapter.DslAdapter
import com.angcyo.dsladapter.find
import com.angcyo.engrave.ble.dslitem.CommandHistoryItem
import com.angcyo.engrave.ble.dslitem.CommandInputItem
import com.angcyo.item.style.itemEditText
import com.angcyo.objectbox.laser.pecker.LPBox
import com.angcyo.objectbox.laser.pecker.entity.CommandEntity
import com.angcyo.objectbox.laser.pecker.entity.CommandEntity_
import com.angcyo.objectbox.page

/**
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2022/10/25
 */
class CommandFragment : BaseDslFragment() {

    init {
        fragmentTitle = "指令记录"
        enableRefresh = true
    }

    override fun onInitDslLayout(recyclerView: RecyclerView, dslAdapter: DslAdapter) {
        super.onInitDslLayout(recyclerView, dslAdapter)
        dslAdapter.headerItems.add(CommandInputItem())
    }

    override fun onLoadData() {
        super.onLoadData()
        val list = CommandEntity::class.page(page, LPBox.PACKAGE_NAME) {
            orderDesc(CommandEntity_.sendTime)
        }
        loadDataEnd(CommandHistoryItem::class, list) { entity ->
            itemCommandEntity = entity

            itemClick = {
                _adapter.find<CommandInputItem> { it is CommandInputItem }?.apply {
                    itemEditText = entity.command
                    updateAdapterItem()
                }
            }
        }
    }

}