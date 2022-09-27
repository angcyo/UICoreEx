package com.angcyo.engrave.dslitem.engrave

import android.widget.TextView
import com.angcyo.library.unit.InchValueUnit
import com.angcyo.library.unit.MmValueUnit
import com.angcyo.canvas.utils.CanvasConstant
import com.angcyo.canvas.utils.canvasDecimal
import com.angcyo.dsladapter.DslAdapterItem
import com.angcyo.engrave.R
import com.angcyo.engrave.data.EngraveOptionInfo
import com.angcyo.engrave.data.HawkEngraveKeys
import com.angcyo.item.keyboard.keyboardNumberWindow
import com.angcyo.item.style.ITextItem
import com.angcyo.item.style.TextItemConfig
import com.angcyo.item.style.itemText
import com.angcyo.library.ex._string
import com.angcyo.widget.DslViewHolder
import com.angcyo.widget.base.bindInRootView

/**
 * 雕刻物理直径item
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2022/07/14
 */
class EngraveOptionDiameterItem : DslAdapterItem(), ITextItem {

    /**雕刻选项*/
    var itemEngraveOptionInfo: EngraveOptionInfo? = null

    override var textItemConfig: TextItemConfig = TextItemConfig()

    init {
        itemText = "${_string(R.string.object_diameter)}:"
        itemLayoutId = R.layout.item_engrave_data_diameter
    }

    override fun onItemBind(
        itemHolder: DslViewHolder,
        itemPosition: Int,
        adapterItem: DslAdapterItem,
        payloads: List<Any>
    ) {
        super.onItemBind(itemHolder, itemPosition, adapterItem, payloads)

        val valueUnit = CanvasConstant.valueUnit
        itemHolder.tv(R.id.diameter_text_view)?.text = itemEngraveOptionInfo?.diameterPixel?.run {
            valueUnit.convertPixelToValue(this).canvasDecimal(2)
        } ?: "0"

        itemHolder.tv(R.id.diameter_unit_view)?.text = when (valueUnit) {
            is InchValueUnit -> "in"
            is MmValueUnit -> "mm"
            else -> "pixel"
        }

        bindDiameter(itemHolder)
    }

    /**绑定事件*/
    fun bindDiameter(itemHolder: DslViewHolder) {
        itemHolder.click(R.id.diameter_text_view) {
            if (!itemHolder.isInRecyclerView()) {
                bindInRootView(itemHolder.itemView)
            }

            itemHolder.context.keyboardNumberWindow(it) {
                onDismiss = {
                    if (itemHolder.isInRecyclerView()) {
                        updateAdapterItem()
                    } else {
                        bindInRootView(itemHolder.itemView)
                    }
                    false
                }
                keyboardBindTextView = it as? TextView
                onNumberResultAction = { number ->
                    val value = CanvasConstant.valueUnit.convertValueToPixel(number)
                    itemEngraveOptionInfo?.diameterPixel = value
                    HawkEngraveKeys.lastDiameterPixel = value

                    //通知item改变
                    itemChanging = true
                }
            }
        }
    }
}