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

    private val L = ErrorCorrectionLevel.L.toStr()
    private val M = ErrorCorrectionLevel.M.toStr()
    private val Q = ErrorCorrectionLevel.Q.toStr()
    private val H = ErrorCorrectionLevel.H.toStr()


    /**条码格式*/
    val barcodeFormat: BarcodeFormat?
        get() = _elementBean?.toBarcodeFormat()

    init {
        itemClick = {
            showItemWheelDialog(it.context)
        }
        /*itemWheelToTextAction = {
            if (barcodeFormat == BarcodeFormat.QR_CODE) {
                if (it == L) {
                    "$L ~7%"
                } else if (it == M) {
                    "$M ~15%"
                } else if (it == Q) {
                    "$Q ~25%"
                } else if (it == H) {
                    "$H ~30%"
                } else {
                    it.toStr()
                }
            } else {
                it.toStr()
            }
        }*/
    }

    override fun onSelfSetItemData(data: Any?) {
        super.onSelfSetItemData(data)

        val barcodeFormat = barcodeFormat
        if (barcodeFormat == BarcodeFormat.QR_CODE) {
            itemWheelList = listOf(L, M, Q, H)
            updateWheelSelectedIndex(_elementBean?.eclevel)
        } else if (barcodeFormat == BarcodeFormat.PDF_417) {
            itemWheelList = (0..7).toList()
            updateWheelSelectedIndex(_elementBean?.errorLevel)
        } else if (barcodeFormat == BarcodeFormat.AZTEC) {
            itemWheelList = (0..100).toList()
            updateWheelSelectedIndex(_elementBean?.errorLevel)
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