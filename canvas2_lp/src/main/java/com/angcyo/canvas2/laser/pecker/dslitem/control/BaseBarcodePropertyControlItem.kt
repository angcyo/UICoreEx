package com.angcyo.canvas2.laser.pecker.dslitem.control

import com.angcyo.canvas.render.core.CanvasRenderDelegate
import com.angcyo.canvas.render.renderer.BaseRenderer
import com.angcyo.canvas2.laser.pecker.R
import com.angcyo.canvas2.laser.pecker.dslitem.ICanvasRendererItem
import com.angcyo.dialog2.dslitem.IWheelItem
import com.angcyo.dialog2.dslitem.WheelItemConfig
import com.angcyo.dsladapter.DslAdapterItem
import com.angcyo.item.style.ILabelItem
import com.angcyo.item.style.ITextItem
import com.angcyo.item.style.LabelItemConfig
import com.angcyo.item.style.TextItemConfig
import com.angcyo.laserpacker.bean.LPElementBean
import com.angcyo.library.ex._drawable
import com.angcyo.widget.DslViewHolder

/**
 * 条码属性修改
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2023/09/09
 */
abstract class BaseBarcodePropertyControlItem : DslAdapterItem(), ILabelItem, ITextItem, IWheelItem,
    ICanvasRendererItem {

    override var itemRenderer: BaseRenderer? = null
        set(value) {
            field = value
            onSelfSetItemData(itemData)
        }

    override var itemRenderDelegate: CanvasRenderDelegate? = null

    override var wheelItemConfig: WheelItemConfig = WheelItemConfig()
    override var labelItemConfig: LabelItemConfig = LabelItemConfig()
    override var textItemConfig: TextItemConfig = TextItemConfig()

    var itemElementBean: LPElementBean? = null
        get() = field ?: _elementBean
        set(value) {
            field = value
            onSelfSetItemData(value)
        }

    init {
        itemLayoutId = R.layout.item_barcode_property_control_layout
        textItemConfig.itemTextStyle.rightDrawable =
            _drawable(R.drawable.barcode_property_control_svg)
    }

    override fun onItemBind(
        itemHolder: DslViewHolder,
        itemPosition: Int,
        adapterItem: DslAdapterItem,
        payloads: List<Any>
    ) {
        super.onItemBind(itemHolder, itemPosition, adapterItem, payloads)
    }
}