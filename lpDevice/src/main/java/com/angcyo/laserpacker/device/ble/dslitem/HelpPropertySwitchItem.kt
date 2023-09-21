package com.angcyo.laserpacker.device.ble.dslitem

import com.angcyo.bluetooth.fsc.laserpacker.LaserPeckerConfigHelper
import com.angcyo.dialog.messageDialog
import com.angcyo.dsladapter.DslAdapterItem
import com.angcyo.github.SwitchButton
import com.angcyo.item.DslPropertySwitchItem
import com.angcyo.item.style.itemLabel
import com.angcyo.laserpacker.device.R
import com.angcyo.library.ex._color
import com.angcyo.library.ex._drawable
import com.angcyo.library.ex._string
import com.angcyo.library.ex.hawkGetBoolean
import com.angcyo.library.ex.hawkPut
import com.angcyo.widget.DslViewHolder
import com.angcyo.widget.base.clickIt
import com.angcyo.widget.span.span

/**
 * 带有帮助属性的开关按钮
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2023/09/21
 */
class HelpPropertySwitchItem : DslPropertySwitchItem() {

    /**帮助链接地址*/
    var itemHelpUrl: String? = null

    /**不在提示的key*/
    var itemFlagPromptKey: String? = null

    /**是否要提示*/
    private val needFlagTip: Boolean
        get() = itemFlagPromptKey?.hawkGetBoolean() != true

    override fun onItemBind(
        itemHolder: DslViewHolder,
        itemPosition: Int,
        adapterItem: DslAdapterItem,
        payloads: List<Any>
    ) {
        super.onItemBind(itemHolder, itemPosition, adapterItem, payloads)
        itemHolder.tv(labelItemConfig.itemLabelViewId)?.apply {
            text = if (itemHelpUrl.isNullOrBlank()) {
                itemLabel
            } else {
                clickIt {
                    openFlagHelp()
                }
                span {
                    append(itemLabel)
                    append(" ")
                    appendDrawable(_drawable(R.drawable.rotate_flag_help_svg))
                }
            }
        }
    }

    //打开帮助
    private fun openFlagHelp() {
        val helpUrl = itemHelpUrl
        if (!helpUrl.isNullOrBlank()) {
            LaserPeckerConfigHelper.onOpenUrlAction?.invoke(helpUrl)
        }
    }

    override fun onSelfItemInterceptSwitchChanged(
        itemHolder: DslViewHolder,
        view: SwitchButton,
        checked: Boolean
    ): Boolean {
        return if (checked && needFlagTip && !itemHelpUrl.isNullOrBlank()) {
            itemHolder.context.messageDialog {
                dialogTitle = _string(R.string.rotate_flag_tip_title, itemLabel ?: "")
                dialogMessage = span {
                    append(_string(R.string.see_tutorial)) {
                        foregroundColor = _color(R.color.lib_link)
                    }
                }
                negativeButtonText = _string(R.string.dialog_negative)
                dialogNotPromptKey = itemFlagPromptKey

                positiveButton { dialog, dialogViewHolder ->
                    dialogNotPromptKey?.hawkPut(_dialogIsNotPrompt)
                    dialog.dismiss()
                    onSelfItemSwitchChanged(true)
                }

                dialogInitOverride = { dialog, dialogViewHolder ->
                    dialogViewHolder.click(R.id.dialog_message_view) {
                        openFlagHelp()
                    }
                }
            }
            true
        } else {
            false
        }
    }
}