package com.angcyo.canvas.laser.pecker

import android.content.Context
import android.graphics.RectF
import android.view.View
import androidx.annotation.Keep
import com.angcyo.bluetooth.fsc.laserpacker.LaserPeckerModel
import com.angcyo.canvas.CanvasDelegate
import com.angcyo.canvas.graphics.addMultiplicationTable
import com.angcyo.canvas.graphics.addParameterComparisonTable
import com.angcyo.canvas.graphics.addVisualChart
import com.angcyo.canvas.utils.CanvasConstant
import com.angcyo.core.vmApp
import com.angcyo.dialog.TargetWindow
import com.angcyo.dialog.dismissWindow
import com.angcyo.dialog.inputDialog
import com.angcyo.dialog.popup.ShadowAnchorPopupConfig
import com.angcyo.dsladapter.drawBottom
import com.angcyo.http.rx.doBack
import com.angcyo.item.DslBlackButtonItem
import com.angcyo.item.DslSwitchInfoItem
import com.angcyo.item.style.itemInfoText
import com.angcyo.item.style.itemSwitchChangedAction
import com.angcyo.item.style.itemSwitchChecked
import com.angcyo.laserpacker.device.HawkEngraveKeys
import com.angcyo.laserpacker.device.engraveStrokeLoadingCaller
import com.angcyo.library._screenHeight
import com.angcyo.library._screenWidth
import com.angcyo.library.annotation.MM
import com.angcyo.library.component.HawkPropertyValue
import com.angcyo.library.component.pad.isInPadMode
import com.angcyo.library.ex._dimen
import com.angcyo.library.ex._string
import com.angcyo.library.ex.dpi
import com.angcyo.library.ex.isShowDebug
import com.angcyo.library.unit.InchValueUnit
import com.angcyo.library.unit.MmValueUnit
import com.angcyo.library.unit.PixelValueUnit
import com.angcyo.library.unit.toPixel
import com.angcyo.widget.DslViewHolder
import com.angcyo.widget.recycler.renderDslAdapter
import com.hingin.umeng.UMEvent
import com.hingin.umeng.umengEventValue
import kotlin.math.min

/**
 * 画图设置弹窗
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2022/05/16
 */
class CanvasSettingPopupConfig : ShadowAnchorPopupConfig() {

    @Keep
    companion object {
        /**参数表网格数量
         * [com.angcyo.canvas.graphics.GraphicsKtxKt.addParameterComparisonTable]
         */
        internal var lastGridCount: Int by HawkPropertyValue<Any, Int>(10)

        /**功率*深度 <= this 时才需要添加到参数表
         * [com.angcyo.canvas.graphics.GraphicsKtxKt.addParameterComparisonTable]
         * */
        internal var lastPowerDepth: Int by HawkPropertyValue<Any, Int>(2500)

        /**[addParameterComparisonTable] 字体大小*/
        @MM
        internal var lastFontSize: Int by HawkPropertyValue<Any, Int>(0)

        /**[addParameterComparisonTable] 网格间隙*/
        @MM
        internal var lastGridMargin: Int by HawkPropertyValue<Any, Int>(5)
    }

    /**画布*/
    var canvasDelegate: CanvasDelegate? = null

    init {
        contentLayoutId = R.layout.canvas_setting_layout
        triangleMinMargin = 24 * dpi
        //yoff = -10 * dpi
        offsetY = -10 * dpi
    }

    override fun initContentLayout(window: TargetWindow, viewHolder: DslViewHolder) {
        super.initContentLayout(window, viewHolder)
        val canvasViewBox = canvasDelegate?.getCanvasViewBox()
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
                                "$lastGridCount,${lastPowerDepth},${lastGridMargin},${lastFontSize}"

                            onInputResult = { dialog, inputText ->
                                val list = inputText.toString().split(",")
                                lastGridCount = list.getOrNull(0)?.toIntOrNull() ?: lastGridCount
                                lastPowerDepth = list.getOrNull(1)?.toIntOrNull() ?: lastPowerDepth
                                lastGridMargin = list.getOrNull(2)?.toIntOrNull() ?: lastGridMargin
                                lastFontSize = list.getOrNull(3)?.toIntOrNull() ?: lastFontSize

                                engraveStrokeLoadingCaller { isCancel, loadEnd ->
                                    doBack {
                                        HawkEngraveKeys.enableItemEngraveParams = true //必须
                                        HawkEngraveKeys.enableSingleItemTransfer = true //必须
                                        canvasDelegate?.addParameterComparisonTable(
                                            previewBounds,
                                            lastGridCount,
                                            lastPowerDepth,
                                            lastGridMargin,
                                            lastFontSize
                                        )
                                        loadEnd(true, null)
                                    }
                                }
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
                                canvasDelegate?.addMultiplicationTable(previewBounds)
                                loadEnd(true, null)
                            }
                        }
                    }
                }
            }

            if (HawkEngraveKeys.enableVisualChartTable) {
                DslBlackButtonItem()() {
                    itemButtonText = "添加视力表"
                    itemClick = {
                        window.dismissWindow()
                        engraveStrokeLoadingCaller { isCancel, loadEnd ->
                            doBack {
                                HawkEngraveKeys.enableSingleItemTransfer = true //必须
                                canvasDelegate?.addVisualChart(previewBounds)
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
                    itemSwitchChecked = canvasViewBox?.valueUnit is PixelValueUnit
                    drawBottom(_dimen(R.dimen.lib_line_px), 0, 0)
                    itemExtendLayoutId = R.layout.canvas_extent_switch_item
                    itemSwitchChangedAction = {
                        canvasViewBox?.updateCoordinateSystemUnit(
                            if (it) {
                                (get("inch") as? DslSwitchInfoItem)?.apply {
                                    itemSwitchChecked = false
                                    updateAdapterItem()
                                }
                                CanvasConstant.CANVAS_VALUE_UNIT =
                                    CanvasConstant.CANVAS_VALUE_UNIT_PIXEL
                                PixelValueUnit()
                            } else {
                                CanvasConstant.CANVAS_VALUE_UNIT =
                                    CanvasConstant.CANVAS_VALUE_UNIT_MM
                                MmValueUnit()
                            }
                        )
                    }
                }
            }
            DslSwitchInfoItem()() {
                itemTag = "inch"
                itemInfoText = _string(R.string.canvas_inch_unit)
                itemSwitchChecked = canvasViewBox?.valueUnit is InchValueUnit
                drawBottom(_dimen(R.dimen.lib_line_px), 0, 0)
                itemExtendLayoutId = R.layout.canvas_extent_switch_item
                itemSwitchChangedAction = {
                    canvasViewBox?.updateCoordinateSystemUnit(
                        if (it) {
                            if (isShowDebug()) {
                                (get("pixel") as? DslSwitchInfoItem)?.apply {
                                    itemSwitchChecked = false
                                    updateAdapterItem()
                                }
                            }
                            CanvasConstant.CANVAS_VALUE_UNIT = CanvasConstant.CANVAS_VALUE_UNIT_INCH
                            InchValueUnit()
                        } else {
                            CanvasConstant.CANVAS_VALUE_UNIT = CanvasConstant.CANVAS_VALUE_UNIT_MM
                            MmValueUnit()
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
                itemSwitchChecked = CanvasConstant.CANVAS_DRAW_GRID
                drawBottom(_dimen(R.dimen.lib_line_px), 0, 0)
                itemExtendLayoutId = R.layout.canvas_extent_switch_item
                itemSwitchChangedAction = {
                    CanvasConstant.CANVAS_DRAW_GRID = it
                    if (it) {
                        canvasDelegate?.xAxis?.drawGridLine = true
                        canvasDelegate?.yAxis?.drawGridLine = true
                    } else {
                        canvasDelegate?.xAxis?.drawGridLine = false
                        canvasDelegate?.yAxis?.drawGridLine = false
                    }
                    canvasDelegate?.refresh()
                }
            }
            DslSwitchInfoItem()() {
                itemInfoText = _string(R.string.canvas_smart_assistant)
                itemSwitchChecked = canvasDelegate?.smartAssistant?.enable == true
                itemExtendLayoutId = R.layout.canvas_extent_switch_item
                itemSwitchChangedAction = {
                    canvasDelegate?.smartAssistant?.enable = it
                    CanvasConstant.CANVAS_SMART_ASSISTANT = it
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