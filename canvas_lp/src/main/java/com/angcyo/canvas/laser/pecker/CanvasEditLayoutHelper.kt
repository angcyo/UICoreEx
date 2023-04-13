package com.angcyo.canvas.laser.pecker

import android.graphics.Color
import android.graphics.Paint
import android.view.Gravity
import android.widget.LinearLayout
import com.angcyo.bluetooth.fsc.laserpacker.LaserPeckerConfigHelper
import com.angcyo.bluetooth.fsc.laserpacker.LaserPeckerModel
import com.angcyo.canvas.CanvasDelegate
import com.angcyo.canvas.CanvasView
import com.angcyo.canvas.Strategy
import com.angcyo.canvas.core.renderer.GroupRenderer
import com.angcyo.canvas.core.renderer.SelectGroupRenderer
import com.angcyo.canvas.graphics.IGraphicsParser
import com.angcyo.canvas.items.data.DataItemRenderer
import com.angcyo.canvas.items.renderer.BaseItemRenderer
import com.angcyo.canvas.laser.pecker.dslitem.*
import com.angcyo.canvas.utils.isJustGroupRenderer
import com.angcyo.core.vmApp
import com.angcyo.dialog.popup.MenuPopupConfig
import com.angcyo.dialog.popup.menuPopupWindow
import com.angcyo.dsladapter.*
import com.angcyo.fragment.AbsFragment
import com.angcyo.laserpacker.LPDataConstant
import com.angcyo.bluetooth.fsc.laserpacker.HawkEngraveKeys
import com.angcyo.laserpacker.toPaintStyle
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
        val closeImageEditItemsFun =
            LaserPeckerConfigHelper.readDeviceSettingConfig()?.closeImageEditItemsFun
        if (!closeImageEditItemsFun.have("_bw_")) {
            CanvasImageFilterItem()() {
                itemIco = R.drawable.canvas_bitmap_black_white
                itemText = _string(R.string.canvas_black_white)
                itemRenderer = renderer
                itemImageFilter = LPDataConstant.DATA_MODE_BLACK_WHITE
                itemClick = {
                    updateItemSelected(!itemIsSelected)
                    if (itemIsSelected) {
                        CanvasBitmapHandler.handleBlackWhite(it, fragment, renderer) {
                            itemIsSelected = false
                            updateAllItemBy { it is CanvasImageFilterItem }
                        }
                        UMEvent.CANVAS_IMAGE_BW.umengEventValue()
                    }
                }
            }
        }
        if (!closeImageEditItemsFun.have("_dithering_")) {
            CanvasImageFilterItem()() {
                itemIco = R.drawable.canvas_bitmap_dithering
                itemText = _string(R.string.canvas_dithering)
                itemRenderer = renderer
                itemImageFilter = LPDataConstant.DATA_MODE_DITHERING
                itemClick = {
                    updateItemSelected(!itemIsSelected)
                    if (itemIsSelected) {
                        CanvasBitmapHandler.handleDithering(it, fragment, renderer) {
                            itemIsSelected = false
                            updateAllItemBy { it is CanvasImageFilterItem }
                        }
                        UMEvent.CANVAS_IMAGE_DITHERING.umengEventValue()
                    }
                }
            }
        }
        if (!closeImageEditItemsFun.have("_gcode_")) {
            CanvasImageFilterItem()() {
                itemIco = R.drawable.canvas_bitmap_gcode
                itemText = _string(R.string.canvas_gcode)
                itemRenderer = renderer
                itemImageFilter = LPDataConstant.DATA_MODE_GCODE
                itemClick = {
                    updateItemSelected(!itemIsSelected)
                    if (itemIsSelected) {
                        CanvasBitmapHandler.handleGCode(it, fragment, renderer) {
                            itemIsSelected = false
                            updateAllItemBy { it is CanvasImageFilterItem }
                        }
                        UMEvent.CANVAS_IMAGE_GCODE.umengEventValue()
                    }
                }
            }
        }

        if (isDebugType()) {
            if (!closeImageEditItemsFun.have("_grey_")) {
                CanvasImageFilterItem()() {
                    itemIco = R.drawable.canvas_bitmap_grey
                    itemText = _string(R.string.canvas_grey)
                    itemRenderer = renderer
                    itemImageFilter = LPDataConstant.DATA_MODE_GREY
                    itemClick = {
                        CanvasBitmapHandler.handleGrey(it, fragment, renderer) {
                            itemIsSelected = false
                            updateAllItemBy { it is CanvasImageFilterItem }
                        }
                        UMEvent.CANVAS_IMAGE_GREY.umengEventValue()
                    }
                }
            }
        }
        if (!closeImageEditItemsFun.have("_print_")) {
            CanvasImageFilterItem()() {
                itemIco = R.drawable.canvas_bitmap_prints
                itemText = _string(R.string.canvas_prints)
                itemRenderer = renderer
                itemImageFilter = LPDataConstant.DATA_MODE_PRINT
                itemClick = {
                    updateItemSelected(!itemIsSelected)
                    if (itemIsSelected) {
                        CanvasBitmapHandler.handlePrint(it, fragment, renderer) {
                            itemIsSelected = false
                            updateAllItemBy { it is CanvasImageFilterItem }
                        }
                        UMEvent.CANVAS_IMAGE_PRINT.umengEventValue()
                    }
                }
            }
        }
        if (!closeImageEditItemsFun.have("_seal_")) {
            CanvasImageFilterItem()() {
                itemIco = R.drawable.canvas_bitmap_seal
                itemText = _string(R.string.canvas_seal)
                itemRenderer = renderer
                itemImageFilter = LPDataConstant.DATA_MODE_SEAL
                itemClick = {
                    updateItemSelected(!itemIsSelected)
                    if (itemIsSelected) {
                        CanvasBitmapHandler.handleSeal(it, fragment, renderer) {
                            itemIsSelected = false
                            updateAllItemBy { it is CanvasImageFilterItem }
                        }
                        UMEvent.CANVAS_IMAGE_SEAL.umengEventValue()
                    }
                }
            }
        }
        if (isDebugType() && !closeImageEditItemsFun.have("_mesh_")) {
            //扭曲
            CanvasImageFilterItem()() {
                itemIco = R.drawable.canvas_actions_ico
                itemText = _string(R.string.canvas_mesh)
                itemRenderer = renderer
                itemIsMeshItem = true
                itemClick = {
                    updateItemSelected(!itemIsSelected)
                    if (itemIsSelected) {
                        CanvasBitmapHandler.handleMesh(it, fragment, renderer) {
                            itemIsSelected = false
                            updateAllItemBy { it is CanvasImageFilterItem }
                        }
                        UMEvent.CANVAS_IMAGE_MESH.umengEventValue()
                    }
                }
                drawCanvasRight()
            }
        }
        if (!closeImageEditItemsFun.have("_crop_")) {
            //剪裁用的是原图
            CanvasImageFilterItem()() {
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
    }

    //endregion ---图片---

    //region ---文本---

    fun DslAdapter.renderTextEditItems(renderer: DataItemRenderer) {
        val closeTextEditItemsFun =
            LaserPeckerConfigHelper.readDeviceSettingConfig()?.closeTextEditItemsFun
        //字体
        if (!closeTextEditItemsFun.have("_typeface_")) {
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
        }
        //样式
        if (!closeTextEditItemsFun.have("_style_")) {
            TextStyleSelectItem()() {
                itemRenderer = renderer
            }
        }
        //对齐
        if (!closeTextEditItemsFun.have("_align_")) {
            TextAlignSelectItem()() {
                itemRenderer = renderer
                drawCanvasRight()
            }
        }
        //属性调整
        TextPropertyControlItem()() {
            itemRenderer = renderer
            drawCanvasRight()
        }
        //栅格化
        if (HawkEngraveKeys.enableRasterize) {
            CanvasControlItem2()() {
                itemIco = R.drawable.canvas_text_rasterize_ico
                itemText = _string(R.string.canvas_rasterize)
                itemClick = {
                    renderer.dataItem?.itemRasterize(renderer)
                }
            }
        }
        //曲线
        if (isDebugType()) {
            CanvasControlItem2()() {
                itemIco = R.drawable.canvas_text_curve_ico
                itemText = _string(R.string.canvas_curve)
            }
        }
        if (!closeTextEditItemsFun.have("_orientation_")) {
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

    //region ---形状/Path---

    fun DslAdapter.renderShapeEditItems(fragment: AbsFragment, renderer: DataItemRenderer) {
        val dataBean = renderer.dataItem?.dataBean
        val type = dataBean?.mtype
        if (type == LPDataConstant.DATA_TYPE_RECT ||
            type == LPDataConstant.DATA_TYPE_POLYGON ||
            type == LPDataConstant.DATA_TYPE_PENTAGRAM
        ) {
            //多边形/星星
            //属性调整
            ShapePropertyControlItem()() {
                itemRenderer = renderer
            }
        }

        //描边/填充 按钮切换
        var strokeControlItem: DslAdapterItem? = null
        var fillControlItem: DslAdapterItem? = null

        val paintStyle = dataBean?.paintStyle?.toPaintStyle()

        CanvasControlItem2()() {
            strokeControlItem = this
            itemIco = R.drawable.canvas_style_stroke_ico
            itemText = _string(R.string.canvas_stroke)
            itemIsSelected = paintStyle == Paint.Style.STROKE
            itemClick = {
                renderer.dataItem?.updatePaintStyle(Paint.Style.STROKE, renderer)
                fillControlItem?.updateItemSelected(false)
                updateItemSelected(true)
            }
        }

        var afterItemCount = 0 //后面item的数量, 用来控制是否需要绘制分割线
        if (HawkEngraveKeys.enableRasterize) {
            afterItemCount++
        }
        if (HawkEngraveKeys.enablePathFill) {
            afterItemCount++
        }
        CanvasControlItem2()() {
            fillControlItem = this
            itemIco = R.drawable.canvas_style_fill_ico
            itemText = _string(R.string.canvas_fill)
            if (afterItemCount <= 0) {
                drawCanvasRight()
            }
            itemIsSelected = paintStyle == Paint.Style.FILL
            itemClick = {
                renderer.dataItem?.updatePaintStyle(Paint.Style.FILL, renderer)
                strokeControlItem?.updateItemSelected(false)
                updateItemSelected(true)
            }
            afterItemCount--
        }
        /*//有切割图层之后, 就不能出现这个了
        CanvasControlItem2()() {
            itemIco = R.drawable.canvas_style_stroke_fill_ico
            itemText = _string(R.string.canvas_fill_stroke)
            itemClick = {
                renderer.dataItem?.updatePaintStyle(Paint.Style.FILL_AND_STROKE, renderer)
            }
        }*/
        if (HawkEngraveKeys.enablePathFill) {
            CanvasControlItem2()() {
                itemIco = R.drawable.canvas_path_fill_svg
                itemText = _string(R.string.canvas_path_fill)
                if (afterItemCount <= 0) {
                    drawCanvasRight()
                }
                itemUpdateCheckColorAction = {
                    if (IGraphicsParser.isNeedGCodeFill(dataBean)) {
                        _color(R.color.colorAccent).alphaRatio(0.5f)
                    } else {
                        Color.TRANSPARENT
                    }
                }
                itemClick = {
                    updateItemSelected(!itemIsSelected)
                    if (itemIsSelected) {
                        CanvasBitmapHandler.handlePathFill(it, fragment, renderer) {
                            updateItemSelected(false)
                        }
                    }
                }
                afterItemCount--
            }
        }
        //栅格化
        if (HawkEngraveKeys.enableRasterize) {
            CanvasControlItem2()() {
                itemIco = R.drawable.canvas_text_rasterize_ico
                itemText = _string(R.string.canvas_rasterize)
                if (afterItemCount <= 0) {
                    drawCanvasRight()
                }
                itemClick = {
                    renderer.dataItem?.itemRasterize(renderer)
                }
                afterItemCount--
            }
        }
    }

    //endregion ---形状/Path---

    //region ---Group---

    /**群组, 多选*/
    fun DslAdapter.renderGroupEditItems(fragment: AbsFragment, renderer: GroupRenderer) {
        //对齐
        CanvasControlItem2()() {
            itemIco = R.drawable.canvas_align_left_ico
            itemText = _string(R.string.canvas_align)
            itemEnable = renderer.subItemList.size >= 2//2个以上的元素才支持对齐
            itemClick = {
                fragment.context.menuPopupWindow(it) {
                    renderAdapterAction = {
                        CanvasAlignItem()() {
                            itemIco = R.drawable.canvas_align_left_ico
                            itemText = _string(R.string.canvas_align_left)
                            itemRenderer = renderer
                            itemAlign = Gravity.LEFT
                        }
                        CanvasAlignItem()() {
                            itemIco = R.drawable.canvas_align_right_ico
                            itemText = _string(R.string.canvas_align_right)
                            itemRenderer = renderer
                            itemAlign = Gravity.RIGHT
                        }

                        //
                        CanvasAlignItem()() {
                            itemIco = R.drawable.canvas_align_top_ico
                            itemText = _string(R.string.canvas_align_top)
                            itemRenderer = renderer
                            itemAlign = Gravity.TOP
                        }
                        CanvasAlignItem()() {
                            itemIco = R.drawable.canvas_align_bottom_ico
                            itemText = _string(R.string.canvas_align_bottom)
                            itemRenderer = renderer
                            itemAlign = Gravity.BOTTOM
                        }

                        //
                        CanvasAlignItem()() {
                            itemIco = R.drawable.canvas_align_horizontal_ico
                            itemText = _string(R.string.canvas_align_horizontal)
                            itemRenderer = renderer
                            itemAlign = Gravity.CENTER_HORIZONTAL
                        }
                        CanvasAlignItem()() {
                            itemIco = R.drawable.canvas_align_vertical_ico
                            itemText = _string(R.string.canvas_align_vertical)
                            itemRenderer = renderer
                            itemAlign = Gravity.CENTER_VERTICAL
                        }
                        CanvasAlignItem()() {
                            itemIco = R.drawable.canvas_align_center_ico
                            itemText = _string(R.string.canvas_align_center)
                            itemRenderer = renderer
                            itemAlign = Gravity.CENTER
                        }
                    }
                }
            }
        }

        //分布
        CanvasControlItem2()() {
            itemIco = R.drawable.canvas_flat_horizontal_svg
            itemText = _string(R.string.canvas_flat)
            itemEnable = renderer.subItemList.size >= 3//3个以上的元素才支持分布
            drawCanvasRight()
            itemClick = {
                fragment.context.menuPopupWindow(it) {
                    renderAdapterAction = {
                        CanvasFlatItem()() {
                            itemIco = R.drawable.canvas_flat_horizontal_svg
                            itemText = _string(R.string.canvas_flat_horizontal)
                            itemRenderer = renderer
                            itemFlat = LinearLayout.HORIZONTAL
                        }
                        CanvasFlatItem()() {
                            itemIco = R.drawable.canvas_flat_vertical_svg
                            itemText = _string(R.string.canvas_flat_vertical)
                            itemRenderer = renderer
                            itemFlat = LinearLayout.VERTICAL
                        }
                    }
                }
            }
        }

        //群组
        CanvasControlItem2()() { //编组
            itemIco = R.drawable.canvas_group_group_svg
            itemText = _string(R.string.canvas_group_group)
            itemEnable = renderer is SelectGroupRenderer
            itemClick = {
                renderer.canvasDelegate.groupGroup(renderer, Strategy.normal)
            }
        }
        CanvasControlItem2()() { //解组
            itemIco = R.drawable.canvas_group_dissolve_svg
            itemText = _string(R.string.canvas_group_dissolve)
            itemEnable = renderer.isJustGroupRenderer()
            drawCanvasRight()
            itemClick = {
                renderer.canvasDelegate.groupDissolve(renderer, Strategy.normal)
            }
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
            }
        }

        //图层排序
        CanvasControlItem2()() {
            itemIco = R.drawable.canvas_layer_sort
            itemText = _string(R.string.canvas_sort)
            itemEnable = true
            drawCanvasRight()
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

        //坐标编辑
        CanvasEditControlItem()() {
            itemRenderer = renderer
            itemCanvasDelegate = canvasView.canvasDelegate
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
