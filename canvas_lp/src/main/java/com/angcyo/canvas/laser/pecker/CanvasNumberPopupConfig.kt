package com.angcyo.canvas.laser.pecker

import android.content.Context
import android.view.View
import android.widget.PopupWindow
import com.angcyo.dialog.TargetWindow
import com.angcyo.dialog.popup.ShadowAnchorPopupConfig
import com.angcyo.dsladapter.DslAdapterItem
import com.angcyo.canvas.laser.pecker.dslitem.CanvasNumberImageItem
import com.angcyo.canvas.laser.pecker.dslitem.CanvasNumberIncrementItem
import com.angcyo.canvas.laser.pecker.dslitem.CanvasNumberItem
import com.angcyo.item.style.itemText
import com.angcyo.widget.DslViewHolder
import com.angcyo.widget.base.appendDslItem

/**
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2022/05/14
 */
class CanvasNumberPopupConfig : ShadowAnchorPopupConfig() {

    /**点击按键的回调
     * [number] -0,表示退格
     * [number] -1,表示--
     * [number] +1,表示++
     * @return true 表示自动销毁window*/
    var onClickNumberAction: (number: String) -> Boolean = { false }

    init {
        contentLayoutId = R.layout.canvas_number_keyboard_layout
    }

    override fun initContentLayout(window: TargetWindow, viewHolder: DslViewHolder) {
        super.initContentLayout(window, viewHolder)
        val list = mutableListOf<DslAdapterItem>()
        for (i in 1..9) {
            list.add(createNumberItem(window, "$i"))
        }
        list.add(createNumberItem(window, "."))
        list.add(createNumberItem(window, "0"))
        list.add(createNumberImageItem(window))
        list.add(createNumberIncrementItem(window))
        viewHolder.group(R.id.lib_flow_layout)?.appendDslItem(list)
    }

    /**数字键和.号*/
    fun createNumberItem(window: TargetWindow, number: String): DslAdapterItem =
        CanvasNumberItem().apply {
            itemText = number
            itemClick = {
                if (onClickNumberAction(number)) {
                    if (window is PopupWindow) {
                        window.dismiss()
                    }
                }
            }
        }

    /**回退键*/
    fun createNumberImageItem(window: TargetWindow): DslAdapterItem =
        CanvasNumberImageItem().apply {
            itemClick = {
                if (onClickNumberAction("-0")) {
                    if (window is PopupWindow) {
                        window.dismiss()
                    }
                }
            }
        }

    /**自增/自减键*/
    fun createNumberIncrementItem(window: TargetWindow): DslAdapterItem =
        CanvasNumberIncrementItem().apply {
            itemIncrementAction = {
                if (onClickNumberAction(
                        if (it) "+1" else "-1"
                    )
                ) {
                    if (window is PopupWindow) {
                        window.dismiss()
                    }
                }
            }
        }
}

/**Dsl*/
fun Context.canvasNumberWindow(anchor: View?, config: CanvasNumberPopupConfig.() -> Unit): Any {
    val popupConfig = CanvasNumberPopupConfig()
    popupConfig.anchor = anchor
    popupConfig.config()
    return popupConfig.show(this)
}