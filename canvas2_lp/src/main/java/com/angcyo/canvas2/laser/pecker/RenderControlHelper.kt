package com.angcyo.canvas2.laser.pecker

import android.graphics.Color
import android.graphics.Paint
import android.view.ViewGroup
import android.widget.LinearLayout
import com.angcyo.bluetooth.fsc.laserpacker.HawkEngraveKeys
import com.angcyo.bluetooth.fsc.laserpacker.LaserPeckerConfigHelper
import com.angcyo.bluetooth.fsc.laserpacker.LaserPeckerModel
import com.angcyo.canvas.render.core.Reason
import com.angcyo.canvas.render.core.Strategy
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
import com.angcyo.canvas2.laser.pecker.dslitem.control.EditControlItem
import com.angcyo.canvas2.laser.pecker.dslitem.control.ImageFilterItem
import com.angcyo.canvas2.laser.pecker.dslitem.control.LayerSortMenuItem
import com.angcyo.canvas2.laser.pecker.dslitem.control.PathStyleItem
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
import com.angcyo.canvas2.laser.pecker.util.LPBitmapHandler
import com.angcyo.canvas2.laser.pecker.util.LPElementHelper
import com.angcyo.canvas2.laser.pecker.util.lpElementBean
import com.angcyo.core.vmApp
import com.angcyo.dsladapter.DslAdapter
import com.angcyo.dsladapter.DslAdapterItem
import com.angcyo.dsladapter.eachItem
import com.angcyo.dsladapter.updateAllItemBy
import com.angcyo.dsladapter.updateItemSelected
import com.angcyo.item.style.itemNewHawkKeyStr
import com.angcyo.laserpacker.LPDataConstant
import com.angcyo.library.annotation.CallPoint
import com.angcyo.library.ex._color
import com.angcyo.library.ex._string
import com.angcyo.library.ex.alphaRatio
import com.angcyo.library.ex.have
import com.angcyo.library.ex.isDebug
import com.angcyo.library.ex.isDebugType
import com.angcyo.library.ex.size
import com.angcyo.library.utils.BuildHelper.isCpu64
import com.angcyo.transition.dslTransition
import com.angcyo.widget.recycler.renderDslAdapter
import com.hingin.umeng.UMEvent
import com.hingin.umeng.umengEventValue

/**
 * 选中/取消选中元素后控制布局助手
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2023/03/08
 */
class RenderControlHelper(override val renderLayoutHelper: RenderLayoutHelper) : IControlHelper {

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
            renderControlItems(selectorRenderer)
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
    fun renderControlItems(renderer: BaseRenderer? = selectorManager?.getTargetSelectorRenderer()) {
        //控制item渲染
        _rootViewHolder?.canvasControlRv?.renderDslAdapter {
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

    /**渲染图片编辑控制items*/
    private fun DslAdapter.renderBitmapEditItems(renderer: BaseRenderer) {
        val closeImageEditItemsFun =
            LaserPeckerConfigHelper.readDeviceSettingConfig()?.closeImageEditItemsFun

        //隐藏某些功能, 在32位的设备上
        val hideIn32 = HawkEngraveKeys.checkCpu32 && !isCpu64

        if (!closeImageEditItemsFun.have("_bw_")) {
            ImageFilterItem()() {
                itemIco = R.drawable.canvas_bitmap_black_white
                itemText = _string(R.string.canvas_black_white)
                itemRenderer = renderer
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
        if (!hideIn32 && !closeImageEditItemsFun.have("_dithering_")) {
            ImageFilterItem()() {
                itemIco = R.drawable.canvas_bitmap_dithering
                itemText = _string(R.string.canvas_dithering)
                itemRenderer = renderer
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
                itemRenderer = renderer
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

        if (hideIn32 || isDebugType()) {
            if (!closeImageEditItemsFun.have("_grey_")) {
                ImageFilterItem()() {
                    itemIco = R.drawable.canvas_bitmap_grey
                    itemText = _string(R.string.canvas_grey)
                    itemRenderer = renderer
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
        if (!hideIn32 && !closeImageEditItemsFun.have("_print_")) {
            ImageFilterItem()() {
                itemIco = R.drawable.canvas_bitmap_prints
                itemText = _string(R.string.canvas_prints)
                itemRenderer = renderer
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
                itemRenderer = renderer
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
                itemRenderer = renderer
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
        if (!closeImageEditItemsFun.have("_crop_")) {
            //剪裁用的是原图
            ImageFilterItem()() {
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

    /**渲染文本编辑控制items*/
    private fun DslAdapter.renderTextEditItems(renderer: BaseRenderer) {
        val closeTextEditItemsFun =
            LaserPeckerConfigHelper.readDeviceSettingConfig()?.closeTextEditItemsFun
        val isText = renderer.lpElementBean()?.mtype == LPDataConstant.DATA_TYPE_TEXT
        if (!isText) {
            //非文本类型, 不显示文本相关属性
            return
        }
        //字体
        if (!closeTextEditItemsFun.have("_typeface_")) {
            TextTypefaceSelectItem()() {
                initItem(renderer)
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
        //栅格化
        if (HawkEngraveKeys.enableRasterize) {
            CanvasIconItem()() {
                initItem(renderer)
                itemIco = R.drawable.canvas_text_rasterize_ico
                itemText = _string(R.string.canvas_rasterize)
                itemNewHawkKeyStr = "rasterize"
                itemClick = {
                    LPElementHelper.rasterizeRenderer(renderer, itemRenderDelegate)
                }
            }
        }
        //曲线
        if (isDebugType()) {
            CanvasIconItem()() {
                initItem(renderer)
                itemIco = R.drawable.canvas_text_curve_ico
                itemText = _string(R.string.canvas_curve)
                itemNewHawkKeyStr = "curve"
            }
        }
        if (!closeTextEditItemsFun.have("_orientation_")) {
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
                itemClick = {
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
        if (HawkEngraveKeys.enableRasterize) {
            afterItemCount++
        }
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
        //栅格化
        if (HawkEngraveKeys.enableRasterize) {
            CanvasIconItem()() {
                initItem(renderer)
                itemIco = R.drawable.canvas_text_rasterize_ico
                itemText = _string(R.string.canvas_rasterize)
                itemNewHawkKeyStr = "rasterize"
                if (afterItemCount <= 0) {
                    drawCanvasRight()
                }
                itemClick = {
                    LPElementHelper.rasterizeRenderer(renderer, renderDelegate)
                }
                afterItemCount--
            }
        }
    }

    //endregion ---形状/Path---

    //region ---Group---

    /**群组, 多选*/
    fun DslAdapter.renderGroupEditItems(renderer: CanvasGroupRenderer) {

        //群组栅格化
        if (HawkEngraveKeys.enableRasterize) {
            CanvasIconItem()() {
                initItem(renderer)
                itemIco = R.drawable.canvas_text_rasterize_ico
                itemText = _string(R.string.canvas_rasterize)
                itemNewHawkKeyStr = "rasterize"
                drawCanvasRight()
                itemClick = {
                    LPElementHelper.rasterizeRenderer(renderer, itemRenderDelegate)
                }
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
            itemEnable = renderer.isSelectorGroupRenderer() &&
                    renderer.rendererList.size() >= 3//3个以上的元素才支持分布
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