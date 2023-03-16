package com.angcyo.canvas2.laser.pecker

import android.view.ViewGroup
import android.widget.LinearLayout
import com.angcyo.bluetooth.fsc.laserpacker.LaserPeckerModel
import com.angcyo.canvas.render.core.CanvasRenderDelegate
import com.angcyo.canvas.render.core.CanvasSelectorManager
import com.angcyo.canvas.render.core.component.CanvasSelectorComponent
import com.angcyo.canvas.render.renderer.BaseRenderer
import com.angcyo.canvas.render.renderer.CanvasGroupRenderer
import com.angcyo.canvas.render.util.renderElement
import com.angcyo.canvas.render.util.textElement
import com.angcyo.canvas2.laser.pecker.dialog.canvasFontWindow
import com.angcyo.canvas2.laser.pecker.dslitem.CanvasIconItem
import com.angcyo.canvas2.laser.pecker.dslitem.ICanvasRendererItem
import com.angcyo.canvas2.laser.pecker.dslitem.control.*
import com.angcyo.canvas2.laser.pecker.dslitem.item.ControlEditItem
import com.angcyo.canvas2.laser.pecker.element.ILaserPeckerElement
import com.angcyo.canvas2.laser.pecker.util.LPBitmapHandler
import com.angcyo.canvas2.laser.pecker.util.LPConstant
import com.angcyo.core.vmApp
import com.angcyo.dsladapter.*
import com.angcyo.engrave.data.HawkEngraveKeys
import com.angcyo.fragment.AbsLifecycleFragment
import com.angcyo.library.annotation.CallPoint
import com.angcyo.library.ex._string
import com.angcyo.library.ex.have
import com.angcyo.library.ex.isDebugType
import com.angcyo.transition.dslTransition
import com.angcyo.widget.recycler.renderDslAdapter
import com.hingin.umeng.UMEvent
import com.hingin.umeng.umengEventValue

/**
 * 选中/取消选中元素后控制布局助手
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2023/03/08
 */
class RenderControlHelper(val renderLayoutHelper: RenderLayoutHelper) {

    val canvasRenderDelegate: CanvasRenderDelegate?
        get() = renderLayoutHelper._rootViewHolder?.renderDelegate

    val selectorManager: CanvasSelectorManager?
        get() = canvasRenderDelegate?.selectorManager

    /**编辑item*/
    val editItem: DslAdapterItem?
        get() = renderLayoutHelper.findTagItem(ControlEditItem.TAG_EDIT_ITEM)

    /**是否可以编辑*/
    val isEditEnable: Boolean
        get() = editItem == null || editItem?.itemEnable == true

    val fragment: AbsLifecycleFragment
        get() = renderLayoutHelper.canvasFragment.fragment

    //region ---基础---

    /**调用入口, 选中元素改变后, 重新渲染对应的编辑属性item*/
    @CallPoint
    fun bindControlLayout() {
        val selectorRenderer = selectorManager?.getTargetSelectorRenderer()
        if (selectorRenderer == null) {
            hideControlLayout()
        } else {
            showControlLayout()
            renderControlItemLayout(selectorRenderer)
        }
    }

    /**控制布局的可见性控制
     * [visible] 是否要可见*/
    @CallPoint
    fun visibleControlLayout(visible: Boolean) {
        if (visible) {
            val selectorRenderer = selectorManager?.getTargetSelectorRenderer()
            showControlLayout()
            renderControlItemLayout(selectorRenderer)
        } else {
            hideControlLayout()
        }
    }

    /**当有元素属性更新时, 触发更新显示*/
    @CallPoint
    fun updateControlLayout() {
        val vh = renderLayoutHelper._rootViewHolder ?: return
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
        itemRenderDelegate = canvasRenderDelegate
    }

    //endregion ---基础---

    //region ---core---

    /**显示控制布局*/
    private fun showControlLayout() {
        if (isEditEnable) {
            editItem?.updateItemSelected(true)
            renderLayoutHelper._rootViewHolder?.apply {
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

    /**隐藏控制布局*/
    private fun hideControlLayout() {
        if (isEditEnable) {
            editItem?.updateItemSelected(false)
            renderLayoutHelper._rootViewHolder?.apply {
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
    private fun renderControlItemLayout(renderer: BaseRenderer?) {
        //控制item渲染
        renderLayoutHelper._rootViewHolder?.canvasControlRv?.renderDslAdapter {
            renderLayoutHelper.hookUpdateDepend(this)

            //渲染不同的控制item
            when (renderer) {
                //多选
                is CanvasSelectorComponent -> Unit
                //群组
                is CanvasGroupRenderer -> Unit
                //单元素
                else -> {
                    val element = renderer?.renderElement
                    if (element is ILaserPeckerElement) {
                        when (element.elementBean.mtype) {
                            LPConstant.DATA_TYPE_BITMAP -> renderBitmapEditItems(renderer)
                            LPConstant.DATA_TYPE_TEXT -> renderTextEditItems(renderer)
                            LPConstant.DATA_TYPE_LINE,
                            LPConstant.DATA_TYPE_OVAL,
                            LPConstant.DATA_TYPE_RECT,
                            LPConstant.DATA_TYPE_POLYGON,
                            LPConstant.DATA_TYPE_PENTAGRAM,
                            LPConstant.DATA_TYPE_SVG,
                            LPConstant.DATA_TYPE_GCODE,
                            LPConstant.DATA_TYPE_LOVE -> Unit//renderShapeEditItems(renderer)
                        }
                    }
                }
            }

            //公共编辑
            renderCommonEditItems(renderer)
        }
    }

    //endregion ---core---

    //region ---Bitmap---

    /**渲染图片编辑控制items*/
    private fun DslAdapter.renderBitmapEditItems(renderer: BaseRenderer) {
        val closeImageEditItemsFun = HawkEngraveKeys.closeImageEditItemsFun
        if (!closeImageEditItemsFun.have("_bw_")) {
            ImageFilterItem()() {
                itemIco = R.drawable.canvas_bitmap_black_white
                itemText = _string(R.string.canvas_black_white)
                itemRenderer = renderer
                itemImageFilter = LPConstant.DATA_MODE_BLACK_WHITE
                itemClick = {
                    updateItemSelected(!itemIsSelected)
                    if (itemIsSelected) {
                        LPBitmapHandler.handleBlackWhite(
                            canvasRenderDelegate,
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
                itemRenderer = renderer
                itemImageFilter = LPConstant.DATA_MODE_DITHERING
                itemClick = {
                    updateItemSelected(!itemIsSelected)
                    if (itemIsSelected) {
                        LPBitmapHandler.handleDithering(
                            canvasRenderDelegate,
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
        if (!closeImageEditItemsFun.have("_gcode_")) {
            ImageFilterItem()() {
                itemIco = R.drawable.canvas_bitmap_gcode
                itemText = _string(R.string.canvas_gcode)
                itemRenderer = renderer
                itemImageFilter = LPConstant.DATA_MODE_GCODE
                itemClick = {
                    updateItemSelected(!itemIsSelected)
                    if (itemIsSelected) {
                        LPBitmapHandler.handleGCode(canvasRenderDelegate, it, fragment, renderer) {
                            itemIsSelected = false
                            updateAllItemBy { it is ImageFilterItem }
                        }
                        UMEvent.CANVAS_IMAGE_GCODE.umengEventValue()
                    }
                }
            }
        }

        if (isDebugType()) {
            if (!closeImageEditItemsFun.have("_grey_")) {
                ImageFilterItem()() {
                    itemIco = R.drawable.canvas_bitmap_grey
                    itemText = _string(R.string.canvas_grey)
                    itemRenderer = renderer
                    itemImageFilter = LPConstant.DATA_MODE_GREY
                    itemClick = {
                        LPBitmapHandler.handleGrey(canvasRenderDelegate, it, fragment, renderer) {
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
                itemRenderer = renderer
                itemImageFilter = LPConstant.DATA_MODE_PRINT
                itemClick = {
                    updateItemSelected(!itemIsSelected)
                    if (itemIsSelected) {
                        LPBitmapHandler.handlePrint(canvasRenderDelegate, it, fragment, renderer) {
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
                itemImageFilter = LPConstant.DATA_MODE_SEAL
                itemClick = {
                    updateItemSelected(!itemIsSelected)
                    if (itemIsSelected) {
                        LPBitmapHandler.handleSeal(canvasRenderDelegate, it, fragment, renderer) {
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
                        LPBitmapHandler.handleMesh(canvasRenderDelegate, it, fragment, renderer) {
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
                        LPBitmapHandler.handleCrop(canvasRenderDelegate, it, fragment, renderer) {
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
        val closeTextEditItemsFun = HawkEngraveKeys.closeTextEditItemsFun
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
            TextStyleSelectItem()() {
                initItem(renderer)
            }
        }
        //对齐
        if (!closeTextEditItemsFun.have("_align_")) {
            TextAlignSelectItem()() {
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
                itemIco = R.drawable.canvas_text_rasterize_ico
                itemText = _string(R.string.canvas_rasterize)
                itemClick = {
                    //renderer.dataItem?.itemRasterize(renderer)
                }
            }
        }
        //曲线
        if (isDebugType()) {
            CanvasIconItem()() {
                itemIco = R.drawable.canvas_text_curve_ico
                itemText = _string(R.string.canvas_curve)
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
                itemText = "紧凑"
                itemIco = R.drawable.canvas_text_style
                itemIsSelected = renderer.textElement?.textProperty?.isCompactText == true
                itemClick = {
                    updateItemSelected(!itemIsSelected)

                    renderer.textElement?.updateTextProperty(renderer, itemRenderDelegate) {
                        isCompactText = itemIsSelected
                    }
                }
            }
        }
    }

    //endregion ---文本---

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
            LayerSortItem()() {
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