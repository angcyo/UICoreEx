package com.angcyo.engrave.dslitem.engrave

import android.content.Context
import com.angcyo.dialog2.dslitem.DslLabelWheelItem
import com.angcyo.dsladapter.DslAdapterItem
import com.angcyo.engrave.R
import com.angcyo.engrave.data.EngraveDataParam
import com.angcyo.engrave.data.HawkEngraveKeys
import com.angcyo.widget.DslViewHolder

/**
 * 雕刻参数选项item
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2022/06/02
 */
class EngraveOptionWheelItem : DslLabelWheelItem() {

    /**数据*/
    var itemEngraveDataParam: EngraveDataParam? = null

    init {
        itemLayoutId = R.layout.item_engrave_option_layout

        itemWheelSelector = { dialog, index, item ->
            //赋值操作
            when (itemTag) {
                EngraveDataParam::materialName.name -> {
                    //当切换了材质
                    /*itemEngraveDataParam?.apply {
                        val materialEntity = itemWheelList?.get(index) as? MaterialEntity
                        material = materialEntity?.toText()?.toString() ?: material
                        power = materialEntity?.power?.toByte() ?: power
                        depth = materialEntity?.depth?.toByte() ?: depth

                        //更新其他
                        _updatePowerDepthItem()
                    }*/
                }
                EngraveDataParam::power.name -> {
                    itemEngraveDataParam?.apply {
                        power = getSelectedInt(index, power)
                        HawkEngraveKeys.lastPower = power
                    }
                }
                EngraveDataParam::depth.name -> {
                    itemEngraveDataParam?.apply {
                        depth = getSelectedInt(index, depth)
                        HawkEngraveKeys.lastDepth = depth
                    }
                }
                EngraveDataParam::time.name -> {
                    itemEngraveDataParam?.apply {
                        time = getSelectedInt(index, time)
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
            EngraveDataParam::power.name, EngraveDataParam::depth.name -> "%"
            else -> null
        }
        super.onItemBind(itemHolder, itemPosition, adapterItem, payloads)
    }

    override fun showWheelDialog(context: Context) {
        super.showWheelDialog(context)
    }

    /**获取选中的byte数据*/
    fun getSelectedInt(index: Int, def: Int): Int =
        itemWheelList?.get(index)?.toString()?.toIntOrNull() ?: def
}