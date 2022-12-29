package com.angcyo.engrave.ble.dslitem

import com.angcyo.bluetooth.fsc.laserpacker.LaserPeckerHelper
import com.angcyo.canvas.data.CanvasProjectItemBean.Companion.MM_UNIT
import com.angcyo.canvas.utils.CanvasConstant
import com.angcyo.dsladapter.DslAdapterItem
import com.angcyo.engrave.EngraveFlowDataHelper
import com.angcyo.engrave.R
import com.angcyo.engrave.dslitem.engrave.EngraveFinishInfoItem
import com.angcyo.engrave.dslitem.engrave.EngraveLabelItem
import com.angcyo.engrave.toEngraveTime
import com.angcyo.engrave.transition.EngraveTransitionManager
import com.angcyo.engrave.transition.IEngraveTransition
import com.angcyo.glide.loadImage
import com.angcyo.item.DslTagGroupItem
import com.angcyo.library.ex._string
import com.angcyo.library.ex.or
import com.angcyo.library.ex.toStr
import com.angcyo.library.ex.visible
import com.angcyo.library.unit.convertPixelToValueUnit
import com.angcyo.objectbox.laser.pecker.entity.EngraveConfigEntity
import com.angcyo.objectbox.laser.pecker.entity.EngraveDataEntity
import com.angcyo.objectbox.laser.pecker.entity.TransferDataEntity
import com.angcyo.widget.DslViewHolder
import com.angcyo.widget.base.resetDslItem

/**
 * 雕刻历史item
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2022/07/05
 */
class EngraveHistoryItem : DslTagGroupItem() {

    /**雕刻的数据实体*/
    var itemEngraveDataEntity: EngraveDataEntity? = null
        set(value) {
            field = value
            if (value != null) {
                itemTransferDataEntity = EngraveFlowDataHelper.getTransferData(value.index)
                itemTransferDataEntity?.taskId?.let {
                    itemEngraveConfigEntityList = EngraveFlowDataHelper.getTaskEngraveConfig(it)
                }
            }
        }

    /**可视化的单位*/
    private val valueUnit = CanvasConstant.valueUnit

    private val mmValueUnit = MM_UNIT

    /**传输的数据, 如果没有传输的数据, 说明数据不在本机产生, 则只能直接雕刻, 不能预览*/
    var itemTransferDataEntity: TransferDataEntity? = null

    /**雕刻的配置的数据*/
    var itemEngraveConfigEntityList: List<EngraveConfigEntity>? = null

    /**是否显示了所有*/
    var _isShowAll = false

    init {
        itemLayoutId = R.layout.item_engrave_history_layout
        itemTagLayoutId = R.layout.dsl_tag_item2
    }

    override fun onItemBind(
        itemHolder: DslViewHolder,
        itemPosition: Int,
        adapterItem: DslAdapterItem,
        payloads: List<Any>
    ) {
        super.onItemBind(itemHolder, itemPosition, adapterItem, payloads)

        //文件名
        val name = (itemTransferDataEntity?.name ?: itemEngraveDataEntity?.index?.toStr()).or()
        itemHolder.tv(R.id.file_name_view)?.text = "${_string(R.string.engrave_file_name)}:${name}"

        if (itemTransferDataEntity == null) {
            //未在本机的雕刻历史//只能直接雕刻的数据
            itemHolder.visible(R.id.lib_image_view, false)
            itemHolder.visible(R.id.lib_flow_layout, false)
            itemHolder.visible(R.id.layer_wrap_layout, false)
            itemHolder.visible(R.id.show_all_view, false)
        } else {
            itemHolder.visible(R.id.lib_image_view, true)
            itemHolder.visible(R.id.lib_flow_layout, true)
            itemHolder.tv(R.id.show_all_view)?.text =
                if (_isShowAll) _string(R.string.hide_all_label) else _string(R.string.show_all_label)

            //预览图
            val previewImagePath =
                IEngraveTransition.getEngravePreviewBitmapPath(itemEngraveDataEntity?.index)
            itemHolder.img(R.id.lib_image_view)?.loadImage(previewImagePath)

            //图层信息
            val engraveConfigEntityList = itemEngraveConfigEntityList
            val itemList = mutableListOf<DslAdapterItem>()
            /* 2022-12-28 不支持恢复任务雕刻, 所以只能显示一个图层
            EngraveTransitionManager.engraveLayerList.forEach { layerInfo ->
                engraveConfigEntityList?.find { it.layerMode == layerInfo.layerMode }
                    ?.let { engraveConfigEntity ->
                        itemList.add(EngraveLabelItem().apply {
                            itemText = layerInfo.label
                        })
                        itemList.add(EngraveFinishInfoItem().apply {
                            itemTaskId = engraveConfigEntity.taskId
                            itemLayerMode = engraveConfigEntity.layerMode
                        })
                    }
            }*/
            //只能显示单个数据的信息
            engraveConfigEntityList?.find { it.layerMode == itemTransferDataEntity?.layerMode }
                ?.let { engraveConfigEntity ->
                    itemList.add(EngraveLabelItem().apply {
                        itemText =
                            EngraveTransitionManager.getEngraveLayer(engraveConfigEntity.layerMode)?.label
                    })
                    itemList.add(EngraveFinishInfoItem().apply {
                        itemTaskId = engraveConfigEntity.taskId
                        itemLayerMode = engraveConfigEntity.layerMode
                    })
                }
            itemHolder.visible(R.id.show_all_view, itemList.isNotEmpty())
            itemHolder.group(R.id.layer_wrap_layout)?.apply {
                visible(itemList.isNotEmpty() && _isShowAll)
                if (itemList.isNotEmpty()) {
                    resetDslItem(itemList)
                }
            }

            itemHolder.click(R.id.show_all_view) {
                _isShowAll = !_isShowAll
                updateAdapterItem()
            }
        }
    }

    override fun initLabelDesList() {
        val transferDataEntity = itemTransferDataEntity
        val engraveConfigEntityList = itemEngraveConfigEntityList
        renderLabelDesList {

            if (transferDataEntity != null) {
                transferDataEntity.productName?.let { name ->
                    add(formatLabelDes(_string(R.string.device_models), name))
                }

                val pxInfo = LaserPeckerHelper.findPxInfo(transferDataEntity.dpi)
                add(formatLabelDes(_string(R.string.resolution_ratio), pxInfo.des))

                val originWidth = transferDataEntity.originWidth
                val originHeight = transferDataEntity.originHeight
                if (originWidth != null && originHeight != null) {
                    add(
                        formatLabelDes(
                            _string(R.string.print_range),
                            buildString {
                                append(_string(R.string.width))
                                append(
                                    valueUnit.convertPixelToValueUnit(
                                        mmValueUnit.convertValueToPixel(originWidth),
                                        false
                                    )
                                )
                                append(" ")
                                append(_string(R.string.height))
                                append(
                                    valueUnit.convertPixelToValueUnit(
                                        mmValueUnit.convertValueToPixel(originHeight),
                                        false
                                    )
                                )
                            }
                        )
                    )
                }
            }

            val startEngraveTime = itemEngraveDataEntity?.startTime ?: 0
            val endEngraveTime = itemEngraveDataEntity?.finishTime ?: 0
            if (endEngraveTime > 0 && startEngraveTime > 0 && endEngraveTime > startEngraveTime) {
                val engraveTime = (endEngraveTime - startEngraveTime).toEngraveTime()
                add(formatLabelDes(_string(R.string.work_time), engraveTime))
            }

            //---

            engraveConfigEntityList?.apply {
                firstOrNull()?.let {
                    //材质
                    add(
                        formatLabelDes(
                            _string(R.string.custom_material),
                            EngraveFlowDataHelper.getEngraveMaterNameByKey(it.materialKey)
                        )
                    )
                }
            }

            /*   if (engraveConfigEntityList.firstOrNull() != null) {
                   if (engraveConfigEntity.precision > 0) {
                       add(
                           labelDes(
                               _string(R.string.engrave_precision),
                               "${engraveConfigEntity.precision}"
                           )
                       )
                   }
               }*/

            /*add(labelDes(_string(R.string.custom_power), "${engraveConfigEntity.power}%"))
            add(labelDes(_string(R.string.custom_speed), "${engraveConfigEntity.depth}%"))
            */
            //---
        }
    }
}