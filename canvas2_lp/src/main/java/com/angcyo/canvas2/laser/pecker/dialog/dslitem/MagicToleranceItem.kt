package com.angcyo.canvas2.laser.pecker.dialog.dslitem

import android.view.ViewGroup
import com.angcyo.canvas2.laser.pecker.R
import com.angcyo.canvas2.laser.pecker.dslitem.CanvasOutlineOffsetItem
import com.angcyo.dsladapter.DslAdapterItem
import com.angcyo.item.style.itemInfoText
import com.angcyo.library.ex._string
import com.angcyo.library.unit.unitDecimal
import com.angcyo.widget.DslViewHolder
import com.angcyo.widget.base.updateDslItem
import com.angcyo.widget.progress.DslSeekBar

/**
 * 魔棒容差item
 * Email:angcyo@126.com
 * @author angcyo
 * @date 2023/05/28
 */
class MagicToleranceItem : CanvasOutlineOffsetItem() {

    /**容器布局*/
    var itemParentLayout: ViewGroup? = null

    init {
        itemInfoText = _string(R.string.canvas_magic_wand_tolerance)
        maxValue = 100f
        minValue = 0f
    }

    override fun onItemBind(
        itemHolder: DslViewHolder,
        itemPosition: Int,
        adapterItem: DslAdapterItem,
        payloads: List<Any>
    ) {
        super.onItemBind(itemHolder, itemPosition, adapterItem, payloads)

        itemHolder.v<DslSeekBar>(R.id.lib_seek_view)?.apply {
            showCenterProgressTip = false
        }
    }

    override fun bindValue(itemHolder: DslViewHolder) {
        super.bindValue(itemHolder)
    }

    override fun formatValue(value: Float): String {
        return itemValue.unitDecimal(0)
    }

    override fun updateAdapterItem(payload: Any?, useFilterList: Boolean) {
        super.updateAdapterItem(payload, useFilterList)
        itemParentLayout?.let {
            it.updateDslItem(this)
        }
    }
}