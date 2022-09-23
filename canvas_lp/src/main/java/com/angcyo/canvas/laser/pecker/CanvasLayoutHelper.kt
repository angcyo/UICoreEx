package com.angcyo.canvas.laser.pecker

import android.graphics.RectF
import android.graphics.drawable.BitmapDrawable
import android.view.MotionEvent
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
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
import com.angcyo.canvas.graphics.*
import com.angcyo.canvas.items.PictureBitmapItem
import com.angcyo.canvas.items.PictureShapeItem
import com.angcyo.canvas.items.data.DataItemRenderer
import com.angcyo.canvas.items.renderer.BaseItemRenderer
import com.angcyo.canvas.items.renderer.IItemRenderer
import com.angcyo.canvas.items.renderer.PictureItemRenderer
import com.angcyo.canvas.items.renderer.PictureTextItemRenderer
import com.angcyo.canvas.laser.pecker.CanvasEditLayoutHelper.drawCanvasRight
import com.angcyo.canvas.laser.pecker.CanvasEditLayoutHelper.renderCommonEditItems
import com.angcyo.canvas.laser.pecker.CanvasEditLayoutHelper.renderGroupEditItems
import com.angcyo.canvas.laser.pecker.CanvasEditLayoutHelper.renderImageEditItems
import com.angcyo.canvas.laser.pecker.CanvasEditLayoutHelper.renderShapeEditItems
import com.angcyo.canvas.laser.pecker.CanvasEditLayoutHelper.renderTextEditItems
import com.angcyo.canvas.laser.pecker.dslitem.*
import com.angcyo.canvas.utils.*
import com.angcyo.core.vmApp
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
import com.pixplicity.sharp.SharpDrawable

/**
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2022/05/09
 */
class CanvasLayoutHelper(val fragment: AbsFragment) {

    companion object {
        const val TAG_EDIT_ITEM = "tag_edit_item"
    }

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
        _selectedCanvasItem?.updateItemSelected(false)
        _selectedCanvasItem = null
    }

    /**选中一个新的Item, 并取消之前选中的.
     * 互斥操作逻辑*/
    fun selectedItemWith(vh: DslViewHolder, canvasView: CanvasView, item: DslAdapterItem?) {
        if (_selectedCanvasItem == item) {
            return
        }
        cancelSelectedItem()
        _selectedCanvasItem = item

        //取消选中后, 恢复是否需要编辑
        if (item == null) {
            val itemRenderer = canvasView.canvasDelegate.getSelectedRenderer()
            if (itemRenderer == null) {
                vh.showControlLayout(canvasView, false, false)
            } else {
                vh.showControlLayout(canvasView, true, true)
            }
        }
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
                    updateItemSelected(!itemIsSelected)
                    fragment.context.canvasMaterialWindow(it) {
                        onDismiss = {
                            updateItemSelected(false)
                            false
                        }
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
                    updateItemSelected(!itemIsSelected)

                    if (itemIsSelected) {
                        vh.showControlLayout(canvasView, false, true)
                        selectedItemWith(vh, canvasView, this)
                        showShapeSelectLayout(vh, canvasView)
                        UMEvent.CANVAS_SHAPE.umengEventValue()
                    } else {
                        selectedItemWith(vh, canvasView, null)
                    }
                }
            }
            CanvasControlItem2()() {
                itemIco = R.drawable.canvas_doodle_ico
                itemText = _string(R.string.canvas_doodle)
                itemEnable = true
                itemClick = {
                    UMEvent.CANVAS_DOODLE.umengEventValue()
                    fragment.context.doodleDialog {
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
                itemTag = TAG_EDIT_ITEM
                itemClick = {
                    updateItemSelected(!itemIsSelected)
                    if (itemIsSelected) {
                        selectedItemWith(vh, canvasView, this)
                    }
                    vh.showControlLayout(canvasView, itemIsSelected, itemIsSelected)
                }
            }
            CanvasControlItem2()() {
                _layerCanvasItem = this
                itemIco = R.drawable.canvas_layer_ico
                itemText = _string(R.string.canvas_layer)
                itemEnable = true
                itemClick = {
                    vh.gone(R.id.canvas_layer_layout, itemIsSelected)
                    updateItemSelected(!itemIsSelected)

                    if (itemIsSelected) {
                        updateLayerControlLayout(vh, canvasView)
                    }
                }
            }
            if (isDebug()) {
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
            }
            CanvasControlItem2()() {
                itemIco = R.drawable.canvas_setting_ico
                itemText = _string(R.string.canvas_setting)
                itemEnable = true

                itemClick = {
                    updateItemSelected(!itemIsSelected)

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
                vh.showControlLayout(canvasView, false, false)
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

    /**隐藏/显示编辑控制布局
     * [canvasView]
     * [needEdit] 是否需要渲染编辑item
     * [visible] 隐藏/显示
     * */
    fun DslViewHolder.showControlLayout(
        canvasView: CanvasView,
        needEdit: Boolean = true,
        visible: Boolean = true
    ) {
        val itemRecyclerView = v<RecyclerView>(R.id.canvas_item_view)
        val editItem = itemRecyclerView?._dslAdapter?.findItemByTag(TAG_EDIT_ITEM)
        editItem?.updateItemSelected(visible && needEdit)

        if (visible) {
            if (needEdit) {
                //显示编辑控制布局
                val itemRenderer = canvasView.canvasDelegate.getSelectedRenderer()
                renderSelectedItemEditControlLayout(this, canvasView, itemRenderer)
            }
        } else {
            //隐藏编辑控制
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
    fun renderSelectedItemEditControlLayout(
        vh: DslViewHolder,
        canvasView: CanvasView,
        itemRenderer: BaseItemRenderer<*>?
    ): Boolean {
        var result = true
        vh.rv(R.id.canvas_control_view)?.renderDslAdapter {
            hookUpdateDepend()

            if (itemRenderer is DataItemRenderer) {
                val dataBean = itemRenderer.rendererItem?.dataBean
                when (dataBean?.mtype) {
                    CanvasConstant.DATA_TYPE_BITMAP -> renderImageEditItems(fragment, itemRenderer)
                    CanvasConstant.DATA_TYPE_TEXT -> renderTextEditItems(itemRenderer)
                    CanvasConstant.DATA_TYPE_LINE,
                    CanvasConstant.DATA_TYPE_OVAL,
                    CanvasConstant.DATA_TYPE_RECT,
                    CanvasConstant.DATA_TYPE_POLYGON,
                    CanvasConstant.DATA_TYPE_PENTAGRAM,
                    CanvasConstant.DATA_TYPE_LOVE -> renderShapeEditItems(itemRenderer)
                }
            } else if (itemRenderer is SelectGroupRenderer) {
                renderGroupEditItems(itemRenderer)
            } else {
                //vh.showControlLayout(false)
                result = false
            }

            //
            renderCommonEditItems(canvasView, fragment, itemRenderer)
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
                itemClick = {
                    canvasView.canvasDelegate.addLineRender()
                }
            }
            ShapeItem(canvasView)() {
                itemIco = R.drawable.canvas_shape_oval_ico
                itemText = _string(R.string.canvas_oval)
                itemClick = {
                    canvasView.canvasDelegate.addOvalRender()
                }
            }
            ShapeItem(canvasView)() {
                itemIco = R.drawable.canvas_shape_rectangle_ico
                itemText = _string(R.string.canvas_rectangle)
                itemClick = {
                    canvasView.canvasDelegate.addRectRender()
                }
            }
            ShapeItem(canvasView)() {
                itemIco = R.drawable.canvas_shape_polygon_ico
                itemText = _string(R.string.canvas_polygon)
                itemClick = {
                    canvasView.canvasDelegate.addPolygonRender()
                }
            }
            ShapeItem(canvasView)() {
                itemIco = R.drawable.canvas_shape_pentagram_ico
                itemText = _string(R.string.canvas_pentagram)
                itemClick = {
                    canvasView.canvasDelegate.addPentagramRender()
                }
            }
            ShapeItem(canvasView)() {
                itemIco = R.drawable.canvas_shape_love_ico
                itemText = _string(R.string.canvas_love)
                itemClick = {
                    canvasView.canvasDelegate.addLoveRender()
                }
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

    //</editor-fold desc="文本属性控制">

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

    //<editor-fold desc="Undo">

    /**undo redo*/
    fun _updateUndoLayout(viewHolder: DslViewHolder = fragment._vh) {
        viewHolder.group(R.id.undo_wrap_layout)
            ?.resetDslItem(listOf(_undoCanvasItem, _redoCanvasItem))
    }

    //</editor-fold desc="Undo">

}