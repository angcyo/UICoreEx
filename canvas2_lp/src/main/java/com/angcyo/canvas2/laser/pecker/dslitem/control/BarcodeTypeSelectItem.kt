package com.angcyo.canvas2.laser.pecker.dslitem.control

import com.angcyo.bluetooth.fsc.laserpacker._deviceSettingBean
import com.angcyo.canvas2.laser.pecker.R
import com.angcyo.canvas2.laser.pecker.element.LPTextElement.Companion.toBarcodeFormat
import com.angcyo.canvas2.laser.pecker.util.lpTextElement
import com.angcyo.dialog2.dslitem.itemWheelBean
import com.angcyo.dialog2.dslitem.itemWheelList
import com.angcyo.dialog2.dslitem.updateWheelSelectedIndex
import com.angcyo.dsladapter.DslAdapterItem
import com.angcyo.item.style.itemLabel
import com.angcyo.library.ex._drawable
import com.angcyo.library.ex._string
import com.angcyo.library.ex.dpi
import com.angcyo.library.ex.setSize
import com.angcyo.widget.DslViewHolder
import com.angcyo.widget.span.span
import com.google.zxing.BarcodeFormat
import com.google.zxing.aztec.encoder.Encoder

/**
 * 1D/2D条形码类型切换item
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2023/09/09
 */
class BarcodeTypeSelectItem : BaseBarcodePropertyControlItem() {

    init {
        itemClick = {
            showItemWheelDialog(it.context)
        }
    }

    override fun onItemBind(
        itemHolder: DslViewHolder,
        itemPosition: Int,
        adapterItem: DslAdapterItem,
        payloads: List<Any>
    ) {
        super.onItemBind(itemHolder, itemPosition, adapterItem, payloads)
    }

    override fun onSelfSetItemData(data: Any?) {
        super.onSelfSetItemData(data)

        if (elementBean?.is2DCodeElement == true) {
            itemWheelList = _deviceSettingBean?.barcode2DTypeList
        } else {
            itemWheelList = _deviceSettingBean?.barcode1DTypeList
        }

        updateWheelSelectedIndex(elementBean?.coding)

        //标识变量元素icon
        itemLabel = span {
            if (elementBean?.is1DCodeElement == true) {
                appendDrawable(_drawable(R.drawable.canvas_var_barcode_ico)?.setSize(14 * dpi))
                append(" ")
            } else if (elementBean?.is2DCodeElement == true) {
                appendDrawable(_drawable(R.drawable.canvas_var_qrcode_ico)?.setSize(14 * dpi))
                append(" ")
            }
            append(_string(R.string.variable_file_type))
        }
    }

    override fun onItemChangeListener(item: DslAdapterItem) {
        super.onItemChangeListener(item)

        itemRenderer?.lpTextElement()
            ?.updateVariableTextProperty(itemRenderer, itemRenderDelegate) {
                elementBean.coding = itemWheelBean()

                when (elementBean.toBarcodeFormat()) {
                    BarcodeFormat.PDF_417 -> {
                        if (elementBean.errorLevel == null) {
                            elementBean.errorLevel = 2
                        }
                    }

                    BarcodeFormat.AZTEC -> {
                        if (elementBean.errorLevel == null) {
                            elementBean.errorLevel = Encoder.DEFAULT_EC_PERCENT
                        }
                    }

                    else -> Unit
                }
            }
    }

}