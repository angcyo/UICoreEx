package com.angcyo.canvas2.laser.pecker.engrave.dslitem.engrave

import android.content.Context
import com.angcyo.bluetooth.fsc.laserpacker.HawkEngraveKeys
import com.angcyo.bluetooth.fsc.laserpacker.command.EngraveCmd
import com.angcyo.canvas2.laser.pecker.R
import com.angcyo.dialog2.dslitem.DslLabelWheelItem
import com.angcyo.dialog2.dslitem.getSelectedWheelIntData
import com.angcyo.dialog2.dslitem.itemWheelList
import com.angcyo.dialog2.dslitem.itemWheelSelectorAction
import com.angcyo.dialog2.dslitem.itemWheelUnit
import com.angcyo.dsladapter.DslAdapterItem
import com.angcyo.engrave2.EngraveFlowDataHelper
import com.angcyo.laserpacker.bean.LPElementBean
import com.angcyo.laserpacker.device.ensurePrintPrecision
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
open class EngraveOptionWheelItem : DslLabelWheelItem() {

    /**参数配置实体*/
    var itemEngraveConfigEntity: EngraveConfigEntity? = null

    /**单元素参数配置*/
    var itemEngraveItemBean: LPElementBean? = null

    init {
        itemLayoutId = R.layout.item_engrave_option_layout

        itemWheelSelectorAction = { dialog, index, item ->
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
                        EngraveFlowDataHelper.updateOrGenerateEngraveConfigByMaterial(
                            taskId,
                            materialEntity!!
                        )
                    }
                    //单文件雕刻参数
                    itemEngraveItemBean?.apply {
                        val materialEntity = itemWheelList?.get(index) as? MaterialEntity
                        printType = materialEntity?.type ?: printType
                        printPower = materialEntity?.power ?: printPower
                        printDepth = materialEntity?.depth ?: printDepth
                        printPrecision =
                            (materialEntity?.precision ?: printPrecision).ensurePrintPrecision()
                        materialCode = materialEntity?.code
                        materialKey = materialEntity?.key
                        materialName = materialEntity?.name

                        laserFrequency = materialEntity?.laserFrequency
                    }
                }

                MaterialEntity::power.name -> {
                    itemEngraveConfigEntity?.apply {
                        power = getSelectedWheelIntData(index, power)
                        HawkEngraveKeys.lastPower = power
                        lpSaveEntity()
                    }
                    //单文件雕刻参数
                    itemEngraveItemBean?.apply {
                        HawkEngraveKeys.lastPower =
                            getSelectedWheelIntData(index, printPower ?: HawkEngraveKeys.lastPower)
                        printPower = HawkEngraveKeys.lastPower
                    }
                }

                MaterialEntity::depth.name -> {
                    itemEngraveConfigEntity?.apply {
                        depth = getSelectedWheelIntData(index, depth)
                        HawkEngraveKeys.lastDepth = depth
                        lpSaveEntity()
                    }
                    //单文件雕刻参数
                    itemEngraveItemBean?.apply {
                        HawkEngraveKeys.lastDepth =
                            getSelectedWheelIntData(index, printDepth ?: HawkEngraveKeys.lastDepth)
                        printDepth = HawkEngraveKeys.lastDepth
                    }
                }

                MaterialEntity.SPEED -> {
                    itemEngraveConfigEntity?.apply {
                        depth = EngraveCmd.speedToDepth(
                            getSelectedWheelIntData(index, EngraveCmd.depthToSpeed(depth))
                        )
                        HawkEngraveKeys.lastDepth = depth
                        lpSaveEntity()
                    }
                    //单文件雕刻参数
                    itemEngraveItemBean?.apply {
                        HawkEngraveKeys.lastDepth = EngraveCmd.speedToDepth(
                            getSelectedWheelIntData(
                                index,
                                EngraveCmd.depthToSpeed(printDepth ?: HawkEngraveKeys.lastDepth)
                            )
                        )
                        printDepth = HawkEngraveKeys.lastDepth
                    }
                }

                EngraveConfigEntity::time.name -> {
                    itemEngraveConfigEntity?.apply {
                        time = getSelectedWheelIntData(index, time)
                        lpSaveEntity()
                    }
                    //单文件雕刻参数
                    itemEngraveItemBean?.apply {
                        printCount = getSelectedWheelIntData(index, printCount ?: 1)
                    }
                }

                EngraveConfigEntity::precision.name -> {
                    itemEngraveConfigEntity?.apply {
                        precision = getSelectedWheelIntData(index, precision)
                        HawkEngraveKeys.lastPrecision = precision
                        lpSaveEntity()
                    }
                    //单文件雕刻参数
                    itemEngraveItemBean?.apply {
                        HawkEngraveKeys.lastPrecision = getSelectedWheelIntData(
                            index,
                            printPrecision ?: HawkEngraveKeys.lastPrecision
                        ).ensurePrintPrecision()
                        printPrecision = HawkEngraveKeys.lastPrecision
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
}