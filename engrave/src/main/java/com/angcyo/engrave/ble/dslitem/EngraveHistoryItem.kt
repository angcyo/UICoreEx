package com.angcyo.engrave.ble.dslitem

import com.angcyo.bluetooth.fsc.laserpacker.LaserPeckerHelper
import com.angcyo.bluetooth.fsc.laserpacker.command.DataCmd
import com.angcyo.canvas.data.CanvasProjectItemBean.Companion.MM_UNIT
import com.angcyo.canvas.utils.CanvasConstant
import com.angcyo.dsladapter.DslAdapterItem
import com.angcyo.engrave.EngraveFlowDataHelper
import com.angcyo.engrave.R
import com.angcyo.engrave.toEngraveTime
import com.angcyo.engrave.transition.EngraveTransitionManager
import com.angcyo.engrave.transition.IEngraveTransition
import com.angcyo.glide.loadImage
import com.angcyo.item.DslTagGroupItem
import com.angcyo.item.data.LabelDesData
import com.angcyo.library.ex._string
import com.angcyo.library.ex.isDebug
import com.angcyo.library.ex.or
import com.angcyo.library.ex.toStr
import com.angcyo.library.unit.convertPixelToValueUnit
import com.angcyo.objectbox.laser.pecker.entity.EngraveConfigEntity
import com.angcyo.objectbox.laser.pecker.entity.EngraveDataEntity
import com.angcyo.objectbox.laser.pecker.entity.TransferDataEntity
import com.angcyo.widget.DslViewHolder

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
                itemEngraveConfigEntity = EngraveFlowDataHelper.getEngraveConfig(
                    itemTransferDataEntity?.taskId,
                    itemTransferDataEntity?.layerMode ?: 0
                )
            }
        }

    /**可视化的单位*/
    private val valueUnit = CanvasConstant.valueUnit

    private val mmValueUnit = MM_UNIT

    /**传输的数据, 如果没有传输的数据, 说明数据不在本机产生, 则只能直接雕刻, 不能预览*/
    var itemTransferDataEntity: TransferDataEntity? = null

    /**雕刻的配置的数据*/
    var itemEngraveConfigEntity: EngraveConfigEntity? = null

    init {
        itemLayoutId = R.layout.item_engrave_history_layout
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
        itemHolder.tv(R.id.lib_text_view)?.text = name
        itemHolder.tv(R.id.lib_text_view2)?.text = name

        itemHolder.visible(R.id.lib_text_view2, itemTransferDataEntity == null)

        itemHolder.visible(R.id.lib_text_view, itemTransferDataEntity != null)
        itemHolder.visible(R.id.lib_image_view, itemTransferDataEntity != null)
        itemHolder.visible(R.id.lib_flow_layout, itemTransferDataEntity != null)

        if (itemTransferDataEntity == null) {
            //只能直接雕刻的数据
        } else {
            //预览图
            val previewImagePath =
                IEngraveTransition.getEngravePreviewBitmapPath(itemEngraveDataEntity?.index)
            itemHolder.img(R.id.lib_image_view)?.loadImage(previewImagePath)
        }
    }

    override fun initLabelDesList() {
        val transferDataEntity = itemTransferDataEntity
        val engraveConfigEntity = itemEngraveConfigEntity
        renderLabelDesList {

            if (isDebug()) {
                add(LabelDesData("索引", "${itemEngraveDataEntity?.index}"))
                engraveConfigEntity?.exDevice?.let {
                    add(LabelDesData("模式", it))
                }
            }

            /*add(
                LabelDesData(_string(R.string.engrave_file_name), itemTransferDataEntity?.name.or())
            )*/

            if (transferDataEntity != null) {
                EngraveTransitionManager.getEngraveLayer(transferDataEntity.layerMode)?.let {
                    add(LabelDesData(_string(R.string.engrave_layer_config), it.label))
                }

                var width = transferDataEntity.width
                var height = transferDataEntity.height
                if (transferDataEntity.engraveDataType == DataCmd.ENGRAVE_TYPE_GCODE) {
                    width /= 10
                    height /= 10
                }
                add(
                    LabelDesData(
                        _string(R.string.print_range),
                        buildString {
                            append(
                                valueUnit.convertPixelToValueUnit(
                                    mmValueUnit.convertValueToPixel(width.toFloat())
                                )
                            )
                            append(" × ")
                            append(
                                valueUnit.convertPixelToValueUnit(
                                    mmValueUnit.convertValueToPixel(height.toFloat())
                                )
                            )
                        }
                    )
                )

                val pxInfo = LaserPeckerHelper.findPxInfo(transferDataEntity.dpi)
                add(LabelDesData(_string(R.string.resolution_ratio), pxInfo.des))
            }

            //---

            if (engraveConfigEntity != null) {
                //材质
                add(
                    LabelDesData(
                        _string(R.string.custom_material),
                        EngraveFlowDataHelper.getEngraveMaterNameByKey(engraveConfigEntity.materialKey)
                    )
                )

                if ((engraveConfigEntity.precision ?: 0) > 0) {
                    add(
                        LabelDesData(
                            _string(R.string.engrave_precision),
                            "${engraveConfigEntity.precision}"
                        )
                    )
                }

                add(LabelDesData(_string(R.string.custom_power), "${engraveConfigEntity.power}%"))
                add(LabelDesData(_string(R.string.custom_speed), "${engraveConfigEntity.depth}%"))
            }

            //---

            val startEngraveTime = itemEngraveDataEntity?.startTime ?: 0
            val endEngraveTime = itemEngraveDataEntity?.finishTime ?: 0
            if (endEngraveTime > 0 && startEngraveTime > 0 && endEngraveTime > startEngraveTime) {
                val engraveTime = (endEngraveTime - startEngraveTime).toEngraveTime()
                add(LabelDesData(_string(R.string.work_time), engraveTime))
            }
        }
    }

}