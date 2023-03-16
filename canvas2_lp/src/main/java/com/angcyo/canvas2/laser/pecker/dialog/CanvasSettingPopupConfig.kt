package com.angcyo.canvas2.laser.pecker.dialog

import android.content.Context
import android.graphics.RectF
import android.view.View
import com.angcyo.bluetooth.fsc.laserpacker.LaserPeckerModel
import com.angcyo.canvas.render.core.CanvasRenderDelegate
import com.angcyo.canvas.render.unit.InchRenderUnit
import com.angcyo.canvas.render.unit.MmRenderUnit
import com.angcyo.canvas.render.unit.PxRenderUnit
import com.angcyo.canvas2.laser.pecker.R
import com.angcyo.canvas2.laser.pecker.util.LPConstant
import com.angcyo.core.vmApp
import com.angcyo.dialog.TargetWindow
import com.angcyo.dialog.dismissWindow
import com.angcyo.dialog.inputDialog
import com.angcyo.dialog.popup.ShadowAnchorPopupConfig
import com.angcyo.dsladapter.drawBottom
import com.angcyo.engrave.data.HawkEngraveKeys
import com.angcyo.engrave.engraveStrokeLoadingCaller
import com.angcyo.http.rx.doBack
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
import com.angcyo.library.ex.isShowDebug
import com.angcyo.library.unit.toPixel
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
    var canvasDelegate: CanvasRenderDelegate? = null

    init {
        contentLayoutId = R.layout.dialog_setting_layout
        triangleMinMargin = 24 * dpi
        //yoff = -10 * dpi
        offsetY = -10 * dpi
    }

    override fun initContentLayout(window: TargetWindow, viewHolder: DslViewHolder) {
        super.initContentLayout(window, viewHolder)
        val canvasViewBox = canvasDelegate?.renderViewBox
        viewHolder.rv(R.id.lib_recycler_view)?.renderDslAdapter {

            val previewBounds =
                vmApp<LaserPeckerModel>().productInfoData.value?.previewBounds ?: RectF(
                    0f,
                    0f,
                    100f.toPixel(),
                    100f.toPixel()
                )

            if (HawkEngraveKeys.enableParameterComparisonTable) {
                DslBlackButtonItem()() {
                    itemButtonText = "添加参数对照表"
                    itemClick = {
                        window.dismissWindow()

                        it.context.inputDialog {
                            dialogTitle = "阈值"
                            canInputEmpty = false
                            hintInputString = "粒度数量,功率*深度阈值,网格间隙,字体大小"
                            defaultInputString =
                                "${HawkEngraveKeys.lastGridCount},${HawkEngraveKeys.lastPowerDepth},${HawkEngraveKeys.lastGridMargin},${HawkEngraveKeys.lastFontSize}"

                            onInputResult = { dialog, inputText ->
                                val list = inputText.toString().split(",")
                                HawkEngraveKeys.lastGridCount = list.getOrNull(0)?.toIntOrNull()
                                    ?: HawkEngraveKeys.lastGridCount
                                HawkEngraveKeys.lastPowerDepth = list.getOrNull(1)?.toIntOrNull()
                                    ?: HawkEngraveKeys.lastPowerDepth
                                HawkEngraveKeys.lastGridMargin = list.getOrNull(2)?.toIntOrNull()
                                    ?: HawkEngraveKeys.lastGridMargin
                                HawkEngraveKeys.lastFontSize = list.getOrNull(3)?.toIntOrNull()
                                    ?: HawkEngraveKeys.lastFontSize

                                /*engraveStrokeLoadingCaller { isCancel, loadEnd ->
                                    doBack {
                                        HawkEngraveKeys.enableItemEngraveParams = true //必须
                                        HawkEngraveKeys.enableSingleItemTransfer = true //必须
                                        canvasDelegate?.addParameterComparisonTable(
                                            previewBounds,
                                            HawkEngraveKeys.lastGridCount,
                                            HawkEngraveKeys.lastPowerDepth,
                                            HawkEngraveKeys.lastGridMargin,
                                            HawkEngraveKeys.lastFontSize,
                                        )
                                        loadEnd(true, null)
                                    }
                                }*/
                                false
                            }
                        }
                    }
                }
            }
            if (HawkEngraveKeys.enableMultiplicationTable) {
                DslBlackButtonItem()() {
                    itemButtonText = "添加乘法口诀表"
                    itemClick = {
                        window.dismissWindow()
                        engraveStrokeLoadingCaller { isCancel, loadEnd ->
                            doBack {
                                HawkEngraveKeys.enableSingleItemTransfer = true //必须
                                //canvasDelegate?.addMultiplicationTable(previewBounds)
                                loadEnd(true, null)
                            }
                        }
                    }
                }
            }

            if (HawkEngraveKeys.enableVisualChart) {
                DslBlackButtonItem()() {
                    itemButtonText = "添加视力表"
                    itemClick = {
                        window.dismissWindow()
                        engraveStrokeLoadingCaller { isCancel, loadEnd ->
                            doBack {
                                HawkEngraveKeys.enableSingleItemTransfer = true //必须
                                //canvasDelegate?.addVisualChart(previewBounds)
                                loadEnd(true, null)
                            }
                        }
                    }
                }
            }

            if (HawkEngraveKeys.enablePixelUnit) {
                DslSwitchInfoItem()() {
                    itemTag = "pixel"
                    itemInfoText = _string(R.string.canvas_pixel_unit)
                    itemSwitchChecked = canvasDelegate?.axisManager?.renderUnit is PxRenderUnit
                    drawBottom(_dimen(R.dimen.lib_line_px), 0, 0)
                    itemExtendLayoutId = R.layout.canvas_extent_switch_item
                    itemSwitchChangedAction = {
                        canvasDelegate?.axisManager?.updateRenderUnit(
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
                itemSwitchChecked = canvasDelegate?.axisManager?.renderUnit is InchRenderUnit
                drawBottom(_dimen(R.dimen.lib_line_px), 0, 0)
                itemExtendLayoutId = R.layout.canvas_extent_switch_item
                itemSwitchChangedAction = {
                    canvasDelegate?.axisManager?.updateRenderUnit(
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
                        canvasDelegate?.axisManager?.enableRenderGrid = true
                        canvasDelegate?.axisManager?.enableRenderGrid = true
                    } else {
                        canvasDelegate?.axisManager?.enableRenderGrid = false
                        canvasDelegate?.axisManager?.enableRenderGrid = false
                    }
                    canvasDelegate?.refresh()
                }
            }
            /*DslSwitchInfoItem()() {
                itemInfoText = _string(R.string.canvas_smart_assistant)
                itemSwitchChecked = canvasDelegate?.smartAssistant?.enable == true
                itemExtendLayoutId = R.layout.canvas_extent_switch_item
                itemSwitchChangedAction = {
                    canvasDelegate?.smartAssistant?.enable = it
                    LPConstant.CANVAS_SMART_ASSISTANT = it
                    if (it) {
                        UMEvent.SMART_ASSISTANT.umengEventValue()
                    }
                }
            }*/
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