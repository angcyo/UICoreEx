package com.angcyo.canvas2.laser.pecker.dialog

import android.app.Dialog
import android.content.Context
import androidx.lifecycle.Lifecycle
import com.angcyo.bluetooth.fsc.enqueue
import com.angcyo.bluetooth.fsc.laserpacker.LaserPeckerHelper
import com.angcyo.bluetooth.fsc.laserpacker.LaserPeckerModel
import com.angcyo.bluetooth.fsc.laserpacker.command.EngravePreviewCmd
import com.angcyo.bluetooth.fsc.laserpacker.command.ExitCmd
import com.angcyo.bluetooth.fsc.laserpacker.parse.QueryStateParser
import com.angcyo.bluetooth.fsc.laserpacker.syncQueryDeviceState
import com.angcyo.canvas.render.renderer.BaseRenderer
import com.angcyo.canvas2.laser.pecker.R
import com.angcyo.canvas2.laser.pecker.engrave.LPDataTransitionHelper
import com.angcyo.canvas2.laser.pecker.engrave.dslitem.preview.PreviewBrightnessItem
import com.angcyo.canvas2.laser.pecker.util.LPRendererHelper
import com.angcyo.canvas2.laser.pecker.util.lpElement
import com.angcyo.core.vmApp
import com.angcyo.dialog.DslDialogConfig
import com.angcyo.dialog.configBottomDialog
import com.angcyo.engrave2.data.PreviewInfo
import com.angcyo.engrave2.data.TransferState
import com.angcyo.engrave2.model.PreviewModel
import com.angcyo.engrave2.model.TransferModel
import com.angcyo.laserpacker.bean.LPElementBean
import com.angcyo.laserpacker.device.HawkEngraveKeys
import com.angcyo.laserpacker.device.engraveStrokeLoading
import com.angcyo.library._screenHeight
import com.angcyo.library._screenWidth
import com.angcyo.library.annotation.DSL
import com.angcyo.library.component._delay
import com.angcyo.library.component.pad.isInPadMode
import com.angcyo.library.ex._string
import com.angcyo.library.ex.uuid
import com.angcyo.library.toast
import com.angcyo.objectbox.laser.pecker.entity.TransferConfigEntity
import com.angcyo.objectbox.laser.pecker.entity.TransferDataEntity
import com.angcyo.viewmodel.observe
import com.angcyo.widget.DslViewHolder
import com.angcyo.widget.base.appendDslItem
import kotlin.math.min

/**
 * 路径预览对话框
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2022/10/21
 */
class PathPreviewDialogConfig : DslDialogConfig() {

    /**自动雕刻模式*/
    val transferModel = vmApp<TransferModel>()
    val previewModel = vmApp<PreviewModel>()
    val laserPeckerModel = vmApp<LaserPeckerModel>()

    /**预览的数据*/
    var elementBean: LPElementBean? = null

    var previewInfo: PreviewInfo? = null

    val uuid = uuid()

    init {
        dialogLayoutId = R.layout.dialog_path_preview_layout
        previewInfo = previewModel.previewInfoData.value

        laserPeckerModel.deviceStateData.observe(this, allowBackward = false) {
            if (it != null) {
                if (it.mode == QueryStateParser.WORK_MODE_ENGRAVE_PREVIEW && it.workState == 0x01) {
                    //向量预览中...
                    _dialogViewHolder?.gone(R.id.start_button)
                } else {
                    _dialogViewHolder?.visible(R.id.start_button)
                }
            }
        }
    }

    override fun initDialogView(dialog: Dialog, dialogViewHolder: DslViewHolder) {
        super.initDialogView(dialog, dialogViewHolder)
        elementBean?.let { itemBean ->
            val renderer = LPRendererHelper.parseElementRenderer(itemBean)
            val element = renderer?.lpElement() ?: return

            dialogViewHolder.img(R.id.lib_image_view)?.apply {
                setImageDrawable(element.requestElementRenderDrawable(null))
                rotation = itemBean.angle
            }

            //激光亮度
            dialogViewHolder.group(R.id.brightness_wrap_layout)
                ?.appendDslItem(PreviewBrightnessItem().apply {
                    observeItemChange {
                        itemBean.index?.let { _previewFlashBitmapCmd(it) }
                    }
                })

            dialogViewHolder.click(R.id.cancel_button) {
                dialog.cancel()
            }

            dialogViewHolder.click(R.id.start_button) {
                val index = itemBean.index
                if (index != null) {
                    TransferModel.checkIndex(index) {
                        if (it) {
                            //索引已存在, 直接预览
                            sendPreviewFlashBitmapCmd(dialogViewHolder, index)
                        } else {
                            startPathPreview(dialogViewHolder, renderer)
                        }
                    }
                } else {
                    startPathPreview(dialogViewHolder, renderer)
                }
            }
        }
    }

    var _isPathPreview = false

    var _transferDataEntity: TransferDataEntity? = null

    /**开始路径预览*/
    fun startPathPreview(dialogViewHolder: DslViewHolder, renderer: BaseRenderer?) {
        if (_transferDataEntity != null) {
            sendPreviewFlashBitmapCmd(dialogViewHolder, _transferDataEntity!!.index)
            return
        }
        //loading
        dialogViewHolder.context.engraveStrokeLoading { isCancel, loadEnd ->
            ExitCmd().enqueue { bean, error ->
                if (error != null) {
                    toast("cmd exception!")
                    loadEnd(bean, null)
                    return@enqueue
                }

                val transferConfigEntity = TransferConfigEntity().apply {
                    name = "PathPreview"
                    dpi = LaserPeckerHelper.DPI_254
                }

                LPDataTransitionHelper.transitionRenderer(renderer, transferConfigEntity)
                    .let { transferDataEntity ->
                        if (transferDataEntity == null) {
                            toast(_string(R.string.data_exception))
                            loadEnd(null, null)
                        } else {
                            //开始传输数据
                            transferModel.transferData(TransferState(uuid), transferDataEntity) {
                                loadEnd(transferDataEntity, null)
                                if (it != null) {
                                    toast(_string(R.string.transfer_data_exception))
                                } else {
                                    _transferDataEntity = transferDataEntity
                                    sendPreviewFlashBitmapCmd(
                                        dialogViewHolder,
                                        transferDataEntity.index
                                    )
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

    /**发送预览flash图片指令*/
    fun sendPreviewFlashBitmapCmd(dialogViewHolder: DslViewHolder, index: Int) {
        ExitCmd().enqueue { bean, error ->
            if (error == null) {
                dialogViewHolder.visible(R.id.brightness_wrap_layout)
                _isPathPreview = true
                _previewFlashBitmapCmd(index)
                delayCheckDeviceState()
            }
        }
    }

    fun _previewFlashBitmapCmd(index: Int) {
        EngravePreviewCmd.previewFlashBitmapCmd(index, HawkEngraveKeys.lastPwrProgress)
            .enqueue()
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
    elementBean: LPElementBean,
    config: PathPreviewDialogConfig.() -> Unit
): Dialog {
    return PathPreviewDialogConfig().run {
        dialogContext = this@pathPreviewDialog
        this.elementBean = elementBean
        configBottomDialog()
        if (isInPadMode()) {
            dialogWidth = min(_screenWidth, _screenHeight) * 3 / 5
        }
        config()
        show()
    }
}