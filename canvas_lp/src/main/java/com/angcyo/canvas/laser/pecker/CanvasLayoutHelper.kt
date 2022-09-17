package com.angcyo.canvas.laser.pecker

import android.graphics.Paint
import android.graphics.RectF
import android.graphics.drawable.BitmapDrawable
import android.view.Gravity
import android.view.MotionEvent
import android.view.ViewGroup
import android.widget.LinearLayout
import com.angcyo.bluetooth.fsc.laserpacker.LaserPeckerModel
import com.angcyo.bluetooth.fsc.laserpacker.parse.QueryStateParser
import com.angcyo.canvas.CanvasDelegate
import com.angcyo.canvas.CanvasView
import com.angcyo.canvas.Reason
import com.angcyo.canvas.Strategy
import com.angcyo.canvas.core.CanvasUndoManager
import com.angcyo.canvas.core.ICanvasListener
import com.angcyo.canvas.core.IRenderer
import com.angcyo.canvas.core.renderer.SelectGroupRenderer
import com.angcyo.canvas.items.*
import com.angcyo.canvas.items.renderer.*
import com.angcyo.canvas.laser.pecker.dslitem.*
import com.angcyo.canvas.utils.*
import com.angcyo.core.vmApp
import com.angcyo.dialog.popup.MenuPopupConfig
import com.angcyo.dialog.popup.menuPopupWindow
import com.angcyo.doodle.ui.doodleDialog
import com.angcyo.dsladapter.*
import com.angcyo.dsladapter.item.IFragmentItem
import com.angcyo.engrave.EngraveHelper
import com.angcyo.fragment.AbsFragment
import com.angcyo.gcode.GCodeDrawable
import com.angcyo.library.ex.*
import com.angcyo.transition.dslTransition
import com.angcyo.widget.DslViewHolder
import com.angcyo.widget.base.resetDslItem
import com.angcyo.widget.recycler.renderDslAdapter
import com.hingin.umeng.UMEvent
import com.hingin.umeng.umengEventValue
import com.pixplicity.sharp.Sharp
import com.pixplicity.sharp.SharpDrawable

/**
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2022/05/09
 */
class CanvasLayoutHelper(val fragment: AbsFragment) {

    /**当前选中的[DslAdapterItem], 用来实现底部控制按钮互斥操作*/
    var _selectedCanvasItem: DslAdapterItem? = null

    var _canvasView: CanvasView? = null

    var _undoCanvasItem: CanvasControlItem2 = CanvasControlItem2().apply {
        itemIco = R.drawable.canvas_undo_ico
        itemText = _string(R.string.canvas_undo)
        itemEnable = false

        _undoCanvasItem = this
        itemClick = {
            _canvasView?.canvasDelegate?.getCanvasUndoManager()?.undo()
        }
    }
    var _redoCanvasItem: CanvasControlItem2 = CanvasControlItem2().apply {
        itemIco = R.drawable.canvas_redo_ico
        itemText = _string(R.string.canvas_redo)
        itemEnable = false

        _redoCanvasItem = this
        itemClick = {
            _canvasView?.canvasDelegate?.getCanvasUndoManager()?.redo()
        }
    }

    /**图层item*/
    var _layerCanvasItem: DslAdapterItem? = null

    /**监听, 并赋值[IFragmentItem]*/
    fun DslAdapter.hookUpdateDepend() {
        observeItemUpdateDepend {
            adapterItems.forEach {
                if (it is IFragmentItem) {
                    it.itemFragment = fragment
                }
            }
        }
    }

    /**取消正在选中的项状态*/
    fun cancelSelectedItem() {
        if (_selectedCanvasItem?.itemIsSelected == true) {
            _selectedCanvasItem?.itemIsSelected = false
            _selectedCanvasItem?.updateAdapterItem()
        }
        _selectedCanvasItem = null
    }

    /**选中一个新的Item, 并取消之前选中的*/
    fun selectedItemWith(item: DslAdapterItem) {
        if (_selectedCanvasItem == item) {
            return
        }
        cancelSelectedItem()
        _selectedCanvasItem = item
    }

    /**绑定画图支持的功能列表*/
    fun bindItems(vh: DslViewHolder, canvasView: CanvasView, adapter: DslAdapter) {
        _canvasView = canvasView
        adapter.render {
            hookUpdateDepend()
            //
            AddImageItem()() {
                itemCanvasDelegate = canvasView.canvasDelegate
            }
            AddTextItem()() {
                itemCanvasDelegate = canvasView.canvasDelegate
            }
            CanvasControlItem2()() {
                itemIco = R.drawable.canvas_material_ico
                itemText = _string(R.string.canvas_material)
                itemEnable = true

                itemClick = {
                    fragment.context.canvasMaterialWindow(it) {
                        onDrawableAction = { data, drawable ->
                            when (drawable) {
                                is BitmapDrawable -> {
                                    //bitmap
                                    canvasView.canvasDelegate.addPictureBitmapRenderer(drawable.bitmap)
                                }
                                is GCodeDrawable -> {
                                    //gcode
                                    canvasView.canvasDelegate.addPictureGCodeRenderer(
                                        data as String,
                                        drawable
                                    )
                                }
                                is SharpDrawable -> {
                                    //svg
                                    canvasView.canvasDelegate.addPictureSharpRenderer(
                                        data as String,
                                        drawable
                                    )
                                }
                                else -> {
                                    //other
                                    canvasView.canvasDelegate.addPictureDrawableRenderer(drawable)
                                }
                            }
                            UMEvent.CANVAS_MATERIAL.umengEventValue()
                        }
                    }
                }
            }
            CanvasControlItem2()() {
                itemIco = R.drawable.canvas_shapes_ico
                itemText = _string(R.string.canvas_shapes)
                itemClick = {
                    vh.showControlLayout(canvasView, !itemIsSelected)
                    itemIsSelected = !itemIsSelected
                    updateAdapterItem()

                    if (itemIsSelected) {
                        selectedItemWith(this)
                        showShapeSelectLayout(vh, canvasView)
                        UMEvent.CANVAS_SHAPE.umengEventValue()
                    }
                }
            }
            CanvasControlItem2()() {
                itemIco = R.drawable.canvas_doodle_ico
                itemText = _string(R.string.canvas_doodle)
                itemEnable = true
                itemClick = {
                    UMEvent.CANVAS_DOODLE.umengEventValue()
                    fragment.context?.doodleDialog {
                        onDoodleResultAction = {
                            canvasView.canvasDelegate.addPictureBitmapRenderer(it)
                        }
                    }
                }
                drawCanvasRight()
            }
            //
            CanvasControlItem2()() {
                itemIco = R.drawable.canvas_edit_ico
                itemText = _string(R.string.canvas_edit)
                itemEnable = true
                itemClick = {
                    vh.showControlLayout(canvasView, !itemIsSelected)
                    itemIsSelected = !itemIsSelected
                    updateAdapterItem()

                    if (itemIsSelected) {
                        selectedItemWith(this)
                        showEditControlLayout(
                            vh,
                            canvasView,
                            canvasView.canvasDelegate.getSelectedRenderer()
                        )
                    }
                }
            }
            CanvasControlItem2()() {
                _layerCanvasItem = this
                itemIco = R.drawable.canvas_layer_ico
                itemText = _string(R.string.canvas_layer)
                itemEnable = true
                itemClick = {
                    vh.gone(R.id.canvas_layer_layout, itemIsSelected)
                    itemIsSelected = !itemIsSelected
                    updateAdapterItem()

                    if (itemIsSelected) {
                        updateLayerControlLayout(vh, canvasView)
                    }
                }
            }
            CanvasControlItem2()() {
                itemIco = R.drawable.canvas_actions_ico
                itemText = _string(R.string.canvas_operate)
                itemEnable = true
                itemClick = {
                    canvasView.canvasDelegate.getSelectedRenderer()?.let { renderer ->
                        if (renderer is PictureItemRenderer) {
                            renderer.getRendererRenderItem()?.let { item ->
                                if (item is PictureShapeItem) {
                                    fragment.loadingAsync({
                                        item.shapePath.let { path ->
                                            CanvasDataHandleOperate.pathToGCode(
                                                path,
                                                renderer.getRotateBounds(),
                                                renderer.rotate
                                            )
                                        }
                                    }) {
                                        //no op
                                    }
                                }
                            }
                        }
                    }
                }
            }
            CanvasControlItem2()() {
                itemIco = R.drawable.canvas_setting_ico
                itemText = _string(R.string.canvas_setting)
                itemEnable = true

                itemClick = {
                    itemIsSelected = !itemIsSelected
                    updateAdapterItem()

                    if (itemIsSelected) {
                        it.context.canvasSettingWindow(it) {
                            canvasDelegate = canvasView.canvasDelegate
                            onDismiss = {
                                itemIsSelected = false
                                updateAdapterItem()
                                false
                            }
                        }
                    }
                }
            }
        }

        //
        _updateUndoLayout(vh)

        //
        initCanvasListener(vh, canvasView)
    }

    /**事件监听*/
    fun initCanvasListener(vh: DslViewHolder, canvasView: CanvasView) {
        //事件监听
        canvasView.canvasDelegate.canvasListenerList.add(object : ICanvasListener {

            override fun onDoubleTapItem(itemRenderer: IItemRenderer<*>) {
                super.onDoubleTapItem(itemRenderer)
                if (itemRenderer is PictureTextItemRenderer) {
                    val renderItem = itemRenderer.rendererItem
                    /*if (renderItem is PictureTextItem) {
                        fragment.context?.inputDialog {
                            inputViewHeight = 100 * dpi
                            defaultInputString = renderItem.text
                            maxInputLength = AddTextItem.MAX_INPUT_LENGTH
                            inputHistoryHawkKey = AddTextItem.KEY_ADD_TEXT
                            onInputResult = { dialog, inputText ->
                                if (inputText.isNotEmpty()) {
                                    itemRenderer.updateItemText("$inputText")
                                }
                                false
                            }
                        }
                    }*/
                } else if (itemRenderer is PictureItemRenderer) {
                    val renderItem = itemRenderer.rendererItem
                    if (renderItem is PictureBitmapItem) {
                        val renderer = itemRenderer as PictureItemRenderer<PictureBitmapItem>
                        when (renderItem.dataType) {
                            CanvasConstant.DATA_TYPE_BARCODE -> {
                                //条形码
                                AddTextItem.inputBarcode(canvasView, renderer)
                            }
                            CanvasConstant.DATA_TYPE_QRCODE -> {
                                //二维码
                                AddTextItem.inputQrCode(canvasView, renderer)
                            }
                            else -> {
                                //图片
                            }
                        }
                    }
                }
            }

            override fun onItemRendererAdd(itemRenderer: IItemRenderer<*>) {
                super.onItemRendererAdd(itemRenderer)
                if (itemRenderer is BaseItemRenderer<*>) {
                    addLayerItem(vh, canvasView, itemRenderer)
                }
            }

            override fun onItemRendererRemove(itemRenderer: IItemRenderer<*>) {
                super.onItemRendererRemove(itemRenderer)
                if (itemRenderer is BaseItemRenderer<*>) {
                    removeLayerItem(vh, canvasView, itemRenderer)
                }
            }

            override fun onItemVisibleChanged(itemRenderer: IRenderer, visible: Boolean) {
                super.onItemVisibleChanged(itemRenderer, visible)
                updateLayerLayout(vh)
            }

            override fun onItemRenderUpdate(itemRenderer: IRenderer) {
                super.onItemRenderUpdate(itemRenderer)
                updateLayerLayout(vh)
            }

            override fun onItemBoundsChanged(
                itemRenderer: IRenderer,
                reason: Reason,
                oldBounds: RectF
            ) {
                super.onItemBoundsChanged(itemRenderer, reason, oldBounds)
                updateControlLayout(vh, canvasView)
                updateLayerLayout(vh)

                val peckerModel = vmApp<LaserPeckerModel>()
                if (peckerModel.deviceModelData.value == QueryStateParser.WORK_MODE_ENGRAVE_PREVIEW &&
                    itemRenderer == canvasView.canvasDelegate.getSelectedRenderer()
                ) {
                    //设备正在预览模式, 更新预览
                    if (itemRenderer is BaseItemRenderer<*>) {
                        EngraveHelper.sendPreviewRange(itemRenderer, false, true)
                    }
                }
            }

            override fun onItemLockScaleRatioChanged(item: BaseItemRenderer<*>) {
                super.onItemLockScaleRatioChanged(item)
                updateControlLayout(vh, canvasView)
            }

            override fun onItemSortChanged(itemList: List<BaseItemRenderer<*>>) {
                super.onItemSortChanged(itemList)
                //updateControlLayout(vh, canvasView)
                updateLayerControlLayout(vh, canvasView)
            }

            override fun onClearSelectItem(itemRenderer: IItemRenderer<*>) {
                super.onClearSelectItem(itemRenderer)
                cancelSelectedItem()
                vh.showControlLayout(canvasView, false)
                updateLayerLayout(vh)
            }

            override fun onSelectedItem(
                itemRenderer: IItemRenderer<*>,
                oldItemRenderer: IItemRenderer<*>?
            ) {
                super.onSelectedItem(itemRenderer, oldItemRenderer)
                if (itemRenderer == oldItemRenderer) {
                    //重复选择
                    return
                }
                cancelSelectedItem()

                //显示控制布局
                vh.showControlLayout(canvasView)

                //更新图层
                updateLayerLayout(vh)

                //显示对应的控制Item布局, 没有则隐藏布局
                vh.showControlLayout(canvasView, false)

                //预览选中的元素边框
                val peckerModel = vmApp<LaserPeckerModel>()
                if (peckerModel.deviceModelData.value == QueryStateParser.WORK_MODE_ENGRAVE_PREVIEW) {
                    //设备正在预览模式, 更新预览
                    if (itemRenderer is BaseItemRenderer<*>) {
                        EngraveHelper.sendPreviewRange(itemRenderer, false, true)
                    }
                }
            }

            override fun onCanvasUndoChanged(undoManager: CanvasUndoManager) {
                super.onCanvasUndoChanged(undoManager)
                _undoCanvasItem.apply {
                    itemEnable = undoManager.canUndo()
                    if (isShowDebug()) {
                        itemTextSuperscript = "${undoManager.undoStack.size()}"
                    }
                }
                _redoCanvasItem.apply {
                    itemEnable = undoManager.canRedo()
                    if (isShowDebug()) {
                        itemTextSuperscript = "${undoManager.redoStack.size()}"
                    }
                }
                _updateUndoLayout()
            }

            override fun onCanvasInterceptTouchEvent(
                canvasDelegate: CanvasDelegate,
                event: MotionEvent
            ): Boolean {
                if (event.actionMasked == MotionEvent.ACTION_DOWN) {
                    //点击画布, 隐藏图层布局
                    vh.gone(R.id.canvas_layer_layout, true)
                    _layerCanvasItem?.apply {
                        itemIsSelected = false
                        updateAdapterItem()
                    }
                }
                return super.onCanvasInterceptTouchEvent(canvasDelegate, event)
            }
        })
    }

    /**隐藏控制布局*/
    fun DslViewHolder.showControlLayout(canvasView: CanvasView, visible: Boolean = true) {
        if (!visible) {
            //如果要隐藏控制布局时, 判断是否已经选中了item
            val itemRenderer = canvasView.canvasDelegate.getSelectedRenderer()
            if (itemRenderer != null && showSelectedItemControlLayout(
                    this,
                    canvasView,
                    itemRenderer
                )
            ) {
                //被处理
                return
            }
        }

        dslTransition(itemView as ViewGroup) {
            onCaptureStartValues = {
                //invisible(R.id.canvas_control_layout)
            }
            onCaptureEndValues = {
                visible(R.id.canvas_control_layout, visible)
            }
        }
    }

    /**根据选中的item, 显示对应的控制布局
     * @return 表示是否处理了*/
    fun showSelectedItemControlLayout(
        vh: DslViewHolder,
        canvasView: CanvasView,
        itemRenderer: IItemRenderer<*>
    ): Boolean {
        var result = true
        if (itemRenderer is PictureTextItemRenderer) {
            //选中TextItemRenderer时的控制菜单
            renderTextControlLayout(vh, canvasView, itemRenderer)
        } else if (itemRenderer is PictureItemRenderer) {
            val renderItem = itemRenderer.rendererItem
            if (renderItem is PictureShapeItem || renderItem is PictureSharpItem) {
                //shape or sharp
                renderShapeControlLayout(vh, canvasView, itemRenderer)
            } else if (renderItem is PictureBitmapItem) {
                renderBitmapControlLayout(vh, canvasView, itemRenderer)
            } else {
                //vh.showControlLayout(false)
                result = false
            }
        } else if (itemRenderer is SelectGroupRenderer) {
            renderGroupControlLayout(vh, canvasView, itemRenderer)
        } else if (itemRenderer is DrawableItemRenderer) {
            val itemDrawable = (itemRenderer.getRendererRenderItem() as? DrawableItem)?.drawable
            if (itemDrawable is SharpDrawable) {
                renderSVGControlLayout(vh, canvasView, itemRenderer)
            } else {
                result = false
            }
        } else {
            //vh.showControlLayout(false)
            result = false
        }
        return result
    }

    /**显示形状选择布局*/
    fun showShapeSelectLayout(vh: DslViewHolder, canvasView: CanvasView) {
        vh.rv(R.id.canvas_control_view)?.renderDslAdapter {
            hookUpdateDepend()
            ShapeItem(canvasView)() {
                itemIco = R.drawable.canvas_shape_line_ico
                itemText = _string(R.string.canvas_line)
                shapePath = ShapesHelper.linePath()
            }
            ShapeItem(canvasView)() {
                itemIco = R.drawable.canvas_shape_circle_ico
                itemText = _string(R.string.canvas_circle)
                shapePath = ShapesHelper.circlePath()
            }
            ShapeItem(canvasView)() {
                itemIco = R.drawable.canvas_shape_triangle_ico
                itemText = _string(R.string.canvas_triangle)
                shapePath = ShapesHelper.trianglePath()
            }
            ShapeItem(canvasView)() {
                itemIco = R.drawable.canvas_shape_square_ico
                itemText = _string(R.string.canvas_square)
                shapePath = ShapesHelper.squarePath()
            }
            ShapeItem(canvasView)() {
                itemIco = R.drawable.canvas_shape_pentagon_ico
                itemText = _string(R.string.canvas_pentagon)
                shapePath = ShapesHelper.pentagonPath()
            }
            ShapeItem(canvasView)() {
                itemIco = R.drawable.canvas_shape_hexagon_ico
                itemText = _string(R.string.canvas_hexagon)
                shapePath = ShapesHelper.hexagonPath()
            }
            ShapeItem(canvasView)() {
                itemIco = R.drawable.canvas_shape_octagon_ico
                itemText = _string(R.string.canvas_octagon)
                shapePath = ShapesHelper.octagonPath()
            }
            ShapeItem(canvasView)() {
                itemIco = R.drawable.canvas_shape_rhombus_ico
                itemText = _string(R.string.canvas_rhombus)
                shapePath = ShapesHelper.rhombusPath()
            }
            ShapeItem(canvasView)() {
                itemIco = R.drawable.canvas_shape_pentagram_ico
                itemText = _string(R.string.canvas_pentagram)
                shapePath = ShapesHelper.pentagramPath()
            }
            ShapeItem(canvasView)() {
                itemIco = R.drawable.canvas_shape_love_ico
                itemText = _string(R.string.canvas_love)
                //shapePath = ShapesHelper.lovePath()
                shapePath =
                    Sharp.loadPath("M12 21.593c-5.63-5.539-11-10.297-11-14.402 0-3.791 3.068-5.191 5.281-5.191 1.312 0 4.151.501 5.719 4.457 1.59-3.968 4.464-4.447 5.726-4.447 2.54 0 5.274 1.621 5.274 5.181 0 4.069-5.136 8.625-11 14.402")
            }
        }
    }

    /**显示图片选择布局*/
    @Deprecated("2022-9-17 合并在Text对话框中")
    fun showBitmapSelectLayout(vh: DslViewHolder, canvasView: CanvasView) {
        vh.rv(R.id.canvas_control_view)?.renderDslAdapter {
            hookUpdateDepend()
            AddImageItem()() {
                itemCanvasDelegate = canvasView.canvasDelegate
            }
            CanvasControlItem()() {
                itemIco = R.drawable.canvas_barcode_ico
                itemText = _string(R.string.canvas_barcode)
                itemClick = {
                    AddTextItem.inputBarcode(canvasView, null)
                }
            }
            CanvasControlItem()() {
                itemIco = R.drawable.canvas_qrcode_ico
                itemText = _string(R.string.canvas_qrcode)
                itemClick = {
                    AddTextItem.inputQrCode(canvasView, null)
                }
            }
        }
    }

    //<editor-fold desc="文本属性控制">

    /**更新文本样式和其他控制布局*/
    fun updateControlLayout(vh: DslViewHolder, canvasView: CanvasView) {
        if (vh.isVisible(R.id.canvas_control_layout)) {
            vh.rv(R.id.canvas_control_view)?._dslAdapter?.apply {
                eachItem { index, dslAdapterItem ->
                    if (dslAdapterItem is CanvasEditControlItem) {
                        dslAdapterItem.itemRenderer =
                            canvasView.canvasDelegate.getSelectedRenderer()
                        dslAdapterItem.itemCanvasDelegate = canvasView.canvasDelegate
                    } else if (dslAdapterItem is CanvasArrangeItem) {
                        dslAdapterItem.itemRenderer =
                            canvasView.canvasDelegate.getSelectedRenderer()
                        dslAdapterItem.itemCanvasDelegate = canvasView.canvasDelegate
                    }
                }
                updateAllItem()
            }
        }
    }

    fun DslAdapterItem.drawCanvasRight(
        insertRight: Int = _dimen(R.dimen.lib_line_px),
        offsetTop: Int = _dimen(R.dimen.lib_xhdpi),
        offsetBottom: Int = _dimen(R.dimen.lib_xhdpi),
        color: Int = _color(R.color.canvas_dark_gray)
    ) {
        drawRight(insertRight, offsetTop, offsetBottom, color)
    }

    /**统一样式的item*/
    fun renderTextControlLayout(
        vh: DslViewHolder,
        canvasView: CanvasView,
        renderer: BaseItemRenderer<*>
    ) {
        vh.rv(R.id.canvas_control_view)?.renderDslAdapter {
            hookUpdateDepend()

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
            CanvasControlItem2()() {
                itemIco = R.drawable.canvas_text_style
                itemText = _string(R.string.canvas_style)
                itemClick = { anchor ->
                    updateItemSelected(!itemIsSelected)

                    if (itemIsSelected) {
                        UMEvent.CANVAS_SHAPE.umengEventValue()

                        anchor.context.menuPopupWindow(anchor) {
                            renderAdapterAction = {
                                //
                                TextStrokeStyleItem()() {
                                    itemIco = R.drawable.canvas_text_style_solid
                                    itemText = _string(R.string.canvas_solid)
                                    itemStyle = Paint.Style.FILL
                                    itemRenderer = renderer
                                }
                                TextStrokeStyleItem()() {
                                    itemIco = R.drawable.canvas_text_style_stroke
                                    itemText = _string(R.string.canvas_hollow)
                                    itemStyle = Paint.Style.STROKE
                                    itemRenderer = renderer
                                }
                                //
                                TextStyleItem()() {
                                    itemIco = R.drawable.canvas_text_bold_style_ico
                                    itemText = _string(R.string.canvas_bold)
                                    itemStyle = PictureTextItem.TEXT_STYLE_BOLD
                                    itemRenderer = renderer
                                }
                                TextStyleItem()() {
                                    itemIco = R.drawable.canvas_text_italic_style_ico
                                    itemText = _string(R.string.canvas_italic)
                                    itemStyle = PictureTextItem.TEXT_STYLE_ITALIC
                                    itemRenderer = renderer
                                }
                                TextStyleItem()() {
                                    itemIco = R.drawable.canvas_text_under_line_style_ico
                                    itemText = _string(R.string.canvas_under_line)
                                    itemStyle = PictureTextItem.TEXT_STYLE_UNDER_LINE
                                    itemRenderer = renderer
                                }
                                TextStyleItem()() {
                                    itemIco = R.drawable.canvas_text_delete_line_style_ico
                                    itemText = _string(R.string.canvas_delete_line)
                                    itemStyle = PictureTextItem.TEXT_STYLE_DELETE_LINE
                                    itemRenderer = renderer
                                }
                            }
                            onDismiss = {
                                updateItemSelected(false)
                                false
                            }
                        }
                    }
                }
            }
            //对齐
            CanvasControlItem2()() {
                itemIco = R.drawable.canvas_text_align
                itemText = _string(R.string.canvas_align)
                itemClick = { anchor ->
                    updateItemSelected(!itemIsSelected)

                    if (itemIsSelected) {
                        UMEvent.CANVAS_SHAPE.umengEventValue()
                        anchor.context.menuPopupWindow(anchor) {
                            renderAdapterAction = {
                                TextAlignItem()() {
                                    itemIco = R.drawable.canvas_text_style_align_left_ico
                                    itemText = _string(R.string.canvas_align_left)
                                    itemAlign = Paint.Align.LEFT
                                    itemRenderer = renderer
                                }
                                TextAlignItem()() {
                                    itemIco = R.drawable.canvas_text_style_align_center_ico
                                    itemText = _string(R.string.canvas_align_center)
                                    itemAlign = Paint.Align.CENTER
                                    itemRenderer = renderer
                                }
                                TextAlignItem()() {
                                    itemIco = R.drawable.canvas_text_style_align_right_ico
                                    itemText = _string(R.string.canvas_align_right)
                                    itemAlign = Paint.Align.RIGHT
                                    itemRenderer = renderer
                                }
                            }
                            onDismiss = {
                                updateItemSelected(false)
                                false
                            }
                        }
                    }
                }
                drawCanvasRight()
            }

            TextOrientationItem()() {
                itemIco = R.drawable.canvas_text_style_standard_ico
                itemText = _string(R.string.canvas_standard)
                itemOrientation = LinearLayout.HORIZONTAL
                itemRenderer = renderer
            }
            TextOrientationItem()() {
                itemIco = R.drawable.canvas_text_style_vertical_ico
                itemText = _string(R.string.canvas_vertical)
                itemOrientation = LinearLayout.VERTICAL
                itemRenderer = renderer
                drawCanvasRight()
            }
        }
    }

    //</editor-fold desc="文本属性控制">

    //<editor-fold desc="形状控制">

    /**形状属性控制item*/
    fun renderShapeControlLayout(
        vh: DslViewHolder,
        canvasView: CanvasView,
        itemRenderer: IItemRenderer<*>
    ) {
        vh.rv(R.id.canvas_control_view)?.renderDslAdapter {
            hookUpdateDepend()
            CanvasControlItem()() {
                itemIco = R.drawable.canvas_style_stroke_ico
                itemText = _string(R.string.canvas_stroke)
                itemTintColor = false
                itemClick = {
                    if (itemRenderer is PictureItemRenderer) {
                        itemRenderer.updatePaintStyle(Paint.Style.STROKE)
                    }
                }
            }
            CanvasControlItem()() {
                itemIco = R.drawable.canvas_style_fill_ico
                itemText = _string(R.string.canvas_fill)
                itemTintColor = false
                itemClick = {
                    if (itemRenderer is PictureItemRenderer) {
                        itemRenderer.updatePaintStyle(Paint.Style.FILL)
                    }
                }
            }
            CanvasControlItem()() {
                itemIco = R.drawable.canvas_style_stroke_fill_ico
                itemText = _string(R.string.canvas_fill_stroke)
                itemTintColor = true
                itemClick = {
                    if (itemRenderer is PictureItemRenderer) {
                        itemRenderer.updatePaintStyle(Paint.Style.FILL_AND_STROKE)
                    }
                }
            }
        }
    }

    /**SVG属性控制item*/
    fun renderSVGControlLayout(
        vh: DslViewHolder,
        canvasView: CanvasView,
        itemRenderer: DrawableItemRenderer<*>
    ) {
        vh.rv(R.id.canvas_control_view)?.renderDslAdapter {
            hookUpdateDepend()
            CanvasControlItem()() {
                itemIco = R.drawable.canvas_style_stroke_ico
                itemText = _string(R.string.canvas_stroke)
                itemTintColor = false
                itemClick = {
                    itemRenderer.updatePaintStyle(Paint.Style.STROKE)
                }
            }
            CanvasControlItem()() {
                itemIco = R.drawable.canvas_style_fill_ico
                itemText = _string(R.string.canvas_fill)
                itemTintColor = false
                itemClick = {
                    itemRenderer.updatePaintStyle(Paint.Style.FILL)
                }
            }
            CanvasControlItem()() {
                itemIco = R.drawable.canvas_style_stroke_fill_ico
                itemText = _string(R.string.canvas_fill_stroke)
                itemTintColor = true
                itemClick = {
                    itemRenderer.updatePaintStyle(Paint.Style.FILL_AND_STROKE)
                }
            }
        }
    }

    //</editor-fold desc="形状控制">

    //<editor-fold desc="图片控制">

    /**图片属性控制item*/
    fun renderBitmapControlLayout(
        vh: DslViewHolder,
        canvasView: CanvasView,
        itemRenderer: IItemRenderer<*>
    ) {
        if (itemRenderer !is PictureItemRenderer<*>) {
            return
        }
        if (itemRenderer.getRendererRenderItem() !is PictureBitmapItem) {
            return
        }
        val renderer = itemRenderer as PictureItemRenderer<PictureBitmapItem>
        vh.rv(R.id.canvas_control_view)?.renderDslAdapter {
            hookUpdateDepend()
            CanvasControlItem2()() {
                itemIco = R.drawable.canvas_bitmap_prints
                itemText = _string(R.string.canvas_prints)
                itemClick = {
                    CanvasBitmapHandler.handlePrint(it, fragment, renderer)
                    UMEvent.CANVAS_IMAGE_PRINT.umengEventValue()
                }
            }
            CanvasControlItem2()() {
                itemIco = R.drawable.canvas_bitmap_gcode
                itemText = _string(R.string.canvas_gcode)
                itemClick = {
                    CanvasBitmapHandler.handleGCode(it, fragment, renderer)
                    UMEvent.CANVAS_IMAGE_GCODE.umengEventValue()
                }
            }
            CanvasControlItem2()() {
                itemIco = R.drawable.canvas_bitmap_black_white
                itemText = _string(R.string.canvas_black_white)
                itemClick = {
                    CanvasBitmapHandler.handleBlackWhite(it, fragment, renderer)
                    UMEvent.CANVAS_IMAGE_BW.umengEventValue()
                }
            }
            CanvasControlItem2()() {
                itemIco = R.drawable.canvas_bitmap_dithering
                itemText = _string(R.string.canvas_dithering)
                itemClick = {
                    CanvasBitmapHandler.handleDithering(it, fragment, renderer)
                    UMEvent.CANVAS_IMAGE_DITHERING.umengEventValue()
                }
            }
            CanvasControlItem2()() {
                itemIco = R.drawable.canvas_bitmap_grey
                itemText = _string(R.string.canvas_grey)
                itemClick = {
                    CanvasBitmapHandler.handleGrey(it, fragment, renderer)
                    UMEvent.CANVAS_IMAGE_GREY.umengEventValue()
                }
            }
            CanvasControlItem2()() {
                itemIco = R.drawable.canvas_bitmap_seal
                itemText = _string(R.string.canvas_seal)
                itemClick = {
                    CanvasBitmapHandler.handleSeal(it, fragment, renderer)
                    UMEvent.CANVAS_IMAGE_SEAL.umengEventValue()
                }
            }
            CanvasControlItem2()() {
                itemIco = R.drawable.canvas_bitmap_crop
                itemText = _string(R.string.canvas_crop)
                itemClick = {
                    CanvasBitmapHandler.handleCrop(it, fragment, renderer)
                    UMEvent.CANVAS_IMAGE_CROP.umengEventValue()
                }
            }
        }
    }

    //</editor-fold desc="图片控制">

    //<editor-fold desc="图层控制">

    /**更新图层布局*/
    fun updateLayerLayout(vh: DslViewHolder) {
        if (vh.isVisible(R.id.canvas_layer_layout)) {
            vh.rv(R.id.canvas_layer_view)?._dslAdapter?.updateAllItem()
        }
    }

    /**移除一个渲染图层*/
    fun removeLayerItem(vh: DslViewHolder, canvasView: CanvasView, item: IRenderer) {
        if (vh.isVisible(R.id.canvas_layer_layout)) {
            vh.rv(R.id.canvas_layer_view)?._dslAdapter?.apply {
                render {
                    eachItem { index, dslAdapterItem ->
                        if (dslAdapterItem is CanvasLayerItem && dslAdapterItem.itemRenderer == item) {
                            dslAdapterItem.removeAdapterItem()
                        }
                    }
                    if (canvasView.canvasDelegate.itemsRendererList.isEmpty()) {
                        setAdapterStatus(DslAdapterStatusItem.ADAPTER_STATUS_EMPTY)
                    }
                }
            }
        }
    }

    /**添加一个渲染图层*/
    fun addLayerItem(
        vh: DslViewHolder,
        canvasView: CanvasView,
        item: BaseItemRenderer<*>
    ) {
        if (vh.isVisible(R.id.canvas_layer_layout)) {
            vh.rv(R.id.canvas_layer_view)?._dslAdapter?.apply {
                render {
                    CanvasLayerItem()(0) {
                        itemCanvasDelegate = canvasView.canvasDelegate
                        itemRenderer = item
                    }
                    setAdapterStatus(DslAdapterStatusItem.ADAPTER_STATUS_NONE)
                }
            }
        }
    }

    var _layerDragHelper: DragCallbackHelper? = null

    /**显示图层item*/
    fun updateLayerControlLayout(vh: DslViewHolder, canvasView: CanvasView) {
        vh.rv(R.id.canvas_layer_view)?.apply {
            if (_layerDragHelper == null) {
                _layerDragHelper =
                    DragCallbackHelper.install(this, DragCallbackHelper.FLAG_VERTICAL).apply {
                        onClearView = { recyclerView, viewHolder ->
                            if (_dragHappened) {
                                //发生过拖拽
                                val list = mutableListOf<BaseItemRenderer<*>>()
                                _dslAdapter?.eachItem { index, dslAdapterItem ->
                                    if (dslAdapterItem is CanvasLayerItem) {
                                        dslAdapterItem.itemRenderer?.let {
                                            list.add(0, it)
                                        }
                                    }
                                }
                                canvasView.canvasDelegate.arrangeSort(list, Strategy.normal)
                            }
                        }
                    }
            }
            renderDslAdapter {
                hookUpdateDepend()
                canvasView.canvasDelegate.itemsRendererList.forEach {
                    CanvasLayerItem()(0) {
                        itemCanvasDelegate = canvasView.canvasDelegate
                        itemRenderer = it

                        itemSortAction = {
                            it.itemView.longFeedback()
                            _layerDragHelper?.startDrag(it)
                        }
                    }
                }
                if (canvasView.canvasDelegate.itemsRendererList.isEmpty()) {
                    setAdapterStatus(DslAdapterStatusItem.ADAPTER_STATUS_EMPTY)
                } else {
                    setAdapterStatus(DslAdapterStatusItem.ADAPTER_STATUS_NONE)
                }
            }
        }
    }

    //</editor-fold desc="图层控制">

    //<editor-fold desc="编辑控制">

    /**编辑控制*/
    fun showEditControlLayout(
        vh: DslViewHolder,
        canvasView: CanvasView,
        renderer: BaseItemRenderer<*>?
    ) {
        vh.rv(R.id.canvas_control_view)?.renderDslAdapter {
            hookUpdateDepend()

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
    }

    //</editor-fold desc="编辑控制">

    //<editor-fold desc="群组控制">

    /**群组控制*/
    fun renderGroupControlLayout(
        vh: DslViewHolder,
        canvasView: CanvasView,
        renderer: SelectGroupRenderer
    ) {
        vh.rv(R.id.canvas_control_view)?.renderDslAdapter {
            hookUpdateDepend()
            //
            CanvasControlItem()() {
                itemIco = R.drawable.canvas_align_left_ico
                itemText = _string(R.string.canvas_align_left)
                itemRenderer = renderer
                itemClick = {
                    renderer.updateAlign(Gravity.LEFT)
                }
            }
            CanvasControlItem()() {
                itemIco = R.drawable.canvas_align_right_ico
                itemText = _string(R.string.canvas_align_right)
                itemRenderer = renderer
                itemClick = {
                    renderer.updateAlign(Gravity.RIGHT)
                }
                drawCanvasRight()
            }

            //
            CanvasControlItem()() {
                itemIco = R.drawable.canvas_align_top_ico
                itemText = _string(R.string.canvas_align_top)
                itemRenderer = renderer
                itemClick = {
                    renderer.updateAlign(Gravity.TOP)
                }
            }
            CanvasControlItem()() {
                itemIco = R.drawable.canvas_align_bottom_ico
                itemText = _string(R.string.canvas_align_bottom)
                itemRenderer = renderer
                itemClick = {
                    renderer.updateAlign(Gravity.BOTTOM)
                }
                drawCanvasRight()
            }

            //
            CanvasControlItem()() {
                itemIco = R.drawable.canvas_align_horizontal_ico
                itemText = _string(R.string.canvas_align_horizontal)
                itemRenderer = renderer
                itemClick = {
                    renderer.updateAlign(Gravity.CENTER_HORIZONTAL)
                }
            }
            CanvasControlItem()() {
                itemIco = R.drawable.canvas_align_vertical_ico
                itemText = _string(R.string.canvas_align_vertical)
                itemRenderer = renderer
                itemClick = {
                    renderer.updateAlign(Gravity.CENTER_VERTICAL)
                }
            }
            CanvasControlItem()() {
                itemIco = R.drawable.canvas_align_center_ico
                itemText = _string(R.string.canvas_align_center)
                itemRenderer = renderer
                itemClick = {
                    renderer.updateAlign(Gravity.CENTER)
                }
            }
        }
    }
    //</editor-fold desc="群组控制">

    //<editor-fold desc="Undo">

    /**undo redo*/
    fun _updateUndoLayout(viewHolder: DslViewHolder = fragment._vh) {
        viewHolder.group(R.id.undo_wrap_layout)
            ?.resetDslItem(listOf(_undoCanvasItem, _redoCanvasItem))
    }

    //</editor-fold desc="Undo">

}