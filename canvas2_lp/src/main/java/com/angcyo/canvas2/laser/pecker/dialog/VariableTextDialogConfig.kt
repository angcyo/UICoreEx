package com.angcyo.canvas2.laser.pecker.dialog

import android.app.Dialog
import android.content.Context
import android.view.WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING
import com.angcyo.canvas2.laser.pecker.R
import com.angcyo.canvas2.laser.pecker.dialog.dslitem.VariableTextAddItem
import com.angcyo.canvas2.laser.pecker.dialog.dslitem.VariableTextEditItem
import com.angcyo.dialog.DslDialogConfig
import com.angcyo.dialog.configFullScreenDialog
import com.angcyo.dsladapter.DslAdapter
import com.angcyo.dsladapter.renderAdapterEmptyStatus
import com.angcyo.library.L
import com.angcyo.library.ex._string
import com.angcyo.widget.DslViewHolder
import com.angcyo.widget._rv
import com.angcyo.widget.recycler.renderDslAdapter

/**
 * 变量模板界面弹窗
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2023/08/30
 */
class VariableTextDialogConfig(context: Context? = null) : DslDialogConfig(context) {

    private var _adapter: DslAdapter? = null

    init {
        dialogLayoutId = R.layout.variable_text_dialog_layout
        softInputMode = SOFT_INPUT_ADJUST_NOTHING
        canceledOnTouchOutside = false
    }

    override fun initDialogView(dialog: Dialog, dialogViewHolder: DslViewHolder) {
        super.initDialogView(dialog, dialogViewHolder)
        dialogViewHolder.tv(R.id.dialog_title_view)?.text = _string(R.string.canvas_variable_text)
        dialogViewHolder.enable(R.id.dialog_positive_button, false)

        //确定
        dialogViewHolder.click(R.id.dialog_positive_button) {
            dialog.dismiss()
        }

        //back
        dialogViewHolder.click(R.id.dialog_negative_button) {
            dialog.dismiss()
        }

        //rv
        dialogViewHolder._rv(R.id.lib_recycler_view)?.renderDslAdapter {
            _adapter = this
            renderAdapterEmptyStatus(R.layout.variable_text_empty_layout)
        }

        //control
        dialogViewHolder._rv(R.id.variable_text_item_view)?.renderDslAdapter {
            VariableTextAddItem()() {
                itemClick = {
                    it.context.addVariableTextDialog {
                        onApplyVariableAction = { bean ->
                            L.w(bean)
                        }
                    }
                }
            }
            VariableTextEditItem()() {
                itemEnable = false
                itemClick = {

                }
            }
        }
    }
}

/** 底部弹出涂鸦对话框 */
fun Context.variableTextDialog(config: VariableTextDialogConfig.() -> Unit): Dialog {
    return VariableTextDialogConfig().run {
        configFullScreenDialog(this@variableTextDialog)
        config()
        show()
    }
}