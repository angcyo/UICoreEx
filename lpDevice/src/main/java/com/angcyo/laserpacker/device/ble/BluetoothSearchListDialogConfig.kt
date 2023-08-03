package com.angcyo.laserpacker.device.ble

import android.app.Dialog
import android.content.Context
import com.angcyo.dialog.BaseDialogConfig
import com.angcyo.dialog.configBottomDialog
import com.angcyo.laserpacker.device.R
import com.angcyo.library.annotation.DSL
import com.angcyo.library.ex._string
import com.angcyo.widget.DslViewHolder

/**
 * SPP模式蓝牙搜索列表界面
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2022/05/26
 */
class BluetoothSearchListDialogConfig(context: Context? = null) : BaseDialogConfig(context) {

    /**连接成功后, 是否关闭界面*/
    var connectedDismiss: Boolean
        get() = bluetoothSearchHelper.connectedDismiss
        set(value) {
            bluetoothSearchHelper.connectedDismiss = value
        }

    /**蓝牙搜索布局助手*/
    val bluetoothSearchHelper = BluetoothSearchHelper()

    init {
        dialogLayoutId = R.layout.dialog_bluetooth_search_list_layout
        dialogTitle = _string(R.string.discover_devices)
    }

    override fun initDialogView(dialog: Dialog, dialogViewHolder: DslViewHolder) {
        super.initDialogView(dialog, dialogViewHolder)
        //初始化
        bluetoothSearchHelper.initLayout(this, dialogViewHolder, dialog)
    }

    override fun onDialogCancel(dialog: Dialog, dialogViewHolder: DslViewHolder) {
        super.onDialogCancel(dialog, dialogViewHolder)
    }

    override fun onDialogDestroy(dialog: Dialog, dialogViewHolder: DslViewHolder) {
        super.onDialogDestroy(dialog, dialogViewHolder)
        bluetoothSearchHelper.stopScan()
    }
}

/**蓝牙搜索列表对话框*/
@DSL
fun Context.bluetoothSearchListDialog(config: BluetoothSearchListDialogConfig.() -> Unit) {
    return BluetoothSearchListDialogConfig(this).run {
        configBottomDialog(this@bluetoothSearchListDialog)
        // dialogThemeResId = R.style.LibDialogBaseFullTheme
        config()
        show()
    }
}

