package com.angcyo.engrave.dslitem.preview

import android.widget.TextView
import com.angcyo.canvas.utils.CanvasConstant
import com.angcyo.dsladapter.DslAdapterItem
import com.angcyo.engrave.R
import com.angcyo.engrave.data.HawkEngraveKeys
import com.angcyo.item.keyboard.keyboardNumberWindow
import com.angcyo.library.unit.unitDecimal
import com.angcyo.objectbox.laser.pecker.lpSaveEntity
import com.angcyo.widget.DslViewHolder

/**
 * 雕刻物理直径item
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2022/07/14
 */

class PreviewDiameterItem : BasePreviewItem() {

    init {
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
        val diameterPixel =
            itemPreviewConfigEntity?.diameterPixel ?: HawkEngraveKeys.lastDiameterPixel
        val diameterValue = valueUnit.convertPixelToValue(diameterPixel)

        itemHolder.tv(R.id.diameter_text_view)?.text = diameterValue.unitDecimal(2)
        itemHolder.tv(R.id.diameter_unit_view)?.text = valueUnit.getUnit()

        bindDiameter(itemHolder)
    }

    /**绑定事件, 物理直径输入*/
    fun bindDiameter(itemHolder: DslViewHolder) {
        itemHolder.click(R.id.diameter_text_view) {
            itemHolder.context.keyboardNumberWindow(it) {
                onDismiss = {
                    if (itemHolder.isInRecyclerView()) {
                        updateAdapterItem()
                    }
                    false
                }
                keyboardBindTextView = it as? TextView
                bindPendingDelay = -1 //关闭限流输入
                onNumberResultAction = { number ->
                    val value = CanvasConstant.valueUnit.convertValueToPixel(number)
                    itemPreviewConfigEntity?.diameterPixel = value
                    itemPreviewConfigEntity?.lpSaveEntity()
                    HawkEngraveKeys.lastDiameterPixel = value

                    //通知机器
                    previewModel.refreshPreview()
                }
            }
        }
    }
}
