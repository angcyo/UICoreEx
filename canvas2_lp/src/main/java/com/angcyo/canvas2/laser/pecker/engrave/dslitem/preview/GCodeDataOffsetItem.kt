package com.angcyo.canvas2.laser.pecker.engrave.dslitem.preview

import android.widget.TextView
import androidx.annotation.Keep
import com.angcyo.bluetooth.fsc.laserpacker.HawkEngraveKeys
import com.angcyo.bluetooth.fsc.laserpacker.deviceSettingBean
import com.angcyo.canvas2.laser.pecker.R
import com.angcyo.core.component.model.LanguageModel
import com.angcyo.dialog.TargetWindow
import com.angcyo.dsladapter.DslAdapterItem
import com.angcyo.item.keyboard.NumberKeyboardPopupConfig
import com.angcyo.item.keyboard.keyboardNumberWindow
import com.angcyo.library.annotation.Pixel
import com.angcyo.library.component.hawk.HawkPropertyValue
import com.angcyo.library.unit.IValueUnit
import com.angcyo.library.unit.unitDecimal
import com.angcyo.widget.DslViewHolder

/**
 * 画笔模块, 校准偏移item
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2023/06/13
 */
class GCodeDataOffsetItem : DslAdapterItem() {

    @Keep
    companion object {

        /**校准之后, GCode数据, 额外需要偏移的距离*/
        @Pixel
        var offsetLeft: Float by HawkPropertyValue<Any, Float>(0f)

        @Pixel
        var offsetTop: Float by HawkPropertyValue<Any, Float>(0f)

        /**打开url*/
        var onOpenUrlAction: ((url: String) -> Unit)? = null
    }

    init {
        itemLayoutId = R.layout.item_calibration_offset
    }

    override fun onItemBind(
        itemHolder: DslViewHolder,
        itemPosition: Int,
        adapterItem: DslAdapterItem,
        payloads: List<Any>
    ) {
        super.onItemBind(itemHolder, itemPosition, adapterItem, payloads)

        val unit = IValueUnit.MM_RENDER_UNIT
        itemHolder.tv(R.id.left_unit_view)?.text = unit.getUnit()
        itemHolder.tv(R.id.top_unit_view)?.text = unit.getUnit()

        itemHolder.tv(R.id.left_text_view)?.text = unit.convertPixelToValue(offsetLeft)
            .unitDecimal(HawkEngraveKeys.diameterPrecision)

        itemHolder.tv(R.id.top_text_view)?.text = unit.convertPixelToValue(offsetTop)
            .unitDecimal(HawkEngraveKeys.diameterPrecision)

        //
        bindLeft(itemHolder)
        bindtop(itemHolder)

        //
        itemHolder.click(R.id.lib_label_view) { view ->
            val config = deviceSettingBean
            val isZh = LanguageModel.getCurrentLanguage().lowercase().startsWith("zh")
            val url = if (isZh) {
                config?.dataOffsetHelpUrlZh ?: config?.dataOffsetHelpUrl
            } else {
                config?.dataOffsetHelpUrl
            }
            url?.let {
                if (it.isNotEmpty()) {
                    onOpenUrlAction?.invoke(it)
                }
            }
        }
    }

    /**左偏移*/
    fun bindLeft(itemHolder: DslViewHolder) {
        itemHolder.click(R.id.left_text_view) {
            itemHolder.context.keyboardNumberWindow(it) {
                onDismiss = this@GCodeDataOffsetItem::onPopupDismiss
                keyboardBindTextView = it as? TextView
                bindPendingDelay = -1 //关闭限流输入
                addKeyboardStyle(NumberKeyboardPopupConfig.STYLE_PLUS_MINUS)
                onNumberResultAction = { value ->
                    val x = IValueUnit.MM_RENDER_UNIT.convertValueToPixel(value)
                    offsetLeft = x
                }
            }
        }
    }

    /**上偏移*/
    fun bindtop(itemHolder: DslViewHolder) {
        itemHolder.click(R.id.top_text_view) {
            itemHolder.context.keyboardNumberWindow(it) {
                onDismiss = this@GCodeDataOffsetItem::onPopupDismiss
                keyboardBindTextView = it as? TextView
                bindPendingDelay = -1 //关闭限流输入
                addKeyboardStyle(NumberKeyboardPopupConfig.STYLE_PLUS_MINUS)
                onNumberResultAction = { value ->
                    val x = IValueUnit.MM_RENDER_UNIT.convertValueToPixel(value)
                    offsetTop = x
                }
            }
        }
    }

    /**popup销毁后, 刷新item*/
    fun onPopupDismiss(window: TargetWindow): Boolean {
        updateAdapterItem()
        itemChanging = true
        return false
    }

}