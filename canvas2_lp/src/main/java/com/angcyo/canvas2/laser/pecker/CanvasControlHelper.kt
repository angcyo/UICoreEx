package com.angcyo.canvas2.laser.pecker

import android.view.ViewGroup
import com.angcyo.bluetooth.fsc.laserpacker.LaserPeckerModel
import com.angcyo.canvas.render.core.CanvasRenderDelegate
import com.angcyo.canvas.render.core.CanvasSelectorManager
import com.angcyo.canvas.render.core.component.CanvasSelectorComponent
import com.angcyo.canvas.render.renderer.BaseRenderer
import com.angcyo.canvas.render.renderer.CanvasElementRenderer
import com.angcyo.canvas.render.renderer.CanvasGroupRenderer
import com.angcyo.canvas2.laser.pecker.dslitem.ControlEditItem
import com.angcyo.canvas2.laser.pecker.dslitem.ICanvasRendererItem
import com.angcyo.canvas2.laser.pecker.dslitem.control.AlignDeviceItem
import com.angcyo.canvas2.laser.pecker.dslitem.control.EditControlItem
import com.angcyo.canvas2.laser.pecker.dslitem.control.LayerSortItem
import com.angcyo.core.vmApp
import com.angcyo.dsladapter.DslAdapter
import com.angcyo.dsladapter.DslAdapterItem
import com.angcyo.dsladapter.eachItem
import com.angcyo.dsladapter.updateItemSelected
import com.angcyo.library.annotation.CallPoint
import com.angcyo.transition.dslTransition
import com.angcyo.widget.recycler.renderDslAdapter

/**
 * 选中/取消选中元素后控制布局助手
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2023/03/08
 */
class CanvasControlHelper(val canvasLayoutHelper: CanvasLayoutHelper) {

    val canvasRenderDelegate: CanvasRenderDelegate?
        get() = canvasLayoutHelper._rootViewHolder?.canvasDelegate

    val selectorManager: CanvasSelectorManager?
        get() = canvasRenderDelegate?.selectorManager

    /**编辑item*/
    val editItem: DslAdapterItem?
        get() = canvasLayoutHelper.findTagItem(ControlEditItem.TAG_EDIT_ITEM)

    /**是否可以编辑*/
    val isEditEnable: Boolean
        get() = editItem == null || editItem?.itemEnable == true

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
        val vh = canvasLayoutHelper._rootViewHolder ?: return
        if (vh.isVisible(R.id.canvas_control_layout)) {
            vh.canvasControlAdapter?.apply {
                eachItem { index, item ->
                    if (item is ICanvasRendererItem) {
                        val selectorRenderer = selectorManager?.getTargetSelectorRenderer()
                        item.itemRenderer = selectorRenderer
                        item.itemRenderDelegate = canvasRenderDelegate
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
            canvasLayoutHelper._rootViewHolder?.apply {
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
            canvasLayoutHelper._rootViewHolder?.apply {
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
        canvasLayoutHelper._rootViewHolder?.canvasControlRv?.renderDslAdapter {
            canvasLayoutHelper.hookUpdateDepend(this)

            when (renderer) {
                //多选
                is CanvasSelectorComponent -> Unit
                //群组
                is CanvasGroupRenderer -> Unit
                //单元素
                else -> {
                    if (renderer is CanvasElementRenderer) {
                        val element = renderer.element
                    }
                }
            }

            //公共编辑
            renderCommonEditItems(renderer)
        }
    }

    //endregion ---core---

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