package com.angcyo.canvas.laser.pecker.dslitem

import android.graphics.Color
import android.graphics.PointF
import android.graphics.RectF
import android.widget.TextView
import com.angcyo.canvas.CanvasDelegate
import com.angcyo.canvas.core.IRenderer
import com.angcyo.canvas.core.renderer.SelectGroupRenderer
import com.angcyo.canvas.items.data.DataItemRenderer
import com.angcyo.canvas.items.renderer.BaseItemRenderer
import com.angcyo.canvas.laser.pecker.R
import com.angcyo.canvas.utils.canvasDecimal
import com.angcyo.canvas.utils.isLineShape
import com.angcyo.dialog.TargetWindow
import com.angcyo.dialog.singleColorPickerDialog
import com.angcyo.dsladapter.DslAdapterItem
import com.angcyo.item.keyboard.keyboardNumberWindow
import com.angcyo.library.ex.gone
import com.angcyo.library.gesture.RectScaleGestureHandler
import com.angcyo.library.toastQQ
import com.angcyo.widget.DslViewHolder
import com.angcyo.widget.base.clickIt
import com.jaredrummler.android.colorpicker.ColorPanelView

/**
 * 编辑控制item, 其他item动态添加
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2022/05/14
 */
class CanvasEditControlItem : DslAdapterItem() {

    var itemRenderer: IRenderer? = null

    var itemCanvasDelegate: CanvasDelegate? = null

    val _tempPoint = PointF()

    init {
        itemLayoutId = R.layout.item_canvas_edit_control_layout
    }

    override fun onItemBind(
        itemHolder: DslViewHolder,
        itemPosition: Int,
        adapterItem: DslAdapterItem,
        payloads: List<Any>
    ) {
        super.onItemBind(itemHolder, itemPosition, adapterItem, payloads)

        //color
        bindColor(itemHolder)

        val renderer = itemRenderer
        val canvasDelegate = itemCanvasDelegate
        if (canvasDelegate != null && renderer is BaseItemRenderer<*>) {
            //val drawable = renderer._rendererItem?.itemDrawable ?: itemRenderer?.preview()
            itemHolder.img(R.id.item_drawable_view)?.apply {
                //visible(drawable != null)
                //itemHolder.visible(R.id.item_drawable_line_view, drawable != null)
                //setImageDrawable(drawable)
                gone()
            }
            itemHolder.selected(R.id.item_lock_view, renderer.isLockScaleRatio)

            val canvasViewBox = canvasDelegate.getCanvasViewBox()
            //宽高
            val bounds = renderer.getBounds()
            val rotateBounds = renderer.getRotateBounds()
            val renderRotateBounds = renderer.getRenderRotateBounds()
            val widthValue =
                canvasViewBox.valueUnit.convertPixelToValue(bounds.width()).canvasDecimal(2)
            val heightValue =
                canvasViewBox.valueUnit.convertPixelToValue(bounds.height()).canvasDecimal(2)

            itemHolder.tv(R.id.item_width_view)?.text = widthValue
            itemHolder.tv(R.id.item_height_view)?.text = heightValue

            //如果是线, 只支持调整宽度
            itemHolder.enable(R.id.item_height_view, !renderer.isLineShape())
            itemHolder.enable(R.id.item_lock_view, !renderer.isLineShape())

            //xy坐标
            _tempPoint.set(renderRotateBounds.left, renderRotateBounds.top)
            val value = canvasViewBox.calcDistanceValueWithOrigin(_tempPoint)

            val x = value.x.canvasDecimal(2)
            val y = value.y.canvasDecimal(2)

            itemHolder.tv(R.id.item_axis_x_view)?.text = x
            itemHolder.tv(R.id.item_axis_y_view)?.text = y

            //旋转
            itemHolder.tv(R.id.item_rotate_view)?.text = "${renderer.rotate.canvasDecimal(2)}°"

            //等比
            itemHolder.click(R.id.item_lock_view) {
                if (canvasDelegate.getSelectedRenderer() != null) {
                    canvasDelegate.controlHandler.setLockScaleRatio(!renderer.isLockScaleRatio)
                    canvasDelegate.refresh()
                    updateAdapterItem()
                }
            }

            //click事件绑定
            bindWidthHeight(itemHolder)
            bindAxis(itemHolder)
            bindRotate(itemHolder)
            bindFlip(itemHolder)
        } else {
            itemHolder.gone(R.id.item_drawable_view)
            itemHolder.gone(R.id.item_drawable_line_view)
            itemHolder.tv(R.id.item_width_view)?.text = null
            itemHolder.tv(R.id.item_height_view)?.text = null
            itemHolder.tv(R.id.item_axis_x_view)?.text = null
            itemHolder.tv(R.id.item_axis_y_view)?.text = null
            itemHolder.tv(R.id.item_rotate_view)?.text = null
        }
    }

    fun DslViewHolder.isLockRatio() = view(R.id.item_lock_view)?.isSelected == true

    /**绑定宽高事件*/
    fun bindWidthHeight(itemHolder: DslViewHolder) {
        itemHolder.click(R.id.item_width_view) {
            val renderer = itemRenderer
            if (renderer is BaseItemRenderer<*>) {
                itemHolder.context.keyboardNumberWindow(it) {
                    onDismiss = this@CanvasEditControlItem::onPopupDismiss
                    keyboardBindTextView = it as? TextView
                    onNumberResultAction = { toWidth ->
                        val width =
                            itemCanvasDelegate?.getCanvasViewBox()?.valueUnit?.convertValueToPixel(
                                toWidth
                            ) ?: toWidth //这个宽度是外边框的宽度, 所以需要映射到真实矩形上
                        val lockRatio = itemHolder.isLockRatio()

                        //1
                        /*val rotateBounds = renderer.getRotateBounds()
                        val bounds = renderer.getBounds()

                        val scaleWidth = width / rotateBounds.width()

                        val newWidth = bounds.width() * scaleWidth
                        val newHeight = if (lockRatio) {
                            bounds.height() * scaleWidth
                        } else {
                            bounds.height()
                        }*/

                        //2
                        val bounds = renderer.getBounds()
                        val newWidth = width.toFloat()
                        val scaleWidth = newWidth / bounds.width()
                        val newHeight = if (lockRatio) {
                            bounds.height() * scaleWidth
                        } else {
                            bounds.height()
                        }

                        val newBounds = RectF()
                        newBounds.set(bounds)
                        val anchor = renderer.getBoundsScaleAnchor()
                        RectScaleGestureHandler.rectUpdateTo(
                            newBounds,
                            newBounds,
                            newWidth,
                            newHeight,
                            renderer.rotate,
                            anchor.x,
                            anchor.y
                        )

                        itemCanvasDelegate?.addChangeItemBounds(renderer, newBounds)
                    }
                }
            }
        }
        itemHolder.click(R.id.item_height_view) {
            val renderer = itemRenderer
            if (renderer is BaseItemRenderer<*>) {
                itemHolder.context.keyboardNumberWindow(it) {
                    onDismiss = this@CanvasEditControlItem::onPopupDismiss
                    keyboardBindTextView = it as? TextView
                    onNumberResultAction = { toHeight ->
                        val height =
                            itemCanvasDelegate?.getCanvasViewBox()?.valueUnit?.convertValueToPixel(
                                toHeight
                            ) ?: toHeight

                        val lockRatio = itemHolder.isLockRatio()

                        //1
                        /*val rotateBounds = renderer.getRotateBounds()
                        val bounds = renderer.getBounds()

                        val scaleHeight = height / rotateBounds.height()

                        val newHeight = bounds.height() * scaleHeight
                        val newWidth = if (lockRatio) {
                            bounds.width() * scaleHeight
                        } else {
                            bounds.width()
                        }*/

                        //2
                        val bounds = renderer.getBounds()
                        val newHeight = height.toFloat()
                        val scaleScale = newHeight / bounds.height()
                        val newWidth = if (lockRatio) {
                            bounds.width() * scaleScale
                        } else {
                            bounds.width()
                        }

                        val newBounds = RectF()
                        newBounds.set(bounds)
                        val anchor = renderer.getBoundsScaleAnchor()
                        RectScaleGestureHandler.rectUpdateTo(
                            newBounds,
                            newBounds,
                            newWidth,
                            newHeight,
                            renderer.rotate,
                            anchor.x,
                            anchor.y
                        )

                        itemCanvasDelegate?.addChangeItemBounds(renderer, newBounds)

                    }
                }
            }
        }
    }

    /**绑定xy轴事件*/
    fun bindAxis(itemHolder: DslViewHolder) {
        itemHolder.click(R.id.item_axis_x_view) {
            val renderer = itemRenderer
            if (renderer is BaseItemRenderer<*>) {
                itemHolder.context.keyboardNumberWindow(it) {
                    onDismiss = this@CanvasEditControlItem::onPopupDismiss
                    keyboardBindTextView = it as? TextView
                    onNumberResultAction = { toX ->
                        val x =
                            itemCanvasDelegate?.getCanvasViewBox()?.valueUnit?.convertValueToPixel(
                                toX
                            ) ?: toX
                        val rotate = renderer.getRotateBounds()
                        val bounds = RectF(renderer.getBounds())
                        val dx = x - rotate.left
                        bounds.offset(dx.toFloat(), 0f)
                        itemCanvasDelegate?.addChangeItemBounds(
                            renderer,
                            bounds
                        )
                    }
                }
            }
        }
        itemHolder.click(R.id.item_axis_y_view) {
            val renderer = itemRenderer
            if (renderer is BaseItemRenderer<*>) {
                itemHolder.context.keyboardNumberWindow(it) {
                    onDismiss = this@CanvasEditControlItem::onPopupDismiss
                    keyboardBindTextView = it as? TextView
                    onNumberResultAction = { toY ->
                        val y =
                            itemCanvasDelegate?.getCanvasViewBox()?.valueUnit?.convertValueToPixel(
                                toY
                            ) ?: toY
                        val rotate = renderer.getRotateBounds()
                        val bounds = RectF(renderer.getBounds())
                        val dy = y - rotate.top
                        bounds.offset(0f, dy.toFloat())
                        itemCanvasDelegate?.addChangeItemBounds(
                            renderer,
                            bounds
                        )
                    }
                }
            }
        }
    }

    /**绑定旋转事件*/
    fun bindRotate(itemHolder: DslViewHolder) {
        itemHolder.click(R.id.item_rotate_view) {
            val renderer = itemRenderer
            if (renderer is BaseItemRenderer<*>) {
                itemHolder.context.keyboardNumberWindow(it) {
                    onDismiss = this@CanvasEditControlItem::onPopupDismiss
                    keyboardBindTextView = it as? TextView
                    onNumberResultAction = { toRotate ->
                        itemCanvasDelegate?.addChangeItemRotate(
                            renderer,
                            renderer.rotate,
                            toRotate.toFloat()
                        )
                    }
                }
            }
        }
    }

    /**颜色*/
    fun bindColor(itemHolder: DslViewHolder) {
        val renderer = itemRenderer

        //是否需要显示颜色控件
        var showColorView = false
        var color: Int = Color.TRANSPARENT //颜色
        if (renderer is DataItemRenderer) {
            val item = renderer.getRendererRenderItem()
            /*if (item is PictureTextItem || item is PictureShapeItem) {
                showColorView = true
                color = renderer.paint.color
            }*/
        }

        //init
        itemHolder.visible(R.id.item_color_view, showColorView)
        itemHolder.visible(R.id.item_drawable_line_view, showColorView)
        itemHolder.v<ColorPanelView>(R.id.item_color_view)?.apply {
            setColor(color)

            clickIt {
                /*itemHolder.context.hsvColorPickerDialog {
                    initialColor = color
                    showAlphaSlider = false
                    colorPickerAction = { dialog, color ->
                        if (renderer is BaseItemRenderer<*>) {
                            renderer.updatePaintColor(color)

                            //自举更新
                            updateAdapterItem()
                        }
                        false
                    }
                }*/

                itemHolder.context.singleColorPickerDialog {
                    initialColor = color
                    colorPickerResultAction = { dialog, color ->
                        if (renderer is BaseItemRenderer<*>) {
                            //renderer.updatePaintColor(color)

                            //自举更新
                            updateAdapterItem()
                        }
                        false
                    }
                }
            }
        }
    }

    /**翻转*/
    fun bindFlip(itemHolder: DslViewHolder) {
        val renderer = itemRenderer

        itemHolder.click(R.id.flip_horizontal_layout) {
            when (renderer) {
                is DataItemRenderer -> renderer.getRendererRenderItem()?.toggleFlipX(renderer)
                is SelectGroupRenderer -> itemCanvasDelegate?.apply {
                    itemsOperateHandler.toggleFlipX(this, renderer.selectItemList)
                }
                else -> toastQQ("nonsupport!")
            }
        }
        itemHolder.click(R.id.flip_vertical_layout) {
            when (renderer) {
                is DataItemRenderer -> renderer.getRendererRenderItem()?.toggleFlipY(renderer)
                is SelectGroupRenderer -> itemCanvasDelegate?.apply {
                    itemsOperateHandler.toggleFlipY(this, renderer.selectItemList)
                }
                else -> toastQQ("nonsupport!")
            }
        }
    }

    /**popup销毁后, 刷新item*/
    fun onPopupDismiss(window: TargetWindow): Boolean {
        updateAdapterItem()
        return false
    }
}