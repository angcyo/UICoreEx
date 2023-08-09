package com.angcyo.canvas2.laser.pecker.history.dslitem

import com.angcyo.bluetooth.fsc.laserpacker.DeviceStateModel
import com.angcyo.bluetooth.fsc.laserpacker.HawkEngraveKeys
import com.angcyo.bluetooth.fsc.laserpacker.LaserPeckerHelper
import com.angcyo.bluetooth.fsc.laserpacker.parse.toDeviceStr
import com.angcyo.canvas2.laser.pecker.R
import com.angcyo.canvas2.laser.pecker.engrave.dslitem.engrave.EngraveFinishInfoItem
import com.angcyo.canvas2.laser.pecker.engrave.dslitem.engrave.EngraveLabelItem
import com.angcyo.canvas2.laser.pecker.util.LPConstant
import com.angcyo.core.component.model.NightModel
import com.angcyo.core.vmApp
import com.angcyo.dsladapter.DslAdapterItem
import com.angcyo.engrave2.model.PreviewModel
import com.angcyo.glide.loadImage
import com.angcyo.item.DslTagGroupItem
import com.angcyo.item.data.LabelDesData
import com.angcyo.laserpacker.device.DeviceHelper
import com.angcyo.laserpacker.device.LayerHelper
import com.angcyo.library.annotation.MM
import com.angcyo.library.annotation.Pixel
import com.angcyo.library.ex._string
import com.angcyo.library.ex.dpi
import com.angcyo.library.ex.or
import com.angcyo.library.ex.setBounds
import com.angcyo.library.ex.size
import com.angcyo.library.ex.visible
import com.angcyo.objectbox.laser.pecker.entity.EngraveConfigEntity
import com.angcyo.objectbox.laser.pecker.entity.MaterialEntity
import com.angcyo.objectbox.laser.pecker.entity.TransferDataEntity
import com.angcyo.widget.DslViewHolder
import com.angcyo.widget.base.dslViewHolder
import com.angcyo.widget.base.resetChild
import com.angcyo.widget.base.resetDslItem
import com.angcyo.widget.flow
import com.angcyo.widget.span.span
import kotlin.math.absoluteValue
import kotlin.math.min

/**
 * 雕刻历史item
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2022/07/05
 */
open class EngraveHistoryItem : DslTagGroupItem() {

    /**可视化的单位*/
    private val valueUnit = LPConstant.renderUnit

    /**传输的数据, 如果没有传输的数据*/
    var itemTransferDataEntityList: List<TransferDataEntity>? = null

    /**雕刻的配置的数据*/
    var itemEngraveConfigEntityList: List<EngraveConfigEntity>? = null

    /**是否显示了所有*/
    var _isShowDetail = false

    private val nightModel = vmApp<NightModel>()

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
            itemHolder.visible(R.id.image_flow_layout, false)
            itemHolder.visible(R.id.lib_flow_layout, false)
            itemHolder.visible(R.id.layer_wrap_layout, false)
            itemHolder.visible(R.id.show_all_view, false)
        } else {
            itemHolder.visible(R.id.image_flow_layout, true)
            itemHolder.visible(R.id.lib_flow_layout, true)

            val dataSize = itemTransferDataEntityList.size()
            itemHolder.tv(R.id.show_all_view)?.text =
                if (_isShowDetail) _string(R.string.hide_all_label) else {
                    if (dataSize > HawkEngraveKeys.maxShowTransferImageCount) {
                        "${_string(R.string.show_all_label)}(${dataSize})"
                    } else {
                        _string(R.string.show_all_label)
                    }
                }

            //预览图
            val showItemCount = if (_isShowDetail) dataSize else min(
                dataSize,
                HawkEngraveKeys.maxShowTransferImageCount
            ) //没有显示详情的情况下, 最多显示3个
            itemHolder.flow(R.id.image_flow_layout)
                ?.resetChild(
                    showItemCount,
                    R.layout.layout_engrave_image
                ) { itemView, itemIndex ->
                    val item = itemTransferDataEntityList?.getOrNull(itemIndex)
                    val viewHolder = itemView.dslViewHolder()
                    val previewImagePath =
                        DeviceHelper.getEngravePreviewBitmapPath(item?.index)
                    viewHolder.img(R.id.lib_image_view)?.loadImage(previewImagePath)
                }

            //图层信息
            val engraveConfigEntityList = itemEngraveConfigEntityList
            val itemList = mutableListOf<DslAdapterItem>()

            val includeCutLayer = needCheckCut(
                engraveConfigEntityList?.lastOrNull()?.softwareVersion,
                engraveConfigEntityList?.lastOrNull()?.moduleState
            )
            if (showAllEngraveLayerData()) {
                LayerHelper.getEngraveLayerList(includeCutLayer).forEach { layerInfo ->
                    val findData =
                        itemTransferDataEntityList?.find { it.layerId == layerInfo.layerId }
                    if (findData != null) {
                        //对应的图层有对应的数据, 才显示对应的雕刻参数信息
                        engraveConfigEntityList?.find { it.layerId == layerInfo.layerId }
                            ?.let { engraveConfigEntity ->
                                createEngraveConfig(itemList, engraveConfigEntity)
                            }
                    }
                }
            } else {
                //只能显示单个数据的信息
                engraveConfigEntityList?.find { it.layerId == lastEngraveDataEntity?.layerId }
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
        val engraveConfigEntity = engraveConfigEntityList?.lastOrNull()
        //val isPenMode = vmApp<DeviceStateModel>().isPenMode(engraveConfigEntity?.moduleState)

        renderLabelDesList {

            if (transferDataEntity != null) {
                transferDataEntity.productName?.let { name ->
                    val exDevice = engraveConfigEntity?.exDevice?.toDeviceStr()
                    val des = buildString {
                        append(name)
                        if (!exDevice.isNullOrBlank()) {
                            append("-$exDevice")
                        }
                        if (HawkEngraveKeys.enableShowHistoryAddress) {
                            append("/${transferDataEntity.deviceAddress.or()}")
                        }
                    }
                    add(formatLabelDes(_string(R.string.device_models), des))
                }

                //尺寸
                val transferDataSize = itemTransferDataEntityList.size()
                if (transferDataSize == 1) {
                    //只有1个时, 则显示实际的宽高
                    val originWidth = transferDataEntity.originWidth
                    val originHeight = transferDataEntity.originHeight
                    if (originWidth != null && originHeight != null) {
                        add(widthHeightLabelDes(originWidth, originHeight, true))
                    }
                } else if (transferDataSize > 1) {
                    //有多个数据时, 显示旋转后的实际宽高
                    @Pixel
                    val previewInfo = PreviewModel.createPreviewInfo(transferDataEntityList)
                    val previewWidth = previewInfo?.originBounds?.width()
                    val previewHeight = previewInfo?.originBounds?.height()
                    if (previewWidth != null && previewHeight != null) {
                        add(widthHeightLabelDes(previewWidth, previewHeight, false))
                    }
                }

                //偏移
                @MM
                var maxOffsetLeft = 0f
                var maxOffsetTop = 0f

                var offsetLeft = 0f
                var offsetTop = 0f

                @MM
                for (transferData in transferDataEntityList) {
                    val left = transferData.offsetLeft ?: 0f
                    val top = transferData.offsetTop ?: 0f
                    if (left.absoluteValue > maxOffsetLeft.absoluteValue) {
                        offsetLeft = left
                        maxOffsetLeft = left
                    }
                    if (top.absoluteValue > maxOffsetTop.absoluteValue) {
                        offsetTop = top
                        maxOffsetTop = top
                    }
                }
                if (offsetLeft != 0f || offsetTop != 0f) {
                    add(offsetLabelDes(offsetLeft, offsetTop))
                }
            }

            //时长
            onInitEngraveTime(this)

            //---

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

    private fun needCheckCut(softwareVersion: Int?, moduleState: Int?): Boolean {
        return LaserPeckerHelper.parseProductInfo(
            softwareVersion ?: 0, 0
        )?.isCSeries() == true && vmApp<DeviceStateModel>().isCutModule(moduleState)
    }

    private fun widthHeightLabelDes(width: Float, height: Float, isMm: Boolean): LabelDesData {
        val w = valueUnit.formatValue(
            if (isMm) width else valueUnit.convertPixelToValue(width),
            true,
            true
        )
        val h = valueUnit.formatValue(
            if (isMm) height else valueUnit.convertPixelToValue(height),
            true,
            true
        )
        return formatLabelDes(
            _string(R.string.print_range),
            buildString {
                append(_string(R.string.width))
                append(":")
                append(w)
                append(" ")
                append(_string(R.string.height))
                append(":")
                append(h)
            }
        )
    }

    @MM
    private fun offsetLabelDes(left: Float, top: Float, isMm: Boolean = true): LabelDesData {
        val l = valueUnit.formatValue(
            if (isMm) left else valueUnit.convertPixelToValue(left),
            true,
            true
        )
        val t = valueUnit.formatValue(
            if (isMm) top else valueUnit.convertPixelToValue(top),
            true,
            true
        )
        return formatLabelDes(
            _string(R.string.calibration_offset_label),
            buildString {
                append(_string(R.string.calibration_offset_left))
                append(l)
                append(" ")
                append(_string(R.string.calibration_offset_top))
                append(t)
            }
        )
    }

    /**是否要显示所有的图层雕刻参数信息*/
    fun showAllEngraveLayerData(): Boolean = this is EngraveTaskHistoryItem

    /**创建参数信息*/
    fun createEngraveConfig(
        list: MutableList<DslAdapterItem>,
        engraveConfigEntity: EngraveConfigEntity
    ) {
        list.add(EngraveLabelItem().apply {
            val label = LayerHelper.getEngraveLayerInfo(engraveConfigEntity.layerId)?.label.or()
            if (HawkEngraveKeys.enableConfigIcon) {
                itemText = span {
                    append(label)
                    append(" ")
                    MaterialEntity.createLaserTypeDrawable(engraveConfigEntity.type.toInt())?.let {
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
            itemLayerId = engraveConfigEntity.layerId
        })
    }

    /**雕刻时长, 或者雕刻任务的总时长*/
    open fun onInitEngraveTime(list: MutableList<LabelDesData>) {

    }
}