package com.angcyo.canvas2.laser.pecker

import com.angcyo.canvas.render.core.*
import com.angcyo.canvas.render.core.component.BaseControlPoint
import com.angcyo.canvas.render.core.component.CanvasRenderProperty
import com.angcyo.canvas.render.core.component.CanvasSelectorComponent
import com.angcyo.canvas.render.data.TouchSelectorInfo
import com.angcyo.canvas.render.renderer.BaseRenderer
import com.angcyo.canvas.render.unit.IRenderUnit
import com.angcyo.canvas2.laser.pecker.dslitem.CanvasIconItem
import com.angcyo.canvas2.laser.pecker.dslitem.CanvasLayerItem
import com.angcyo.canvas2.laser.pecker.dslitem.CanvasLayerNameItem
import com.angcyo.canvas2.laser.pecker.dslitem.ICanvasRendererItem
import com.angcyo.canvas2.laser.pecker.dslitem.item.*
import com.angcyo.canvas2.laser.pecker.util.LPConstant
import com.angcyo.canvas2.laser.pecker.util.LPRendererHelper
import com.angcyo.canvas2.laser.pecker.util.lpTextElement
import com.angcyo.canvas2.laser.pecker.util.saveProjectState
import com.angcyo.dialog.popup.MenuPopupConfig
import com.angcyo.dialog.recyclerPopupWindow
import com.angcyo.dsladapter.*
import com.angcyo.dsladapter.item.IFragmentItem
import com.angcyo.engrave.IEngraveCanvasFragment
import com.angcyo.engrave.data.HawkEngraveKeys
import com.angcyo.engrave.transition.EngraveTransitionManager
import com.angcyo.http.rx.doMain
import com.angcyo.library.annotation.CallPoint
import com.angcyo.library.component.pad.isInPadMode
import com.angcyo.library.ex.*
import com.angcyo.tablayout.DslTabLayout
import com.angcyo.widget.DslViewHolder
import com.angcyo.widget.base.resetDslItem
import com.angcyo.widget.recycler.renderDslAdapter

/**
 * 画板界面
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2023/03/03
 */
class RenderLayoutHelper(val canvasFragment: IEngraveCanvasFragment) {

    //region ---基础---

    /**调用入口*/
    @CallPoint
    fun bindCanvasLayout(vh: DslViewHolder) {
        _rootViewHolder = vh

        //恢复设置
        restoreCanvasSetting()

        //功能item渲染
        vh.canvasItemRv?.renderDslAdapter {
            hookUpdateDepend(this)

            //需要关闭的功能
            val closeCanvasItemsFun = HawkEngraveKeys.closeCanvasItemsFun

            if (!closeCanvasItemsFun.have("_image_")) {
                AddBitmapItem()() {
                    initItem()
                }
            }
            if (!closeCanvasItemsFun.have("_text_")) {
                AddTextItem()() {
                    initItem()
                }
            }
            //素材
            if (!closeCanvasItemsFun.have("_material_")) {
                materialCanvasItem() {
                    initItem()
                }
            }
            if (!closeCanvasItemsFun.have("_shapes_")) {
                AddShapesItem()() {
                    initItem()
                }
            }
            if (!closeCanvasItemsFun.have("_doodle_")) {
                AddDoodleItem()() {
                    initItem()
                }
            }
            //

            ControlEditItem()() {//edit
                initItem()
                itemRenderLayoutHelper = this@RenderLayoutHelper
            }

            if (!closeCanvasItemsFun.have("_layer_")) {
                if (isInPadMode()) {
                    //updateLayerListLayout(vh, canvasView)
                } else {
                    ControlLayerItem()() {
                        initItem()
                        itemRenderLayoutHelper = this@RenderLayoutHelper
                    }
                }
            }
            if (isDebugType()) {
                if (!closeCanvasItemsFun.have("_operate_")) {
                    ControlOperateItem()() {
                        initItem()
                    }
                }
            }
            if (!closeCanvasItemsFun.have("_setting_")) {
                ControlSettingItem()() {
                    initItem()
                }
            }
        }

        //事件
        bindCanvasListener()

        //undo
        updateUndoLayout()
    }

    private var _selectItem: DslAdapterItem? = null

    /**切换底部选中的item*/
    @CallPoint
    fun changeSelectItem(toItem: DslAdapterItem?) {
        if (_selectItem == toItem) {
            return
        }
        _selectItem?.updateItemSelected(false)
        _selectItem = toItem
    }

    //endregion ---基础---

    //region ---init---

    /**控制布局助手*/
    val renderControlHelper = RenderControlHelper(this)

    internal var _rootViewHolder: DslViewHolder? = null

    val canvasRenderDelegate: CanvasRenderDelegate?
        get() = _rootViewHolder?.renderDelegate

    /**恢复界面渲染界面设置*/
    private fun restoreCanvasSetting() {
        canvasRenderDelegate?.axisManager?.apply {
            //绘制网格恢复
            enableRenderGrid = LPConstant.CANVAS_DRAW_GRID
            //单位恢复
            updateRenderUnit(LPConstant.renderUnit)
        }
        //智能指南恢复
        //canvasDelegate.smartAssistant.enable = CanvasConstant.CANVAS_SMART_ASSISTANT
    }

    /**监听, 并赋值[IFragmentItem]*/
    fun hookUpdateDepend(adapter: DslAdapter) {
        adapter.apply {
            observeItemUpdateDepend {
                adapterItems.forEach {
                    if (it is IFragmentItem) {
                        it.itemFragment = canvasFragment.fragment
                    }
                }
            }
        }
    }

    /**初始化*/
    private fun ICanvasRendererItem.initItem(renderer: BaseRenderer? = null) {
        itemRenderer = renderer
        itemRenderDelegate = _rootViewHolder?.renderDelegate
    }

    /**素材item, 暴露给外部配置*/
    var materialCanvasItem: CanvasIconItem = AddMaterialItem()

    /**查找item*/
    fun findTagItem(tag: String): DslAdapterItem? {
        return _rootViewHolder?.canvasItemAdapter?.findItemByTag(tag)
    }

    /**绑定事件*/
    private fun bindCanvasListener() {
        val renderDelegate = _rootViewHolder?.renderDelegate
        renderDelegate?.addCanvasRenderListener(object :
            BaseCanvasRenderListener() {

            override fun onRenderUndoChange(undoManager: CanvasUndoManager) {
                updateUndoLayout()
            }

            override fun onSelectorRendererChange(
                selectorComponent: CanvasSelectorComponent,
                from: List<BaseRenderer>,
                to: List<BaseRenderer>
            ) {
                renderControlHelper.bindControlLayout()
                renderLayerListLayout()
            }

            override fun onRendererFlagsChange(
                renderer: BaseRenderer,
                oldFlags: Int,
                newFlags: Int,
                reason: Reason
            ) {
                if (renderer is CanvasSelectorComponent) {
                    if (reason.renderFlag.have(BaseRenderer.RENDERER_FLAG_LOCK_SCALE)) {
                        //锁的状态改变
                        renderControlHelper.updateControlLayout()
                    }
                }
                if (reason.controlType.have(BaseControlPoint.CONTROL_TYPE_DATA)) {
                    //数据改变, 比如切换了图片算法/填充/描边等
                    renderControlHelper.updateControlLayout()
                }
                if (reason.renderFlag.have(BaseRenderer.RENDERER_FLAG_REQUEST_DRAWABLE) ||
                    reason.renderFlag.have(BaseRenderer.RENDERER_FLAG_REQUEST_PROPERTY)
                ) {
                    renderLayerListLayout()
                }
            }

            override fun onRendererPropertyChange(
                renderer: BaseRenderer,
                fromProperty: CanvasRenderProperty?,
                toProperty: CanvasRenderProperty?,
                reason: Reason
            ) {
                renderControlHelper.updateControlLayout()
            }

            override fun onRenderUnitChange(from: IRenderUnit, to: IRenderUnit) {
                renderControlHelper.updateControlLayout()
            }

            override fun onDoubleTapItem(
                selectorManager: CanvasSelectorManager,
                renderer: BaseRenderer
            ) {
                renderer.lpTextElement()?.let {
                    AddTextItem.amendInputText(canvasRenderDelegate, renderer)
                }
            }

            override fun onSelectorRendererList(
                selectorManager: CanvasSelectorManager,
                selectorInfo: TouchSelectorInfo
            ) {
                canvasRenderDelegate?.view?.let { view ->
                    view.recyclerPopupWindow {
                        showOnViewBottom(view)
                        renderAdapter {
                            selectorInfo.touchRendererList.forEach { renderer ->
                                CanvasLayerItem()() { //元素
                                    initItem(renderer)
                                    itemShowSeeView = false
                                    itemShowLockView = false
                                    itemShowEngraveParams = false
                                    itemFlag = MenuPopupConfig.FLAG_ITEM_DISMISS
                                    itemLongClick = null
                                    itemClick = {
                                        canvasRenderDelegate?.selectorManager?.resetSelectorRenderer(
                                            listOf(renderer),
                                            Reason.user
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            override fun onElementRendererListChange(
                from: List<BaseRenderer>,
                to: List<BaseRenderer>,
                op: List<BaseRenderer>
            ) {
                if (!renderDelegate.asyncManager.hasAsyncTask()) {
                    renderDelegate.saveProjectState()
                }
                renderLayerListLayout()
            }

            override fun onAsyncStateChange(uuid: String, state: Int) {
                if (!renderDelegate.asyncManager.hasAsyncTask()) {
                    renderDelegate.saveProjectState()
                }
            }
        })
    }

    //endregion ---init---

    //region---Undo---

    val undoManager: CanvasUndoManager?
        get() = _rootViewHolder?.renderDelegate?.undoManager

    /**撤销item*/
    private var _undoCanvasItem: CanvasIconItem = CanvasIconItem().apply {
        itemIco = R.drawable.canvas_undo_ico
        itemText = _string(R.string.canvas_undo)
        itemEnable = false

        _undoCanvasItem = this
        itemClick = {
            undoManager?.undo()
        }
    }

    /**重做item*/
    private var _redoCanvasItem: CanvasIconItem = CanvasIconItem().apply {
        itemIco = R.drawable.canvas_redo_ico
        itemText = _string(R.string.canvas_redo)
        itemEnable = false

        _redoCanvasItem = this
        itemClick = {
            undoManager?.redo()
        }
    }

    /**更新撤销回退item
     * [disable] 是否强制禁用2个item*/
    private fun updateUndoLayout(disable: Boolean = findTagItem(ControlEditItem.TAG_EDIT_ITEM)?.itemEnable == false) {
        _undoCanvasItem.apply {
            itemEnable = if (disable) false else undoManager?.canUndo() == true
            if (isShowDebug()) {
                itemTextSuperscript = "${undoManager?.undoStack.size()}"
            }
        }
        _redoCanvasItem.apply {
            itemEnable = if (disable) false else undoManager?.canRedo() == true
            if (isShowDebug()) {
                itemTextSuperscript = "${undoManager?.redoStack.size()}"
            }
        }
        _updateUndoLayout()
    }

    /**undo redo*/
    private fun _updateUndoLayout() {
        doMain {
            _rootViewHolder?.group(R.id.undo_wrap_layout)
                ?.resetDslItem(listOf(_undoCanvasItem, _redoCanvasItem))
        }
    }

    //endregion---Undo---

    //region---Layer---

    var _layerDragHelper: DragCallbackHelper? = null
    var _layerTabLayout: DslTabLayout? = null

    /**显示/更新图层item*/
    fun renderLayerListLayout() {
        val vh = _rootViewHolder ?: return
        val delegate = canvasRenderDelegate ?: return

        if (!vh.isVisible(R.id.canvas_layer_layout)) {
            return
        }

        _layerTabLayout = vh.v(R.id.layer_tab_view)
        _layerTabLayout?.configTabLayoutConfig {
            onSelectIndexChange = { fromIndex, selectIndexList, reselect, fromUser ->
                vh.post {
                    //刷新界面
                    renderLayerListLayout()
                }
            }
        }
        val tabIndex = _layerTabLayout?.currentItemIndex ?: 0

        vh.rv(R.id.canvas_layer_view)?.apply {
            if (tabIndex == 0) {
                //正常图层
                if (_layerDragHelper == null) {
                    _layerDragHelper =
                        DragCallbackHelper.install(this, DragCallbackHelper.FLAG_VERTICAL).apply {
                            enableLongPressDrag = false//长按拖拽关闭
                            onClearView = { recyclerView, viewHolder ->
                                if (_dragHappened) {
                                    //发生过拖拽
                                    val list = mutableListOf<BaseRenderer>()
                                    _dslAdapter?.eachItem { index, dslAdapterItem ->
                                        if (dslAdapterItem is CanvasLayerItem) {
                                            dslAdapterItem.itemRenderer?.let {
                                                list.add(0, it)
                                            }
                                        }
                                    }
                                    delegate.renderManager.arrangeSort(list, Strategy.normal)
                                }
                            }
                        }
                }
                renderDslAdapter {
                    hookUpdateDepend(this)
                    val allElementRendererList =
                        delegate.renderManager.getAllElementRendererList(false)
                    allElementRendererList.forEach { renderer ->
                        //后面添加的元素, 在顶部显示
                        CanvasLayerItem()(0) {
                            initItem(renderer)

                            itemSortAction = {
                                _layerDragHelper?.startDrag(it)
                            }
                        }
                    }
                    if (allElementRendererList.isEmpty()) {
                        setAdapterStatus(DslAdapterStatusItem.ADAPTER_STATUS_EMPTY)
                    } else {
                        setAdapterStatus(DslAdapterStatusItem.ADAPTER_STATUS_NONE)
                    }
                }
            } else if (tabIndex == 1) {
                //雕刻图层
                if (_layerDragHelper != null) {
                    _layerDragHelper?.detachFromRecyclerView()
                    _layerDragHelper = null
                }
                renderDslAdapter {
                    EngraveTransitionManager.engraveLayerList.forEach {
                        CanvasLayerNameItem()() {//雕刻图层
                            itemGroupExtend = true
                            itemChanging = true
                            itemLayerInfo = it

                            itemLoadSubList = {
                                /*val itemList =
                                    EngraveTransitionManager.getRendererList(canvasDelegate, it)
                                        .mapTo(mutableListOf<DslAdapterItem>()) { renderer ->
                                            CanvasBaseLayerItem().apply {//元素
                                                initItem(renderer)
                                                itemClick = {
                                                    showItemRendererBounds()
                                                    if (HawkEngraveKeys.enableItemEngraveParams) {
                                                        //显示单元素雕刻参数
                                                        engraveCanvasFragment.engraveFlowLayoutHelper.startEngraveItemConfig(
                                                            engraveCanvasFragment,
                                                            renderer
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                itemSubList.resetAll(itemList)*/
                            }
                        }
                    }
                }
            }
        }
        updateLayerControlLayout()
    }

    /**更新图层下面的几个控制按钮*/
    fun updateLayerControlLayout() {
        val vh = _rootViewHolder ?: return
        val delegate = canvasRenderDelegate ?: return
        val tabIndex = _layerTabLayout?.currentItemIndex ?: 0
        vh.visible(R.id.layer_control_layout, tabIndex == 0)
        vh.visible(R.id.layer_control_line_view, tabIndex == 0)
        if (tabIndex == 0) {
            //control
            val list = delegate.selectorManager.getSelectorRendererList(false)
            vh.enable(R.id.layer_control_delete_view, list.isNotEmpty())
            vh.enable(R.id.layer_control_visible_view, list.isNotEmpty())
            vh.enable(R.id.layer_control_copy_view, list.isNotEmpty())

            vh.click(R.id.layer_control_delete_view) {
                delegate.renderManager.removeElementRenderer(list, Strategy.normal)
            }
            vh.click(R.id.layer_control_visible_view) {
                delegate.renderManager.updateRendererVisible(list, false, Reason.user, delegate)
            }
            vh.click(R.id.layer_control_copy_view) {
                LPRendererHelper.copyRenderer(delegate, list, true)
            }
        }
    }

    //endregion---Layer---

}