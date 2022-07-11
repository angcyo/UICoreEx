package com.angcyo.canvas.laser.pecker

import android.content.Context
import android.graphics.drawable.Drawable
import android.view.View
import com.angcyo.canvas.laser.pecker.dslitem.CanvasMaterialItem
import com.angcyo.canvas.utils.loadAssetsSvgPath
import com.angcyo.dialog.TargetWindow
import com.angcyo.dialog.popup.ShadowAnchorPopupConfig
import com.angcyo.dsladapter.initItemGapStyle
import com.angcyo.library.ex._dimen
import com.angcyo.library.ex.filterAssets
import com.angcyo.widget.DslViewHolder
import com.angcyo.widget.recycler.renderDslAdapter

/**
 * 素材选择
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2022/07/09
 */
class CanvasMaterialPopupConfig : ShadowAnchorPopupConfig() {

    /**回调*/
    var onDrawableAction: (drawable: Drawable) -> Unit = { }

    init {
        contentLayoutId = R.layout.canvas_material_layout
    }

    override fun initContentLayout(window: TargetWindow, viewHolder: DslViewHolder) {
        super.initContentLayout(window, viewHolder)

        val context = viewHolder.context
        viewHolder.rv(R.id.lib_recycler_view)?.renderDslAdapter {
            context.filterAssets("svg") { true }?.forEach {
                CanvasMaterialItem()() {
                    itemDrawable = loadAssetsSvgPath("svg/$it")
//                    itemDrawable = loadAssetsSvg("svg/$it")

                    //网格线
                    initItemGapStyle(_dimen(R.dimen.lib_line))

                    itemClick = {
                        itemDrawable?.let(onDrawableAction)
                        hide()
                    }
                }
            }
        }
    }
}

/**Dsl*/
fun Context.canvasMaterialWindow(
    anchor: View?,
    config: CanvasMaterialPopupConfig.() -> Unit
): TargetWindow {
    val popupConfig = CanvasMaterialPopupConfig()
    popupConfig.anchor = anchor
    popupConfig.config()
    return popupConfig.show(this)
}