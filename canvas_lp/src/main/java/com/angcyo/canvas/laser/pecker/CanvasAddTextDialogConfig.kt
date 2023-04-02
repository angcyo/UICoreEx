package com.angcyo.canvas.laser.pecker

import android.app.Dialog
import android.content.Context
import android.text.InputType
import com.angcyo.dialog.InputDialogConfig
import com.angcyo.dialog.configBottomDialog
import com.angcyo.laserpacker.LPDataConstant
import com.angcyo.library.annotation.DSL
import com.angcyo.library.ex._string
import com.angcyo.library.ex.dpi
import com.angcyo.widget.DslViewHolder
import com.angcyo.widget.tab

/**添加文本/二维码/条形码
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2022/09/17
 */
class CanvasAddTextDialogConfig : InputDialogConfig() {

    companion object {
        const val MAX_INPUT_LENGTH = 100

        const val KEY_ADD_TEXT = "key_canvas_add_text"
    }

    /**
     * [text] 文本内容
     * [type] 数据类型
     * [com.angcyo.canvas.utils.CanvasConstant.DATA_TYPE_QRCODE]
     * [com.angcyo.canvas.utils.CanvasConstant.DATA_TYPE_BARCODE]
     * [com.angcyo.canvas.utils.CanvasConstant.DATA_TYPE_TEXT]
     * */
    var onAddTextAction: (text: CharSequence, type: Int) -> Unit = { _, _ ->

    }

    /**是否可以切换类型*/
    var canSwitchType: Boolean = true

    /**选中的数据类型*/
    var dataType: Int = LPDataConstant.DATA_TYPE_TEXT

    init {
        inputViewHeight = 100 * dpi
        maxInputLength = MAX_INPUT_LENGTH
        inputHistoryHawkKey = KEY_ADD_TEXT
        canInputEmpty = false
        trimInputText = false
        dialogLayoutId = R.layout.dialog_add_text_layout

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