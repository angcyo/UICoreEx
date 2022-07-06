package com.angcyo.engrave.canvas

import android.content.Context
import android.view.View
import com.angcyo.canvas.CanvasDelegate
import com.angcyo.canvas.core.IValueUnit
import com.angcyo.canvas.core.InchValueUnit
import com.angcyo.canvas.core.MmValueUnit
import com.angcyo.canvas.core.PixelValueUnit
import com.angcyo.dialog.TargetWindow
import com.angcyo.dialog.popup.ShadowAnchorPopupConfig
import com.angcyo.dsladapter.drawBottom
import com.angcyo.engrave.R
import com.angcyo.item.DslSwitchInfoItem
import com.angcyo.item.style.itemInfoText
import com.angcyo.item.style.itemSwitchChangedAction
import com.angcyo.item.style.itemSwitchChecked
import com.angcyo.library.component.HawkPropertyValue
import com.angcyo.library.ex._dimen
import com.angcyo.library.ex._string
import com.angcyo.library.ex.dpi
import com.angcyo.library.ex.isShowDebug
import com.angcyo.widget.DslViewHolder
import com.angcyo.widget.recycler.renderDslAdapter

/**
 * 画图设置弹窗
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2022/05/16
 */
class CanvasSettingPopupConfig : ShadowAnchorPopupConfig() {

    companion object {

        /**像素单位*/
        const val CANVAS_VALUE_UNIT_PIXEL = 1

        /**厘米单位*/
        const val CANVAS_VALUE_UNIT_MM = 2

        /**英寸单位*/
        const val CANVAS_VALUE_UNIT_INCH = 3

        /**单温状态, 持久化*/
        var CANVAS_VALUE_UNIT: Int by HawkPropertyValue<Any, Int>(2)

        /**是否开启智能指南, 持久化*/
        var CANVAS_SMART_ASSISTANT: Boolean by HawkPropertyValue<Any, Boolean>(true)

        /**单位*/
        val valueUnit: IValueUnit
            get() = when (CANVAS_VALUE_UNIT) {
                CANVAS_VALUE_UNIT_PIXEL -> PixelValueUnit()
                CANVAS_VALUE_UNIT_INCH -> InchValueUnit()
                else -> MmValueUnit()
            }
    }

    var canvasDelegate: CanvasDelegate? = null

    init {
        contentLayoutId = R.layout.canvas_setting_layout
        triangleMinMargin = 24 * dpi
        yoff = -10 * dpi
    }

    override fun initContentLayout(window: TargetWindow, viewHolder: DslViewHolder) {
        super.initContentLayout(window, viewHolder)
        val canvasViewBox = canvasDelegate?.getCanvasViewBox()
        viewHolder.rv(R.id.lib_recycler_view)?.renderDslAdapter {
            if (isShowDebug()) {
                DslSwitchInfoItem()() {
                    itemTag = "pixel"
                    itemInfoText = "像素"
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
                                CANVAS_VALUE_UNIT = CANVAS_VALUE_UNIT_PIXEL
                                PixelValueUnit()
                            } else {
                                CANVAS_VALUE_UNIT = CANVAS_VALUE_UNIT_MM
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
                            CANVAS_VALUE_UNIT = CANVAS_VALUE_UNIT_INCH
                            InchValueUnit()
                        } else {
                            CANVAS_VALUE_UNIT = CANVAS_VALUE_UNIT_MM
                            MmValueUnit()
                        }
                    )
                }
            }
            DslSwitchInfoItem()() {
                itemInfoText = _string(R.string.canvas_grid)
                itemSwitchChecked = canvasDelegate?.xAxis?.drawGridLine == true
                drawBottom(_dimen(R.dimen.lib_line_px), 0, 0)
                itemExtendLayoutId = R.layout.canvas_extent_switch_item
                itemSwitchChangedAction = {
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
                    CANVAS_SMART_ASSISTANT = it
                }
            }
        }
    }
}

/**Dsl*/
fun Context.canvasSettingWindow(anchor: View?, config: CanvasSettingPopupConfig.() -> Unit): Any {
    val popupConfig = CanvasSettingPopupConfig()
    popupConfig.anchor = anchor
    popupConfig.config()
    return popupConfig.show(this)
}