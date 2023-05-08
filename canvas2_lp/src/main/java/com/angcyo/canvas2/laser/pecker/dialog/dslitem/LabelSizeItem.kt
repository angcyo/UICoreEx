package com.angcyo.canvas2.laser.pecker.dialog.dslitem

import android.widget.TextView
import com.angcyo.canvas2.laser.pecker.R
import com.angcyo.canvas2.laser.pecker.dialog.ParameterComparisonTableDialogConfig
import com.angcyo.canvas2.laser.pecker.dialog.updateTablePreview
import com.angcyo.dialog.TargetWindow
import com.angcyo.dsladapter.DslAdapterItem
import com.angcyo.item.keyboard.NumberKeyboardPopupConfig
import com.angcyo.item.keyboard.keyboardNumberWindow
import com.angcyo.library.annotation.MM
import com.angcyo.library.ex.clamp
import com.angcyo.widget.DslViewHolder

/**
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2023/04/04
 */
class LabelSizeItem : DslAdapterItem() {

    /**字体大小*/
    @MM
    var itemTextFontSize: Float = 8f

    /**格子的边距*/
    @MM
    var itemGridItemMargin: Float = 2f

    /**回调*/
    var onItemChangeAction: () -> Unit = { }

    init {
        itemLayoutId = R.layout.item_label_size_layout
    }

    override fun onItemBind(
        itemHolder: DslViewHolder,
        itemPosition: Int,
        adapterItem: DslAdapterItem,
        payloads: List<Any>
    ) {
        super.onItemBind(itemHolder, itemPosition, adapterItem, payloads)
        itemHolder.tv(R.id.font_size_view)?.text = "${itemTextFontSize.toInt()}"
        itemHolder.tv(R.id.grid_margin_view)?.text = "${itemGridItemMargin.toInt()}"
        itemHolder.tv(R.id.char_space_view)?.text =
            "${ParameterComparisonTableDialogConfig.ptcCharSpace}"

        itemHolder.click(R.id.font_size_view) {
            itemHolder.context.keyboardNumberWindow(it) {
                numberItemSize = ParameterComparisonTableDialogConfig.keyboardNumSize
                onDismiss = this@LabelSizeItem::onPopupDismiss
                keyboardBindTextView = it as? TextView
                bindPendingDelay = -1 //关闭限流输入
                removeKeyboardStyle(NumberKeyboardPopupConfig.STYLE_DECIMAL)
                removeKeyboardStyle(NumberKeyboardPopupConfig.STYLE_INCREMENT)
                onNumberResultAction = { value ->
                    val v = clamp(value, 1f, 100f)
                    itemTextFontSize = v
                    onItemChangeAction()
                }
            }
        }

        itemHolder.click(R.id.char_space_view) {
            itemHolder.context.keyboardNumberWindow(it) {
                numberItemSize = ParameterComparisonTableDialogConfig.keyboardNumSize
                onDismiss = this@LabelSizeItem::onPopupDismiss
                keyboardBindTextView = it as? TextView
                bindPendingDelay = -1 //关闭限流输入
                removeKeyboardStyle(NumberKeyboardPopupConfig.STYLE_INCREMENT)
                onNumberResultAction = { value ->
                    ParameterComparisonTableDialogConfig.ptcCharSpace = value
                    onItemChangeAction()
                }
            }
        }

        itemHolder.click(R.id.grid_margin_view) {
            itemHolder.context.keyboardNumberWindow(it) {
                numberItemSize = ParameterComparisonTableDialogConfig.keyboardNumSize
                onDismiss = this@LabelSizeItem::onPopupDismiss
                keyboardBindTextView = it as? TextView
                bindPendingDelay = -1 //关闭限流输入
                removeKeyboardStyle(NumberKeyboardPopupConfig.STYLE_DECIMAL)
                removeKeyboardStyle(NumberKeyboardPopupConfig.STYLE_INCREMENT)
                onNumberResultAction = { value ->
                    val v = clamp(value, 1f, 100f)
                    itemGridItemMargin = v
                    onItemChangeAction()
                }
            }
        }
    }

    /**popup销毁后, 刷新item*/
    fun onPopupDismiss(window: TargetWindow): Boolean {
        itemDslAdapter.updateTablePreview()
        updateAdapterItem()
        return false
    }
}