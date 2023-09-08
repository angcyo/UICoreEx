package com.angcyo.canvas2.laser.pecker.dialog.dslitem

import com.angcyo.dsladapter.DslAdapter
import com.angcyo.dsladapter.DslAdapterItem
import com.angcyo.dsladapter.eachItem
import com.angcyo.item.style.itemIncrementMaxValue
import com.angcyo.item.style.itemIncrementValue
import com.angcyo.laserpacker.bean.LPVariableBean
import com.angcyo.library.ex.size
import com.angcyo.library.ex.toStr

/**
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2023/09/04
 */
abstract class BaseVarItem : DslAdapterItem()

interface IVarFileItem {

    /**当[bean]某些数据发生改变后, 更新当前的item*/
    fun updateVarFileItemFromBean(bean: LPVariableBean?) {
        if (this is VarFileLineIncrementItem) {
            itemIncrementValue = bean?.stepVal?.toStr()
            bean?.let {
                if (it.type == LPVariableBean.TYPE_EXCEL) {
                    itemIncrementMaxValue = it.getColumnDataList().size()
                } else {
                    itemIncrementMaxValue = it._txtLinesList.size()
                }
            }
        }
        if (this is VarFileLineNumberItem) {
            itemIncrementValue = bean?.current?.toStr()
            bean?.let {
                if (it.type == LPVariableBean.TYPE_EXCEL) {
                    itemIncrementMaxValue = it.getColumnDataList().size()
                } else {
                    itemIncrementMaxValue = it._txtLinesList.size()
                }
            }
        }
    }
}

/**数据结构*/
val DslAdapterItem._itemVariableBean: LPVariableBean?
    get() = itemData as? LPVariableBean

/**[com.angcyo.canvas2.laser.pecker.dialog.dslitem.IVarFileItem.updateVarFileItemFromBean]*/
fun DslAdapter.updateVarFileItem(bean: LPVariableBean?) {
    eachItem { index, dslAdapterItem ->
        if (dslAdapterItem is IVarFileItem) {
            dslAdapterItem.updateVarFileItemFromBean(bean)
        }
    }
}