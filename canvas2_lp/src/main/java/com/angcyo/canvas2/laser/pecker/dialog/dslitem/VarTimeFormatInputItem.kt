package com.angcyo.canvas2.laser.pecker.dialog.dslitem

import com.angcyo.canvas2.laser.pecker.R
import com.angcyo.dsladapter.DslAdapterItem
import com.angcyo.item.style.itemLabel
import com.angcyo.library.ex._dimen
import com.angcyo.library.ex._drawable
import com.angcyo.library.toastQQ
import com.angcyo.widget.DslViewHolder

/**
 * 自定义时间格式输入item
 *
 * [VarDateFormatInputItem]
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2023/09/06
 */
class VarTimeFormatInputItem : LPSingleInputItem() {

    init {
        itemLabel = ""
        labelItemConfig.itemLabelTextStyle.apply {
            paddingRight = _dimen(R.dimen.lib_xxhdpi)
            paddingTop = _dimen(R.dimen.lib_hdpi)
            paddingBottom = paddingTop
            rightDrawable = _drawable(R.drawable.canvas_invert_help_svg)
        }
    }

    override fun onItemBind(
        itemHolder: DslViewHolder,
        itemPosition: Int,
        adapterItem: DslAdapterItem,
        payloads: List<Any>
    ) {
        super.onItemBind(itemHolder, itemPosition, adapterItem, payloads)

        itemHolder.click(labelItemConfig.itemLabelViewId) {
            toastQQ("时间格式帮助")
        }
    }
}