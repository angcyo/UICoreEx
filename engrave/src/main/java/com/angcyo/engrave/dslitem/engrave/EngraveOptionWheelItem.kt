package com.angcyo.engrave.dslitem.engrave

import android.content.Context
import com.angcyo.bluetooth.fsc.laserpacker.command.EngraveCmd
import com.angcyo.dialog2.dslitem.DslLabelWheelItem
import com.angcyo.dsladapter.DslAdapterItem
import com.angcyo.engrave.EngraveFlowDataHelper
import com.angcyo.engrave.R
import com.angcyo.engrave.data.HawkEngraveKeys
import com.angcyo.objectbox.laser.pecker.entity.EngraveConfigEntity
import com.angcyo.objectbox.laser.pecker.entity.MaterialEntity
import com.angcyo.objectbox.laser.pecker.lpSaveEntity
import com.angcyo.widget.DslViewHolder

/**
 * 雕刻参数选项item
 *
 * 3合1 item
 * [com.angcyo.engrave.dslitem.engrave.EngravePropertyItem]
 *
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2022/06/02
 */
class EngraveOptionWheelItem : DslLabelWheelItem() {

    /**参数配置实体*/
    var itemEngraveConfigEntity: EngraveConfigEntity? = null

    init {
        itemLayoutId = R.layout.item_engrave_option_layout

        itemWheelSelector = { dialog, index, item ->
            //赋值操作
            when (itemTag) {
                MaterialEntity::name.name -> {
                    //当切换了材质, 需要同时更新其他图层配置的材质信息和功率/深度等信息
                    /*itemEngraveDataParam?.apply {
                        val materialEntity = itemWheelList?.get(index) as? MaterialEntity
                        material = materialEntity?.toText()?.toString() ?: material
                        power = materialEntity?.power?.toByte() ?: power
                        depth = materialEntity?.depth?.toByte() ?: depth

                        //更新其他
                        _updatePowerDepthItem()
                    }*/
                    itemEngraveConfigEntity?.apply {
                        val materialEntity = itemWheelList?.get(index) as? MaterialEntity
                        EngraveFlowDataHelper.generateEngraveConfigByMaterial(
                            taskId, materialEntity?.key, materialEntity
                        )
                    }
                }
                MaterialEntity::power.name -> {
                    itemEngraveConfigEntity?.apply {
                        power = getSelectedInt(index, power)
                        HawkEngraveKeys.lastPower = power
                        lpSaveEntity()
                    }
                }
                MaterialEntity::depth.name -> {
                    itemEngraveConfigEntity?.apply {
                        depth = getSelectedInt(index, depth)
                        HawkEngraveKeys.lastDepth = depth
                        lpSaveEntity()
                    }
                }
                MaterialEntity.SPEED -> {
                    itemEngraveConfigEntity?.apply {
                        depth = EngraveCmd.speedToDepth(
                            getSelectedInt(index, EngraveCmd.depthToSpeed(depth))
                        )
                        HawkEngraveKeys.lastDepth = depth
                        lpSaveEntity()
                    }
                }
                EngraveConfigEntity::time.name -> {
                    itemEngraveConfigEntity?.apply {
                        time = getSelectedInt(index, time)
                        lpSaveEntity()
                    }
                }
                EngraveConfigEntity::precision.name -> {
                    itemEngraveConfigEntity?.apply {
                        precision = getSelectedInt(index, precision)
                        HawkEngraveKeys.lastPrecision = precision
                        lpSaveEntity()
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
            MaterialEntity::power.name, MaterialEntity::depth.name, MaterialEntity.SPEED -> "%"
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