package com.angcyo.canvas2.laser.pecker.dialog.dslitem

import android.widget.TextView
import com.angcyo.canvas2.laser.pecker.R
import com.angcyo.dialog.TargetWindow
import com.angcyo.dsladapter.DslAdapterItem
import com.angcyo.item.keyboard.NumberKeyboardPopupConfig
import com.angcyo.item.keyboard.keyboardNumberWindow
import com.angcyo.library.ex.Action
import com.angcyo.library.ex.clamp
import com.angcyo.widget.DslViewHolder

/**
 * 材质参数对照表-横纵格子数量item
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2023/04/04
 */
class GridCountItem : DslAdapterItem() {

    /**列数*/
    var itemColumns: Int = 10

    /**行数*/
    var itemRows: Int = 10

    /**阈值*/
    var itemPowerDepthThreshold: Float = 2400f

    /**回调*/
    var onItemChangeAction: Action = {}

    init {
        itemLayoutId = R.layout.item_grid_count_layout
    }

    override fun onItemBind(
        itemHolder: DslViewHolder,
        itemPosition: Int,
        adapterItem: DslAdapterItem,
        payloads: List<Any>
    ) {
        super.onItemBind(itemHolder, itemPosition, adapterItem, payloads)
        itemHolder.tv(R.id.columns_text_view)?.text = "$itemColumns"
        itemHolder.tv(R.id.rows_text_view)?.text = "$itemRows"
        itemHolder.tv(R.id.threshold_text_view)?.text = "${itemPowerDepthThreshold.toInt()}"

        itemHolder.click(R.id.columns_text_view) {
            itemHolder.context.keyboardNumberWindow(it) {
                onDismiss = this@GridCountItem::onPopupDismiss
                keyboardBindTextView = it as? TextView
                bindPendingDelay = -1 //关闭限流输入
                removeKeyboardStyle(NumberKeyboardPopupConfig.STYLE_DECIMAL)
                onNumberResultAction = { value ->
                    val count = clamp(value.toInt(), 1, 100)
                    itemColumns = count
                    onItemChangeAction()
                }
            }
        }

        itemHolder.click(R.id.rows_text_view) {
            itemHolder.context.keyboardNumberWindow(it) {
                onDismiss = this@GridCountItem::onPopupDismiss
                keyboardBindTextView = it as? TextView
                bindPendingDelay = -1 //关闭限流输入
                removeKeyboardStyle(NumberKeyboardPopupConfig.STYLE_DECIMAL)
                onNumberResultAction = { value ->
                    val count = clamp(value.toInt(), 1, 100)
                    itemRows = count
                    onItemChangeAction()
                }
            }
        }

        itemHolder.click(R.id.threshold_text_view) {
            itemHolder.context.keyboardNumberWindow(it) {
                onDismiss = this@GridCountItem::onPopupDismiss
                keyboardBindTextView = it as? TextView
                bindPendingDelay = -1 //关闭限流输入
                removeKeyboardStyle(NumberKeyboardPopupConfig.STYLE_DECIMAL)
                onNumberResultAction = { value ->
                    val count = clamp(value, 1f, 99999f)
                    itemPowerDepthThreshold = count
                    onItemChangeAction()
                }
            }
        }
    }

    /**popup销毁后, 刷新item*/
    fun onPopupDismiss(window: TargetWindow): Boolean {
        updateAdapterItem()
        return false
    }

}