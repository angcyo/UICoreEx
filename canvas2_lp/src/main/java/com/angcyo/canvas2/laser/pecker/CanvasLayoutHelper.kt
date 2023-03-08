package com.angcyo.canvas2.laser.pecker

import com.angcyo.canvas.render.core.CanvasUndoManager
import com.angcyo.canvas.render.core.ICanvasRenderListener
import com.angcyo.canvas.render.core.Reason
import com.angcyo.canvas.render.core.component.CanvasRenderProperty
import com.angcyo.canvas.render.core.component.CanvasSelectorComponent
import com.angcyo.canvas.render.renderer.BaseRenderer
import com.angcyo.canvas2.laser.pecker.dslitem.*
import com.angcyo.dsladapter.DslAdapter
import com.angcyo.dsladapter.DslAdapterItem
import com.angcyo.dsladapter.findItemByTag
import com.angcyo.dsladapter.item.IFragmentItem
import com.angcyo.dsladapter.updateItemSelected
import com.angcyo.engrave.IEngraveCanvasFragment
import com.angcyo.engrave.data.HawkEngraveKeys
import com.angcyo.http.rx.doMain
import com.angcyo.library.annotation.CallPoint
import com.angcyo.library.component.pad.isInPadMode
import com.angcyo.library.ex._string
import com.angcyo.library.ex.have
import com.angcyo.library.ex.isShowDebug
import com.angcyo.library.ex.size
import com.angcyo.widget.DslViewHolder
import com.angcyo.widget.base.resetDslItem
import com.angcyo.widget.recycler.renderDslAdapter

/**
 * 画板界面
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2023/03/03
 */
class CanvasLayoutHelper(val canvasFragment: IEngraveCanvasFragment) {

    //region ---基础---

    /**调用入口*/
    @CallPoint
    fun bindCanvasLayout(vh: DslViewHolder) {
        _rootViewHolder = vh

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
                itemCanvasLayoutHelper = this@CanvasLayoutHelper
            }

            if (!closeCanvasItemsFun.have("_layer_")) {
                if (isInPadMode()) {
                    //updateLayerListLayout(vh, canvasView)
                } else {
                    ControlLayerItem()() {
                        initItem()
                    }
                }
            }
            /*if (isDebugType()) {
                if (!closeCanvasItemsFun.have("_operate_")) {
                    CanvasControlItem2()() {
                        itemIco = R.drawable.canvas_actions_ico
                        itemText = _string(R.string.canvas_operate)
                        itemEnable = true
                        itemClick = {
                            engraveCanvasFragment.engraveFlowLayoutHelper.startPreview(
                                engraveCanvasFragment
                            )
                        }
                    }
                }
            }*/
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
    val canvasControlHelper = CanvasControlHelper(this)

    internal var _rootViewHolder: DslViewHolder? = null

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
        itemRenderDelegate = _rootViewHolder?.canvasDelegate
    }

    /**素材item, 暴露给外部配置*/
    var materialCanvasItem: CanvasIconItem = AddMaterialItem()

    /**查找item*/
    fun findTagItem(tag: String): DslAdapterItem? {
        return _rootViewHolder?.canvasItemAdapter?.findItemByTag(tag)
    }

    /**绑定事件*/
    private fun bindCanvasListener() {
        _rootViewHolder?.canvasDelegate?.addCanvasRenderListener(object : ICanvasRenderListener {
            override fun onRenderUndoChange(undoManager: CanvasUndoManager) {
                updateUndoLayout()
            }

            override fun onSelectorRendererChange(
                selectorComponent: CanvasSelectorComponent,
                from: List<BaseRenderer>,
                to: List<BaseRenderer>
            ) {
                canvasControlHelper.bindControlLayout()
            }

            override fun onRendererFlagsChange(
                renderer: BaseRenderer,
                oldFlags: Int,
                newFlags: Int,
                reason: Reason
            ) {
                if (renderer is CanvasSelectorComponent) {
                    if (reason.renderFlag == BaseRenderer.RENDERER_FLAG_LOCK_SCALE) {
                        //锁的状态改变
                        canvasControlHelper.updateControlLayout()
                    }
                }
            }

            override fun onRendererPropertyChange(
                renderer: BaseRenderer,
                fromProperty: CanvasRenderProperty?,
                toProperty: CanvasRenderProperty?,
                reason: Reason
            ) {
                canvasControlHelper.updateControlLayout()
            }
        })
    }

    //endregion ---init---

    //<editor-fold desc="Undo">

    val undoManager: CanvasUndoManager?
        get() = _rootViewHolder?.canvasDelegate?.undoManager

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

    //</editor-fold desc="Undo">


}