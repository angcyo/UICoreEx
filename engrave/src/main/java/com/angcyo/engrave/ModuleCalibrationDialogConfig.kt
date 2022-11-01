package com.angcyo.engrave

import android.app.Dialog
import android.content.Context
import com.angcyo.bluetooth.fsc.enqueue
import com.angcyo.bluetooth.fsc.laserpacker.command.EngravePreviewCmd
import com.angcyo.dialog.DslDialogConfig
import com.angcyo.dialog.configBottomDialog
import com.angcyo.library.annotation.DSL
import com.angcyo.widget.DslViewHolder

/**
 * 校准对话框
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2022/11/01
 */
class ModuleCalibrationDialogConfig : DslDialogConfig() {

    /**校准是否完成的回调*/
    var onModuleCalibrationAction: (Boolean) -> Unit = {}

    init {
        dialogLayoutId = R.layout.dialog_module_calibration_layout
        cancelable = false
    }

    override fun initDialogView(dialog: Dialog, dialogViewHolder: DslViewHolder) {
        super.initDialogView(dialog, dialogViewHolder)

        dialogViewHolder.click(R.id.cancel_button) {
            dialog.cancel()
        }

        dialogViewHolder.click(R.id.start_button) {
            EngravePreviewCmd.startCalibrationCmd().enqueue { bean, error ->
                if (error == null) {
                    dialogViewHolder.invisible(R.id.start_button)
                    dialogViewHolder.visible(R.id.finish_button)
                }
            }
        }

        dialogViewHolder.click(R.id.finish_button) {
            EngravePreviewCmd.finishCalibrationCmd().enqueue { bean, error ->
                if (error == null) {
                    dialog.dismiss()
                    onModuleCalibrationAction(true)
                }
            }
        }
    }

    override fun onDialogCancel(dialog: Dialog, dialogViewHolder: DslViewHolder) {
        super.onDialogCancel(dialog, dialogViewHolder)
        onModuleCalibrationAction(false)
    }
}

@DSL
fun Context.moduleCalibrationDialog(config: ModuleCalibrationDialogConfig.() -> Unit = {}): Dialog {
    return ModuleCalibrationDialogConfig().run {
        dialogContext = this@moduleCalibrationDialog
        configBottomDialog()
        config()
        show()
    }
}