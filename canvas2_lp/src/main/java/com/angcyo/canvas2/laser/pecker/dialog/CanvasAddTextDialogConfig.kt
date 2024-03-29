package com.angcyo.canvas2.laser.pecker.dialog

import android.app.Dialog
import android.content.Context
import android.text.InputType
import com.angcyo.bluetooth.fsc.laserpacker.HawkEngraveKeys
import com.angcyo.bluetooth.fsc.laserpacker._deviceSettingBean
import com.angcyo.canvas2.laser.pecker.R
import com.angcyo.dialog.InputDialogConfig
import com.angcyo.dialog.configBottomDialog
import com.angcyo.dialog.itemsDialog
import com.angcyo.laserpacker.LPDataConstant
import com.angcyo.library.annotation.DSL
import com.angcyo.library.ex._string
import com.angcyo.library.ex.dpi
import com.angcyo.library.ex.isDebug
import com.angcyo.widget.DslViewHolder
import com.angcyo.widget.base.setInputText
import com.angcyo.widget.tab

/**添加文本/二维码/条形码
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2023-3-13
 */
class CanvasAddTextDialogConfig : InputDialogConfig() {

    companion object {
        const val KEY_ADD_TEXT = "key_canvas_add_text"
    }

    /**
     * [text] 文本内容
     * [type] 数据类型
     * [com.angcyo.laserpacker.LPDataConstant.DATA_TYPE_QRCODE]
     * [com.angcyo.laserpacker.LPDataConstant.DATA_TYPE_BARCODE]
     * [com.angcyo.laserpacker.LPDataConstant.DATA_TYPE_TEXT]
     * */
    var onAddTextAction: (text: CharSequence, type: Int) -> Unit = { _, _ ->

    }

    /**是否可以切换类型*/
    var canSwitchType: Boolean = true

    /**选中的数据类型*/
    var dataType: Int = LPDataConstant.DATA_TYPE_TEXT

    init {
        inputViewHeight = 100 * dpi
        maxInputLength = HawkEngraveKeys.maxInputTextLengthLimit
        inputHistoryHawkKey = KEY_ADD_TEXT
        canInputEmpty = false
        trimInputText = false
        dialogLayoutId = R.layout.dialog_add_text_layout
        inputHistoryLayoutId = R.layout.app_input_history_layout
        onInputResult = { dialog, inputText ->
            onAddTextAction(inputText, dataType)
            false
        }
    }

    override fun initDialogView(dialog: Dialog, dialogViewHolder: DslViewHolder) {
        super.initDialogView(dialog, dialogViewHolder)
        dialogViewHolder.visible(R.id.lib_flow_layout, true)
        dialogViewHolder.tab(R.id.lib_tab_layout)?.apply {
            //是否激活切换
            itemEnableSelector = canSwitchType
            observeIndexChange { fromIndex, toIndex, reselect, fromUser ->
                if (!reselect) {
                    when (toIndex) {
                        1 -> {
                            //二维码 BarcodeFormat.QR_CODE
                            dataType = LPDataConstant.DATA_TYPE_QRCODE
                            inputType = InputType.TYPE_CLASS_TEXT
                            digits = null
                        }

                        2 -> {
                            //条形码 BarcodeFormat.CODE_128
                            dataType = LPDataConstant.DATA_TYPE_BARCODE
                            inputType =
                                InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS
                            digits = _string(R.string.lib_barcode_digits)
                        }

                        else -> {
                            //文本
                            dataType = LPDataConstant.DATA_TYPE_TEXT
                            inputType = InputType.TYPE_CLASS_TEXT
                            digits = null
                        }
                    }
                    updateEditView(dialogViewHolder)
                }
            }
            setCurrentItem(
                when (dataType) {
                    LPDataConstant.DATA_TYPE_QRCODE -> 1
                    LPDataConstant.DATA_TYPE_BARCODE -> 2
                    else -> 0
                }
            )
        }

        //快捷文本输入
        val quickTextList = _deviceSettingBean?.quickTextInputAction ?: emptyList()
        dialogViewHolder.visible(
            R.id.text_quick_action_view,
            isDebug() && quickTextList.isNotEmpty()
        )
        dialogViewHolder.click(R.id.text_quick_action_view) {
            it.context.itemsDialog {
                dialogTitle = "快捷指令"
                for (quickActionBean in quickTextList) {
                    addDialogItem {
                        itemText = quickActionBean.label
                        itemClick = {
                            dialogViewHolder.ev(R.id.edit_text_view)
                                ?.setInputText(quickActionBean.value, false)
                        }
                    }
                }
            }
        }
    }

    override fun initControlLayout(dialog: Dialog, dialogViewHolder: DslViewHolder) {
        super.initControlLayout(dialog, dialogViewHolder)
    }
}

@DSL
fun Context.addTextDialog(config: CanvasAddTextDialogConfig.() -> Unit): Dialog {
    return CanvasAddTextDialogConfig().run {
        dialogContext = this@addTextDialog
        configBottomDialog()
        config()
        show()
    }
}