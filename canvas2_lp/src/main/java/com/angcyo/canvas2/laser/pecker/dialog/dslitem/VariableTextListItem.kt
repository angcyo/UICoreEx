package com.angcyo.canvas2.laser.pecker.dialog.dslitem

import com.angcyo.canvas2.laser.pecker.R
import com.angcyo.canvas2.laser.pecker.dialog.addVariableTextDialog
import com.angcyo.canvas2.laser.pecker.dialog.toVariableTypeIco
import com.angcyo.dsladapter.DragCallbackHelper
import com.angcyo.dsladapter.DslAdapterItem
import com.angcyo.laserpacker.LPDataConstant
import com.angcyo.laserpacker.bean.LPVariableBean
import com.angcyo.library.ex._drawable
import com.angcyo.widget.DslViewHolder
import com.angcyo.widget.span.span

/**
 * 变量文本添加后列表中的item
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2023/09/08
 */
class VariableTextListItem : DslAdapterItem() {

    /**整个元素的类型*/
    var itemVarElementType: Int = LPDataConstant.DATA_TYPE_VARIABLE_TEXT

    /**是否是编辑模式*/
    var itemEditMode: Boolean = false

    /**拖拽助手*/
    var itemDragHelper: DragCallbackHelper? = null

    /**编辑保存后的回调*/
    var itemEditChangedAction: (newBean: LPVariableBean) -> Unit = {}

    init {
        itemLayoutId = R.layout.item_var_text_list_layout
    }

    override fun onItemBind(
        itemHolder: DslViewHolder,
        itemPosition: Int,
        adapterItem: DslAdapterItem,
        payloads: List<Any>
    ) {
        super.onItemBind(itemHolder, itemPosition, adapterItem, payloads)

        val variableBean = _itemVariableBean
        itemHolder.img(R.id.lib_image_view)
            ?.setImageDrawable(variableBean?.type?.toVariableTypeIco(true, variableBean))
        itemHolder.tv(R.id.lib_text_view)?.text =
            if (variableBean?.type == LPVariableBean.TYPE_FIXED && (variableBean._isEnter || variableBean._isSpace)) {
                span {
                    if (variableBean._isEnter) {
                        appendDrawable(_drawable(R.drawable.ic_enter_svg))
                    } else if (variableBean._isSpace) {
                        appendDrawable(_drawable(R.drawable.ic_space_svg))
                    }
                }
            } else {
                variableBean?.toText()
            }
        //长按拖拽
        itemHolder.img(R.id.lib_right_ico_view)
            ?.setImageResource(if (itemEditMode) R.drawable.core_sort_svg else R.drawable.var_text_next_svg)
        if (itemEditMode) {
            //item 进入编辑模式
            itemHolder.longClick(R.id.lib_right_ico_view) {
                itemDragHelper?.startDrag(itemHolder, this)
            }
            itemHolder.longClickItem {
                itemDragHelper?.startDrag(itemHolder, this)
            }
            itemHolder.clickItem()
        } else {
            itemHolder.longClick(R.id.lib_right_ico_view)
            itemHolder.longClickItem()

            //点击编辑
            itemHolder.clickItem {
                it.context.addVariableTextDialog {
                    addVarElementType = itemVarElementType
                    editVariableBean = variableBean?.copy()
                    onApplyVariableAction = { bean ->
                        itemEditChangedAction(bean)
                    }
                }
            }
        }
    }

}