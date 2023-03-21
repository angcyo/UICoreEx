package com.angcyo.canvas2.laser.pecker.dslitem

import android.graphics.drawable.Drawable
import com.angcyo.bluetooth.fsc.laserpacker.LaserPeckerHelper
import com.angcyo.bluetooth.fsc.laserpacker.LaserPeckerModel
import com.angcyo.bluetooth.fsc.laserpacker.isOverflowProductBounds
import com.angcyo.canvas.render.core.CanvasRenderDelegate
import com.angcyo.canvas.render.core.component.BaseControlPoint
import com.angcyo.canvas.render.data.RenderParams
import com.angcyo.canvas.render.renderer.BaseRenderer
import com.angcyo.canvas.render.renderer.CanvasElementRenderer
import com.angcyo.canvas.render.renderer.CanvasGroupRenderer
import com.angcyo.canvas2.laser.pecker.R
import com.angcyo.canvas2.laser.pecker.toTypeNameString
import com.angcyo.canvas2.laser.pecker.util.lpElementBean
import com.angcyo.core.vmApp
import com.angcyo.dialog.messageDialog
import com.angcyo.dsladapter.DslAdapterItem
import com.angcyo.engrave.EngraveHelper
import com.angcyo.engrave.data.HawkEngraveKeys
import com.angcyo.library.ex._string
import com.angcyo.library.ex.dp
import com.angcyo.widget.DslViewHolder

/**
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2022/09/29
 */
open class CanvasBaseLayerItem : DslAdapterItem(), ICanvasRendererItem {

    //region ---core---

    override var itemRenderer: BaseRenderer? = null

    override var itemRenderDelegate: CanvasRenderDelegate? = null

    val operateRenderer: BaseRenderer?
        get() = if (itemRenderer is CanvasGroupRenderer) {
            (itemRenderer as CanvasGroupRenderer).rendererList.firstOrNull()
        } else {
            itemRenderer
        }

    val itemRenderParams = RenderParams(renderDst = this, overrideSize = 30 * dp)

    //endregion ---core---

    //region ---计算属性---

    val itemLayerHide: Boolean get() = itemRenderer?.isVisible == false

    val itemLayerLock: Boolean get() = itemRenderer?.isLock == true

    val itemItemDrawable: Drawable?
        get() = itemRenderer?.requestRenderDrawable(itemRenderParams)

    val itemItemName: CharSequence? get() = itemRenderer?.lpElementBean()?.name

    /**当前的[itemRenderer]范围是否超出设备物理尺寸*/
    val isOverflowBounds: Boolean
        get() = itemRenderer?.renderProperty?.getRenderBounds().isOverflowProductBounds()

    /**是否需要参数先死*/
    var itemShowEngraveParams = true

    //endregion ---计算属性---

    init {
        itemLayoutId = R.layout.item_canvas_base_layer_layout
    }

    override fun onItemBind(
        itemHolder: DslViewHolder,
        itemPosition: Int,
        adapterItem: DslAdapterItem,
        payloads: List<Any>
    ) {
        super.onItemBind(itemHolder, itemPosition, adapterItem, payloads)
        val renderer = itemRenderer

        //item 名称
        itemHolder.tv(R.id.layer_item_name_view)?.text =
            itemItemName ?: operateRenderer?.lpElementBean()?.mtype.toTypeNameString()

        itemHolder.img(R.id.layer_item_drawable_view)
            ?.setImageDrawable(itemItemDrawable /*?: renderer?.preview(itemRenderParams)*/)

        //元素参数
        if (itemShowEngraveParams && HawkEngraveKeys.enableItemEngraveParams && renderer is CanvasElementRenderer) {
            itemHolder.visible(R.id.layer_item_params_view)
            itemHolder.tv(R.id.layer_item_params_view)?.text = buildString {
                renderer.lpElementBean()?.let { bean ->
                    if (vmApp<LaserPeckerModel>().isC1()) {
                        append(_string(R.string.engrave_precision));append(":")
                        append(bean.printPrecision ?: HawkEngraveKeys.lastPrecision);append(" ")
                    } else {
                        LaserPeckerHelper.findProductSupportLaserTypeList()
                            .find {
                                it.type == (bean.printType
                                    ?: EngraveHelper.getProductLaserType()).toByte()
                            }
                            ?.let { append(it.toText());append(" ") }
                    }

                    append(_string(R.string.custom_power));append(":")
                    append(bean.printPower ?: HawkEngraveKeys.lastPower);append("% ")

                    append(_string(R.string.custom_speed));append(":")
                    append(bean.printDepth ?: HawkEngraveKeys.lastDepth);append("% ")

                    append(_string(R.string.print_times));append(":")
                    append(bean.printCount ?: 1);append(" ")
                }
            }
        } else {
            itemHolder.gone(R.id.layer_item_params_view)
        }

        itemHolder.visible(R.id.layer_item_warn_view, isOverflowBounds)
        itemHolder.click(R.id.layer_item_warn_view) {
            it.context.messageDialog {
                dialogTitle = _string(R.string.engrave_bounds_warn)
                dialogMessage = _string(R.string.engrave_overflow_bounds_message)
            }
        }
    }

    /**将画布移动显示到目标元素*/
    fun showItemRendererBounds() {
        itemRenderer?.let {
            if (it.isVisible) {
                it.renderProperty?.getRenderBounds()?.let { bounds ->
                    itemRenderDelegate?.showRectBounds(
                        bounds,
                        BaseControlPoint.DEFAULT_CONTROL_POINT_SIZE,
                        false
                    )
                }
            }
        }
    }
}