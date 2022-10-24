package com.angcyo.engrave

import android.app.Dialog
import android.content.Context
import androidx.lifecycle.Lifecycle
import com.angcyo.bluetooth.fsc.enqueue
import com.angcyo.bluetooth.fsc.laserpacker.LaserPeckerModel
import com.angcyo.bluetooth.fsc.laserpacker.command.EngravePreviewCmd
import com.angcyo.bluetooth.fsc.laserpacker.command.ExitCmd
import com.angcyo.bluetooth.fsc.laserpacker.syncQueryDeviceState
import com.angcyo.canvas.data.CanvasProjectItemBean
import com.angcyo.canvas.graphics.GraphicsHelper
import com.angcyo.core.vmApp
import com.angcyo.dialog.DslDialogConfig
import com.angcyo.dialog.configBottomDialog
import com.angcyo.engrave.data.HawkEngraveKeys
import com.angcyo.engrave.data.PreviewInfo
import com.angcyo.engrave.data.TransferState
import com.angcyo.engrave.model.AutoEngraveModel
import com.angcyo.engrave.model.PreviewModel
import com.angcyo.engrave.model.TransferModel
import com.angcyo.library.annotation.DSL
import com.angcyo.library.component._delay
import com.angcyo.library.ex.uuid
import com.angcyo.library.toast
import com.angcyo.objectbox.laser.pecker.entity.TransferDataEntity
import com.angcyo.widget.DslViewHolder

/**
 * 路径预览对话框
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2022/10/21
 */
class PathPreviewDialogConfig : DslDialogConfig() {

    /**自动雕刻模式*/
    val autoEngraveModel = vmApp<AutoEngraveModel>()
    val transferModel = vmApp<TransferModel>()
    val previewModel = vmApp<PreviewModel>()
    val laserPeckerModel = vmApp<LaserPeckerModel>()

    /**预览的数据*/
    var projectItemBean: CanvasProjectItemBean? = null

    var previewInfo: PreviewInfo? = null

    val uuid = uuid()

    init {
        dialogLayoutId = R.layout.dialog_path_preview_layout
        previewInfo = previewModel.previewInfoData.value

        /*laserPeckerModel.deviceStateData.observe(this, allowBackward = false) {
            if (it != null && it.isModeEngravePreview()) {

            }
        }*/
    }

    override fun initDialogView(dialog: Dialog, dialogViewHolder: DslViewHolder) {
        super.initDialogView(dialog, dialogViewHolder)
        projectItemBean?.let { itemBean ->
            val dataItem = GraphicsHelper.parseRenderItemFrom(itemBean)
            dialogViewHolder.img(R.id.lib_image_view)?.apply {
                setImageDrawable(dataItem?.drawable)
                rotation = itemBean.angle
            }

            dialogViewHolder.click(R.id.cancel_button) {
                dialog.cancel()
            }

            dialogViewHolder.click(R.id.start_button) {
                startPathPreview(dialogViewHolder, itemBean)
            }
        }
    }

    var _isPathPreview = false

    var _transferDataEntity: TransferDataEntity? = null

    /**开始路径预览*/
    fun startPathPreview(dialogViewHolder: DslViewHolder, itemBean: CanvasProjectItemBean) {
        if (_transferDataEntity != null) {
            _isPathPreview = true
            EngravePreviewCmd.previewFlashBitmapCmd(
                _transferDataEntity!!.index,
                HawkEngraveKeys.lastPwrProgress
            ).enqueue()
            delayCheckDeviceState()
            return
        }
        //loading
        dialogViewHolder.context.strokeLoading2 { isCancel, loadEnd ->
            ExitCmd().enqueue { bean, error ->
                if (error != null) {
                    toast("cmd exception!")
                    loadEnd(bean, null)
                    return@enqueue
                }
                autoEngraveModel.startCreateData(uuid, itemBean) { transferDataEntity ->
                    if (transferDataEntity == null) {
                        toast("data exception!")
                        loadEnd(null, null)
                    } else {
                        //开始传输数据
                        transferModel.transferData(TransferState(uuid), transferDataEntity) {
                            loadEnd(transferDataEntity, null)
                            if (it != null) {
                                toast("transfer data exception!")
                            } else {
                                _transferDataEntity = transferDataEntity
                                _isPathPreview = true
                                EngravePreviewCmd.previewFlashBitmapCmd(
                                    transferDataEntity.index,
                                    HawkEngraveKeys.lastPwrProgress
                                ).enqueue()
                                delayCheckDeviceState()
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onDialogDestroy(dialog: Dialog, dialogViewHolder: DslViewHolder) {
        super.onDialogDestroy(dialog, dialogViewHolder)
        if (_isPathPreview) {
            ExitCmd().enqueue { bean, error ->
                previewModel.startPreview(previewInfo)
                syncQueryDeviceState()
            }
        }
    }

    var _isDelayCheck = false

    /**持续检查工作作态*/
    fun delayCheckDeviceState() {
        if (_isDelayCheck) {
            return
        }
        _delay(1_000) {
            if (lifecycleRegistry.currentState == Lifecycle.State.DESTROYED) {
                _isDelayCheck = false
                return@_delay
            }
            //延迟1秒后, 继续查询状态
            laserPeckerModel.queryDeviceState() { bean, error ->
                _isDelayCheck = false
                if (error == null) {
                    //没有错误, 继续查询
                    delayCheckDeviceState()
                }
            }
        }
        _isDelayCheck = true
    }
}

@DSL
fun Context.pathPreviewDialog(
    projectItemBean: CanvasProjectItemBean,
    config: PathPreviewDialogConfig.() -> Unit
): Dialog {
    return PathPreviewDialogConfig().run {
        dialogContext = this@pathPreviewDialog
        this.projectItemBean = projectItemBean
        configBottomDialog()
        config()
        show()
    }
}