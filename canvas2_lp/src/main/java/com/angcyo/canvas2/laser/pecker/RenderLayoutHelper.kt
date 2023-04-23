package com.angcyo.canvas2.laser.pecker

import android.graphics.Matrix
import android.view.MotionEvent
import com.angcyo.bluetooth.fsc.laserpacker.HawkEngraveKeys
import com.angcyo.bluetooth.fsc.laserpacker.LaserPeckerConfigHelper
import com.angcyo.canvas.render.core.BaseCanvasRenderListener
import com.angcyo.canvas.render.core.CanvasRenderDelegate
import com.angcyo.canvas.render.core.CanvasSelectorManager
import com.angcyo.canvas.render.core.CanvasUndoManager
import com.angcyo.canvas.render.core.Reason
import com.angcyo.canvas.render.core.Strategy
import com.angcyo.canvas.render.core.component.BaseControlPoint
import com.angcyo.canvas.render.core.component.CanvasSelectorComponent
import com.angcyo.canvas.render.core.component.PointTouchComponent
import com.angcyo.canvas.render.data.TouchSelectorInfo
import com.angcyo.canvas.render.renderer.BaseRenderer
import com.angcyo.canvas.render.renderer.CanvasGroupRenderer
import com.angcyo.canvas.render.state.GroupStateStack
import com.angcyo.canvas.render.state.IStateStack
import com.angcyo.canvas2.laser.pecker.ProductLayoutHelper.Companion.TAG_MAIN
import com.angcyo.canvas2.laser.pecker.dslitem.CanvasIconItem
import com.angcyo.canvas2.laser.pecker.dslitem.CanvasLayerBaseItem
import com.angcyo.canvas2.laser.pecker.dslitem.CanvasLayerItem
import com.angcyo.canvas2.laser.pecker.dslitem.CanvasLayerNameItem
import com.angcyo.canvas2.laser.pecker.dslitem.ICanvasRendererItem
import com.angcyo.canvas2.laser.pecker.dslitem.item.AddBitmapItem
import com.angcyo.canvas2.laser.pecker.dslitem.item.AddDoodleItem
import com.angcyo.canvas2.laser.pecker.dslitem.item.AddMaterialItem
import com.angcyo.canvas2.laser.pecker.dslitem.item.AddShapesItem
import com.angcyo.canvas2.laser.pecker.dslitem.item.AddTextItem
import com.angcyo.canvas2.laser.pecker.dslitem.item.ControlEditItem
import com.angcyo.canvas2.laser.pecker.dslitem.item.ControlLayerItem
import com.angcyo.canvas2.laser.pecker.dslitem.item.ControlOperateItem
import com.angcyo.canvas2.laser.pecker.dslitem.item.ControlSettingItem
import com.angcyo.canvas2.laser.pecker.dslitem.item.ShapesItem
import com.angcyo.canvas2.laser.pecker.engrave.LPEngraveHelper
import com.angcyo.canvas2.laser.pecker.engrave.LPPreviewHelper
import com.angcyo.canvas2.laser.pecker.manager.saveProjectStateV2
import com.angcyo.canvas2.laser.pecker.util.LPConstant
import com.angcyo.canvas2.laser.pecker.util.LPRendererHelper
import com.angcyo.canvas2.laser.pecker.util.lpElementBean
import com.angcyo.canvas2.laser.pecker.util.lpTextElement
import com.angcyo.dialog.popup.MenuPopupConfig
import com.angcyo.dialog.recyclerPopupWindow
import com.angcyo.dsladapter.DragCallbackHelper
import com.angcyo.dsladapter.DslAdapter
import com.angcyo.dsladapter.DslAdapterItem
import com.angcyo.dsladapter.DslAdapterStatusItem
import com.angcyo.dsladapter.eachItem
import com.angcyo.dsladapter.findItemByTag
import com.angcyo.dsladapter.item.IFragmentItem
import com.angcyo.dsladapter.updateItemSelected
import com.angcyo.http.rx.doMain
import com.angcyo.laserpacker.LPDataConstant
import com.angcyo.laserpacker.device.LayerHelper
import com.angcyo.laserpacker.generateGroupName
import com.angcyo.library.L
import com.angcyo.library.annotation.CallPoint
import com.angcyo.library.component.pad.isInPadMode
import com.angcyo.library.component.pool.acquireTempRectF
import com.angcyo.library.ex._string
import com.angcyo.library.ex.have
import com.angcyo.library.ex.isDebug
import com.angcyo.library.ex.isShowDebug
import com.angcyo.library.ex.isTouchDown
import com.angcyo.library.ex.longFeedback
import com.angcyo.library.ex.mH
import com.angcyo.library.ex.resetAll
import com.angcyo.library.ex.size
import com.angcyo.library.ex.uuid
import com.angcyo.library.unit.IRenderUnit
import com.angcyo.tablayout.DslTabLayout
import com.angcyo.widget.DslViewHolder
import com.angcyo.widget.base.resetDslItem
import com.angcyo.widget.base.showPopupMenu
import com.angcyo.widget.recycler.renderDslAdapter
import kotlin.collections.set

/**
 * 画板界面
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2023/03/03
 */
class RenderLayoutHelper(val renderFragment: IEngraveRenderFragment) {

    //region ---基础---

    /**调用入口*/
    @CallPoint
    fun bindRenderLayout(vh: DslViewHolder) {
        _rootViewHolder = vh

        //恢复设置
        restoreCanvasSetting()

        //功能item渲染
        vh.canvasItemRv?.renderDslAdapter {
            hookUpdateDepend(this)

            //需要关闭的功能
            val closeCanvasItemsFun =
                LaserPeckerConfigHelper.readDeviceSettingConfig()?.closeCanvasItemsFun

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
                    itemRenderLayoutHelper = this@RenderLayoutHelper
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
                    renderLayerListLayout()
                } else {
                    ControlLayerItem()() {
                        initItem()
                        itemRenderLayoutHelper = this@RenderLayoutHelper
                    }
                }
            }
            if (isDebug()) {
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

        //product
        productLayoutHelper.bindProductLayout()
    }

    //禁用之前是否是编辑item
    private var _disableBeforeIsEditItem = false

    /**禁用编辑item*/
    fun disableEditItem(disable: Boolean) {
        val editItem = renderControlHelper.editItem
        if (editItem?.itemEnable == !disable) {
            return
        }

        if (disable) {
            //禁用
            _disableBeforeIsEditItem = false
            val item = _selectItem
            if (editItem?.itemIsSelected == true || (item != null && item.itemTag == ControlEditItem.TAG_EDIT_ITEM)) {
                renderControlHelper.hideControlLayout(true)
                _disableBeforeIsEditItem = true
                changeSelectItem(null)
            }
        }
        editItem?.apply {
            itemEnable = !disable
            updateAdapterItem()
        }
        updateUndoLayout()
        if (!disable && _disableBeforeIsEditItem) {
            //启用, 恢复之前的编辑状态
            renderControlHelper.bindControlLayout()
            changeSelectItem(renderControlHelper.editItem) //切换到编辑状态
        }
    }

    private var _selectItem: DslAdapterItem? = null

    /**切换底部选中的item*/
    @CallPoint
    fun changeSelectItem(toItem: DslAdapterItem?) {
        val oldItem = _selectItem
        if (oldItem == toItem) {
            return
        }
        oldItem?.updateItemSelected(false)
        _selectItem = toItem
    }

    /**渲染形状列表
     * [visible] 是否要渲染*/
    fun renderShapesItems(item: DslAdapterItem, visible: Boolean) {
        if (visible) {
            changeSelectItem(item)
            renderControlHelper.showControlLayout(item)
            _rootViewHolder?.canvasControlRv?.renderDslAdapter {
                hookUpdateDepend(this)
                ShapesItem()() {
                    initItem()
                    itemIco = R.drawable.canvas_shape_line_ico
                    itemText = _string(R.string.canvas_line)
                    itemShapeType = LPDataConstant.DATA_TYPE_LINE
                }
                ShapesItem()() {
                    initItem()
                    itemIco = R.drawable.canvas_shape_oval_ico
                    itemText = _string(R.string.canvas_oval)
                    itemShapeType = LPDataConstant.DATA_TYPE_OVAL
                }
                ShapesItem()() {
                    initItem()
                    itemIco = R.drawable.canvas_shape_rectangle_ico
                    itemText = _string(R.string.canvas_rectangle)
                    itemShapeType = LPDataConstant.DATA_TYPE_RECT
                }
                ShapesItem()() {
                    initItem()
                    itemIco = R.drawable.canvas_shape_polygon_ico
                    itemText = _string(R.string.canvas_polygon)
                    itemShapeType = LPDataConstant.DATA_TYPE_POLYGON
                }
                ShapesItem()() {
                    initItem()
                    itemIco = R.drawable.canvas_shape_pentagram_ico
                    itemText = _string(R.string.canvas_pentagram)
                    itemShapeType = LPDataConstant.DATA_TYPE_PENTAGRAM
                }
                ShapesItem()() {
                    initItem()
                    itemIco = R.drawable.canvas_shape_love_ico
                    itemText = _string(R.string.canvas_love)
                    itemShapeType = LPDataConstant.DATA_TYPE_LOVE
                }
            }
        } else {
            renderControlHelper.bindControlLayout()
        }
    }

    //endregion ---基础---

    //region ---init---

    /**控制布局助手*/
    val renderControlHelper = RenderControlHelper(this)

    /**产品布局助手*/
    val productLayoutHelper = ProductLayoutHelper(this)

    internal var _rootViewHolder: DslViewHolder? = null

    val delegate: CanvasRenderDelegate?
        get() = _rootViewHolder?.renderDelegate

    /**恢复界面渲染界面设置*/
    private fun restoreCanvasSetting() {
        delegate?.axisManager?.apply {
            //绘制网格恢复
            enableRenderGrid = LPConstant.CANVAS_DRAW_GRID
            //单位恢复
            updateRenderUnit(LPConstant.renderUnit)
        }
        //智能指南恢复
        delegate?.controlManager?.apply {
            smartAssistantComponent.isEnableComponent = LPConstant.CANVAS_SMART_ASSISTANT
        }
    }

    /**监听, 并赋值[IFragmentItem]*/
    fun hookUpdateDepend(adapter: DslAdapter) {
        adapter.apply {
            observeItemUpdateDepend {
                adapterItems.forEach {
                    if (it is IFragmentItem) {
                        it.itemFragment = renderFragment.fragment
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

            override fun onDispatchTouchEvent(event: MotionEvent) {
                if (event.isTouchDown()) {
                    //点击空白处, 隐藏图层控制布局
                    if (!isInPadMode()) {
                        showLayerLayout(false)
                    }
                }
            }

            /**画笔缩放比例改变后, 反向放大画笔绘制*/
            override fun onRenderBoxMatrixUpdate(
                newMatrix: Matrix,
                reason: Reason,
                finish: Boolean
            ) {
                if (finish && reason.controlType.have(BaseControlPoint.CONTROL_TYPE_SCALE)) {
                    val list = renderDelegate.renderManager.getAllElementRendererList(true, false)
                    for (renderer in list) {
                        val bean = renderer.lpElementBean()
                        when {
                            bean?.isPathElement == true -> {
                                if (bean.paintStyle == 1 || bean.isLineShape) {
                                    //描边的矢量图形, 画布缩放后, 反向放大画笔绘制
                                    renderer.requestUpdatePropertyFlag(
                                        Reason.preview,
                                        renderDelegate
                                    )
                                }
                            }

                            bean?.mtype == LPDataConstant.DATA_TYPE_BITMAP -> {
                                if (bean.imageFilter == LPDataConstant.DATA_MODE_GCODE) {
                                    renderer.requestUpdatePropertyFlag(
                                        Reason.preview,
                                        renderDelegate
                                    )
                                }
                            }
                        }
                    }
                }
            }

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

                //更新预览的范围
                val deviceStateModel = renderFragment.engraveFlowLayoutHelper.deviceStateModel
                if (deviceStateModel.deviceStateData.value?.isModeEngravePreview() == true) {
                    //设备正在预览模式, 更新预览
                    LPPreviewHelper.updatePreviewByRenderer(renderFragment, to)
                }
            }

            override fun onRendererFlagsChange(
                renderer: BaseRenderer,
                oldFlags: Int,
                newFlags: Int,
                reason: Reason
            ) {
                var needUpdateControlLayout = false
                if (renderer is CanvasSelectorComponent) {
                    if (reason.renderFlag.have(BaseRenderer.RENDERER_FLAG_LOCK_SCALE)) {
                        //锁的状态改变
                        needUpdateControlLayout = true
                    }
                }
                if (reason.controlType.have(BaseControlPoint.CONTROL_TYPE_DATA)) {
                    //数据改变, 比如切换了图片算法/填充/描边等

                    val index = renderer.lpElementBean()?.index
                    if (index != null) {
                        L.i("数据改变,清空索引:${index} $reason")
                        renderer.lpElementBean()?.index = null //清空数据索引
                    }

                    needUpdateControlLayout = true

                    //更新预览的范围
                    val deviceStateModel = renderFragment.engraveFlowLayoutHelper.deviceStateModel
                    if (deviceStateModel.deviceStateData.value?.isModeEngravePreview() == true &&
                        delegate?.isRendererSelector(renderer) == true
                    ) {
                        //设备正在预览模式, 更新预览
                        LPPreviewHelper.updatePreviewByRenderer(
                            renderFragment,
                            delegate?.getSelectorSingleElementRendererList(true, false)
                        )
                    }
                }
                if (reason.renderFlag.have(BaseRenderer.RENDERER_FLAG_REQUEST_PROPERTY)) {
                    renderLayerListLayout()
                    needUpdateControlLayout = true
                }

                if (needUpdateControlLayout) {
                    renderControlHelper.updateControlLayout()
                }
            }

            /*override fun onRendererPropertyChange(
                renderer: BaseRenderer,
                fromProperty: CanvasRenderProperty?,
                toProperty: CanvasRenderProperty?,
                reason: Reason
            ) {
                renderControlHelper.updateControlLayout()
            }*/

            override fun onRenderUnitChange(from: IRenderUnit, to: IRenderUnit) {
                renderControlHelper.updateControlLayout()
            }

            override fun onDoubleTapItem(
                selectorManager: CanvasSelectorManager,
                renderer: BaseRenderer
            ) {
                renderer.lpTextElement()?.let {
                    AddTextItem.amendInputText(delegate, renderer)
                }
            }

            override fun onSelectorRendererList(
                selectorManager: CanvasSelectorManager,
                selectorInfo: TouchSelectorInfo
            ) {
                delegate?.view?.let { view ->
                    view.recyclerPopupWindow {
                        showOnViewBottom(view)
                        renderAdapter {
                            selectorInfo.touchRendererList.reversed().forEach { renderer ->
                                CanvasLayerItem()() { //元素
                                    initItem(renderer)
                                    itemShowSeeView = false
                                    itemShowLockView = false
                                    itemShowEngraveParams = false
                                    itemFlag = MenuPopupConfig.FLAG_ITEM_DISMISS
                                    itemLongClick = null
                                    itemClick = {
                                        delegate?.selectorManager?.resetSelectorRenderer(
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
                op: List<BaseRenderer>,
                reason: Reason
            ) {
                if ((reason.reason == Reason.REASON_CODE ||
                            reason.reason == Reason.REASON_USER) && !renderDelegate.asyncManager.hasAsyncTask()
                ) {
                    renderDelegate.saveProjectStateV2()
                }
                renderFragment.engraveFlowLayoutHelper._engraveItemRenderer?.let {
                    //当前正在配置参数的元素被删除时, 隐藏参数配置界面
                    if (!renderDelegate.getSingleElementRendererListIn(to).contains(it)) {
                        renderFragment.engraveFlowLayoutHelper.hideIfInEngraveItemParamsConfig()
                    }
                }
                renderLayerListLayout()
            }

            override fun onAsyncStateChange(uuid: String, state: Int) {
                if (!renderDelegate.asyncManager.hasAsyncTask()) {
                    renderDelegate.saveProjectStateV2()
                }
            }

            override fun onRendererGroupChange(
                groupRenderer: CanvasGroupRenderer,
                subRendererList: List<BaseRenderer>,
                groupType: Int
            ) {
                var groupId: String? = null
                var groupName: String? = null
                if (groupType == CanvasGroupRenderer.GROUP_TYPE_GROUP) {
                    //群组时, 使用统一的groupId
                    groupId = uuid()
                    groupName = delegate?.getAllElementBean()?.generateGroupName()
                } else {
                    //解组时, 清除groupId
                }
                subRendererList.getAllElementBean().updateGroupInfo(groupId, groupName)
            }

            /**群组状态的保存和恢复*/
            override fun onRendererSaveState(renderer: BaseRenderer, stateStack: IStateStack) {
                if (stateStack is GroupStateStack) {
                    for (subRenderer in renderer.getSingleRendererList(false)) {
                        subRenderer.lpElementBean()?.apply {
                            //保存groupId
                            stateStack.valueMap[subRenderer.uuid] = groupId
                        }
                    }
                }
            }

            override fun onRendererRestoreState(renderer: BaseRenderer, stateStack: IStateStack) {
                if (stateStack is GroupStateStack) {
                    for (subRenderer in renderer.getSingleRendererList(false)) {
                        //恢复groupId
                        subRenderer.lpElementBean()?.groupId =
                            stateStack.valueMap[subRenderer.uuid]?.toString()
                    }
                }
            }

            override fun onPointTouchEvent(component: PointTouchComponent, type: Int) {
                if (component.pointTag == PointTouchComponent.TAG_INITIAL) {
                    if (type == PointTouchComponent.TOUCH_TYPE_CLICK) {
                        //点击左上角
                        if (delegate?.selectorManager?.isSelectorElement == true) {
                            //如果选中了元素, 则显示元素的bounds
                            delegate?.showRendererBounds(delegate?.selectorManager?.selectorComponent)
                        } else {
                            val rect = acquireTempRectF()
                            val renderViewBox = delegate?.renderViewBox ?: return
                            var def = true
                            val primaryLimitBounds =
                                delegate?.renderManager?.limitRenderer?.findLimitInfo { it.tag == TAG_MAIN }?.bounds
                            if (primaryLimitBounds != null) {
                                rect.set(primaryLimitBounds)
                                def = false
                            }
                            if (!def) {
                                delegate?.showRectBounds(rect, offsetRectTop = true)
                            } else {
                                val matrix = Matrix()
                                matrix.setTranslate(0f, 0f)
                                matrix.postScale(
                                    renderViewBox.getScaleX(),
                                    renderViewBox.getScaleY(),
                                    renderViewBox.getOriginPoint().x,
                                    renderViewBox.getOriginPoint().y
                                )
                                renderViewBox.changeRenderMatrix(matrix, true, Reason.user)
                            }
                        }
                    } else if (type == PointTouchComponent.TOUCH_TYPE_LONG_PRESS) {
                        //长按左上角
                        delegate?.let { delegate ->
                            val renderViewBox = delegate.renderViewBox
                            delegate.view.longFeedback()
                            delegate.view.showPopupMenu(R.menu.initial_menu) {
                                offsetX = delegate.initialPointComponent.pointRect.right.toInt()
                                offsetY =
                                    -(delegate.view.mH() - delegate.initialPointComponent.pointRect.bottom.toInt())
                                menuItemClickAction = {
                                    when (it.itemId) {
                                        R.id.menu_clear -> delegate.removeAllElementRenderer(Reason.user)
                                        R.id.menu_reset -> renderViewBox.reset()
                                        R.id.menu_best -> onPointTouchEvent(
                                            component,
                                            PointTouchComponent.TOUCH_TYPE_CLICK
                                        )

                                        R.id.menu_ratio_1 -> renderViewBox.scaleTo(1f, 1f)
                                        R.id.menu_origin -> renderViewBox.translateTo(0f, 0f)
                                    }
                                    true
                                }
                            }
                        }
                    }
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

    /**隐藏或者显示图层布局*/
    fun showLayerLayout(visible: Boolean) {
        _rootViewHolder?.visible(R.id.canvas_layer_layout, visible)
        if (!visible) {
            if (!isInPadMode()) {
                renderFragment.engraveFlowLayoutHelper.hideIfInEngraveItemParamsConfig()
            }
            findTagItem(ControlLayerItem.TAG_LAYER_ITEM)?.updateItemSelected(false)
        }
    }

    /**显示/更新图层item*/
    fun renderLayerListLayout() {
        val vh = _rootViewHolder ?: return
        val delegate = delegate ?: return

        if (!vh.isVisible(R.id.canvas_layer_layout)) {
            return
        }

        _layerTabLayout = vh.v(R.id.layer_tab_view)
        _layerTabLayout?.configTabLayoutConfig {
            onSelectIndexChange = { fromIndex, selectIndexList, reselect, fromUser ->
                val toIndex = selectIndexList.firstOrNull() ?: 0
                if (toIndex != 1) {
                    if (!isInPadMode()) {
                        renderFragment.engraveFlowLayoutHelper.hideIfInEngraveItemParamsConfig()
                    }
                }
                vh.post {
                    //刷新界面
                    renderLayerListLayout()
                }
            }
        }
        val tabIndex = _layerTabLayout?.currentItemIndex ?: 0

        vh.canvasLayerRv?.apply {
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
                                    delegate.renderManager.arrangeElementSortWith(
                                        list,
                                        Reason.user,
                                        Strategy.normal
                                    )
                                }
                            }
                        }
                }
                renderDslAdapter {
                    hookUpdateDepend(this)
                    val allElementRendererList =
                        delegate.renderManager.getAllElementRendererList(false, false)
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
                    LayerHelper.getEngraveLayerList().forEach { layerInfo ->
                        CanvasLayerNameItem()() {//雕刻图层
                            itemGroupExtend = layerInfo.isGroupExtend
                            itemChanging = true
                            itemLayerInfo = layerInfo

                            //直接加载子元素
                            val itemList = LPEngraveHelper.getLayerRendererList(delegate, layerInfo)
                                .mapTo(mutableListOf<DslAdapterItem>()) { renderer ->
                                    CanvasLayerBaseItem().apply {//元素
                                        initItem(renderer)
                                        onItemCutTypeChangeAction = {
                                            //切换类型
                                            renderLayerListLayout()
                                        }
                                        itemClick = {
                                            showItemRendererBounds()
                                            if (HawkEngraveKeys.enableItemEngraveParams) {
                                                //显示单元素雕刻参数
                                                renderFragment.engraveFlowLayoutHelper.startEngraveItemConfig(
                                                    renderFragment,
                                                    renderer
                                                )
                                            }
                                        }
                                    }
                                }
                            itemSubList.resetAll(itemList)
                        }
                    }
                }
            }
        }
        updateLayerControlLayout()
    }

    /**更新图层布局*/
    fun updateLayerListLayout() {
        val vh = _rootViewHolder ?: return
        if (vh.isVisible(R.id.canvas_layer_layout)) {
            vh.canvasLayerAdapter?.updateAllItem()
            updateLayerControlLayout()
        }
    }

    /**更新图层下面的几个控制按钮*/
    fun updateLayerControlLayout() {
        val vh = _rootViewHolder ?: return
        val delegate = delegate ?: return
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
                delegate.renderManager.removeElementRenderer(list, Reason.user, Strategy.normal)
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