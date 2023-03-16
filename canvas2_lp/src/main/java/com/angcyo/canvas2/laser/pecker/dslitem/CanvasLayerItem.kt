package com.angcyo.canvas2.laser.pecker.dslitem

import com.angcyo.canvas.render.core.Reason
import com.angcyo.canvas2.laser.pecker.R
import com.angcyo.canvas2.laser.pecker.util.lpElementBean
import com.angcyo.dialog.inputDialog
import com.angcyo.dsladapter.DslAdapterItem
import com.angcyo.library.ex._string
import com.angcyo.widget.DslViewHolder

/**
 * 图层item
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2022/05/09
 */
class CanvasLayerItem : CanvasBaseLayerItem() {

    //region ---core---

    /**排序事件*/
    var itemSortAction: ((DslViewHolder) -> Unit)? = null

    /**是否要显示可见性view, 在touch多选item时, 不需要显示*/
    var itemShowSeeView = true

    /**是否要显示lock view, 在touch多选item时, 不需要显示*/
    var itemShowLockView = true

    //endregion ---core---

    init {
        itemLayoutId = R.layout.item_canvas_layer_layout

        itemClick = {
            showItemRendererBounds()
        }

        //长按重命名
        itemLongClick = { view ->
            itemRenderer?.lpElementBean()?.let { bean ->
                view.context.inputDialog {
                    dialogTitle = _string(R.string.canvas_rename)
                    maxInputLength = 10
                    defaultInputString = itemItemName
                    onInputResult = { dialog, inputText ->
                        bean.name = "$inputText"
                        updateAdapterItem()
                        false
                    }
                }
            }
            true
        }
    }

    override fun onItemBind(
        itemHolder: DslViewHolder,
        itemPosition: Int,
        adapterItem: DslAdapterItem,
        payloads: List<Any>
    ) {
        super.onItemBind(itemHolder, itemPosition, adapterItem, payloads)

        //高亮选中的item
        itemIsSelected = itemRenderDelegate?.selectorManager?.getSelectorRendererList()
            ?.contains(itemRenderer) == true

        itemHolder.visible(R.id.background_view, itemIsSelected)
        itemHolder.visible(R.id.layer_item_sort_view, itemSortAction != null)

        //可见性
        itemHolder.selected(R.id.layer_item_invisible_view, !itemLayerHide)
        itemHolder.invisible(R.id.layer_item_invisible_view, !itemShowSeeView)

        //锁定
        itemHolder.selected(R.id.layer_item_lock_view, itemLayerLock)
        itemHolder.invisible(R.id.layer_item_lock_view, !itemShowLockView)

        //
        itemHolder.selected(R.id.lib_check_view, itemIsSelected)

        //事件
        itemHolder.longClick(R.id.layer_item_sort_view) {
            //排序
            itemSortAction?.invoke(itemHolder)
        }
        itemHolder.click(R.id.layer_item_invisible_view) {
            //可见
            itemRenderer?.updateVisible(!it.isSelected, Reason.user, itemRenderDelegate)
            updateAdapterItem()
        }
        itemHolder.click(R.id.layer_item_lock_view) {
            //锁定
            itemRenderer?.updateLock(!it.isSelected, Reason.user, itemRenderDelegate)
            updateAdapterItem()
        }

        itemHolder.click(R.id.lib_check_view) {
            itemRenderer?.let { renderer ->
                if (renderer.isVisible) {
                    //可见状态下, 才允许选中
                    itemIsSelected = !itemIsSelected
                    if (itemIsSelected) {
                        itemRenderDelegate?.selectorManager?.addSelectorRenderer(
                            renderer,
                            Reason.user
                        )
                    } else {
                        itemRenderDelegate?.selectorManager?.removeSelectorRenderer(
                            renderer,
                            Reason.user
                        )
                    }
                    updateAdapterItem()
                }
            }
        }
    }

}