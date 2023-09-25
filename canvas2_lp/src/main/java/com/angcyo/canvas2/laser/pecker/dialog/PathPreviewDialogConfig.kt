package com.angcyo.canvas2.laser.pecker.dialog

import android.app.Dialog
import android.content.Context
import androidx.lifecycle.Lifecycle
import com.angcyo.bluetooth.fsc.enqueue
import com.angcyo.bluetooth.fsc.laserpacker.DeviceStateModel
import com.angcyo.bluetooth.fsc.laserpacker.HawkEngraveKeys
import com.angcyo.bluetooth.fsc.laserpacker.LaserPeckerModel
import com.angcyo.bluetooth.fsc.laserpacker.command.EngravePreviewCmd
import com.angcyo.bluetooth.fsc.laserpacker.command.ExitCmd
import com.angcyo.bluetooth.fsc.laserpacker.parse.QueryStateParser
import com.angcyo.bluetooth.fsc.laserpacker.syncQueryDeviceState
import com.angcyo.canvas.render.core.CanvasRenderDelegate
import com.angcyo.canvas.render.data.RenderParams
import com.angcyo.canvas.render.renderer.BaseRenderer
import com.angcyo.canvas2.laser.pecker.R
import com.angcyo.canvas2.laser.pecker.engrave.LPTransferHelper
import com.angcyo.canvas2.laser.pecker.engrave.dslitem.preview.PreviewBrightnessItem
import com.angcyo.canvas2.laser.pecker.util.lpElement
import com.angcyo.canvas2.laser.pecker.util.lpElementBean
import com.angcyo.core.component.model._isDarkMode
import com.angcyo.core.vmApp
import com.angcyo.dialog.DslDialogConfig
import com.angcyo.dialog.configBottomDialog
import com.angcyo.engrave2.data.PreviewInfo
import com.angcyo.engrave2.data.TransferState
import com.angcyo.engrave2.model.PreviewModel
import com.angcyo.engrave2.model.TransferModel
import com.angcyo.laserpacker.device.LayerHelper
import com.angcyo.laserpacker.device.engraveStrokeLoading
import com.angcyo.library._screenHeight
import com.angcyo.library._screenWidth
import com.angcyo.library.annotation.DSL
import com.angcyo.library.component._delay
import com.angcyo.library.component.pad.isInPadMode
import com.angcyo.library.ex._color
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

    var renderDelegate: CanvasRenderDelegate? = null
    var renderUuid: String? = null

    /**自动雕刻模式*/
    private val transferModel = vmApp<TransferModel>()
    private val previewModel = vmApp<PreviewModel>()
    private val laserPeckerModel = vmApp<LaserPeckerModel>()
    private val deviceStateModel = vmApp<DeviceStateModel>()

    private var previewInfo: PreviewInfo? = null

    private val uuid = uuid()

    init {
        dialogLayoutId = R.layout.dialog_path_preview_layout
        previewInfo = previewModel.previewInfoData.value

        deviceStateModel.deviceStateData.observe(this, allowBackward = false) {
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
        val renderer = renderDelegate?.renderManager?.findElementRenderer(renderUuid)
        val itemBean = renderer?.lpElementBean() ?: return
        val element = renderer.lpElement() ?: return

        dialogViewHolder.img(R.id.lib_image_view)?.apply {
            if (_isDarkMode) {
                //暗色适配 com.angcyo.canvas2.laser.pecker.RenderLayoutHelper.bindRenderLayout
                setBackgroundColor(_color(R.color.colorPrimaryDark))
            }
            post {
                setImageDrawable(
                    element.requestElementDrawable(
                        renderer, RenderParams(
                            overrideSize = min(measuredWidth, measuredHeight).toFloat(),
                            overrideSizeNotZoomIn = true
                        )
                    )
                )
            }
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
                    layerJson = LayerHelper.getProductLayerSupportPxJson()
                }

                LPTransferHelper.transitionRenderer(renderer, transferConfigEntity)
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
        EngravePreviewCmd.previewFlashBitmapCmd(index, HawkEngraveKeys.lastPwrProgress).enqueue()
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
            deviceStateModel.queryDeviceState { bean, error ->
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
    uuid: String, config: PathPreviewDialogConfig.() -> Unit
): Dialog {
    return PathPreviewDialogConfig().run {
        dialogContext = this@pathPreviewDialog
        renderUuid = uuid
        configBottomDialog()
        if (isInPadMode()) {
            dialogWidth = min(_screenWidth, _screenHeight) * 3 / 5
        }
        config()
        show()
    }
}