package com.angcyo.canvas2.laser.pecker.dialog

import android.app.Dialog
import android.content.Context
import android.text.SpannableStringBuilder
import androidx.core.widget.NestedScrollView
import com.angcyo.bluetooth.fsc.FscBleApiModel
import com.angcyo.bluetooth.fsc.laserpacker.DeviceStateModel
import com.angcyo.bluetooth.fsc.laserpacker.HawkEngraveKeys
import com.angcyo.bluetooth.fsc.laserpacker.LaserPeckerHelper
import com.angcyo.canvas.render.core.CanvasRenderDelegate
import com.angcyo.canvas2.laser.pecker.R
import com.angcyo.canvas2.laser.pecker.dslitem.CanvasIconItem
import com.angcyo.canvas2.laser.pecker.element.ILaserPeckerElement
import com.angcyo.canvas2.laser.pecker.engrave.LPTransferHelper
import com.angcyo.canvas2.laser.pecker.engrave.dslitem.transfer.TransferDataPxItem
import com.angcyo.core.component.model.DataShareModel
import com.angcyo.core.vmApp
import com.angcyo.dialog.DslDialogConfig
import com.angcyo.dialog.configBottomDialog
import com.angcyo.dialog.numberInputDialog
import com.angcyo.dsladapter.DslAdapterItem
import com.angcyo.dsladapter.paddingHorizontal
import com.angcyo.engrave2.data.TransitionParam
import com.angcyo.engrave2.model.TransferModel
import com.angcyo.engrave2.transition.EngraveTransitionHelper
import com.angcyo.http.rx.doMain
import com.angcyo.item.style.itemCurrentIndex
import com.angcyo.item.style.itemNewHawkKeyStr
import com.angcyo.laserpacker.device.DeviceHelper
import com.angcyo.laserpacker.device.EngraveHelper
import com.angcyo.laserpacker.device.LayerHelper
import com.angcyo.laserpacker.device.engraveLoadingAsync
import com.angcyo.library.annotation.DSL
import com.angcyo.library.ex.MB
import com.angcyo.library.ex.dpi
import com.angcyo.library.ex.nowTimeString
import com.angcyo.library.ex.uuid
import com.angcyo.objectbox.laser.pecker.entity.TransferConfigEntity
import com.angcyo.widget.DslViewHolder
import com.angcyo.widget.base.resetDslItem
import com.angcyo.widget.base.scrollToEnd
import com.angcyo.widget.flow

/**
 * 算法处理调试对话框
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2023/04/15
 */
class ArithmeticHandleDialogConfig(context: Context? = null) : DslDialogConfig(context) {

    /**渲染代理*/
    var canvasRenderDelegate: CanvasRenderDelegate? = null

    /**需要处理的元素*/
    var renderElement: ILaserPeckerElement? = null

    val fscBleApiModel = vmApp<FscBleApiModel>()
    val transferModel = vmApp<TransferModel>()

    init {
        dialogLayoutId = R.layout.dialog_arithmetic_handle_layout
        dialogTitle = "算法调试"
    }

    override fun initDialogView(dialog: Dialog, dialogViewHolder: DslViewHolder) {
        super.initDialogView(dialog, dialogViewHolder)

        //接收返回消息
        val messageBuilder = SpannableStringBuilder()
        vmApp<DataShareModel>().shareTextOnceData.observe(this) { message ->
            message?.let {
                if (messageBuilder.isNotBlank()) {
                    messageBuilder.appendLine()
                }
                messageBuilder.append(it)
                dialogViewHolder.tv(R.id.dialog_message_view)?.text = messageBuilder
                dialogViewHolder.v<NestedScrollView>(R.id.wrap_layout)?.scrollToEnd()
            }
        }

        if (renderElement != null) {
            initTransitionLayout(dialog, dialogViewHolder)
        }

        if (vmApp<DeviceStateModel>().isDeviceConnect()) {
            initTransferLayout(dialog, dialogViewHolder)
        }

        dialogViewHolder.click(R.id.create_data_button) {
            canvasRenderDelegate?.let {
                val taskId = "test-${uuid()}"
                LPTransferHelper.startCreateTransferData(transferModel, taskId, it)
            }
        }

        dialogViewHolder.click(R.id.share_log_button) {
            DeviceHelper.shareEngraveLog()
        }
    }

    /**算法转换测试*/
    private fun initTransitionLayout(dialog: Dialog, dialogViewHolder: DslViewHolder) {
        val transferConfigEntity = TransferConfigEntity().apply {
            taskId = "ArithmeticHandle-${nowTimeString()}"
            name = EngraveHelper.generateEngraveName()
            layerJson = LayerHelper.getProductLayerSupportPxJson()
        }

        dialogViewHolder.group(R.id.dpi_wrap_layout)?.resetDslItem(TransferDataPxItem().apply {
            itemPxList =
                LaserPeckerHelper.findProductLayerSupportPxList(HawkEngraveKeys.lastLayerId)
            selectorCurrentDpi(transferConfigEntity.getLayerConfigDpi(HawkEngraveKeys.lastLayerId))
            itemHidden = itemPxList.isNullOrEmpty() //自动隐藏
            observeItemChange {
                //保存最后一次选择的dpi
                val dpi = itemPxList?.get(itemCurrentIndex)?.dpi ?: LaserPeckerHelper.DPI_254
                transferConfigEntity.layerJson =
                    HawkEngraveKeys.getLayerConfigJson(HawkEngraveKeys.lastLayerId, dpi)
            }
        })

        val padding = 2 * dpi
        val itemList = mutableListOf<DslAdapterItem>()
        itemList.add(CanvasIconItem().apply {
            itemIco = R.drawable.canvas_bitmap_grey
            itemText = "转普通"
            paddingHorizontal(padding)
            itemClick = {
                wrapLoading {
                    EngraveTransitionHelper.transitionToBitmap(renderElement, transferConfigEntity)
                }
            }
        })
        itemList.add(CanvasIconItem().apply {
            itemIco = R.drawable.canvas_bitmap_black_white
            itemText = "转线段"
            paddingHorizontal(padding)
            itemClick = {
                wrapLoading {
                    EngraveTransitionHelper.transitionToBitmapPath(
                        renderElement,
                        transferConfigEntity
                    )
                }
            }
        })
        itemList.add(CanvasIconItem().apply {
            itemIco = R.drawable.canvas_bitmap_dithering
            itemText = "转抖动"
            paddingHorizontal(padding)
            itemClick = {
                wrapLoading {
                    EngraveTransitionHelper.transitionToBitmapDithering(
                        renderElement,
                        transferConfigEntity,
                        TransitionParam(useNewDithering = false)
                    )
                }
            }
        })
        itemList.add(CanvasIconItem().apply {
            itemIco = R.drawable.canvas_bitmap_dithering
            itemText = "转抖动"
            paddingHorizontal(padding)
            itemNewHawkKeyStr = "use_new_dithering"
            itemClick = {
                wrapLoading {
                    EngraveTransitionHelper.transitionToBitmapDithering(
                        renderElement,
                        transferConfigEntity,
                        TransitionParam(useNewDithering = true)
                    )
                }
            }
        })
        itemList.add(CanvasIconItem().apply {
            itemIco = R.drawable.canvas_bitmap_gcode
            itemText = "转GCode"
            paddingHorizontal(padding)
            itemClick = {
                wrapLoading {
                    EngraveTransitionHelper.transitionToGCode(
                        renderElement,
                        transferConfigEntity,
                        TransitionParam()
                    )
                }
            }
        })
        itemList.add(CanvasIconItem().apply {
            itemIco = R.drawable.canvas_bitmap_gcode
            itemText = "转GCode切割"
            paddingHorizontal(padding)
            itemNewHawkKeyStr = "use_gcode_cut_data"
            itemClick = {
                wrapLoading {
                    EngraveTransitionHelper.transitionToGCode(
                        renderElement,
                        transferConfigEntity,
                        TransitionParam(enableGCodeCutData = true)
                    )
                }
            }
        })
        itemList.add(CanvasIconItem().apply {
            itemIco = R.drawable.canvas_bitmap_gcode
            itemText = "转GCode切片"
            paddingHorizontal(padding)
            itemNewHawkKeyStr = "use_gcode_slice_data"
            itemClick = {
                wrapLoading {
                    val bean = renderElement?.elementBean
                    EngraveTransitionHelper.transitionToGCode(
                        renderElement,
                        transferConfigEntity,
                        TransitionParam(enableSlice = true, sliceCount = bean?.sliceCount ?: 1)
                    )
                }
            }
        })
        itemList.add(CanvasIconItem().apply {
            itemIco = R.drawable.canvas_bitmap_gcode
            itemText = "转路径数据"
            paddingHorizontal(padding)
            itemNewHawkKeyStr = "use_path_data"
            itemClick = {
                wrapLoading {
                    EngraveTransitionHelper.transitionToGCode(
                        renderElement,
                        transferConfigEntity,
                        TransitionParam(gcodeUsePathData = true)
                    )
                }
            }
        })
        dialogViewHolder.flow(R.id.lib_flow_layout)?.resetDslItem(itemList)
    }

    /**数据传输测试*/
    private fun initTransferLayout(dialog: Dialog, dialogViewHolder: DslViewHolder) {
        dialogViewHolder.visible(R.id.lib_button_flow_layout)
        dialogViewHolder.click(R.id.send5_button) {
            transferDataTest(dialogViewHolder, it.tag)
        }
        dialogViewHolder.click(R.id.send10_button) {
            transferDataTest(dialogViewHolder, it.tag)
        }
        dialogViewHolder.click(R.id.send20_button) {
            transferDataTest(dialogViewHolder, it.tag)
        }
        dialogViewHolder.click(R.id.send30_button) {
            transferDataTest(dialogViewHolder, it.tag)
        }
        dialogViewHolder.click(R.id.send_custom_button) {
            it.context.numberInputDialog {
                hintInputString = "指定大小(MB)"
                maxInputLength = 3
                onInputResult = { dialog, inputText ->
                    transferDataTest(dialogViewHolder, inputText)
                    false
                }
            }
        }
    }

    private fun transferDataTest(dialogViewHolder: DslViewHolder, size: Any) {
        dialogViewHolder.enable(R.id.lib_button_flow_layout, false)
        transferModel.transferDataTest(
            (size.toString().toLongOrNull() ?: 0) * MB,
            ::sendProgressMessage
        )
        dialogViewHolder.postDelay(1_000) {
            dialogViewHolder.enable(R.id.lib_button_flow_layout, true)
        }
    }

    private fun sendProgressMessage(message: CharSequence?) {
        message?.let {
            doMain {
                _dialogViewHolder?.tv(R.id.send_message_view)?.text = it
                _dialogViewHolder?.v<NestedScrollView>(R.id.wrap_layout2)?.scrollToEnd()
            }
        }
    }

    private fun wrapLoading(action: () -> Unit) {
        engraveLoadingAsync({
            action()
        })
    }
}

/**算法处理对话框*/
@DSL
fun Context.arithmeticHandleDialogConfig(
    renderElement: ILaserPeckerElement?,
    config: ArithmeticHandleDialogConfig.() -> Unit = {}
) {
    return ArithmeticHandleDialogConfig(this).run {
        this.renderElement = renderElement
        configBottomDialog(this@arithmeticHandleDialogConfig)
        config()
        show()
    }
}

