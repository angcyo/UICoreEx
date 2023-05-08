package com.angcyo.canvas2.laser.pecker.dialog

import android.content.Context
import android.view.View
import com.angcyo.bluetooth.fsc.laserpacker.HawkEngraveKeys
import com.angcyo.bluetooth.fsc.laserpacker.LaserPeckerConfigHelper
import com.angcyo.canvas.render.core.CanvasRenderDelegate
import com.angcyo.canvas2.laser.pecker.R
import com.angcyo.canvas2.laser.pecker.util.LPConstant
import com.angcyo.dialog.TargetWindow
import com.angcyo.dialog.dismissWindow
import com.angcyo.dialog.popup.ShadowAnchorPopupConfig
import com.angcyo.dsladapter.drawBottom
import com.angcyo.item.DslBlackButtonItem
import com.angcyo.item.DslSwitchInfoItem
import com.angcyo.item.style.itemInfoText
import com.angcyo.item.style.itemSwitchChangedAction
import com.angcyo.item.style.itemSwitchChecked
import com.angcyo.library._screenHeight
import com.angcyo.library._screenWidth
import com.angcyo.library.component.pad.isInPadMode
import com.angcyo.library.ex._dimen
import com.angcyo.library.ex._string
import com.angcyo.library.ex.dpi
import com.angcyo.library.ex.have
import com.angcyo.library.ex.isDebug
import com.angcyo.library.ex.isShowDebug
import com.angcyo.library.unit.InchRenderUnit
import com.angcyo.library.unit.MmRenderUnit
import com.angcyo.library.unit.PxRenderUnit
import com.angcyo.widget.DslViewHolder
import com.angcyo.widget.recycler.renderDslAdapter
import com.hingin.umeng.UMEvent
import com.hingin.umeng.umengEventValue
import kotlin.math.min

/**
 * 画图设置弹窗
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2023-3-13
 */
class CanvasSettingPopupConfig : ShadowAnchorPopupConfig() {

    /**画布*/
    var delegate: CanvasRenderDelegate? = null

    init {
        contentLayoutId = R.layout.dialog_setting_layout
        triangleMinMargin = 24 * dpi
        //yoff = -10 * dpi
        offsetY = -10 * dpi
    }

    override fun initContentLayout(window: TargetWindow, viewHolder: DslViewHolder) {
        super.initContentLayout(window, viewHolder)
        viewHolder.rv(R.id.lib_recycler_view)?.renderDslAdapter {

            val enableFun = LaserPeckerConfigHelper.readDeviceSettingConfig()?.enableFun
            if (HawkEngraveKeys.enableParameterComparisonTable || enableFun.have("_ParameterComparisonTable_")) {
                DslBlackButtonItem()() {
                    itemButtonText = _string(R.string.add_parameter_comparison_table)
                    itemClick = {
                        window.dismissWindow()
                        it.context.addParameterComparisonTableDialog {
                            renderDelegate = delegate
                        }
                    }
                }
            }
            if (HawkEngraveKeys.enableMultiplicationTable || enableFun.have("_MultiplicationTable_")) {
                DslBlackButtonItem()() {
                    itemButtonText = _string(R.string.add_multiplication_table)
                    itemClick = {
                        window.dismissWindow()
                        HawkEngraveKeys.enableSingleItemTransfer = true //必须
                        ParameterComparisonTableDialogConfig.addMultiplicationTable(delegate)
                    }
                }
            }

            if (HawkEngraveKeys.enableVisualChartTable || enableFun.have("_VisualChartTable_")) {
                DslBlackButtonItem()() {
                    itemButtonText = _string(R.string.add_visual_chart)
                    itemClick = {
                        window.dismissWindow()
                        HawkEngraveKeys.enableSingleItemTransfer = true //必须
                        ParameterComparisonTableDialogConfig.addVisualChart(delegate)
                    }
                }
            }
            if (isDebug()) {
                DslSwitchInfoItem()() {
                    itemInfoText = _string(R.string.canvas_cloud_storage)
                    itemSwitchChecked = HawkEngraveKeys.enableCloudStorage
                    drawBottom(_dimen(R.dimen.lib_line_px), 0, 0)
                    itemExtendLayoutId = R.layout.canvas_extent_switch_item
                    itemSwitchChangedAction = {
                        HawkEngraveKeys.enableCloudStorage = it
                    }
                }
            }
            if (HawkEngraveKeys.enablePixelUnit || enableFun.have("_PixelUnit_")) {
                DslSwitchInfoItem()() {
                    itemTag = "pixel"
                    itemInfoText = _string(R.string.canvas_pixel_unit)
                    itemSwitchChecked = delegate?.axisManager?.renderUnit is PxRenderUnit
                    drawBottom(_dimen(R.dimen.lib_line_px), 0, 0)
                    itemExtendLayoutId = R.layout.canvas_extent_switch_item
                    itemSwitchChangedAction = {
                        delegate?.axisManager?.updateRenderUnit(
                            if (it) {
                                (get("inch") as? DslSwitchInfoItem)?.apply {
                                    itemSwitchChecked = false
                                    updateAdapterItem()
                                }
                                LPConstant.CANVAS_VALUE_UNIT = LPConstant.CANVAS_VALUE_UNIT_PIXEL
                                PxRenderUnit()
                            } else {
                                LPConstant.CANVAS_VALUE_UNIT = LPConstant.CANVAS_VALUE_UNIT_MM
                                MmRenderUnit()
                            }
                        )
                    }
                }
            }
            DslSwitchInfoItem()() {
                itemTag = "inch"
                itemInfoText = _string(R.string.canvas_inch_unit)
                itemSwitchChecked = delegate?.axisManager?.renderUnit is InchRenderUnit
                drawBottom(_dimen(R.dimen.lib_line_px), 0, 0)
                itemExtendLayoutId = R.layout.canvas_extent_switch_item
                itemSwitchChangedAction = {
                    delegate?.axisManager?.updateRenderUnit(
                        if (it) {
                            if (isShowDebug()) {
                                (get("pixel") as? DslSwitchInfoItem)?.apply {
                                    itemSwitchChecked = false
                                    updateAdapterItem()
                                }
                            }
                            LPConstant.CANVAS_VALUE_UNIT = LPConstant.CANVAS_VALUE_UNIT_INCH
                            InchRenderUnit()
                        } else {
                            LPConstant.CANVAS_VALUE_UNIT = LPConstant.CANVAS_VALUE_UNIT_MM
                            MmRenderUnit()
                        }
                    )
                    if (it) {
                        UMEvent.INCH_UNIT.umengEventValue()
                    } else {
                        UMEvent.MM_UNIT.umengEventValue()
                    }
                }
            }
            DslSwitchInfoItem()() {
                itemInfoText = _string(R.string.canvas_grid)
                itemSwitchChecked = LPConstant.CANVAS_DRAW_GRID
                drawBottom(_dimen(R.dimen.lib_line_px), 0, 0)
                itemExtendLayoutId = R.layout.canvas_extent_switch_item
                itemSwitchChangedAction = {
                    LPConstant.CANVAS_DRAW_GRID = it
                    if (it) {
                        delegate?.axisManager?.enableRenderGrid = true
                        delegate?.axisManager?.enableRenderGrid = true
                    } else {
                        delegate?.axisManager?.enableRenderGrid = false
                        delegate?.axisManager?.enableRenderGrid = false
                    }
                    delegate?.refresh()
                }
            }
            DslSwitchInfoItem()() {
                val smartAssistant = delegate?.controlManager?.smartAssistantComponent
                itemInfoText = _string(R.string.canvas_smart_assistant)
                itemSwitchChecked = smartAssistant?.isEnableComponent == true
                itemExtendLayoutId = R.layout.canvas_extent_switch_item
                itemSwitchChangedAction = {
                    smartAssistant?.isEnableComponent = it
                    LPConstant.CANVAS_SMART_ASSISTANT = it
                    if (it) {
                        UMEvent.SMART_ASSISTANT.umengEventValue()
                    }
                }
            }
        }
    }
}

/**Dsl*/
fun Context.canvasSettingWindow(anchor: View?, config: CanvasSettingPopupConfig.() -> Unit): Any {
    val popupConfig = CanvasSettingPopupConfig()
    popupConfig.anchor = anchor
    if (isInPadMode()) {
        popupConfig.width = min(_screenWidth, _screenHeight)
    }
    popupConfig.config()
    return popupConfig.show(this)
}