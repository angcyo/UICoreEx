package com.angcyo.canvas.laser.pecker.dslitem

import android.graphics.drawable.Drawable
import com.angcyo.bluetooth.fsc.laserpacker.LaserPeckerHelper
import com.angcyo.bluetooth.fsc.laserpacker.LaserPeckerModel
import com.angcyo.bluetooth.fsc.laserpacker.isOverflowProductBounds
import com.angcyo.canvas.CanvasDelegate
import com.angcyo.canvas.core.RenderParams
import com.angcyo.canvas.core.renderer.SelectGroupRenderer
import com.angcyo.canvas.items.data.DataItemRenderer
import com.angcyo.canvas.items.renderer.BaseItemRenderer
import com.angcyo.canvas.laser.pecker.R
import com.angcyo.core.vmApp
import com.angcyo.dialog.messageDialog
import com.angcyo.dsladapter.DslAdapterItem
import com.angcyo.engrave.data.HawkEngraveKeys
import com.angcyo.library.ex._string
import com.angcyo.widget.DslViewHolder

/**
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2022/09/29
 */
open class CanvasBaseLayerItem : DslAdapterItem() {

    //region ---core---

    var itemCanvasDelegate: CanvasDelegate? = null

    var itemRenderer: BaseItemRenderer<*>? = null

    val itemRenderParams = RenderParams(false)

    //endregion ---core---

    //region ---计算属性---

    val itemLayerHide: Boolean get() = itemRenderer?.isVisible() == false

    val itemItemDrawable: Drawable? get() = itemRenderer?.getRendererRenderItem()?.itemLayerDrawable

    val itemItemName: CharSequence? get() = itemRenderer?.getRendererRenderItem()?.itemLayerName

    /**当前的[itemRenderer]范围是否超出设备物理尺寸*/
    val isOverflowBounds: Boolean get() = itemRenderer?.getRotateBounds().isOverflowProductBounds()

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
        itemHolder.tv(R.id.layer_item_name_view)?.text = itemItemName
        itemHolder.img(R.id.layer_item_drawable_view)
            ?.setImageDrawable(itemItemDrawable ?: renderer?.preview(itemRenderParams))

        //元素参数
        if (HawkEngraveKeys.enableItemEngraveParams && renderer is DataItemRenderer) {
            itemHolder.visible(R.id.layer_item_params_view)
            itemHolder.tv(R.id.layer_item_params_view)?.text = buildString {
                renderer.dataItem?.dataBean?.let { bean ->
                    if (vmApp<LaserPeckerModel>().isC1()) {
                        append(_string(R.string.engrave_precision));append(":")
                        append(bean.printPrecision ?: HawkEngraveKeys.lastPrecision);append(" ")
                    } else {
                        LaserPeckerHelper.findProductSupportLaserTypeList()
                            .find {
                                it.type == (bean.printType
                                    ?: LaserPeckerHelper.LASER_TYPE_BLUE).toByte()
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
            val selectedRenderer = itemCanvasDelegate?.getSelectedRenderer()
            if (selectedRenderer is SelectGroupRenderer) {
                //no
            } else {
                itemCanvasDelegate?.selectedItem(it)
            }
            if (it.isVisible()) {
                itemCanvasDelegate?.showRectBounds(it.getRotateBounds())
            }
        }
    }
}