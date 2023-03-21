package com.angcyo.canvas2.laser.pecker.dslitem.control

import android.widget.TextView
import com.angcyo.canvas.render.core.CanvasRenderDelegate
import com.angcyo.canvas.render.core.Reason
import com.angcyo.canvas.render.core.Strategy
import com.angcyo.canvas.render.core.component.BaseControlPoint
import com.angcyo.canvas.render.core.component.CanvasSelectorComponent
import com.angcyo.canvas.render.renderer.BaseRenderer
import com.angcyo.canvas.render.unit.IRenderUnit
import com.angcyo.canvas.render.util.canvasDecimal
import com.angcyo.canvas2.laser.pecker.R
import com.angcyo.canvas2.laser.pecker.dslitem.ICanvasRendererItem
import com.angcyo.dialog.TargetWindow
import com.angcyo.dsladapter.DslAdapterItem
import com.angcyo.item.keyboard.keyboardNumberWindow
import com.angcyo.library.ex.gone
import com.angcyo.widget.DslViewHolder

/**
 * 整个常用编辑item
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2023/03/08
 */
class EditControlItem : DslAdapterItem(), ICanvasRendererItem {

    override var itemRenderer: BaseRenderer? = null

    override var itemRenderDelegate: CanvasRenderDelegate? = null

    private val selectorComponent: CanvasSelectorComponent?
        get() = itemRenderDelegate?.selectorManager?.selectorComponent

    private val unit: IRenderUnit
        get() = itemRenderDelegate?.axisManager?.renderUnit!!

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

        val selectorComponent = selectorComponent
        itemRenderDelegate?.selectorManager?.getTargetSelectorRenderer()
        val canvasDelegate = itemRenderDelegate
        if (canvasDelegate != null && selectorComponent is BaseRenderer && selectorComponent.isSelectorElement) {
            //val drawable = renderer._rendererItem?.itemDrawable ?: selectorComponent?.preview()
            itemHolder.img(R.id.item_drawable_view)?.apply {
                //visible(drawable != null)
                itemHolder.visible(R.id.item_drawable_line_view, drawable != null)
                //setImageDrawable(drawable)
                gone()
            }
            itemHolder.selected(R.id.item_lock_view, selectorComponent.isLockScaleRatio)

            //宽高
            selectorComponent.renderProperty?.let { property ->
                val bounds = property.getRenderBounds()

                //w/h
                val widthValue = unit.convertPixelToValue(bounds.width()).canvasDecimal(2)
                val heightValue = unit.convertPixelToValue(bounds.height()).canvasDecimal(2)
                itemHolder.tv(R.id.item_width_view)?.text = widthValue
                itemHolder.tv(R.id.item_height_view)?.text = heightValue

                //x/y坐标
                val xValue = unit.convertPixelToValue(bounds.left).canvasDecimal(2)
                val yValue = unit.convertPixelToValue(bounds.top).canvasDecimal(2)
                itemHolder.tv(R.id.item_axis_x_view)?.text = xValue
                itemHolder.tv(R.id.item_axis_y_view)?.text = yValue

                //旋转度数,角度单位
                itemHolder.tv(R.id.item_rotate_view)?.text = buildString {
                    append(property.angle.canvasDecimal(2))
                    append("°")
                }
            }

            //enable/support
            itemHolder.enable(
                R.id.item_width_view,
                selectorComponent.isSupportControlPoint(BaseControlPoint.CONTROL_TYPE_WIDTH)
            )
            itemHolder.enable(
                R.id.item_height_view,
                selectorComponent.isSupportControlPoint(BaseControlPoint.CONTROL_TYPE_HEIGHT)
            )
            itemHolder.enable(
                R.id.item_lock_view,
                selectorComponent.isSupportControlPoint(BaseControlPoint.CONTROL_TYPE_LOCK)
            )
            itemHolder.enable(
                R.id.item_rotate_view,
                selectorComponent.isSupportControlPoint(BaseControlPoint.CONTROL_TYPE_ROTATE)
            )

            //是否锁定等比
            itemHolder.click(R.id.item_lock_view) {
                canvasDelegate.selectorManager.updateLockScaleRatio(
                    !selectorComponent.isLockScaleRatio,
                    Reason.user.apply {
                        controlType = BaseControlPoint.CONTROL_TYPE_KEEP_GROUP_PROPERTY
                    },
                    canvasDelegate
                )
                updateAdapterItem()
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

    private fun DslViewHolder.isLockRatio() = view(R.id.item_lock_view)?.isSelected == true

    /**绑定宽高事件*/
    private fun bindWidthHeight(itemHolder: DslViewHolder) {
        itemHolder.click(R.id.item_width_view) {
            val renderer = selectorComponent ?: return@click
            val property = renderer.renderProperty ?: return@click
            val bounds = property.getRenderBounds()

            itemHolder.context.keyboardNumberWindow(it) {
                onDismiss = this@EditControlItem::onPopupDismiss
                keyboardBindTextView = it as? TextView
                onNumberResultAction = { toWidth ->
                    val newWidth = unit.convertValueToPixel(toWidth)
                    val lockRatio = itemHolder.isLockRatio()

                    val sx = newWidth / bounds.width()
                    val sy = if (lockRatio) sx else 1f

                    renderer.scale(sx, sy, Reason.user.apply {
                        controlType = BaseControlPoint.CONTROL_TYPE_KEEP_GROUP_PROPERTY or
                                BaseControlPoint.CONTROL_TYPE_DATA or
                                BaseControlPoint.CONTROL_TYPE_WIDTH
                    }, Strategy.normal, itemRenderDelegate)
                }
            }
        }
        itemHolder.click(R.id.item_height_view) {
            val renderer = selectorComponent ?: return@click
            val property = renderer.renderProperty ?: return@click
            val bounds = property.getRenderBounds()
            itemHolder.context.keyboardNumberWindow(it) {
                onDismiss = this@EditControlItem::onPopupDismiss
                keyboardBindTextView = it as? TextView
                onNumberResultAction = { toHeight ->
                    val newHeight = unit.convertValueToPixel(toHeight)
                    val lockRatio = itemHolder.isLockRatio()

                    val sy = newHeight / bounds.height()
                    val sx = if (lockRatio) sy else 1f

                    renderer.scale(sx, sy, Reason.user.apply {
                        controlType = BaseControlPoint.CONTROL_TYPE_KEEP_GROUP_PROPERTY or
                                BaseControlPoint.CONTROL_TYPE_DATA or
                                BaseControlPoint.CONTROL_TYPE_HEIGHT
                    }, Strategy.normal, itemRenderDelegate)
                }
            }
        }
    }

    /**绑定xy轴事件*/
    private fun bindAxis(itemHolder: DslViewHolder) {
        itemHolder.click(R.id.item_axis_x_view) {
            val renderer = selectorComponent ?: return@click
            val property = renderer.renderProperty ?: return@click
            val bounds = property.getRenderBounds()
            itemHolder.context.keyboardNumberWindow(it) {
                onDismiss = this@EditControlItem::onPopupDismiss
                keyboardBindTextView = it as? TextView
                onNumberResultAction = { toX ->
                    val newX = unit.convertValueToPixel(toX)
                    val dx = newX - bounds.left

                    renderer.translate(dx, 0f, Reason.user.apply {
                        controlType = BaseControlPoint.CONTROL_TYPE_KEEP_GROUP_PROPERTY or
                                BaseControlPoint.CONTROL_TYPE_TRANSLATE
                    }, Strategy.normal, itemRenderDelegate)
                }
            }
        }
        itemHolder.click(R.id.item_axis_y_view) {
            val renderer = selectorComponent ?: return@click
            val property = renderer.renderProperty ?: return@click
            val bounds = property.getRenderBounds()
            itemHolder.context.keyboardNumberWindow(it) {
                onDismiss = this@EditControlItem::onPopupDismiss
                keyboardBindTextView = it as? TextView
                onNumberResultAction = { toY ->
                    val newY = unit.convertValueToPixel(toY)
                    val dy = newY - bounds.top

                    renderer.translate(0f, dy, Reason.user.apply {
                        controlType = BaseControlPoint.CONTROL_TYPE_KEEP_GROUP_PROPERTY or
                                BaseControlPoint.CONTROL_TYPE_TRANSLATE
                    }, Strategy.normal, itemRenderDelegate)
                }
            }
        }
    }

    /**绑定旋转事件*/
    private fun bindRotate(itemHolder: DslViewHolder) {
        itemHolder.click(R.id.item_rotate_view) {
            val renderer = selectorComponent ?: return@click
            itemHolder.context.keyboardNumberWindow(it) {
                onDismiss = this@EditControlItem::onPopupDismiss
                keyboardBindTextView = it as? TextView
                onNumberResultAction = { toRotate ->
                    renderer.rotate(toRotate, Reason.user.apply {
                        controlType = BaseControlPoint.CONTROL_TYPE_KEEP_GROUP_PROPERTY or
                                BaseControlPoint.CONTROL_TYPE_DATA or
                                BaseControlPoint.CONTROL_TYPE_ROTATE
                    }, Strategy.normal, itemRenderDelegate)
                }
            }
        }
    }

    /**颜色*/
    private fun bindColor(itemHolder: DslViewHolder) {
        val renderer = selectorComponent

        //是否需要显示颜色控件
        /*var showColorView = false
        var color: Int = Color.TRANSPARENT //颜色
        if (renderer is DataItemRenderer) {
            val item = renderer.getRendererRenderItem()
            *//*if (item is PictureTextItem || item is PictureShapeItem) {
                showColorView = true
                color = renderer.paint.color
            }*//*
        }

        //init
        itemHolder.visible(R.id.item_color_view, showColorView)
        itemHolder.visible(R.id.item_drawable_line_view, showColorView)
        itemHolder.v<ColorPanelView>(R.id.item_color_view)?.apply {
            setColor(color)

            clickIt {
                *//*itemHolder.context.hsvColorPickerDialog {
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
                }*//*

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
        }*/
    }

    /**翻转*/
    private fun bindFlip(itemHolder: DslViewHolder) {
        itemHolder.click(R.id.flip_horizontal_layout) {
            selectorComponent?.flipX(Reason.user, Strategy.normal, itemRenderDelegate)
        }
        itemHolder.click(R.id.flip_vertical_layout) {
            selectorComponent?.flipY(Reason.user, Strategy.normal, itemRenderDelegate)
        }
    }

    /**popup销毁后, 刷新item*/
    private fun onPopupDismiss(window: TargetWindow): Boolean {
        updateAdapterItem()
        return false
    }
}