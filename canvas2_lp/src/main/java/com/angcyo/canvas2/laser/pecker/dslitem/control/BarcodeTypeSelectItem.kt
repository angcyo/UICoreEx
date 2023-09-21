package com.angcyo.canvas2.laser.pecker.dslitem.control

import android.app.Dialog
import com.angcyo.bluetooth.fsc.laserpacker._deviceSettingBean
import com.angcyo.canvas2.laser.pecker.R
import com.angcyo.canvas2.laser.pecker.element.LPTextElement.Companion.toBarcodeFormat
import com.angcyo.canvas2.laser.pecker.util.lpTextElement
import com.angcyo.core.component.model.tintDrawableNight
import com.angcyo.dialog2.dslitem.itemWheelBean
import com.angcyo.dialog2.dslitem.itemWheelList
import com.angcyo.dialog2.dslitem.updateWheelSelectedIndex
import com.angcyo.dsladapter.DslAdapterItem
import com.angcyo.item.style.itemLabel
import com.angcyo.laserpacker.LPDataConstant
import com.angcyo.laserpacker.bean.LPElementBean
import com.angcyo.laserpacker.bean.initFileCacheIfNeed
import com.angcyo.library.annotation.CallPoint
import com.angcyo.library.ex._drawable
import com.angcyo.library.ex._string
import com.angcyo.library.ex.dpi
import com.angcyo.library.ex.setSize
import com.angcyo.library.ex.toStr
import com.angcyo.library.toastQQ
import com.angcyo.qrcode.canCreateBarcode
import com.angcyo.widget.DslViewHolder
import com.angcyo.widget.span.span
import com.google.zxing.BarcodeFormat
import com.google.zxing.aztec.encoder.Encoder
import com.hingin.umeng.UMEvent
import com.hingin.umeng.umengEventValue

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

        if (itemElementBean?.is2DCodeElement == true) {
            itemWheelList = _deviceSettingBean?.barcode2DTypeList
        } else {
            itemWheelList = _deviceSettingBean?.barcode1DTypeList
        }

        updateBarcodeTypeLabel()
    }

    @CallPoint
    fun updateBarcodeTypeLabel() {
        updateWheelSelectedIndex(itemElementBean?.coding)

        //标识变量元素icon
        itemLabel = span {
            if (_deviceSettingBean?.showVariableElementIco == true && itemRenderDelegate != null) {
                if (itemElementBean?.is1DCodeElement == true) {
                    appendDrawable(
                        _drawable(R.drawable.canvas_var_barcode_ico)?.tintDrawableNight()
                            ?.setSize(14 * dpi)
                    )
                    append(" ")
                } else if (itemElementBean?.is2DCodeElement == true) {
                    appendDrawable(
                        _drawable(R.drawable.canvas_var_qrcode_ico)?.tintDrawableNight()
                            ?.setSize(14 * dpi)
                    )
                    append(" ")
                }
            }
            append(_string(R.string.variable_file_type))
        }
        updateAdapterItem()
    }

    override fun onItemChangeListener(item: DslAdapterItem) {
        super.onItemChangeListener(item)
        val textElement = itemRenderer?.lpTextElement()

        if (textElement == null) {
            itemElementBean?.let {
                updateElementBean(it)
            }
        } else {
            textElement.updateVariableTextProperty(itemRenderer, itemRenderDelegate) {
                updateElementBean(elementBean)
            }
        }
    }

    private fun updateElementBean(bean: LPElementBean) {
        bean.coding = itemWheelBean()

        bean.coding?.let { coding ->
            if (bean.is2DCodeElement) {
                UMEvent.CANVAS_VARIABLE_QRCODE.umengEventValue {
                    put(UMEvent.KEY_BARCODE_TYPE, coding)
                }
            } else if (bean.is1DCodeElement) {
                UMEvent.CANVAS_VARIABLE_BARCODE.umengEventValue {
                    put(UMEvent.KEY_BARCODE_TYPE, coding)
                }
            }
        }

        bean.initBarcodeIfNeed()
    }

    override fun onSelfInterceptWheelItemSelector(dialog: Dialog, index: Int, item: Any): Boolean {
        val content = itemElementBean?.getVariableText()
        val coding = item.toStr()
        if (coding.canCreateBarcode(content)) {
            return super.onSelfInterceptWheelItemSelector(dialog, index, item)
        }
        toastQQ(_string(R.string.variable_not_support_barcode_type))
        return true
    }
}

/**初始化字段*/
fun LPElementBean.initVariableIfNeed(newKey: Boolean = false) {
    if (variables == null) {
        variables = mutableListOf()
    } else if (variables !is MutableList) {
        variables = variables?.toMutableList()
    }
    variables?.initFileCacheIfNeed(newKey)
}

/**初始化条形码基础配置*/
fun LPElementBean.initBarcodeIfNeed() {
    initVariableIfNeed()
    if (coding == null) {
        if (is2DCodeElement) {
            _deviceSettingBean?.barcode2DTypeList?.let {
                val def = BarcodeFormat.QR_CODE.toStr()
                if (it.contains(def)) {
                    coding = def
                } else {
                    coding = it.firstOrNull()
                }
            }
        } else if (is1DCodeElement) {
            _deviceSettingBean?.barcode1DTypeList?.let {
                val def = BarcodeFormat.CODE_128.toStr()
                if (it.contains(def)) {
                    coding = def
                } else {
                    coding = it.firstOrNull()
                }
            }

            textShowStyle = textShowStyle ?: LPDataConstant.TEXT_SHOW_STYLE_BOTTOM
            fontSize = _deviceSettingBean?.barcode1DTextSize ?: fontSize
            textAlign = _deviceSettingBean?.barcode1DTextAlign ?: textAlign
        }
    }
    when (toBarcodeFormat()) {
        BarcodeFormat.PDF_417 -> {
            if (errorLevel == null) {
                errorLevel = 2
            }
        }

        BarcodeFormat.AZTEC -> {
            if (errorLevel == null) {
                errorLevel = Encoder.DEFAULT_EC_PERCENT
            }
        }

        else -> Unit
    }
}