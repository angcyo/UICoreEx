package com.angcyo.canvas2.laser.pecker.dslitem.control

import android.widget.TextView
import com.angcyo.canvas.render.core.CanvasRenderDelegate
import com.angcyo.canvas.render.renderer.BaseRenderer
import com.angcyo.canvas.render.util.canvasDecimal
import com.angcyo.canvas2.laser.pecker.R
import com.angcyo.canvas2.laser.pecker.dslitem.ICanvasRendererItem
import com.angcyo.canvas2.laser.pecker.util.lpElement
import com.angcyo.dialog.TargetWindow
import com.angcyo.dsladapter.DslAdapterItem
import com.angcyo.item.keyboard.NumberKeyboardPopupConfig.Companion.STYLE_DECIMAL
import com.angcyo.item.keyboard.keyboardNumberWindow
import com.angcyo.laserpacker.LPDataConstant
import com.angcyo.library.annotation.Pixel
import com.angcyo.library.ex._string
import com.angcyo.library.ex.clamp
import com.angcyo.library.ex.dp
import com.angcyo.library.unit.toMm
import com.angcyo.library.unit.toPixel
import com.angcyo.widget.DslViewHolder

/**
 * 形状属性控制输入item
 * 边数/深度
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2022/09/19
 */
class ShapePropertyControlItem : DslAdapterItem(), ICanvasRendererItem {

    companion object {
        /**最小/最大的边数
         * [com.angcyo.laserpacker.bean.LPElementBean.side]*/
        const val SHAPE_MIN_SIDE = 3
        const val SHAPE_MAX_SIDE = 50

        /**最小/最大的圆角像素*/
        @Pixel
        const val SHAPE_MIN_CORNER = 0f

        @Pixel
        val SHAPE_MAX_CORNER = 50 * dp
    }

    override var itemRenderer: BaseRenderer? = null

    override var itemRenderDelegate: CanvasRenderDelegate? = null

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

        val elementBean = elementBean
        if (elementBean != null) {
            itemHolder.tv(R.id.item_side_count_view)?.text = "${elementBean.side}"
            itemHolder.tv(R.id.item_depth_view)?.text = "${elementBean.depth}"
            //
            val renderUnit = renderUnit
            val cornerPixel = elementBean.rx.toPixel()
            itemHolder.tv(R.id.item_corner_view)?.text =
                renderUnit?.convertPixelToValue(cornerPixel)?.canvasDecimal(2)

            //边数
            if (elementBean.mtype == LPDataConstant.DATA_TYPE_POLYGON ||
                elementBean.mtype == LPDataConstant.DATA_TYPE_PENTAGRAM
            ) {
                itemHolder.visible(R.id.item_side_count_view)
                itemHolder.visible(R.id.side_count_label_view)
            } else {
                itemHolder.gone(R.id.item_side_count_view)
                itemHolder.gone(R.id.side_count_label_view)
            }

            //深度
            if (elementBean.mtype == LPDataConstant.DATA_TYPE_PENTAGRAM) {
                itemHolder.tv(R.id.side_count_label_view)?.text =
                    _string(R.string.canvas_angle_count)//角数
                itemHolder.visible(R.id.item_depth_view)
                itemHolder.visible(R.id.depth_label_view)
            } else {
                itemHolder.tv(R.id.side_count_label_view)?.text =
                    _string(R.string.canvas_side_count)//边数
                itemHolder.gone(R.id.item_depth_view)
                itemHolder.gone(R.id.depth_label_view)
            }

            //圆角
            if (elementBean.mtype == LPDataConstant.DATA_TYPE_RECT) {
                itemHolder.visible(R.id.item_corner_view)
                itemHolder.visible(R.id.corner_label_view)
            } else {
                itemHolder.gone(R.id.item_corner_view)
                itemHolder.gone(R.id.corner_label_view)
            }

            bindSide(itemHolder)
            bindDepth(itemHolder)
            bindCorner(itemHolder)
        } else {
            itemHolder.tv(R.id.item_side_count_view)?.text = null
            itemHolder.tv(R.id.item_depth_view)?.text = null
        }
    }

    /**边数*/
    fun bindSide(itemHolder: DslViewHolder) {
        itemHolder.click(R.id.item_side_count_view) {
            itemHolder.context.keyboardNumberWindow(it) {
                onDismiss = this@ShapePropertyControlItem::onPopupDismiss
                keyboardBindTextView = it as? TextView
                removeKeyboardStyle(STYLE_DECIMAL)
                onNumberResultAction = { number ->
                    val size = clamp(number.toInt(), SHAPE_MIN_SIDE, SHAPE_MAX_SIDE)
                    itemRenderer?.lpElement()?.apply {
                        updateElement(itemRenderer, itemRenderDelegate) {
                            elementBean.side = size
                            lpElement()?.parseElementBean()
                        }
                    }
                }
            }
        }
    }

    /**深度*/
    fun bindDepth(itemHolder: DslViewHolder) {
        itemHolder.click(R.id.item_depth_view) {
            itemHolder.context.keyboardNumberWindow(it) {
                onDismiss = this@ShapePropertyControlItem::onPopupDismiss
                keyboardBindTextView = it as? TextView
                removeKeyboardStyle(STYLE_DECIMAL)
                onNumberResultAction = { number ->
                    val size = clamp(number.toInt(), 1, 100)
                    itemRenderer?.lpElement()?.apply {
                        updateElement(itemRenderer, itemRenderDelegate) {
                            elementBean.depth = size
                            lpElement()?.parseElementBean()
                        }
                    }
                }
            }
        }
    }

    /**圆角*/
    fun bindCorner(itemHolder: DslViewHolder) {
        val renderUnit = renderUnit ?: return
        itemHolder.click(R.id.item_corner_view) {
            itemHolder.context.keyboardNumberWindow(it) {
                onDismiss = this@ShapePropertyControlItem::onPopupDismiss
                keyboardBindTextView = it as? TextView
                onNumberResultAction = { number ->
                    val size = clamp(
                        renderUnit.convertValueToPixel(number),
                        SHAPE_MIN_CORNER,
                        SHAPE_MAX_CORNER
                    ) //pixel
                    itemRenderer?.lpElement()?.apply {
                        updateElement(itemRenderer, itemRenderDelegate) {
                            elementBean.rx = size.toMm()
                            elementBean.ry = elementBean.rx
                            lpElement()?.parseElementBean()
                        }
                    }
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