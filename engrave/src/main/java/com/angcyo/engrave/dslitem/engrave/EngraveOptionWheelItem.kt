package com.angcyo.engrave.dslitem.engrave

import android.content.Context
import com.angcyo.dialog2.dslitem.DslLabelWheelItem
import com.angcyo.dsladapter.DslAdapterItem
import com.angcyo.dsladapter.updateAllItemBy
import com.angcyo.engrave.EngraveHelper
import com.angcyo.engrave.R
import com.angcyo.engrave.data.EngraveOptionInfo
import com.angcyo.engrave.data.HawkKeys
import com.angcyo.library.ex.toHexInt
import com.angcyo.objectbox.laser.pecker.entity.MaterialEntity
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
                    //当切换了材质
                    itemEngraveOptionInfo?.apply {
                        val materialEntity = itemWheelList?.get(index) as? MaterialEntity
                        material = materialEntity?.toText()?.toString() ?: material
                        power = materialEntity?.power?.toByte() ?: power
                        depth = materialEntity?.depth?.toByte() ?: depth

                        //更新其他
                        _updatePowerDepthItem()
                    }
                }
                EngraveOptionInfo::power.name -> {
                    itemEngraveOptionInfo?.apply {
                        power = getSelectedByte(index, power)
                        HawkKeys.lastPower = power.toHexInt()

                        //重置为自定义
                        _updateMaterialItem()
                    }
                }
                EngraveOptionInfo::depth.name -> {
                    itemEngraveOptionInfo?.apply {
                        depth = getSelectedByte(index, depth)
                        HawkKeys.lastDepth = depth.toHexInt()

                        //重置为自定义
                        _updateMaterialItem()
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

    /**更新功率/深度的数据*/
    fun _updatePowerDepthItem() {
        itemDslAdapter?.updateAllItemBy {
            if (it is EngraveOptionWheelItem) {
                if (it.itemTag == EngraveOptionInfo::power.name) {
                    //功率
                    it.itemSelectedIndex = EngraveHelper.findOptionIndex(
                        it.itemWheelList,
                        itemEngraveOptionInfo?.power
                    )
                    true
                } else if (it.itemTag == EngraveOptionInfo::depth.name) {
                    //深度
                    it.itemSelectedIndex = EngraveHelper.findOptionIndex(
                        it.itemWheelList,
                        itemEngraveOptionInfo?.depth
                    )
                    true
                } else {
                    false
                }
            } else {
                false
            }
        }
    }

    /**重置为自定义*/
    fun _updateMaterialItem() {
        itemDslAdapter?.updateAllItemBy {
            if (it is EngraveOptionWheelItem && it.itemTag == EngraveOptionInfo::material.name) {
                //材质item, 选中第一个, 自定义
                it.itemSelectedIndex = 0
                true
            } else {
                false
            }
        }
    }
}