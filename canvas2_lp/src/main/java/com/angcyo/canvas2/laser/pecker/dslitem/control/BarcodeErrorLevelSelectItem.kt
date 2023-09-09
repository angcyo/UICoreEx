package com.angcyo.canvas2.laser.pecker.dslitem.control

import com.angcyo.canvas2.laser.pecker.R
import com.angcyo.canvas2.laser.pecker.element.LPTextElement.Companion.toBarcodeFormat
import com.angcyo.canvas2.laser.pecker.util.lpTextElement
import com.angcyo.dialog2.dslitem.itemWheelBean
import com.angcyo.dialog2.dslitem.itemWheelList
import com.angcyo.dialog2.dslitem.updateWheelSelectedIndex
import com.angcyo.dsladapter.DslAdapterItem
import com.angcyo.item.style.itemLabel
import com.angcyo.library.ex._string
import com.angcyo.library.ex.toStr
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel

/**
 * 错误等级切换item
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2023/09/09
 */
class BarcodeErrorLevelSelectItem : BaseBarcodePropertyControlItem() {

    init {
        itemClick = {
            showItemWheelDialog(it.context)
        }
    }

    override fun onSelfSetItemData(data: Any?) {
        super.onSelfSetItemData(data)

        val barcodeFormat = elementBean?.toBarcodeFormat()
        if (barcodeFormat == BarcodeFormat.QR_CODE) {
            itemWheelList = listOf(
                ErrorCorrectionLevel.L.toStr(),
                ErrorCorrectionLevel.M.toStr(),
                ErrorCorrectionLevel.Q.toStr(),
                ErrorCorrectionLevel.H.toStr()
            )
            updateWheelSelectedIndex(elementBean?.eclevel)
        } else if (barcodeFormat == BarcodeFormat.PDF_417) {
            itemWheelList = (0..7).toList()
            updateWheelSelectedIndex(elementBean?.errorLevel)
        } else if (barcodeFormat == BarcodeFormat.AZTEC) {
            itemWheelList = (0..100).toList()
            updateWheelSelectedIndex(elementBean?.errorLevel)
        }

        itemLabel = _string(R.string.variable_barcode_error_correction)
    }

    override fun onItemChangeListener(item: DslAdapterItem) {
        super.onItemChangeListener(item)

        itemRenderer?.lpTextElement()
            ?.updateVariableTextProperty(itemRenderer, itemRenderDelegate) {
                val barcodeFormat = elementBean.toBarcodeFormat()
                if (barcodeFormat == BarcodeFormat.QR_CODE) {
                    elementBean.eclevel = itemWheelBean()
                } else if (barcodeFormat == BarcodeFormat.PDF_417 || barcodeFormat == BarcodeFormat.AZTEC) {
                    elementBean.errorLevel = itemWheelBean()
                }
            }
    }

}