package com.angcyo.canvas.laser.pecker.dslitem

import android.widget.TextView
import com.angcyo.canvas.core.IRenderer
import com.angcyo.canvas.data.toPixel
import com.angcyo.canvas.items.data.DataItemRenderer
import com.angcyo.canvas.laser.pecker.R
import com.angcyo.canvas.utils.CanvasConstant
import com.angcyo.canvas.utils.canvasDecimal
import com.angcyo.dialog.TargetWindow
import com.angcyo.dsladapter.DslAdapterItem
import com.angcyo.item.keyboard.NumberKeyboardPopupConfig.Companion.STYLE_DECIMAL
import com.angcyo.item.keyboard.keyboardNumberWindow
import com.angcyo.library.annotation.Pixel
import com.angcyo.library.ex.clamp
import com.angcyo.library.ex.dp
import com.angcyo.widget.DslViewHolder

/**
 * 形状属性控制输入item
 * 边数/深度
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2022/09/19
 */
class ShapePropertyControlItem : DslAdapterItem() {

    companion object {
        /**最小/最大的边数
         * [com.angcyo.canvas.data.ItemDataBean.side]*/
        const val SHAPE_MIN_SIDE = 3
        const val SHAPE_MAX_SIDE = 50

        /**最小/最大的圆角像素*/
        @Pixel
        const val SHAPE_MIN_CORNER = 0f

        @Pixel
        val SHAPE_MAX_CORNER = 50 * dp
    }

    var itemRenderer: IRenderer? = null

    init {
        itemLayoutId = R.layout.item_shape_property_control_layout
    }

    override fun onItemBind(
        itemHolder: DslViewHolder,
        itemPosition: Int,
        adapterItem: DslAdapterItem,
        payloads: List<Any>
    ) {
        super.onItemBind(itemHolder, itemPosition, adapterItem, payloads)

        val renderer = itemRenderer
        if (renderer is DataItemRenderer) {
            val dataItem = renderer.getRendererRenderItem()
            itemHolder.tv(R.id.item_side_count_view)?.text = "${dataItem?.dataBean?.side ?: 3}"
            itemHolder.tv(R.id.item_depth_view)?.text = "${dataItem?.dataBean?.depth ?: 40}"
            //
            val valueUit = renderer.canvasViewBox.valueUnit
            val cornerPixel = dataItem?.dataBean?.rx?.toPixel() ?: 0f
            itemHolder.tv(R.id.item_corner_view)?.text =
                valueUit.convertPixelToValue(cornerPixel).canvasDecimal(2)

            //边数
            if (renderer.dataItem?.dataBean?.mtype == CanvasConstant.DATA_TYPE_POLYGON ||
                renderer.dataItem?.dataBean?.mtype == CanvasConstant.DATA_TYPE_PENTAGRAM
            ) {
                itemHolder.visible(R.id.item_side_count_view)
                itemHolder.visible(R.id.side_count_label_view)
            } else {
                itemHolder.gone(R.id.item_side_count_view)
                itemHolder.gone(R.id.side_count_label_view)
            }

            //深度
            if (renderer.dataItem?.dataBean?.mtype == CanvasConstant.DATA_TYPE_PENTAGRAM) {
                itemHolder.visible(R.id.item_depth_view)
                itemHolder.visible(R.id.depth_label_view)
            } else {
                itemHolder.gone(R.id.item_depth_view)
                itemHolder.gone(R.id.depth_label_view)
            }

            //圆角
            if (renderer.dataItem?.dataBean?.mtype == CanvasConstant.DATA_TYPE_RECT) {
                itemHolder.visible(R.id.item_corner_view)
                itemHolder.visible(R.id.corner_label_view)
            } else {
                itemHolder.gone(R.id.item_corner_view)
                itemHolder.gone(R.id.corner_label_view)
            }

            bindSide(itemHolder, renderer)
            bindDepth(itemHolder, renderer)
            bindCorner(itemHolder, renderer)
        } else {
            itemHolder.tv(R.id.item_side_count_view)?.text = null
            itemHolder.tv(R.id.item_depth_view)?.text = null
        }
    }

    /**边数*/
    fun bindSide(itemHolder: DslViewHolder, renderer: DataItemRenderer) {
        itemHolder.click(R.id.item_side_count_view) {
            itemHolder.context.keyboardNumberWindow(it) {
                onDismiss = this@ShapePropertyControlItem::onPopupDismiss
                keyboardBindTextView = it as? TextView
                removeKeyboardStyle(STYLE_DECIMAL)
                onNumberResultAction = { number ->
                    val size = clamp(number.toInt(), SHAPE_MIN_SIDE, SHAPE_MAX_SIDE)
                    renderer.dataShapeItem?.updateSide(size, renderer)
                }
            }
        }
    }

    /**深度*/
    fun bindDepth(itemHolder: DslViewHolder, renderer: DataItemRenderer) {
        itemHolder.click(R.id.item_depth_view) {
            itemHolder.context.keyboardNumberWindow(it) {
                onDismiss = this@ShapePropertyControlItem::onPopupDismiss
                keyboardBindTextView = it as? TextView
                removeKeyboardStyle(STYLE_DECIMAL)
                onNumberResultAction = { number ->
                    val size = clamp(number.toInt(), 1, 100)
                    renderer.dataShapeItem?.updateDepth(size, renderer)
                }
            }
        }
    }

    /**圆角*/
    fun bindCorner(itemHolder: DslViewHolder, renderer: DataItemRenderer) {
        val valueUit = renderer.canvasViewBox.valueUnit
        itemHolder.click(R.id.item_corner_view) {
            itemHolder.context.keyboardNumberWindow(it) {
                onDismiss = this@ShapePropertyControlItem::onPopupDismiss
                keyboardBindTextView = it as? TextView
                removeKeyboardStyle(STYLE_DECIMAL)
                onNumberResultAction = { number ->
                    val size = clamp(
                        valueUit.convertValueToPixel(number),
                        SHAPE_MIN_CORNER,
                        SHAPE_MAX_CORNER
                    ) //pixel
                    renderer.dataShapeItem?.updateCorner(size, renderer)
                }
            }
        }
    }

    /**popup销毁后, 刷新item*/
    fun onPopupDismiss(window: TargetWindow): Boolean {
        updateAdapterItem()
        return false
    }

}