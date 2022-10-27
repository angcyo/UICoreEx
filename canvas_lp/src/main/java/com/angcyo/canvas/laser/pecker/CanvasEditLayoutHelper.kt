package com.angcyo.canvas.laser.pecker

import android.graphics.Paint
import android.view.Gravity
import android.widget.LinearLayout
import com.angcyo.bluetooth.fsc.laserpacker.LaserPeckerModel
import com.angcyo.canvas.CanvasDelegate
import com.angcyo.canvas.CanvasView
import com.angcyo.canvas.core.renderer.SelectGroupRenderer
import com.angcyo.canvas.data.toPaintStyle
import com.angcyo.canvas.items.data.DataItemRenderer
import com.angcyo.canvas.items.renderer.BaseItemRenderer
import com.angcyo.canvas.laser.pecker.dslitem.*
import com.angcyo.canvas.utils.CanvasConstant
import com.angcyo.core.vmApp
import com.angcyo.dialog.popup.MenuPopupConfig
import com.angcyo.dialog.popup.menuPopupWindow
import com.angcyo.dsladapter.DslAdapter
import com.angcyo.dsladapter.DslAdapterItem
import com.angcyo.dsladapter.drawRight
import com.angcyo.dsladapter.updateItemSelected
import com.angcyo.fragment.AbsFragment
import com.angcyo.library.ex.*
import com.hingin.umeng.UMEvent
import com.hingin.umeng.umengEventValue

/**
 * 各元素的编辑控制item
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2022/09/20
 */
object CanvasEditLayoutHelper {

    //region ---图片---

    /**渲染图片编辑控制items*/
    fun DslAdapter.renderImageEditItems(
        fragment: AbsFragment,
        renderer: DataItemRenderer
    ) {
        CanvasControlItem2()() {
            itemIco = R.drawable.canvas_bitmap_prints
            itemText = _string(R.string.canvas_prints)
            itemClick = {
                updateItemSelected(!itemIsSelected)
                if (itemIsSelected) {
                    CanvasBitmapHandler.handlePrint(it, fragment, renderer) {
                        updateItemSelected(false)
                    }
                    UMEvent.CANVAS_IMAGE_PRINT.umengEventValue()
                }
            }
        }
        CanvasControlItem2()() {
            itemIco = R.drawable.canvas_bitmap_gcode
            itemText = _string(R.string.canvas_gcode)
            itemClick = {
                updateItemSelected(!itemIsSelected)
                if (itemIsSelected) {
                    CanvasBitmapHandler.handleGCode(it, fragment, renderer) {
                        updateItemSelected(false)
                    }
                    UMEvent.CANVAS_IMAGE_GCODE.umengEventValue()
                }
            }
        }
        CanvasControlItem2()() {
            itemIco = R.drawable.canvas_bitmap_black_white
            itemText = _string(R.string.canvas_black_white)
            itemClick = {
                updateItemSelected(!itemIsSelected)
                if (itemIsSelected) {
                    CanvasBitmapHandler.handleBlackWhite(it, fragment, renderer) {
                        updateItemSelected(false)
                    }
                    UMEvent.CANVAS_IMAGE_BW.umengEventValue()
                }
            }
        }
        CanvasControlItem2()() {
            itemIco = R.drawable.canvas_bitmap_dithering
            itemText = _string(R.string.canvas_dithering)
            itemClick = {
                updateItemSelected(!itemIsSelected)
                if (itemIsSelected) {
                    CanvasBitmapHandler.handleDithering(it, fragment, renderer) {
                        updateItemSelected(false)
                    }
                    UMEvent.CANVAS_IMAGE_DITHERING.umengEventValue()
                }
            }
        }
        if (isDebug()) {
            CanvasControlItem2()() {
                itemIco = R.drawable.canvas_bitmap_grey
                itemText = _string(R.string.canvas_grey)
                itemClick = {
                    CanvasBitmapHandler.handleGrey(it, fragment, renderer)
                    UMEvent.CANVAS_IMAGE_GREY.umengEventValue()
                }
            }
        }
        CanvasControlItem2()() {
            itemIco = R.drawable.canvas_bitmap_seal
            itemText = _string(R.string.canvas_seal)
            itemClick = {
                updateItemSelected(!itemIsSelected)
                if (itemIsSelected) {
                    CanvasBitmapHandler.handleSeal(it, fragment, renderer) {
                        updateItemSelected(false)
                    }
                    UMEvent.CANVAS_IMAGE_SEAL.umengEventValue()
                }
            }
        }
        //扭曲
        CanvasControlItem2()() {
            itemIco = R.drawable.canvas_actions_ico
            itemText = _string(R.string.canvas_mesh)
            itemClick = {
                updateItemSelected(!itemIsSelected)
                if (itemIsSelected) {
                    CanvasBitmapHandler.handleMesh(it, fragment, renderer) {
                        updateItemSelected(false)
                    }
                    UMEvent.CANVAS_IMAGE_MESH.umengEventValue()
                }
            }
            drawCanvasRight()
        }
        //剪裁用的是原图
        CanvasControlItem2()() {
            itemIco = R.drawable.canvas_bitmap_crop
            itemText = _string(R.string.canvas_crop)
            itemClick = {
                updateItemSelected(!itemIsSelected)
                if (itemIsSelected) {
                    CanvasBitmapHandler.handleCrop(it, fragment, renderer) {
                        updateItemSelected(false)
                    }
                    UMEvent.CANVAS_IMAGE_CROP.umengEventValue()
                }
            }
            drawCanvasRight()
        }
    }

    //endregion ---图片---

    //region ---文本---

    fun DslAdapter.renderTextEditItems(renderer: DataItemRenderer) {
        //字体
        TypefaceSelectItem()() {
            itemRenderer = renderer
            itemClick = { anchor ->
                updateItemSelected(!itemIsSelected)

                if (itemIsSelected) {
                    anchor.context.canvasFontWindow(anchor) {
                        itemRenderer = renderer
                        onDismiss = {
                            updateItemSelected(false)
                            false
                        }
                    }
                }
            }
        }
        //样式
        TextStyleSelectItem()() {
            itemRenderer = renderer
        }
        //对齐
        TextAlignSelectItem()() {
            itemRenderer = renderer
            drawCanvasRight()
        }
        //属性调整
        TextPropertyControlItem()() {
            itemRenderer = renderer
            drawCanvasRight()
        }
        //曲线
        if (isDebugType()) {
            CanvasControlItem2()() {
                itemIco = R.drawable.canvas_text_curve
                itemText = _string(R.string.canvas_curve)
            }
        }

        TextOrientationItem()() {
            itemIco = R.drawable.canvas_text_standard_ico
            itemText = _string(R.string.canvas_standard)
            itemOrientation = LinearLayout.HORIZONTAL
            itemRenderer = renderer
        }
        TextOrientationItem()() {
            itemIco = R.drawable.canvas_text_vertical_ico
            itemText = _string(R.string.canvas_vertical)
            itemOrientation = LinearLayout.VERTICAL
            itemRenderer = renderer
            drawCanvasRight()
        }

        //紧凑
        if (isDebugType()) {
            CanvasControlItem2()() {
                itemText = "紧凑"
                itemIco = R.drawable.canvas_text_style
                itemIsSelected = renderer.getRendererRenderItem()?.dataBean?.isCompactText == true
                itemClick = {
                    updateItemSelected(!itemIsSelected)

                    renderer.dataTextItem?.updateTextCompact(itemIsSelected, renderer)
                }
            }
        }
    }

    //endregion ---文本---

    //region ---形状---

    fun DslAdapter.renderShapeEditItems(renderer: DataItemRenderer) {
        val type = renderer.dataItem?.dataBean?.mtype
        if (type == CanvasConstant.DATA_TYPE_RECT ||
            type == CanvasConstant.DATA_TYPE_POLYGON ||
            type == CanvasConstant.DATA_TYPE_PENTAGRAM
        ) {
            //多边形/星星
            //属性调整
            ShapePropertyControlItem()() {
                itemRenderer = renderer
            }
        }

        var strokeControlItem: DslAdapterItem? = null
        var fillControlItem: DslAdapterItem? = null

        val paintStyle = renderer.dataItem?.dataBean?.paintStyle?.toPaintStyle()

        CanvasControlItem2()() {
            strokeControlItem = this
            itemIco = R.drawable.canvas_style_stroke_ico
            itemText = _string(R.string.canvas_stroke)
            itemIsSelected = paintStyle == Paint.Style.STROKE
            itemClick = {
                renderer.dataItem?.updatePaintStyle(Paint.Style.STROKE, renderer)
                fillControlItem?.itemIsSelected = false
                itemIsSelected = true
                updateAllItem()
            }
        }
        CanvasControlItem2()() {
            fillControlItem = this
            itemIco = R.drawable.canvas_style_fill_ico
            itemText = _string(R.string.canvas_fill)
            drawCanvasRight()
            itemIsSelected = paintStyle == Paint.Style.FILL
            itemClick = {
                renderer.dataItem?.updatePaintStyle(Paint.Style.FILL, renderer)
                strokeControlItem?.itemIsSelected = false
                itemIsSelected = true
                updateAllItem()
            }
        }
        /*//有切割图层之后, 就不能出现这个了
        CanvasControlItem2()() {
            itemIco = R.drawable.canvas_style_stroke_fill_ico
            itemText = _string(R.string.canvas_fill_stroke)
            itemClick = {
                renderer.dataItem?.updatePaintStyle(Paint.Style.FILL_AND_STROKE, renderer)
            }
        }*/
    }

    //endregion ---形状---

    //region ---Path---

    //endregion ---Path---

    //region ---Group---

    fun DslAdapter.renderGroupEditItems(renderer: SelectGroupRenderer) {
        //
        CanvasControlItem2()() {
            itemIco = R.drawable.canvas_align_left_ico
            itemText = _string(R.string.canvas_align_left)
            itemRenderer = renderer
            itemClick = {
                renderer.updateAlign(Gravity.LEFT)
            }
        }
        CanvasControlItem2()() {
            itemIco = R.drawable.canvas_align_right_ico
            itemText = _string(R.string.canvas_align_right)
            itemRenderer = renderer
            itemClick = {
                renderer.updateAlign(Gravity.RIGHT)
            }
            drawCanvasRight()
        }

        //
        CanvasControlItem2()() {
            itemIco = R.drawable.canvas_align_top_ico
            itemText = _string(R.string.canvas_align_top)
            itemRenderer = renderer
            itemClick = {
                renderer.updateAlign(Gravity.TOP)
            }
        }
        CanvasControlItem2()() {
            itemIco = R.drawable.canvas_align_bottom_ico
            itemText = _string(R.string.canvas_align_bottom)
            itemRenderer = renderer
            itemClick = {
                renderer.updateAlign(Gravity.BOTTOM)
            }
            drawCanvasRight()
        }

        //
        CanvasControlItem2()() {
            itemIco = R.drawable.canvas_align_horizontal_ico
            itemText = _string(R.string.canvas_align_horizontal)
            itemRenderer = renderer
            itemClick = {
                renderer.updateAlign(Gravity.CENTER_HORIZONTAL)
            }
        }
        CanvasControlItem2()() {
            itemIco = R.drawable.canvas_align_vertical_ico
            itemText = _string(R.string.canvas_align_vertical)
            itemRenderer = renderer
            itemClick = {
                renderer.updateAlign(Gravity.CENTER_VERTICAL)
            }
        }
        CanvasControlItem2()() {
            itemIco = R.drawable.canvas_align_center_ico
            itemText = _string(R.string.canvas_align_center)
            itemRenderer = renderer
            itemClick = {
                renderer.updateAlign(Gravity.CENTER)
            }
            drawCanvasRight()
        }
    }

    //endregion ---Group---

    //region ---公共的编辑---

    fun DslAdapter.renderCommonEditItems(
        canvasView: CanvasView,
        fragment: AbsFragment,
        renderer: BaseItemRenderer<*>?
    ) {
        vmApp<LaserPeckerModel>().productInfoData.value?.previewBounds?.let { bounds ->
            //设备居中
            CanvasControlItem2()() {
                itemIco = R.drawable.canvas_align_center_ico
                itemText = _string(R.string.canvas_bounds_center)
                itemRenderer = renderer
                itemClick = {
                    renderer?.alignInBounds(bounds)
                }
                drawCanvasRight()
            }
        }

        //坐标编辑
        CanvasEditControlItem()() {
            itemRenderer = renderer
            itemCanvasDelegate = canvasView.canvasDelegate
        }

        //图层排序
        CanvasControlItem2()() {
            itemIco = R.drawable.canvas_layer_sort
            itemText = _string(R.string.canvas_sort)
            itemEnable = true
            itemClick = {
                fragment.context.menuPopupWindow(it) {
                    renderAdapterAction = {
                        CanvasArrangeItem()() {
                            itemArrange = CanvasDelegate.ARRANGE_FORWARD
                            itemRenderer = renderer
                            itemCanvasDelegate = canvasView.canvasDelegate
                        }
                        CanvasArrangeItem()() {
                            itemArrange = CanvasDelegate.ARRANGE_BACKWARD
                            itemRenderer = renderer
                            itemCanvasDelegate = canvasView.canvasDelegate
                        }
                        CanvasArrangeItem()() {
                            itemArrange = CanvasDelegate.ARRANGE_FRONT
                            itemRenderer = renderer
                            itemCanvasDelegate = canvasView.canvasDelegate
                            itemFlag = MenuPopupConfig.FLAG_ITEM_DISMISS
                        }
                        CanvasArrangeItem()() {
                            itemArrange = CanvasDelegate.ARRANGE_BACK
                            itemRenderer = renderer
                            itemCanvasDelegate = canvasView.canvasDelegate
                        }
                    }
                }
            }
        }
    }

    fun DslAdapterItem.drawCanvasRight(
        insertRight: Int = _dimen(R.dimen.lib_line),
        offsetTop: Int = _dimen(R.dimen.lib_drawable_padding),
        offsetBottom: Int = _dimen(R.dimen.lib_drawable_padding),
        color: Int = _color(R.color.canvas_dark_gray)
    ) {
        drawRight(insertRight, offsetTop, offsetBottom, color)
    }

    //endregion ---公共的编辑---
}
