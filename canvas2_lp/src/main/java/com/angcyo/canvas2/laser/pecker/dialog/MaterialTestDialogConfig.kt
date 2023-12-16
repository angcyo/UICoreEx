package com.angcyo.canvas2.laser.pecker.dialog

import android.app.Dialog
import android.content.Context
import androidx.annotation.WorkerThread
import com.angcyo.bluetooth.fsc.laserpacker.DeviceStateModel
import com.angcyo.bluetooth.fsc.laserpacker.HawkEngraveKeys
import com.angcyo.bluetooth.fsc.laserpacker.LaserPeckerHelper
import com.angcyo.bluetooth.fsc.laserpacker.LaserPeckerModel
import com.angcyo.canvas.render.core.CanvasRenderDelegate
import com.angcyo.canvas.render.element.limitElementMaxSizeMatrix
import com.angcyo.canvas.render.renderer.CanvasGroupRenderer
import com.angcyo.canvas2.laser.pecker.R
import com.angcyo.canvas2.laser.pecker.dialog.dslitem.LayerSegmentItem
import com.angcyo.canvas2.laser.pecker.dialog.dslitem.MaterialTestGroupHeaderItem
import com.angcyo.canvas2.laser.pecker.dialog.dslitem.PCTImageItem
import com.angcyo.canvas2.laser.pecker.dialog.dslitem.TablePreviewItem
import com.angcyo.canvas2.laser.pecker.engrave.dslitem.engrave.EngraveLaserSegmentItem
import com.angcyo.canvas2.laser.pecker.engrave.dslitem.engrave.EngraveOptionWheelItem
import com.angcyo.canvas2.laser.pecker.engrave.dslitem.transfer.TransferDataPxItem
import com.angcyo.core.vmApp
import com.angcyo.dialog.BaseRecyclerDialogConfig
import com.angcyo.dialog.configFullScreenDialog
import com.angcyo.dialog.normalIosDialog
import com.angcyo.dialog2.dslitem.getSelectedWheelIntData
import com.angcyo.dialog2.dslitem.itemSelectedIndex
import com.angcyo.dialog2.dslitem.itemWheelList
import com.angcyo.dsladapter.DslAdapterItem
import com.angcyo.dsladapter.renderSubItem
import com.angcyo.item.DslIncrementNumberItem
import com.angcyo.item.style.itemAdjustChangedAfterAction
import com.angcyo.item.style.itemCurrentIndex
import com.angcyo.item.style.itemIncrementMaxValue
import com.angcyo.item.style.itemIncrementMinValue
import com.angcyo.item.style.itemIncrementValue
import com.angcyo.item.style.itemLabelText
import com.angcyo.item.style.itemTabEquWidthCountRange
import com.angcyo.laserpacker.device.DeviceHelper
import com.angcyo.laserpacker.device.EngraveHelper
import com.angcyo.laserpacker.device.LayerHelper
import com.angcyo.laserpacker.device.ensurePrintPrecision
import com.angcyo.library.annotation.DSL
import com.angcyo.library.canvas.core.Reason
import com.angcyo.library.component.Strategy
import com.angcyo.library.ex._dimen
import com.angcyo.library.ex._string
import com.angcyo.library.ex.add
import com.angcyo.library.ex.size
import com.angcyo.objectbox.laser.pecker.entity.EngraveConfigEntity
import com.angcyo.widget.DslViewHolder
import kotlin.math.min

/**
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2023/12/05
 */
class MaterialTestDialogConfig : BaseRecyclerDialogConfig(), IParameterComparisonTableProvider {

    var renderDelegate: CanvasRenderDelegate? = null

    val laserPeckerModel = vmApp<LaserPeckerModel>()
    val deviceStateModel = vmApp<DeviceStateModel>()

    val settingTypeList =
        listOf(_string(R.string.basics_setting), _string(R.string.engrave_setting))

    val settingTypeExtendList = booleanArrayOf(true, true)

    var settingType = 0

    override var elementMargin: Float = 2f
    override var gridPrintType: Byte = DeviceHelper.getProductLaserType()

    init {
        dialogTitle = _string(R.string.canvas_material_test)
        dialogLayoutId = R.layout.dialog_material_test
        dialogMaxHeight = null

        positiveButton { dialog, dialogViewHolder ->
            dialog.dismiss()
            addParameterComparisonTable()
        }

        //隐藏无关的label
        ParameterComparisonTableDialogConfig.hideFunInt =
            0.add(ParameterComparisonTableDialogConfig.HIDE_GRID)
                .add(ParameterComparisonTableDialogConfig.HIDE_LABEL)

        onRenderAdapterAction = {
            val rootAdapter = this
            if (deviceStateModel.isPenMode() || laserPeckerModel.isZOpen()) {
                //画布模式下, 只能用GCode
                ParameterComparisonTableDialogConfig.gridLayerId = LaserPeckerHelper.LAYER_LINE
            }

            TablePreviewItem()() {
                iParameterComparisonTableProvider = this@MaterialTestDialogConfig
            }

            /*DslSegmentSolidTabItem()() {
                itemSegmentList = settingTypeList
                itemCurrentIndex = settingType
                itemTabLayoutWidth = LinearLayout.LayoutParams.MATCH_PARENT
                itemTabEquWidthCountRange = "2~"
                itemTabSelectIndexChangeAction = { _, selectIndexList, _, _ ->
                    settingType = selectIndexList.first()
                    refreshDslAdapter()
                }
            }*/

            MaterialTestGroupHeaderItem()() {
                itemLabel = settingTypeList[0]
                itemGroupExtend = settingTypeExtendList[0]
                observeItemGroupExtendChange {
                    settingTypeExtendList[0] = itemGroupExtend
                }

                renderSubItem {
                    DslIncrementNumberItem()() {
                        itemLabelText = _string(R.string.max_power_label)
                        itemIncrementValue = ParameterComparisonTableDialogConfig.maxPower
                        itemIncrementMinValue = 1
                        itemIncrementMaxValue = 100
                        itemAdjustChangedAfterAction = {
                            ParameterComparisonTableDialogConfig.maxPower =
                                it?.toString()?.toIntOrNull()
                                    ?: ParameterComparisonTableDialogConfig.maxPower
                            rootAdapter.updateTablePreview()
                        }
                        initItem()
                    }
                    DslIncrementNumberItem()() {
                        itemLabelText = _string(R.string.max_depth_label)
                        itemIncrementValue = ParameterComparisonTableDialogConfig.maxDepth
                        itemIncrementMinValue = 1
                        itemIncrementMaxValue = 100
                        itemAdjustChangedAfterAction = {
                            ParameterComparisonTableDialogConfig.maxDepth =
                                it?.toString()?.toIntOrNull()
                                    ?: ParameterComparisonTableDialogConfig.maxDepth
                            rootAdapter.updateTablePreview()
                        }
                        initItem()
                    }
                    DslIncrementNumberItem()() {
                        itemLabelText = _string(R.string.min_power_label)
                        itemIncrementValue = ParameterComparisonTableDialogConfig.minPower
                        itemIncrementMinValue = 1
                        itemIncrementMaxValue = 100
                        itemAdjustChangedAfterAction = {
                            ParameterComparisonTableDialogConfig.minPower =
                                it?.toString()?.toIntOrNull()
                                    ?: ParameterComparisonTableDialogConfig.minPower
                            rootAdapter.updateTablePreview()
                        }
                        initItem()
                    }
                    DslIncrementNumberItem()() {
                        itemLabelText = _string(R.string.min_depth_label)
                        itemIncrementValue = ParameterComparisonTableDialogConfig.minDepth
                        itemIncrementMinValue = 1
                        itemIncrementMaxValue = 100
                        itemAdjustChangedAfterAction = {
                            ParameterComparisonTableDialogConfig.minDepth =
                                it?.toString()?.toIntOrNull()
                                    ?: ParameterComparisonTableDialogConfig.minDepth
                            rootAdapter.updateTablePreview()
                        }
                        initItem()
                    }
                    DslIncrementNumberItem()() {
                        itemLabelText = _string(R.string.add_parameter_comparison_table_columns)
                        itemIncrementValue =
                            ParameterComparisonTableDialogConfig.horizontalGridCount
                        itemIncrementMinValue = 1
                        itemIncrementMaxValue = 20
                        itemAdjustChangedAfterAction = {
                            ParameterComparisonTableDialogConfig.horizontalGridCount =
                                it?.toString()?.toIntOrNull()
                                    ?: ParameterComparisonTableDialogConfig.horizontalGridCount
                            rootAdapter.updateTablePreview()
                        }
                        initItem()
                    }
                    DslIncrementNumberItem()() {
                        itemLabelText = _string(R.string.add_parameter_comparison_table_rows)
                        itemIncrementValue = ParameterComparisonTableDialogConfig.verticalGridCount
                        itemIncrementMinValue = 1
                        itemIncrementMaxValue = 20
                        itemAdjustChangedAfterAction = {
                            ParameterComparisonTableDialogConfig.verticalGridCount =
                                it?.toString()?.toIntOrNull()
                                    ?: ParameterComparisonTableDialogConfig.verticalGridCount
                            rootAdapter.updateTablePreview()
                        }
                        initItem()
                    }
                }
            }

            MaterialTestGroupHeaderItem()() {
                itemLabel = settingTypeList[1]
                itemGroupExtend = settingTypeExtendList[1]
                observeItemGroupExtendChange {
                    settingTypeExtendList[1] = itemGroupExtend
                }
                renderSubItem {

                    //图层选择
                    if (!deviceStateModel.isPenMode()) {
                        LayerSegmentItem()() {
                            itemIncludeCutLayer = true
                            itemCurrentIndex = LayerHelper.getEngraveLayerList(itemIncludeCutLayer)
                                .indexOfFirst { it.layerId == ParameterComparisonTableDialogConfig.gridLayerId }
                            observeItemChange {
                                ParameterComparisonTableDialogConfig.gridLayerId =
                                    currentLayerInfo().layerId
                                //rootAdapter.updateTablePreview()
                                refreshDslAdapter()
                            }
                            itemTabEquWidthCountRange = ""
                            initItem(2)
                        }
                        if (ParameterComparisonTableDialogConfig.gridLayerId == LaserPeckerHelper.LAYER_PICTURE) {
                            PCTImageItem()() {
                                initItem(2)
                            }
                        } else {
                            ParameterComparisonTableDialogConfig.selectImage = null
                        }
                    } else {
                        ParameterComparisonTableDialogConfig.selectImage = null
                    }

                    //激光类型选择 激光光源选择
                    if (laserPeckerModel.productInfoData.value?.isCSeries() != true) {
                        val typeList = LaserPeckerHelper.findProductSupportLaserTypeList()
                        if (typeList.size() > 1) {
                            //激光类型
                            EngraveLaserSegmentItem()() {
                                observeItemChange {
                                    val type = currentLaserTypeInfo().type
                                    gridPrintType = type
                                    HawkEngraveKeys.lastType = type.toInt()

                                    rootAdapter.updateTablePreview()
                                }
                                initItem(2)
                            }
                        }
                    }

                    if (laserPeckerModel.isCSeries()) {
                        //C1 加速级别选择 加速级别
                        EngraveOptionWheelItem()() {
                            itemTag = EngraveConfigEntity::precision.name
                            itemLabelText = _string(R.string.engrave_precision)
                            itemWheelList = EngraveHelper.percentList(5)
                            itemSelectedIndex = EngraveHelper.findOptionIndex(
                                itemWheelList,
                                ParameterComparisonTableDialogConfig.gridPrintPrecision
                            )
                            observeItemChange {
                                ParameterComparisonTableDialogConfig.gridPrintPrecision =
                                    getSelectedWheelIntData(def = ParameterComparisonTableDialogConfig.gridPrintPrecision).ensurePrintPrecision()
                            }
                            initItem(2)
                        }
                    }

                    //---雕刻参数---

                    //分辨率dpi
                    if (LayerHelper.showDpiConfig(ParameterComparisonTableDialogConfig.gridLayerId)) {
                        TransferDataPxItem()() {
                            itemPxList = LaserPeckerHelper.findProductLayerSupportPxList(
                                ParameterComparisonTableDialogConfig.gridLayerId
                            )
                            selectorCurrentDpi(
                                LayerHelper.getProductLastLayerDpi(
                                    ParameterComparisonTableDialogConfig.gridLayerId
                                )
                            )
                            itemHidden = itemPxList.isNullOrEmpty() //自动隐藏
                            observeItemChange {
                                //保存最后一次选择的dpi
                                val dpi =
                                    itemPxList?.get(itemCurrentIndex)?.dpi
                                        ?: LaserPeckerHelper.DPI_254
                                HawkEngraveKeys.updateLayerDpi(
                                    ParameterComparisonTableDialogConfig.gridLayerId,
                                    dpi
                                )

                                rootAdapter.updateTablePreview()
                            }
                            initItem(2)
                        }
                    }

                    //其他数据的参数
                    //功率/深度/次数
                    /*EngravePropertyItem()() {
                        itemShowTimes = false
                        itemShowPopupTip = false
                        itemLabelText = ""
                    }*/
                }
            }

            if (settingType == 0) {
            } else {
            }

            //格子数量选择
            /*GridCountItem()() {
                itemColumns = ParameterComparisonTableDialogConfig.horizontalGridCount
                itemRows = ParameterComparisonTableDialogConfig.verticalGridCount

                onItemChangeAction = {
                    ParameterComparisonTableDialogConfig.horizontalGridCount = itemColumns
                    ParameterComparisonTableDialogConfig.verticalGridCount = itemRows
                }
            }*/

            //强制指定功率深度
            /*AppointPowerDepthItem()()

            //额外的行列范围
            RowsColumnsRangeItem()()

            //雕刻次数
            PrintCountItem()()

            //备注标签
            PCTLabelItem()()

            //字体大小, 边距
            LabelSizeItem()() {
                itemTextFontSize = ParameterComparisonTableDialogConfig.pctTextFontSize
                itemGridItemMargin = ParameterComparisonTableDialogConfig.gridItemMargin

                onItemChangeAction = {
                    ParameterComparisonTableDialogConfig.pctTextFontSize = itemTextFontSize
                    ParameterComparisonTableDialogConfig.gridItemMargin = itemGridItemMargin
                }
            }*/
        }
    }

    private fun DslAdapterItem.initItem(tag: Int = 1) {
        itemPaddingLeft = _dimen(R.dimen.lib_xhdpi) * tag
        itemPaddingRight = itemPaddingLeft
        itemPaddingTop = itemPaddingLeft / 2
        itemPaddingBottom = itemPaddingLeft / 2
    }

    /**添加 功率 深度, 雕刻参数对照表. 耗时操作, 建议在子线程中执行*/
    @WorkerThread
    private fun addParameterComparisonTable() {
        val delegate = renderDelegate ?: return

        val result = ParameterComparisonTableDialogConfig.parseParameterComparisonTable(this)
        if (result.size() == 1) {
            result.first().apply {
                if (this is CanvasGroupRenderer) {
                    var groupRenderProperty = getGroupRenderProperty()
                    var bounds = groupRenderProperty.getRenderBounds()
                    val matrix =
                        bounds.limitElementMaxSizeMatrix(
                            ParameterComparisonTableDialogConfig.tableBounds.width(),
                            ParameterComparisonTableDialogConfig.tableBounds.height()
                        )
                    applyScaleMatrix(matrix, Reason.code, null)
                    groupRenderProperty = getGroupRenderProperty()
                    bounds = groupRenderProperty.getRenderBounds()
                    val cx = min(
                        ParameterComparisonTableDialogConfig.tableBounds.centerX(),
                        ParameterComparisonTableDialogConfig.tableBounds.centerY()
                    )
                    val cy = cx
                    matrix.setTranslate(cx - bounds.centerX(), cy - bounds.centerY())
                    applyTranslateMatrix(matrix, Reason.code, null)
                }
            }
        }

        //添加到渲染器
        delegate.renderManager.addElementRenderer(result, true, Reason.user, Strategy.normal)
    }

    override fun initDialogView(dialog: Dialog, dialogViewHolder: DslViewHolder) {
        super.initDialogView(dialog, dialogViewHolder)
    }

    override fun onDialogBack(dialog: Dialog, dialogViewHolder: DslViewHolder): Boolean {
        dialog.context.normalIosDialog {
            dialogTitle = _string(R.string.variable_back_title)
            dialogMessage = _string(R.string.variable_back_tip)
            positiveButton { dialog2, dialogViewHolder ->
                dialog2.dismiss()
                dialog.dismiss()
            }
        }
        return true
    }
}

@DSL
fun Context.addMaterialTestDialog(config: MaterialTestDialogConfig.() -> Unit): Dialog {
    return MaterialTestDialogConfig().run {
        dialogContext = this@addMaterialTestDialog
        configFullScreenDialog()
        config()
        show()
    }
}