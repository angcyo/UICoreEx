package com.angcyo.canvas2.laser.pecker.dialog.dslitem

import com.angcyo.bluetooth.fsc.laserpacker._deviceSettingBean
import com.angcyo.canvas2.laser.pecker.R
import com.angcyo.core.CoreApplication
import com.angcyo.core.component.model.LanguageModel
import com.angcyo.dsladapter.DslAdapterItem
import com.angcyo.item.style.itemLabel
import com.angcyo.library.ex._dimen
import com.angcyo.library.ex._drawable
import com.angcyo.widget.DslViewHolder

/**
 * 自定义日期格式输入item
 *
 * [VarTimeFormatInputItem]
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2023/09/06
 */
class VarDateFormatInputItem : LPSingleInputItem() {

    private val helpUrl: String?
        get() = LanguageModel.getLanguagePriorityString(
            _deviceSettingBean?.dateFormatHelpUrl,
            _deviceSettingBean?.dateFormatHelpUrlZh
        )

    init {
        itemLabel = ""
        if (!helpUrl.isNullOrBlank()) {
            labelItemConfig.itemLabelTextStyle.apply {
                paddingRight = helpIcoPaddingRight
                paddingTop = _dimen(R.dimen.lib_hdpi)
                paddingBottom = paddingTop
                rightDrawable = _drawable(R.drawable.canvas_invert_help_svg)
            }
        }
    }

    override fun onItemBind(
        itemHolder: DslViewHolder,
        itemPosition: Int,
        adapterItem: DslAdapterItem,
        payloads: List<Any>
    ) {
        super.onItemBind(itemHolder, itemPosition, adapterItem, payloads)

        if (!helpUrl.isNullOrBlank()) {
            itemHolder.click(labelItemConfig.itemLabelViewId) {
                CoreApplication.onOpenUrlAction?.invoke(helpUrl!!)
            }
        }
    }
}