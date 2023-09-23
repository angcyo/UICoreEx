package com.angcyo.usb.storage

import android.app.Dialog
import android.content.Context
import com.angcyo.dialog.DslDialogConfig
import com.angcyo.dialog.configBottomDialog
import com.angcyo.library.annotation.DSL
import com.angcyo.widget.DslViewHolder
import me.jahnen.libaums.core.fs.UsbFile

/**
 * [Dialog]载体
 * Email:angcyo@126.com
 * @author angcyo
 * @date 2023/09/23
 */
class UsbStorageFolderSelectorDialog(context: Context? = null) : DslDialogConfig(context) {

    val usbStorageFolderSelectorHelper = UsbStorageFolderSelectorHelper().apply {
        removeThisAction = {
            _dialog?.dismiss()
        }
    }

    init {
        dialogLayoutId = R.layout.lib_file_selector_fragment
    }

    override fun initDialogView(dialog: Dialog, dialogViewHolder: DslViewHolder) {
        super.initDialogView(dialog, dialogViewHolder)
        usbStorageFolderSelectorHelper.init(dialogViewHolder)
        usbStorageFolderSelectorHelper.firstLoad()
    }

    /**
     * 调用此方法, 配置参数
     * */
    fun usbSelectorConfig(config: UsbSelectorConfig.() -> Unit): UsbStorageFolderSelectorDialog {
        usbStorageFolderSelectorHelper.usbSelectorConfig.config()
        return this
    }

    override fun onDialogBackPressed(dialog: Dialog, dialogViewHolder: DslViewHolder): Boolean {
        usbStorageFolderSelectorHelper.onBackPressed()
        return true
    }
}

@DSL
fun Context.usbStorageFolderSelectorDialog(
    root: UsbFile?,
    config: UsbStorageFolderSelectorDialog.() -> Unit
): Dialog {
    return UsbStorageFolderSelectorDialog().run {
        configBottomDialog(this@usbStorageFolderSelectorDialog)
        dialogWidth = -1
        dialogHeight = -1
        dialogThemeResId = R.style.LibDialogBaseFullTheme
        usbStorageFolderSelectorHelper.usbSelectorConfig.rootDirectory = root
        config()
        show()
    }
}