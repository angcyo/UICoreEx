package com.angcyo.canvas2.laser.pecker

import android.graphics.Color
import android.graphics.Paint
import android.view.ViewGroup
import android.widget.LinearLayout
import com.angcyo.bluetooth.fsc.laserpacker.HawkEngraveKeys
import com.angcyo.bluetooth.fsc.laserpacker.LaserPeckerConfigHelper
import com.angcyo.bluetooth.fsc.laserpacker.LaserPeckerModel
import com.angcyo.bluetooth.fsc.laserpacker._deviceSettingBean
import com.angcyo.canvas.render.core.component.CanvasSelectorComponent
import com.angcyo.canvas.render.element.TextElement
import com.angcyo.canvas.render.renderer.BaseRenderer
import com.angcyo.canvas.render.renderer.CanvasGroupRenderer
import com.angcyo.canvas.render.util.element
import com.angcyo.canvas.render.util.isOnlyGroupRenderer
import com.angcyo.canvas.render.util.isSelectorGroupRenderer
import com.angcyo.canvas.render.util.renderElement
import com.angcyo.canvas2.laser.pecker.dialog.canvasFontWindow
import com.angcyo.canvas2.laser.pecker.dslitem.CanvasIconItem
import com.angcyo.canvas2.laser.pecker.dslitem.ICanvasRendererItem
import com.angcyo.canvas2.laser.pecker.dslitem.control.AlignDeviceItem
import com.angcyo.canvas2.laser.pecker.dslitem.control.BarcodeErrorLevelSelectItem
import com.angcyo.canvas2.laser.pecker.dslitem.control.BarcodeMaskSelectItem
import com.angcyo.canvas2.laser.pecker.dslitem.control.BarcodeShowStyleMenuItem
import com.angcyo.canvas2.laser.pecker.dslitem.control.BarcodeTypeSelectItem
import com.angcyo.canvas2.laser.pecker.dslitem.control.EditControlItem
import com.angcyo.canvas2.laser.pecker.dslitem.control.ImageFilterItem
import com.angcyo.canvas2.laser.pecker.dslitem.control.LayerSortMenuItem
import com.angcyo.canvas2.laser.pecker.dslitem.control.PathStyleItem
import com.angcyo.canvas2.laser.pecker.dslitem.control.PathUnionMenuItem
import com.angcyo.canvas2.laser.pecker.dslitem.control.RendererAlignMenuItem
import com.angcyo.canvas2.laser.pecker.dslitem.control.RendererFlatMenuItem
import com.angcyo.canvas2.laser.pecker.dslitem.control.ShapePropertyControlItem
import com.angcyo.canvas2.laser.pecker.dslitem.control.TextAlignMenuItem
import com.angcyo.canvas2.laser.pecker.dslitem.control.TextOrientationItem
import com.angcyo.canvas2.laser.pecker.dslitem.control.TextPropertyControlItem
import com.angcyo.canvas2.laser.pecker.dslitem.control.TextStyleMenuItem
import com.angcyo.canvas2.laser.pecker.dslitem.control.TextTypefaceSelectItem
import com.angcyo.canvas2.laser.pecker.dslitem.item.ControlEditItem
import com.angcyo.canvas2.laser.pecker.element.LPBitmapElement
import com.angcyo.canvas2.laser.pecker.element.LPPathElement
import com.angcyo.canvas2.laser.pecker.element.LPTextElement
import com.angcyo.canvas2.laser.pecker.element.LPTextElement.Companion.toBarcodeFormat
import com.angcyo.canvas2.laser.pecker.util.LPBitmapHandler
import com.angcyo.canvas2.laser.pecker.util.LPElementHelper
import com.angcyo.canvas2.laser.pecker.util.lpElementBean
import com.angcyo.canvas2.laser.pecker.util.lpTextElement
import com.angcyo.core.vmApp
import com.angcyo.dsladapter.DslAdapter
import com.angcyo.dsladapter.DslAdapterItem
import com.angcyo.dsladapter.eachItem
import com.angcyo.dsladapter.updateAllItemBy
import com.angcyo.dsladapter.updateItemSelected
import com.angcyo.item.style.itemDefaultNew
import com.angcyo.item.style.itemHaveNew
import com.angcyo.item.style.itemNewHawkKeyStr
import com.angcyo.laserpacker.LPDataConstant
import com.angcyo.library.annotation.CallPoint
import com.angcyo.library.canvas.core.Reason
import com.angcyo.library.component.Strategy
import com.angcyo.library.ex._color
import com.angcyo.library.ex._drawable
import com.angcyo.library.ex._string
import com.angcyo.library.ex.alphaRatio
import com.angcyo.library.ex.dpi
import com.angcyo.library.ex.have
import com.angcyo.library.ex.isDebug
import com.angcyo.library.ex.isDebugType
import com.angcyo.library.ex.setSize
import com.angcyo.library.ex.size
import com.angcyo.library.utils.BuildHelper.isCpu64
import com.angcyo.qrcode.code.haveErrorCorrection
import com.angcyo.transition.dslTransition
import com.angcyo.widget.recycler.renderDslAdapter
import com.angcyo.widget.recycler.restoreScrollPosition
import com.angcyo.widget.recycler.saveScrollPosition
import com.angcyo.widget.span.span
import com.google.zxing.BarcodeFormat
import com.hingin.umeng.UMEvent
import com.hingin.umeng.umengEventValue

/**
 * 选中/取消选中元素后控制布局助手
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2023/03/08
 */
class RenderControlHelper(override val renderLayoutHelper: RenderLayoutHelper) : IControlHelper {

    companion object {

        /**曲线文本item*/
        const val TAG_CURVE_ITEM = "tag_curve_item"
    }

    /**编辑item*/
    val editItem: DslAdapterItem?
        get() = renderLayoutHelper.findTagItem(ControlEditItem.TAG_EDIT_ITEM)

    /**是否可以编辑*/
    val isEditEnable: Boolean
        get() = editItem == null || editItem?.itemEnable == true

    //region ---基础---

    /**根据选中状态自动渲染/隐藏布局*/
    @CallPoint
    fun bindControlLayout() {
        renderLayoutHelper.changeSelectItem(null)
        val selectorRenderer = selectorManager?.getTargetSelectorRenderer()
        if (selectorRenderer == null) {
            hideControlLayout()
        } else {
            showControlLayout(null)
            renderControlItems(false, selectorRenderer)
        }
    }

    /**显示控制布局*/
    fun showControlLayout(fromItem: DslAdapterItem?) {
        if (isEditEnable) {
            editItem?.updateItemSelected(fromItem == null || fromItem == editItem)
            _rootViewHolder?.apply {
                //转场动画
                dslTransition(itemView as ViewGroup) {
                    onCaptureStartValues = {
                        //invisible(R.id.canvas_control_layout)
                    }
                    onCaptureEndValues = {
                        visible(R.id.canvas_control_layout, true)
                    }
                }
            }
        }
    }

    /**隐藏控制布局
     * [force] 是否强制隐藏*/
    fun hideControlLayout(force: Boolean = false) {
        if (isEditEnable || force) {
            editItem?.updateItemSelected(false)
            _rootViewHolder?.apply {
                //转场动画
                dslTransition(itemView as ViewGroup) {
                    onCaptureStartValues = {
                        //invisible(R.id.canvas_control_layout)
                    }
                    onCaptureEndValues = {
                        visible(R.id.canvas_control_layout, false)
                    }
                }
            }
        }
    }

    /**追加控制item*/
    fun renderControlItems(
        restoreScrollPosition: Boolean = true,
        renderer: BaseRenderer? = selectorManager?.getTargetSelectorRenderer(),
    ) {
        //控制item渲染
        val canvasControlRv = _rootViewHolder?.canvasControlRv
        canvasControlRv?.stopScroll() //停止滚动后, 再去获取保存的滚动位置. 没啥用!
        val scrollPosition = canvasControlRv?.saveScrollPosition()
        canvasControlRv?.renderDslAdapter {
            renderLayoutHelper.hookUpdateDepend(this)

            //渲染不同的控制item
            when (renderer) {
                //多选 //群组
                is CanvasSelectorComponent, is CanvasGroupRenderer -> renderGroupEditItems(renderer as CanvasGroupRenderer)
                //单元素
                else -> {
                    when (renderer?.renderElement) {
                        is LPBitmapElement -> renderBitmapEditItems(renderer)
                        is LPTextElement -> renderTextEditItems(renderer)
                        is LPPathElement -> renderPathEditItems(renderer)
                    }
                }
            }

            //公共编辑
            renderCommonEditItems(renderer)

            //是否要恢复滚动的位置
            if (restoreScrollPosition && scrollPosition != null) {
                onDispatchUpdatesOnce {
                    canvasControlRv.restoreScrollPosition(scrollPosition)
                }
            }
        }
    }

    /**当有元素属性更新时, 触发更新显示*/
    @CallPoint
    fun updateControlLayout() {
        val vh = _rootViewHolder ?: return
        if (vh.isVisible(R.id.canvas_control_layout)) {
            vh.canvasControlAdapter?.apply {
                eachItem { index, item ->
                    if (item is ICanvasRendererItem) {
                        val selectorRenderer = selectorManager?.getTargetSelectorRenderer()
                        item.initItem(selectorRenderer)
                    }
                }
                updateAllItem()
            }
        }
    }

    /**初始化*/
    private fun ICanvasRendererItem.initItem(renderer: BaseRenderer?) {
        itemRenderer = renderer
        itemRenderDelegate = renderDelegate
    }

    //endregion ---基础---

    //region ---Bitmap---

    /**渲染图片编辑控制items
     * [LPBitmapElement]*/
    private fun DslAdapter.renderBitmapEditItems(renderer: BaseRenderer) {
        val closeImageEditItemsFun = _deviceSettingBean?.closeImageEditItemsFun

        //隐藏某些功能, 在32位的设备上
        val hideIn32 = HawkEngraveKeys.checkCpu32 && !isCpu64

        if (!closeImageEditItemsFun.have("_bw_")) {
            ImageFilterItem()() {
                itemIco = R.drawable.canvas_bitmap_black_white
                itemText = _string(R.string.canvas_black_white)
                initItem(renderer)
                itemImageFilter = LPDataConstant.DATA_MODE_BLACK_WHITE
                itemClick = {
                    updateItemSelected(!itemIsSelected)
                    if (itemIsSelected) {
                        LPBitmapHandler.handleBlackWhite(
                            renderDelegate,
                            it,
                            fragment,
                            renderer
                        ) {
                            itemIsSelected = false
                            updateAllItemBy { it is ImageFilterItem }
                        }
                        UMEvent.CANVAS_IMAGE_BW.umengEventValue()
                    }
                }
            }
        }
        if (!closeImageEditItemsFun.have("_dithering_")) {
            ImageFilterItem()() {
                itemIco = R.drawable.canvas_bitmap_dithering
                itemText = _string(R.string.canvas_dithering)
                initItem(renderer)
                itemImageFilter = LPDataConstant.DATA_MODE_DITHERING
                itemClick = {
                    updateItemSelected(!itemIsSelected)
                    if (itemIsSelected) {
                        LPBitmapHandler.handleDithering(
                            renderDelegate,
                            it,
                            fragment,
                            renderer
                        ) {
                            itemIsSelected = false
                            updateAllItemBy { it is ImageFilterItem }
                        }
                        UMEvent.CANVAS_IMAGE_DITHERING.umengEventValue()
                    }
                }
            }
        }
        if (!hideIn32 && !closeImageEditItemsFun.have("_gcode_")) {
            ImageFilterItem()() {
                itemIco = R.drawable.canvas_bitmap_gcode
                itemText = _string(R.string.canvas_gcode)
                initItem(renderer)
                itemImageFilter = LPDataConstant.DATA_MODE_GCODE
                itemClick = {
                    updateItemSelected(!itemIsSelected)
                    if (itemIsSelected) {
                        LPBitmapHandler.handleGCode(renderDelegate, it, fragment, renderer) {
                            itemIsSelected = false
                            updateAllItemBy { it is ImageFilterItem }
                        }
                        UMEvent.CANVAS_IMAGE_GCODE.umengEventValue()
                    }
                }
            }
        }

        //灰度
        if (isDebugType() || HawkEngraveKeys.enableGrey) {
            if (!closeImageEditItemsFun.have("_grey_")) {
                ImageFilterItem()() {
                    itemIco = R.drawable.canvas_bitmap_grey
                    itemText = _string(R.string.canvas_grey)
                    initItem(renderer)
                    itemImageFilter = LPDataConstant.DATA_MODE_GREY
                    itemClick = {
                        LPBitmapHandler.handleGrey(renderDelegate, it, fragment, renderer) {
                            itemIsSelected = false
                            updateAllItemBy { it is ImageFilterItem }
                        }
                        UMEvent.CANVAS_IMAGE_GREY.umengEventValue()
                    }
                }
            }
        }
        if (!closeImageEditItemsFun.have("_print_")) {
            ImageFilterItem()() {
                itemIco = R.drawable.canvas_bitmap_prints
                itemText = _string(R.string.canvas_prints)
                initItem(renderer)
                itemImageFilter = LPDataConstant.DATA_MODE_PRINT
                itemClick = {
                    updateItemSelected(!itemIsSelected)
                    if (itemIsSelected) {
                        LPBitmapHandler.handlePrint(renderDelegate, it, fragment, renderer) {
                            itemIsSelected = false
                            updateAllItemBy { it is ImageFilterItem }
                        }
                        UMEvent.CANVAS_IMAGE_PRINT.umengEventValue()
                    }
                }
            }
        }
        if (!closeImageEditItemsFun.have("_seal_")) {
            ImageFilterItem()() {
                itemIco = R.drawable.canvas_bitmap_seal
                itemText = _string(R.string.canvas_seal)
                initItem(renderer)
                itemImageFilter = LPDataConstant.DATA_MODE_SEAL
                itemClick = {
                    updateItemSelected(!itemIsSelected)
                    if (itemIsSelected) {
                        LPBitmapHandler.handleSeal(renderDelegate, it, fragment, renderer) {
                            itemIsSelected = false
                            updateAllItemBy { it is ImageFilterItem }
                        }
                        UMEvent.CANVAS_IMAGE_SEAL.umengEventValue()
                    }
                }
            }
        }
        if (isDebugType() && !closeImageEditItemsFun.have("_mesh_")) {
            //扭曲
            ImageFilterItem()() {
                itemIco = R.drawable.canvas_actions_ico
                itemText = _string(R.string.canvas_mesh)
                initItem(renderer)
                itemIsMeshItem = true
                itemClick = {
                    updateItemSelected(!itemIsSelected)
                    if (itemIsSelected) {
                        LPBitmapHandler.handleMesh(renderDelegate, it, fragment, renderer) {
                            itemIsSelected = false
                            updateAllItemBy { it is ImageFilterItem }
                        }
                        UMEvent.CANVAS_IMAGE_MESH.umengEventValue()
                    }
                }
                drawCanvasRight()
            }
        }
        //魔棒
        if (!closeImageEditItemsFun.have("_magicWand_")) {
            //偏移用的是过滤后的图
            CanvasIconItem()() {
                itemIco = R.drawable.canvas_magic_wand
                itemText = _string(R.string.canvas_magic_wand)
                itemNewHawkKeyStr = "magicWand"
                itemDefaultNew = LaserPeckerConfigHelper.haveNew(itemNewHawkKeyStr)
                itemClick = {
                    itemHaveNew = false
                    updateItemSelected(!itemIsSelected)
                    if (itemIsSelected) {
                        LPBitmapHandler.handleMagicWand(renderDelegate, it, fragment, renderer) {
                            updateItemSelected(false)
                        }
                        UMEvent.CANVAS_MAGIC_WAND.umengEventValue()
                    }
                }
                if (closeImageEditItemsFun.have("_outline_") && closeImageEditItemsFun.have("_crop_")) {
                    //右边线
                    drawCanvasRight()
                }
            }
        }
        //偏移
        if (!closeImageEditItemsFun.have("_outline_")) {
            //偏移用的是过滤后的图
            CanvasIconItem()() {
                itemIco = R.drawable.crop_auto_side_icon
                itemText = _string(R.string.canvas_outline)
                itemNewHawkKeyStr = "outline"
                itemDefaultNew = LaserPeckerConfigHelper.haveNew(itemNewHawkKeyStr)
                itemClick = {
                    itemHaveNew = false
                    updateItemSelected(!itemIsSelected)
                    if (itemIsSelected) {
                        LPBitmapHandler.handleOutline(renderDelegate, it, fragment, renderer) {
                            updateItemSelected(false)
                        }
                        UMEvent.CANVAS_IMAGE_OUTLINE.umengEventValue()
                    }
                }
                if (closeImageEditItemsFun.have("_crop_")) {
                    //右边线
                    drawCanvasRight()
                }
            }
        }
        //浮雕切片
        if (isDebug() && !closeImageEditItemsFun.have("_slice_")) {
            ImageFilterItem()() {
                itemIco = R.drawable.canvas_slice_ico
                itemText = _string(R.string.canvas_relief)
                itemIsReliefItem = true
                itemNewHawkKeyStr = "slice"
                itemDefaultNew = LaserPeckerConfigHelper.haveNew(itemNewHawkKeyStr)
                itemEnable = _elementBean?.isSupportSliceElement == true
                initItem(renderer)
                itemClick = {
                    itemHaveNew = false
                    updateItemSelected(!itemIsSelected)
                    if (itemIsSelected) {
                        LPBitmapHandler.handleSlice(renderDelegate, it, fragment, renderer) {
                            updateItemSelected(false)
                        }
                        UMEvent.CANVAS_IMAGE_SLICE.umengEventValue()
                    }
                }
            }
        }
        //剪裁
        if (!closeImageEditItemsFun.have("_crop_")) {
            //剪裁用的是原图
            CanvasIconItem()() {
                itemIco = R.drawable.canvas_bitmap_crop
                itemText = _string(R.string.canvas_crop)
                itemClick = {
                    updateItemSelected(!itemIsSelected)
                    if (itemIsSelected) {
                        LPBitmapHandler.handleCrop(renderDelegate, it, fragment, renderer) {
                            updateItemSelected(false)
                        }
                        UMEvent.CANVAS_IMAGE_CROP.umengEventValue()
                    }
                }
                drawCanvasRight()
            }
        }
    }

    //endregion ---Bitmap---

    //region ---文本---

    /**渲染文本编辑控制items
     * [LPTextElement]*/
    private fun DslAdapter.renderTextEditItems(renderer: BaseRenderer) {
        val closeTextEditItemsFun = _deviceSettingBean?.closeTextEditItemsFun
        val lpElementBean = renderer.lpElementBean()
        val barcodeFormat = lpElementBean?.toBarcodeFormat()

        //---以下是1D条形码相关属性的控制---
        if (lpElementBean?.is1DCodeElement == true) {
            BarcodeTypeSelectItem()() {
                initItem(renderer)
            }
            BarcodeShowStyleMenuItem()() {
                initItem(renderer)
                drawCanvasRight()
            }
        }

        //---以下是2D条形码相关属性的控制---
        if (lpElementBean?.is2DCodeElement == true) {
            val haveErrorCorrection = barcodeFormat?.haveErrorCorrection() == true
            val haveMask = barcodeFormat == BarcodeFormat.QR_CODE

            BarcodeTypeSelectItem()() {
                initItem(renderer)
                if (!haveErrorCorrection && !haveMask) {
                    drawCanvasRight()
                }
            }
            if (haveErrorCorrection) {
                BarcodeErrorLevelSelectItem()() {
                    initItem(renderer)
                    if (barcodeFormat != BarcodeFormat.QR_CODE) {
                        drawCanvasRight()
                    }
                }
            }
            if (haveMask) {
                BarcodeMaskSelectItem()() {
                    initItem(renderer)
                    drawCanvasRight()
                }
            }
        }

        //---以下是纯文本相关属性的控制---

        val isText = lpElementBean?.isRenderTextElement == true
        val isShowBarcodeText =
            lpElementBean?.is1DCodeElement == true && lpElementBean.isShowBarcodeText
        if (!isText && !isShowBarcodeText) {
            //非文本类型, 不显示文本相关属性
            return
        }
        //字体
        if (!closeTextEditItemsFun.have("_typeface_")) {
            TextTypefaceSelectItem()() {
                initItem(renderer)

                //标识变量元素icon
                itemText = span {
                    if (lpElementBean?.isVariableElement == true && _deviceSettingBean?.showVariableElementIco == true) {
                        if (lpElementBean.is1DCodeElement || lpElementBean.is2DCodeElement) {
                            //appendDrawable(_drawable(R.drawable.canvas_var_barcode_ico)?.setSize(14 * dpi))
                        } else {
                            appendDrawable(_drawable(R.drawable.canvas_var_text_ico)?.setSize(14 * dpi))
                        }
                        append(" ")
                    }
                    append(_string(R.string.canvas_font))
                }

                itemClick = { anchor ->
                    updateItemSelected(!itemIsSelected)

                    if (itemIsSelected) {
                        anchor.context.canvasFontWindow(anchor) {
                            initItem(renderer)
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
            TextStyleMenuItem()() {
                initItem(renderer)
            }
        }
        //对齐
        if (!closeTextEditItemsFun.have("_align_")) {
            TextAlignMenuItem()() {
                initItem(renderer)
                drawCanvasRight()
            }
        }
        //属性调整
        TextPropertyControlItem()() {
            initItem(renderer)
            drawCanvasRight()
        }
        //曲线
        if (isText && !closeTextEditItemsFun.have("_curve_")) {
            CanvasIconItem()() {
                initItem(renderer)
                itemIco = R.drawable.canvas_text_curve_ico
                itemText = _string(R.string.canvas_curve)
                itemTag = TAG_CURVE_ITEM
                itemNewHawkKeyStr = "curve"
                val lpTextElement = renderer.lpTextElement()
                itemEnable = lpTextElement?.isSupportCurve == true
                itemDefaultNew = LaserPeckerConfigHelper.haveNew(itemNewHawkKeyStr)
                itemClick = {
                    itemEnable = lpTextElement?.isSupportCurve == true
                    if (itemEnable) {
                        itemHaveNew = false
                        LPBitmapHandler.handleCurveText(renderDelegate, it, fragment, renderer) {
                            itemIsSelected = false
                        }
                        UMEvent.CANVAS_TEXT_CURVE.umengEventValue()
                    } else {
                        updateAdapterItem()
                    }
                }
            }
        }
        //水平/垂直
        if (isText && !closeTextEditItemsFun.have("_orientation_")) {
            TextOrientationItem()() {
                initItem(renderer)
                itemIco = R.drawable.canvas_text_standard_ico
                itemText = _string(R.string.canvas_standard)
                itemOrientation = LinearLayout.HORIZONTAL
            }
            TextOrientationItem()() {
                initItem(renderer)
                itemIco = R.drawable.canvas_text_vertical_ico
                itemText = _string(R.string.canvas_vertical)
                itemOrientation = LinearLayout.VERTICAL
                //itemEnable = renderer.lpTextElement()?.isCurveText != true
                drawCanvasRight()
            }
        }

        //紧凑
        if (isDebugType()) {
            CanvasIconItem()() {
                initItem(renderer)
                itemText = "紧凑"
                itemNewHawkKeyStr = "compact"
                itemIco = R.drawable.canvas_text_style
                itemIsSelected =
                    renderer.element<TextElement>()?.textProperty?.isCompactText == true
                itemDefaultNew = LaserPeckerConfigHelper.haveNew(itemNewHawkKeyStr)
                itemClick = {
                    itemHaveNew = false
                    updateItemSelected(!itemIsSelected)

                    renderer.element<TextElement>()
                        ?.updateTextProperty(renderer, itemRenderDelegate) {
                            isCompactText = itemIsSelected
                        }
                }
            }
        }
    }

    //endregion ---文本---

    //region ---形状/Path---

    /**[LPPathElement]*/
    fun DslAdapter.renderPathEditItems(renderer: BaseRenderer) {
        val elementBean = renderer.lpElementBean()
        val type = elementBean?.mtype
        val isLineShape = elementBean?.isLineShape == true
        if (type == LPDataConstant.DATA_TYPE_RECT ||
            type == LPDataConstant.DATA_TYPE_POLYGON ||
            type == LPDataConstant.DATA_TYPE_PENTAGRAM
        ) {
            //多边形/星星
            //属性调整
            ShapePropertyControlItem()() {
                initItem(renderer)
            }
        }

        PathStyleItem()() {
            initItem(renderer)
            itemIco = R.drawable.canvas_style_stroke_ico
            itemText = _string(R.string.canvas_stroke)
            itemStyle = Paint.Style.STROKE
            if (!isDebug()) {
                itemEnable = !isLineShape
            }
        }

        var afterItemCount = 0 //后面item的数量, 用来控制是否需要绘制分割线
        val enablePathFill = HawkEngraveKeys.enablePathFill && !isLineShape
        if (enablePathFill) {
            afterItemCount++
        }
        PathStyleItem()() {
            initItem(renderer)
            itemIco = R.drawable.canvas_style_fill_ico
            itemText = _string(R.string.canvas_fill)
            itemStyle = Paint.Style.FILL
            if (afterItemCount <= 0) {
                drawCanvasRight()
            }
            afterItemCount--
        }
        /*//有切割图层之后, 就不能出现这个了
        PathStyleItem()() {
            initItem(renderer)
            itemIco = R.drawable.canvas_style_stroke_fill_ico
            itemText = _string(R.string.canvas_fill_stroke)
            itemStyle = Paint.Style.FILL_AND_STROKE
        }*/
        if (enablePathFill) {
            CanvasIconItem()() {
                initItem(renderer)
                itemIco = R.drawable.canvas_path_fill_svg
                itemText = _string(R.string.canvas_path_fill)
                itemNewHawkKeyStr = "pathFill"
                itemDefaultNew = LaserPeckerConfigHelper.haveNew(itemNewHawkKeyStr)
                if (afterItemCount <= 0) {
                    drawCanvasRight()
                }
                itemUpdateCheckColorAction = {
                    if (LPPathElement.isPathFill(elementBean)) {
                        _color(R.color.colorAccent).alphaRatio(0.5f)
                    } else {
                        Color.TRANSPARENT
                    }
                }
                itemClick = {
                    itemHaveNew = false
                    updateItemSelected(!itemIsSelected)
                    if (itemIsSelected) {
                        LPBitmapHandler.handlePathFill(renderDelegate, it, fragment, renderer) {
                            updateItemSelected(false)
                        }
                    }
                }
                afterItemCount--
            }
        }
    }

    //endregion ---形状/Path---

    //region ---Group---

    /**群组, 多选*/
    fun DslAdapter.renderGroupEditItems(renderer: CanvasGroupRenderer) {

        //合并
        if (PathUnionMenuItem.isAllShape(renderer)) {
            PathUnionMenuItem()() {
                initItem(renderer)
                itemEnable = !PathUnionMenuItem.disablePathUnion(renderer)
            }
        }

        //对齐
        RendererAlignMenuItem()() {
            initItem(renderer)
            itemEnable = renderer.isSelectorGroupRenderer() &&
                    renderer.rendererList.size() >= 2//2个以上的元素才支持对齐
        }

        //分布
        RendererFlatMenuItem()() {
            initItem(renderer)
            itemEnable = renderer.isSelectorGroupRenderer() && renderer.rendererList.size() >= 2
            drawCanvasRight()
        }

        //群组
        CanvasIconItem()() { //编组
            itemIco = R.drawable.canvas_group_group_svg
            itemText = _string(R.string.canvas_group_group)
            initItem(renderer)
            itemEnable = renderer.isSelectorGroupRenderer() && renderer.rendererList.size() > 1
            itemClick = {
                //com.angcyo.canvas.render.core.BaseCanvasRenderListener#onRendererGroupChange
                renderer.groupRendererGroup(renderDelegate, Reason.user, Strategy.normal)
            }
        }
        CanvasIconItem()() { //解组
            itemIco = R.drawable.canvas_group_dissolve_svg
            itemText = _string(R.string.canvas_group_dissolve)
            initItem(renderer)
            itemEnable = renderer.isOnlyGroupRenderer()
            drawCanvasRight()
            itemClick = {
                //com.angcyo.canvas.render.core.BaseCanvasRenderListener#onRendererGroupChange
                renderer.groupRendererDissolve(renderDelegate, Reason.user, Strategy.normal)
            }
        }
    }

    //endregion ---Group---

    //region ---公共的编辑---

    private fun DslAdapter.renderCommonEditItems(renderer: BaseRenderer?) {
        if (renderer != null) {

            //栅格化, 2023-09-16挪到此处
            if (HawkEngraveKeys.enableRasterize) {
                CanvasIconItem()() {
                    initItem(renderer)
                    itemIco = R.drawable.canvas_text_rasterize_ico
                    itemText = _string(R.string.canvas_rasterize)
                    itemNewHawkKeyStr = "rasterize"
                    itemDefaultNew = LaserPeckerConfigHelper.haveNew(itemNewHawkKeyStr)
                    itemClick = {
                        itemHaveNew = false
                        LPElementHelper.rasterizeRenderer(renderer, itemRenderDelegate)
                        UMEvent.CANVAS_RASTERIZE.umengEventValue()
                    }
                }
            }

            vmApp<LaserPeckerModel>().productInfoData.value?.previewBounds?.let { bounds ->
                //设备居中
                AlignDeviceItem()() {
                    initItem(renderer)
                    itemDeviceBounds = bounds
                }
            }

            //图层排序
            LayerSortMenuItem()() {
                initItem(renderer)
            }
        }

        //坐标编辑
        EditControlItem()() {
            initItem(renderer)
        }
    }

    //endregion ---公共的编辑---

}