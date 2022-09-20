package com.angcyo.canvas.laser.pecker

import android.content.Context
import android.view.View
import com.angcyo.canvas.laser.pecker.dslitem.CanvasSeekBarItem
import com.angcyo.dialog.TargetWindow
import com.angcyo.dialog.popup.MenuPopupConfig
import com.angcyo.dsladapter.DslAdapter
import com.angcyo.item.style.itemInfoText
import com.angcyo.library.annotation.DSL
import com.angcyo.library.ex._string
import com.angcyo.widget.DslViewHolder
import com.angcyo.widget.recycler.DslRecyclerView

/**
 * 属性调整弹窗
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2022/09/20
 */
class CanvasRegulatePopupConfig2 : MenuPopupConfig() {

    companion object {

    }

    /**需要调整的项目, 需要啥就添加对应的项
     * [com.angcyo.canvas.laser.pecker.CanvasRegulatePopupConfig.KEY_INVERT]
     * [com.angcyo.canvas.laser.pecker.CanvasRegulatePopupConfig.KEY_THRESHOLD]
     * [com.angcyo.canvas.laser.pecker.CanvasRegulatePopupConfig.KEY_LINE_SPACE]
     * [com.angcyo.canvas.laser.pecker.CanvasRegulatePopupConfig.KEY_DIRECTION]
     * [com.angcyo.canvas.laser.pecker.CanvasRegulatePopupConfig.KEY_ANGLE]
     * [com.angcyo.canvas.laser.pecker.CanvasRegulatePopupConfig.KEY_CONTRAST]
     * [com.angcyo.canvas.laser.pecker.CanvasRegulatePopupConfig.KEY_BRIGHTNESS]
     * */
    val regulateList = mutableListOf<String>()

    /**保存修改后的属性, 用来恢复*/
    var property = CanvasRegulatePopupConfig.keepProperty

    override fun initRecyclerView(
        window: TargetWindow,
        viewHolder: DslViewHolder,
        recyclerView: DslRecyclerView,
        adapter: DslAdapter
    ) {
        super.initRecyclerView(window, viewHolder, recyclerView, adapter)
        adapter.apply {
            CanvasSeekBarItem()() {
                itemInfoText = _string(R.string.canvas_threshold) //0-255
            }
        }
    }

}

/**Dsl
 * 画布图片编辑属性弹窗*/
@DSL
fun Context.canvasRegulateWindow2(
    anchor: View?,
    config: CanvasRegulatePopupConfig2.() -> Unit
): TargetWindow {
    val popupConfig = CanvasRegulatePopupConfig2()
    popupConfig.anchor = anchor
    //popupConfig.addRegulate()
    popupConfig.config()
    return popupConfig.show(this)
}