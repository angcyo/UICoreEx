package com.angcyo.canvas2.laser.pecker.dialog

import android.content.Context
import android.view.View
import android.widget.TextView
import com.angcyo.canvas2.laser.pecker.R
import com.angcyo.dialog.TargetWindow
import com.angcyo.dialog.popup.ShadowAnchorPopupConfig
import com.angcyo.library.annotation.DSL
import com.angcyo.library.component.lastContext
import com.angcyo.library.ex.Action
import com.angcyo.library.ex._string
import com.angcyo.library.ex.add
import com.angcyo.library.ex.find
import com.angcyo.library.ex.have
import com.angcyo.library.ex.hideSoftInput
import com.angcyo.library.ex.remove
import com.angcyo.widget.DslViewHolder
import com.angcyo.widget.base.clickIt
import com.angcyo.widget.base.resetChild

/**
 * 材质参数表, 功能隐藏弹窗
 * [com.angcyo.canvas2.laser.pecker.dialog.ParameterComparisonTableDialogConfig]
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2023/08/10
 */
class HidePctFuncPopupConfig : ShadowAnchorPopupConfig() {

    data class HidePctFunc(val func: Int, val label: String)

    /**功能改变通知回调*/
    var onHidePctFuncChangeAction: Action? = null

    init {
        contentLayoutId = R.layout.hide_pct_func_popup_layout
    }

    override fun initContentLayout(window: TargetWindow, viewHolder: DslViewHolder) {
        super.initContentLayout(window, viewHolder)

        val hideFuncList = listOf(
            HidePctFunc(ParameterComparisonTableDialogConfig.HIDE_LABEL, "标签"),
            HidePctFunc(
                ParameterComparisonTableDialogConfig.HIDE_POWER,
                _string(R.string.custom_power)
            ),
            HidePctFunc(
                ParameterComparisonTableDialogConfig.HIDE_DEPTH,
                _string(R.string.custom_speed)
            ),
            HidePctFunc(
                ParameterComparisonTableDialogConfig.HIDE_GRID,
                _string(R.string.canvas_grid)
            ),
            HidePctFunc(ParameterComparisonTableDialogConfig.HIDE_POWER_LABEL, "功率数字"),
            HidePctFunc(ParameterComparisonTableDialogConfig.HIDE_DEPTH_LABEL, "深度数字"),
        )

        viewHolder.group(R.id.lib_flow_layout)?.resetChild(
            hideFuncList,
            R.layout.lib_stroke_text_layout
        ) { itemView, item, itemIndex ->
            itemView.find<TextView>(R.id.lib_text_view)?.text = item.label
            itemView.isSelected = !ParameterComparisonTableDialogConfig.hideFunInt.have(item.func)
            itemView.clickIt {
                ParameterComparisonTableDialogConfig.hideFunInt = if (it.isSelected) {
                    itemView.isSelected = false
                    ParameterComparisonTableDialogConfig.hideFunInt.add(item.func)
                } else {
                    itemView.isSelected = true
                    ParameterComparisonTableDialogConfig.hideFunInt.remove(item.func)
                }
                onHidePctFuncChangeAction?.invoke()
            }
        }
    }
}

@DSL
fun Context.hidePctFuncPopupConfig(
    anchor: View?,
    config: HidePctFuncPopupConfig.() -> Unit
): TargetWindow {
    //显示键盘之前, 需要隐藏键盘
    lastContext.hideSoftInput()
    val popupConfig = HidePctFuncPopupConfig()
    popupConfig.anchor = anchor
    popupConfig.config()
    return popupConfig.show(this)
}