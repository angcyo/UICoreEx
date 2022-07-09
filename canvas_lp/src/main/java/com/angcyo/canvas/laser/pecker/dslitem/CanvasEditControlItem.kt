package com.angcyo.canvas.laser.pecker.dslitem

import android.graphics.Color
import android.graphics.Matrix
import android.graphics.PointF
import android.graphics.RectF
import android.widget.LinearLayout
import com.angcyo.canvas.CanvasDelegate
import com.angcyo.canvas.core.IRenderer
import com.angcyo.canvas.items.PictureShapeItem
import com.angcyo.canvas.items.PictureTextItem
import com.angcyo.canvas.items.renderer.BaseItemRenderer
import com.angcyo.canvas.laser.pecker.canvasNumberWindow
import com.angcyo.canvas.utils.canvasDecimal
import com.angcyo.canvas.utils.lineShapeOrientation
import com.angcyo.dialog.singleColorPickerDialog
import com.angcyo.dsladapter.DslAdapterItem
import com.angcyo.library.ex.ADJUST_TYPE_LT
import com.angcyo.library.ex.adjustSize
import com.angcyo.library.ex.adjustSizeWithRotate
import com.angcyo.library.ex.gone
import com.angcyo.canvas.laser.pecker.R
import com.angcyo.widget.DslViewHolder
import com.angcyo.widget.base.clickIt
import com.jaredrummler.android.colorpicker.ColorPanelView

/**
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2022/05/14
 */
class CanvasEditControlItem : DslAdapterItem() {

    var itemRenderer: IRenderer? = null

    var itemCanvasDelegate: CanvasDelegate? = null

    val _tempPoint = PointF()
    val _tempRect = RectF()
    val _tempMatrix = Matrix()

    /**限流*/
    var pendingDelay = 300L

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
            val rotateBounds = renderer.getRotateBounds()
            val renderRotateBounds = renderer.getRenderRotateBounds()
            val width =
                canvasViewBox.valueUnit.convertPixelToValue(rotateBounds.width()).canvasDecimal(2)
            val height =
                canvasViewBox.valueUnit.convertPixelToValue(rotateBounds.height()).canvasDecimal(2)

            itemHolder.tv(R.id.item_width_view)?.text = width
            itemHolder.tv(R.id.item_height_view)?.text = height

            //非竖线, 才支持调整宽度
            itemHolder.enable(
                R.id.item_width_view,
                renderer.lineShapeOrientation() != LinearLayout.VERTICAL
            )
            //非横线, 才支持调整高度
            itemHolder.enable(
                R.id.item_height_view,
                renderer.lineShapeOrientation() != LinearLayout.HORIZONTAL
            )

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

    var _widthPendingRunnable: Runnable? = null
    var _heightPendingRunnable: Runnable? = null

    /**绑定宽高事件*/
    fun bindWidthHeight(itemHolder: DslViewHolder) {
        itemHolder.click(R.id.item_width_view) {
            val renderer = itemRenderer
            if (renderer is BaseItemRenderer<*>) {
                itemHolder.context.canvasNumberWindow(it) {
                    val builder = StringBuilder()
                    onClickNumberAction = {
                        itemHolder.tv(R.id.item_width_view)?.text = inputValueWith(
                            builder,
                            itemHolder.tv(R.id.item_width_view)?.text?.toString(),
                            it,
                            1f
                        ).apply {
                            itemHolder.removeCallbacks(_widthPendingRunnable)
                            this.toFloatOrNull()?.let { toWidth ->
                                _widthPendingRunnable = Runnable {
                                    val width =
                                        itemCanvasDelegate?.getCanvasViewBox()?.valueUnit?.convertValueToPixel(
                                            toWidth
                                        ) ?: toWidth

                                    //当外边框高度调整时, 计算真实Bounds应该调整的宽高
                                    val from = RectF(renderer.getRotateBounds())
                                    val to = RectF(from)
                                    to.adjustSize(width, from.height(), ADJUST_TYPE_LT)
                                    val result = RectF(renderer.getBounds())
                                    val lockRatio = itemHolder.isLockRatio()
                                    itemCanvasDelegate?.boundsOperateHandler?.calcBoundsWidthHeightWithFrame(
                                        result,
                                        from,
                                        to,
                                        renderer.rotate,
                                        lockRatio
                                    )?.apply {
                                        //计算结果后
                                        val newWidth = this[0]
                                        val newHeight = this[1]

                                        if (newWidth != 0f && newHeight != 0f) {
                                            result.adjustSizeWithRotate(
                                                newWidth, newHeight,
                                                renderer.rotate,
                                                ADJUST_TYPE_LT
                                            )

                                            itemCanvasDelegate?.addChangeItemBounds(
                                                renderer,
                                                result
                                            )
                                        }
                                    }
                                }
                                itemHolder.postDelay(_widthPendingRunnable!!, pendingDelay)
                            }
                        }
                        false
                    }
                }
            }
        }
        itemHolder.click(R.id.item_height_view) {
            val renderer = itemRenderer
            if (renderer is BaseItemRenderer<*>) {
                itemHolder.context.canvasNumberWindow(it) {
                    val builder = StringBuilder()
                    onClickNumberAction = {
                        itemHolder.tv(R.id.item_height_view)?.text = inputValueWith(
                            builder,
                            itemHolder.tv(R.id.item_height_view)?.text?.toString(),
                            it,
                            1f
                        ).apply {
                            itemHolder.removeCallbacks(_heightPendingRunnable)
                            this.toFloatOrNull()?.let { toHeight ->
                                _heightPendingRunnable = Runnable {
                                    val height =
                                        itemCanvasDelegate?.getCanvasViewBox()?.valueUnit?.convertValueToPixel(
                                            toHeight
                                        ) ?: toHeight

                                    //当外边框高度调整时, 计算真实Bounds应该调整的宽高
                                    val from = RectF(renderer.getRotateBounds())
                                    val to = RectF(from)
                                    to.adjustSize(from.width(), height, ADJUST_TYPE_LT)
                                    val result = RectF(renderer.getBounds())
                                    val lockRatio = itemHolder.isLockRatio()
                                    itemCanvasDelegate?.boundsOperateHandler?.calcBoundsWidthHeightWithFrame(
                                        result,
                                        from,
                                        to,
                                        renderer.rotate,
                                        lockRatio
                                    )?.apply {
                                        //计算结果后
                                        val newWidth = this[0]
                                        val newHeight = this[1]

                                        result.adjustSizeWithRotate(
                                            newWidth, newHeight,
                                            renderer.rotate,
                                            ADJUST_TYPE_LT
                                        )

                                        itemCanvasDelegate?.addChangeItemBounds(
                                            renderer,
                                            result
                                        )
                                    }

                                }
                                itemHolder.postDelay(_heightPendingRunnable!!, pendingDelay)
                            }
                        }
                        false
                    }
                }
            }
        }
    }

    var _axisXPendingRunnable: Runnable? = null
    var _axisYPendingRunnable: Runnable? = null

    /**绑定xy轴事件*/
    fun bindAxis(itemHolder: DslViewHolder) {
        itemHolder.click(R.id.item_axis_x_view) {
            val renderer = itemRenderer
            if (renderer is BaseItemRenderer<*>) {
                itemHolder.context.canvasNumberWindow(it) {
                    val builder = StringBuilder()
                    onClickNumberAction = {
                        itemHolder.tv(R.id.item_axis_x_view)?.text = inputValueWith(
                            builder,
                            itemHolder.tv(R.id.item_axis_x_view)?.text?.toString(),
                            it,
                            1f
                        ).apply {
                            itemHolder.removeCallbacks(_axisXPendingRunnable)
                            this.toFloatOrNull()?.let { toX ->
                                _axisXPendingRunnable = Runnable {
                                    val x =
                                        itemCanvasDelegate?.getCanvasViewBox()?.valueUnit?.convertValueToPixel(
                                            toX
                                        ) ?: toX
                                    val rotate = renderer.getRotateBounds()
                                    val bounds = RectF(renderer.getBounds())
                                    val dx = x - rotate.left
                                    bounds.offset(dx, 0f)
                                    itemCanvasDelegate?.addChangeItemBounds(
                                        renderer,
                                        bounds
                                    )
                                }
                                itemHolder.postDelay(_axisXPendingRunnable!!, pendingDelay)
                            }
                        }
                        false
                    }
                }
            }
        }
        itemHolder.click(R.id.item_axis_y_view) {
            val renderer = itemRenderer
            if (renderer is BaseItemRenderer<*>) {
                itemHolder.context.canvasNumberWindow(it) {
                    val builder = StringBuilder()
                    onClickNumberAction = {
                        itemHolder.tv(R.id.item_axis_y_view)?.text = inputValueWith(
                            builder,
                            itemHolder.tv(R.id.item_axis_y_view)?.text?.toString(),
                            it,
                            1f
                        ).apply {
                            itemHolder.removeCallbacks(_axisYPendingRunnable)
                            this.toFloatOrNull()?.let { toY ->
                                _axisYPendingRunnable = Runnable {
                                    val y =
                                        itemCanvasDelegate?.getCanvasViewBox()?.valueUnit?.convertValueToPixel(
                                            toY
                                        ) ?: toY
                                    val rotate = renderer.getRotateBounds()
                                    val bounds = RectF(renderer.getBounds())
                                    val dy = y - rotate.top
                                    bounds.offset(0f, dy)
                                    itemCanvasDelegate?.addChangeItemBounds(
                                        renderer,
                                        bounds
                                    )
                                }
                                itemHolder.postDelay(_axisYPendingRunnable!!, pendingDelay)
                            }
                        }
                        false
                    }
                }
            }
        }
    }

    var _rotatePendingRunnable: Runnable? = null

    /**绑定旋转事件*/
    fun bindRotate(itemHolder: DslViewHolder) {
        itemHolder.click(R.id.item_rotate_view) {
            val renderer = itemRenderer
            if (renderer is BaseItemRenderer<*>) {
                itemHolder.context.canvasNumberWindow(it) {
                    val builder = StringBuilder()
                    onClickNumberAction = {
                        itemHolder.tv(R.id.item_rotate_view)?.text = inputValueWith(
                            builder,
                            itemHolder.tv(R.id.item_rotate_view)?.text?.toString(),
                            it,
                            1f
                        ).apply {
                            itemHolder.removeCallbacks(_rotatePendingRunnable)
                            this.toFloatOrNull()?.let { toRotate ->
                                _rotatePendingRunnable = Runnable {
                                    itemCanvasDelegate?.addChangeItemRotate(
                                        renderer,
                                        toRotate
                                    )
                                }
                                itemHolder.postDelay(_rotatePendingRunnable!!, pendingDelay)
                            }
                        }
                        false
                    }
                }
            }
        }
    }

    /**键盘输入解析*/
    fun inputValueWith(
        newValueBuild: StringBuilder,
        firstValue: String?,
        op: String,
        step: Float
    ): String {
        when (op) {
            "-0" -> {
                //退格操作
                if (newValueBuild.isNotEmpty()) {
                    newValueBuild.deleteCharAt(newValueBuild.lastIndex)
                } else if (firstValue.isNullOrEmpty()) {
                    //no
                } else {
                    newValueBuild.append(firstValue.substring(0, firstValue.lastIndex))
                }
            }
            "-1" -> {
                //自减
                val oldValue = newValueBuild.toString()
                newValueBuild.clear()
                newValueBuild.append(oldValue.toFloatOrNull()?.run { "${this - step}" }
                    ?: "${-step}")
            }
            "+1" -> {
                //自增
                val oldValue = newValueBuild.toString()
                newValueBuild.clear()
                newValueBuild.append(oldValue.toFloatOrNull()?.run { "${this + step}" } ?: "$step")
            }
            "." -> {
                val value = newValueBuild.toString()
                if (value.contains(".")) {
                    //如果已经包含了点, 则忽略
                } else {
                    newValueBuild.append(op)
                }
            }
            else -> {
                newValueBuild.append(op)
            }
        }
        return newValueBuild.toString()
    }

    /**颜色*/
    fun bindColor(itemHolder: DslViewHolder) {
        val renderer = itemRenderer

        //是否需要显示颜色控件
        var showColorView = false
        var color: Int = Color.TRANSPARENT //颜色
        if (renderer is BaseItemRenderer<*>) {
            val item = renderer.getRendererItem()
            if (item is PictureTextItem || item is PictureShapeItem) {
                showColorView = true
                color = item.paint.color
            }
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
                    colorPickerAction = { dialog, color ->
                        if (renderer is BaseItemRenderer<*>) {
                            renderer.updatePaintColor(color)

                            //自举更新
                            updateAdapterItem()
                        }
                        false
                    }
                }
            }
        }
    }
}