package com.angcyo.engrave.ble.dslitem

import android.graphics.Color
import com.angcyo.bluetooth.fsc.laserpacker.LaserPeckerHelper
import com.angcyo.canvas.utils.CanvasConstant
import com.angcyo.dsladapter.DslAdapterItem
import com.angcyo.engrave.EngraveFlowDataHelper
import com.angcyo.engrave.R
import com.angcyo.engrave.dslitem.engrave.EngraveFinishInfoItem
import com.angcyo.engrave.dslitem.engrave.EngraveLabelItem
import com.angcyo.engrave.transition.EngraveTransitionManager
import com.angcyo.engrave.transition.IEngraveTransition
import com.angcyo.glide.loadImage
import com.angcyo.item.DslTagGroupItem
import com.angcyo.item.data.LabelDesData
import com.angcyo.library.ex.*
import com.angcyo.library.unit.IValueUnit.Companion.MM_UNIT
import com.angcyo.library.unit.convertPixelToValueUnit
import com.angcyo.objectbox.laser.pecker.entity.EngraveConfigEntity
import com.angcyo.objectbox.laser.pecker.entity.MaterialEntity
import com.angcyo.objectbox.laser.pecker.entity.TransferDataEntity
import com.angcyo.widget.DslViewHolder
import com.angcyo.widget.base.resetDslItem
import com.angcyo.widget.span.span

/**
 * 雕刻历史item
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2022/07/05
 */
open class EngraveHistoryItem : DslTagGroupItem() {

    /**可视化的单位*/
    private val valueUnit = CanvasConstant.valueUnit

    private val mmValueUnit = MM_UNIT

    /**传输的数据, 如果没有传输的数据*/
    var itemTransferDataEntityList: List<TransferDataEntity>? = null

    /**雕刻的配置的数据*/
    var itemEngraveConfigEntityList: List<EngraveConfigEntity>? = null

    /**是否显示了所有*/
    var _isShowDetail = false

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

        val lastEngraveDataEntity = itemTransferDataEntityList?.lastOrNull()

        //文件名
        val name = lastEngraveDataEntity?.name.or()
        itemHolder.tv(R.id.file_name_view)?.text = "${_string(R.string.engrave_file_name)}:$name"

        if (itemTransferDataEntityList.isNullOrEmpty()) {
            //未在本机的雕刻历史//只能直接雕刻的数据
            itemHolder.visible(R.id.lib_image_view, false)
            itemHolder.visible(R.id.lib_flow_layout, false)
            itemHolder.visible(R.id.layer_wrap_layout, false)
            itemHolder.visible(R.id.show_all_view, false)
        } else {
            itemHolder.visible(R.id.lib_image_view, true)
            itemHolder.visible(R.id.lib_flow_layout, true)
            itemHolder.tv(R.id.show_all_view)?.text =
                if (_isShowDetail) _string(R.string.hide_all_label) else _string(R.string.show_all_label)

            //预览图
            val previewImagePath =
                IEngraveTransition.getEngravePreviewBitmapPath(lastEngraveDataEntity?.index)
            itemHolder.img(R.id.lib_image_view)?.loadImage(previewImagePath)

            //图层信息
            val engraveConfigEntityList = itemEngraveConfigEntityList
            val itemList = mutableListOf<DslAdapterItem>()

            if (showAllEngraveLayerData()) {
                EngraveTransitionManager.engraveLayerList.forEach { layerInfo ->
                    val findData =
                        itemTransferDataEntityList?.find { it.layerMode == layerInfo.layerMode }
                    if (findData != null) {
                        //对应的图层有对应的数据, 才显示对应的雕刻参数信息
                        engraveConfigEntityList?.find { it.layerMode == layerInfo.layerMode }
                            ?.let { engraveConfigEntity ->
                                createEngraveConfig(itemList, engraveConfigEntity)
                            }
                    }
                }
            } else {
                //只能显示单个数据的信息
                engraveConfigEntityList?.find { it.layerMode == lastEngraveDataEntity?.layerMode }
                    ?.let { engraveConfigEntity ->
                        createEngraveConfig(itemList, engraveConfigEntity)
                    }
            }

            itemHolder.visible(R.id.show_all_view, itemList.isNotEmpty())
            itemHolder.group(R.id.layer_wrap_layout)?.apply {
                visible(itemList.isNotEmpty() && _isShowDetail)
                if (itemList.isNotEmpty()) {
                    resetDslItem(itemList)
                }
            }

            itemHolder.click(R.id.show_all_view) {
                _isShowDetail = !_isShowDetail
                updateAdapterItem()
            }
        }
    }

    override fun initLabelDesList() {
        val transferDataEntityList = itemTransferDataEntityList
        val transferDataEntity = transferDataEntityList?.lastOrNull()
        val engraveConfigEntityList = itemEngraveConfigEntityList
        renderLabelDesList {

            if (transferDataEntity != null) {
                transferDataEntity.productName?.let { name ->
                    add(
                        formatLabelDes(
                            _string(R.string.device_models),
                            if (isDebug()) "${name}/${transferDataEntity.deviceAddress.or()}" else name
                        )
                    )
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

            //时长
            onInitEngraveTime(this)

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

    /**是否要显示所有的图层雕刻参数信息*/
    fun showAllEngraveLayerData(): Boolean = this is EngraveTaskHistoryItem

    /**创建参数信息*/
    fun createEngraveConfig(
        list: MutableList<DslAdapterItem>,
        engraveConfigEntity: EngraveConfigEntity
    ) {
        list.add(EngraveLabelItem().apply {
            val label =
                EngraveTransitionManager.getEngraveLayer(engraveConfigEntity.layerMode)?.label.or()
            if (isDebug()) {
                itemText = span {
                    append(label)
                    append(" ")
                    MaterialEntity.createLaserTypeDrawable(
                        engraveConfigEntity.type.toInt(),
                        Color.WHITE
                    )?.let {
                        val size = 20 * dpi
                        appendDrawable(it.setBounds(size, size))
                    }
                }
            } else {
                itemText = label
            }
        })
        list.add(EngraveFinishInfoItem().apply {
            itemTaskId = engraveConfigEntity.taskId
            itemLayerMode = engraveConfigEntity.layerMode
        })
    }

    /**雕刻时长, 或者雕刻任务的总时长*/
    open fun onInitEngraveTime(list: MutableList<LabelDesData>) {

    }
}