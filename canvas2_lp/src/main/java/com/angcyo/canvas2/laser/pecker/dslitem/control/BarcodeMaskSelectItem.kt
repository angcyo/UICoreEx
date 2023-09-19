package com.angcyo.canvas2.laser.pecker.dslitem.control

import com.angcyo.canvas2.laser.pecker.R
import com.angcyo.canvas2.laser.pecker.util.lpTextElement
import com.angcyo.dialog2.dslitem.itemWheelBean
import com.angcyo.dialog2.dslitem.itemWheelList
import com.angcyo.dialog2.dslitem.itemWheelToTextAction
import com.angcyo.dialog2.dslitem.updateWheelSelectedIndex
import com.angcyo.dsladapter.DslAdapterItem
import com.angcyo.item.style.itemLabel
import com.angcyo.library.ex._string
import com.angcyo.library.ex.toStr

/**
 * 二维码掩码切换item
 * [com.angcyo.qrcode.code.QrCodeConfig]
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2023/09/09
 */
class BarcodeMaskSelectItem : BaseBarcodePropertyControlItem() {

    /**选项列表*/
    val itemMaskList = mutableListOf(-1, 0, 1, 2, 3, 4, 5, 6, 7)

    init {
        itemWheelToTextAction = {
            if (it is Number && it == -1) "Auto" else it.toStr()
        }
        itemClick = {
            showItemWheelDialog(it.context)
        }
    }

    override fun onSelfSetItemData(data: Any?) {
        super.onSelfSetItemData(data)
        itemWheelList = itemMaskList
        updateWheelSelectedIndex(_elementBean?.qrMaskPattern ?: -1)
        itemLabel = _string(R.string.variable_barcode_mask)
    }

    override fun onItemChangeListener(item: DslAdapterItem) {
        super.onItemChangeListener(item)

        itemRenderer?.lpTextElement()
            ?.updateVariableTextProperty(itemRenderer, itemRenderDelegate) {
                elementBean.qrMaskPattern = itemWheelBean()
            }
    }

}