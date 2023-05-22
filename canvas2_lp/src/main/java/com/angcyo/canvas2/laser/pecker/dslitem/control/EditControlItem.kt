package com.angcyo.canvas2.laser.pecker.dslitem.control

import android.view.Gravity
import android.widget.TextView
import com.angcyo.bluetooth.fsc.laserpacker.LaserPeckerModel
import com.angcyo.canvas.render.core.CanvasRenderDelegate
import com.angcyo.canvas.render.core.Reason
import com.angcyo.canvas.render.core.Strategy
import com.angcyo.canvas.render.core.component.BaseControlPoint
import com.angcyo.canvas.render.core.component.CanvasRenderProperty
import com.angcyo.canvas.render.core.component.CanvasSelectorComponent
import com.angcyo.canvas.render.renderer.BaseRenderer
import com.angcyo.canvas.render.util.alignInBounds
import com.angcyo.canvas.render.util.canvasDecimal
import com.angcyo.canvas2.laser.pecker.R
import com.angcyo.canvas2.laser.pecker.dslitem.ICanvasRendererItem
import com.angcyo.core.vmApp
import com.angcyo.dialog.TargetWindow
import com.angcyo.dsladapter.DslAdapterItem
import com.angcyo.item.keyboard.DirectionAdjustPopupConfig
import com.angcyo.item.keyboard.directionAdjustWindow
import com.angcyo.item.keyboard.keyboardNumberWindow
import com.angcyo.item.style.INewItem
import com.angcyo.item.style.NewItemConfig
import com.angcyo.item.style.itemHaveNew
import com.angcyo.item.style.itemNewHawkKeyStr
import com.angcyo.library.ex.gone
import com.angcyo.library.unit.IRenderUnit
import com.angcyo.widget.DslViewHolder

/**
 * 整个常用编辑item
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2023/03/08
 */
class EditControlItem : DslAdapterItem(), ICanvasRendererItem, INewItem {

    override var itemRenderer: BaseRenderer? = null

    override var itemRenderDelegate: CanvasRenderDelegate? = null

    private val selectorComponent: CanvasSelectorComponent?
        get() = itemRenderDelegate?.selectorManager?.selectorComponent

    /**单位运算*/
    private val unit: IRenderUnit
        get() = itemRenderDelegate?.axisManager?.renderUnit!!

    /**用来获取宽高位置属性*/
    private val boundsProperty: CanvasRenderProperty?
        get() = selectorComponent?.getGroupRenderProperty()

    /**用来获取旋转角度属性*/
    private val renderProperty: CanvasRenderProperty?
        get() = selectorComponent?.renderProperty

    override var newItemConfig: NewItemConfig = NewItemConfig()

    init {
        itemLayoutId = R.layout.item_render_edit_control_layout
        itemNewHawkKeyStr = "direction_edit_rotate"
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
            boundsProperty?.let { property ->
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
                    append(renderProperty?.angle?.canvasDecimal(2))
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
                    Reason.user,
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

    private fun DslViewHolder.isLockRatio() = view(R.id.item_lock_view)?.isSelected == true &&
            selectorComponent?.isSupportControlPoint(BaseControlPoint.CONTROL_TYPE_HEIGHT) == true

    /**绑定宽高事件*/
    private fun bindWidthHeight(itemHolder: DslViewHolder) {
        itemHolder.click(R.id.item_width_view) {
            val renderer = selectorComponent ?: return@click
            val property = boundsProperty ?: return@click
            val bounds = property.getRenderBounds()

            itemHolder.context.keyboardNumberWindow(it) {
                onDismiss = this@EditControlItem::onPopupDismiss
                keyboardBindTextView = it as? TextView
                onNumberResultAction = { toWidth ->
                    val newWidth = unit.convertValueToPixel(toWidth)
                    val lockRatio = itemHolder.isLockRatio()

                    val boundsWidth = bounds.width()
                    val sx = if (boundsWidth <= 0) {
                        newWidth / bounds.height()
                    } else {
                        newWidth / boundsWidth
                    }

                    val sy = if (lockRatio) sx else 1f

                    renderer.scale(sx, sy, Reason.user.apply {
                        controlType = BaseControlPoint.CONTROL_TYPE_WIDTH
                    }, Strategy.normal, itemRenderDelegate)
                }
            }
        }
        itemHolder.click(R.id.item_height_view) {
            val renderer = selectorComponent ?: return@click
            val property = boundsProperty ?: return@click
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
                        controlType = BaseControlPoint.CONTROL_TYPE_HEIGHT
                    }, Strategy.normal, itemRenderDelegate)
                }
            }
        }
    }

    /**绑定xy轴事件*/
    private fun bindAxis(itemHolder: DslViewHolder) {
        itemHolder.click(R.id.item_axis_x_view) {
            val renderer = selectorComponent ?: return@click
            val property = boundsProperty ?: return@click
            val bounds = property.getRenderBounds()
            itemHolder.context.keyboardNumberWindow(it) {
                onDismiss = this@EditControlItem::onPopupDismiss
                keyboardBindTextView = it as? TextView
                onNumberResultAction = { toX ->
                    val newX = unit.convertValueToPixel(toX)
                    val dx = newX - bounds.left

                    renderer.translate(dx, 0f, Reason.user, Strategy.normal, itemRenderDelegate)
                }
            }
        }
        itemHolder.click(R.id.item_axis_y_view) {
            val renderer = selectorComponent ?: return@click
            val property = boundsProperty ?: return@click
            val bounds = property.getRenderBounds()
            itemHolder.context.keyboardNumberWindow(it) {
                onDismiss = this@EditControlItem::onPopupDismiss
                keyboardBindTextView = it as? TextView
                onNumberResultAction = { toY ->
                    val newY = unit.convertValueToPixel(toY)
                    val dy = newY - bounds.top

                    renderer.translate(0f, dy, Reason.user, Strategy.normal, itemRenderDelegate)
                }
            }
        }
        //方向微调控制
        itemHolder.click(R.id.canvas_direction_view) {
            val delegate = itemRenderDelegate ?: return@click
            val renderer = selectorComponent ?: return@click
            itemHaveNew = false
            updateAdapterItem()

            //显示xy坐标
            renderer.showLocationRender(Reason.preview, null)
            itemHolder.context.directionAdjustWindow(it) {
                showDirectionCenterButton =
                    vmApp<LaserPeckerModel>().productInfoData.value?.previewBounds != null
                onDismiss = {
                    //显示wh大小
                    renderer.showSizeRender(Reason.preview, null)
                    false
                }
                onDirectionAdjustAction = { direction, step ->
                    if (direction == DirectionAdjustPopupConfig.DIRECTION_ROTATE_CW || direction == DirectionAdjustPopupConfig.DIRECTION_ROTATE_CCW) {
                        renderer.showRotateRender(Reason.preview, null)
                        renderer.rotateBy(
                            if (direction == DirectionAdjustPopupConfig.DIRECTION_ROTATE_CW) step else -step,
                            Reason.user,
                            Strategy.normal,
                            delegate
                        )
                    } else if (direction == DirectionAdjustPopupConfig.DIRECTION_CENTER) {
                        renderer.showLocationRender(Reason.preview, null)
                        vmApp<LaserPeckerModel>().productInfoData.value?.previewBounds?.let { bounds ->
                            //设备居中
                            renderer.alignInBounds(
                                itemRenderDelegate,
                                bounds,
                                Gravity.CENTER,
                                Strategy.normal
                            )
                        }
                    } else {
                        renderer.showLocationRender(Reason.preview, null)
                        var dx = 0f
                        var dy = 0f
                        val value = unit.convertValueToPixel(step)
                        when (direction) {
                            DirectionAdjustPopupConfig.DIRECTION_LEFT -> dx = -value
                            DirectionAdjustPopupConfig.DIRECTION_RIGHT -> dx = value
                            DirectionAdjustPopupConfig.DIRECTION_UP -> dy = -value
                            DirectionAdjustPopupConfig.DIRECTION_DOWN -> dy = value
                        }
                        renderer.translate(dx, dy, Reason.user, Strategy.normal, delegate)
                    }
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
                    renderer.rotate(toRotate, Reason.user, Strategy.normal, itemRenderDelegate)
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