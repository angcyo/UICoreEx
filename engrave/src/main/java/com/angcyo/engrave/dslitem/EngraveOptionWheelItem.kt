package com.angcyo.engrave.dslitem

import android.content.Context
import com.angcyo.dialog2.dslitem.DslLabelWheelItem
import com.angcyo.dsladapter.DslAdapterItem
import com.angcyo.engrave.R
import com.angcyo.engrave.data.EngraveOptionInfo

import com.angcyo.widget.DslViewHolder

/**
 * 雕刻选项item
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2022/06/02
 */
class EngraveOptionWheelItem : DslLabelWheelItem() {

    /**数据*/
    var itemEngraveOptionInfo: EngraveOptionInfo? = null

    init {
        itemLayoutId = R.layout.item_engrave_option_layout

        itemWheelSelector = { dialog, index, item ->
            //赋值操作
            when (itemTag) {
                EngraveOptionInfo::material.name -> {
                    itemEngraveOptionInfo?.apply {
                        material = itemWheelList?.get(index)?.toString() ?: material
                    }
                }
                EngraveOptionInfo::power.name -> {
                    itemEngraveOptionInfo?.apply {
                        power = getSelectedByte(index, power)
                    }
                }
                EngraveOptionInfo::depth.name -> {
                    itemEngraveOptionInfo?.apply {
                        depth = getSelectedByte(index, depth)
                    }
                }
                EngraveOptionInfo::time.name -> {
                    itemEngraveOptionInfo?.apply {
                        time = getSelectedByte(index, time)
                    }
                }
            }
            false
        }
    }

    override fun onItemBind(
        itemHolder: DslViewHolder,
        itemPosition: Int,
        adapterItem: DslAdapterItem,
        payloads: List<Any>
    ) {
        itemWheelUnit = when (itemTag) {
            EngraveOptionInfo::power.name, EngraveOptionInfo::depth.name -> "%"
            else -> null
        }
        super.onItemBind(itemHolder, itemPosition, adapterItem, payloads)
    }

    override fun showWheelDialog(context: Context) {
        super.showWheelDialog(context)
    }

    /**获取选中的byte数据*/
    fun getSelectedByte(index: Int, def: Byte): Byte =
        itemWheelList?.get(index)?.toString()?.toIntOrNull()?.toByte() ?: def
}