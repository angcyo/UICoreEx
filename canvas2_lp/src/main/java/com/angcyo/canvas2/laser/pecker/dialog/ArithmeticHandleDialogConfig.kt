package com.angcyo.canvas2.laser.pecker.dialog

import android.app.Dialog
import android.content.Context
import android.text.SpannableStringBuilder
import android.view.View
import com.angcyo.bluetooth.fsc.FscBleApiModel
import com.angcyo.bluetooth.fsc.laserpacker.LaserPeckerHelper
import com.angcyo.canvas2.laser.pecker.R
import com.angcyo.canvas2.laser.pecker.dslitem.CanvasIconItem
import com.angcyo.canvas2.laser.pecker.element.ILaserPeckerElement
import com.angcyo.canvas2.laser.pecker.engrave.dslitem.transfer.TransferDataPxItem
import com.angcyo.core.component.model.DataShareModel
import com.angcyo.core.vmApp
import com.angcyo.dialog.DslDialogConfig
import com.angcyo.dialog.configBottomDialog
import com.angcyo.dialog.numberInputDialog
import com.angcyo.dsladapter.DslAdapterItem
import com.angcyo.engrave2.data.TransitionParam
import com.angcyo.engrave2.model.TransferModel
import com.angcyo.engrave2.transition.EngraveTransitionHelper
import com.angcyo.http.rx.doMain
import com.angcyo.item.style.itemCurrentIndex
import com.angcyo.laserpacker.device.EngraveHelper
import com.angcyo.laserpacker.device.engraveLoadingAsync
import com.angcyo.library.annotation.DSL
import com.angcyo.library.ex.MB
import com.angcyo.library.ex.nowTimeString
import com.angcyo.objectbox.laser.pecker.entity.TransferConfigEntity
import com.angcyo.widget.DslViewHolder
import com.angcyo.widget.base.resetDslItem
import com.angcyo.widget.flow

/**
 * 算法处理调试对话框
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2023/04/15
 */
class ArithmeticHandleDialogConfig(context: Context? = null) : DslDialogConfig(context) {

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

        //标题
        dialogViewHolder.tv(R.id.dialog_title_view)?.apply {
            visibility = if (dialogTitle == null) View.GONE else View.VISIBLE
            text = dialogTitle
        }

        //接收返回消息
        val messageBuilder = SpannableStringBuilder()
        vmApp<DataShareModel>().shareTextOnceData.observe(this) { message ->
            message?.let {
                if (messageBuilder.isNotBlank()) {
                    messageBuilder.appendLine()
                }
                messageBuilder.append(it)
                dialogViewHolder.tv(R.id.dialog_message_view)?.text = messageBuilder
            }
        }

        if (renderElement != null) {
            initTransitionLayout(dialog, dialogViewHolder)
        }

        if (fscBleApiModel.haveDeviceConnected()) {
            initTransferLayout(dialog, dialogViewHolder)
        }
    }

    /**算法转换测试*/
    private fun initTransitionLayout(dialog: Dialog, dialogViewHolder: DslViewHolder) {
        val transferConfigEntity = TransferConfigEntity().apply {
            taskId = "ArithmeticHandle-${nowTimeString()}"
            name = EngraveHelper.generateEngraveName()
            dpi = LaserPeckerHelper.DPI_254
        }

        dialogViewHolder.group(R.id.dpi_wrap_layout)?.resetDslItem(TransferDataPxItem().apply {
            itemPxList = LaserPeckerHelper.findProductSupportPxList()
            selectorCurrentDpi(transferConfigEntity.dpi)
            itemHidden = itemPxList.isNullOrEmpty() //自动隐藏
            observeItemChange {
                //保存最后一次选择的dpi
                val dpi = itemPxList?.get(itemCurrentIndex)?.dpi ?: LaserPeckerHelper.DPI_254
                transferConfigEntity.dpi = dpi
            }
        })

        val itemList = mutableListOf<DslAdapterItem>()
        itemList.add(CanvasIconItem().apply {
            itemIco = R.drawable.canvas_bitmap_grey
            itemText = "转普通"
            itemClick = {
                wrapLoading {
                    EngraveTransitionHelper.transitionToBitmap(renderElement, transferConfigEntity)
                }
            }
        })
        itemList.add(CanvasIconItem().apply {
            itemIco = R.drawable.canvas_bitmap_black_white
            itemText = "转线段"
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
            itemClick = {
                wrapLoading {
                    EngraveTransitionHelper.transitionToBitmapDithering(
                        renderElement,
                        transferConfigEntity,
                        TransitionParam()
                    )
                }
            }
        })
        itemList.add(CanvasIconItem().apply {
            itemIco = R.drawable.canvas_bitmap_gcode
            itemText = "转GCode"
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
        dialogViewHolder.flow(R.id.lib_flow_layout)?.resetDslItem(itemList)
    }

    /**数据传输测试*/
    private fun initTransferLayout(dialog: Dialog, dialogViewHolder: DslViewHolder) {
        dialogViewHolder.visible(R.id.lib_button_flow_layout)
        dialogViewHolder.click(R.id.send5_button) {
            transferModel.transferDataTest(it.tag.toString().toLong() * MB, ::sendProgressMessage)
        }
        dialogViewHolder.click(R.id.send10_button) {
            transferModel.transferDataTest(it.tag.toString().toLong() * MB, ::sendProgressMessage)
        }
        dialogViewHolder.click(R.id.send20_button) {
            transferModel.transferDataTest(it.tag.toString().toLong() * MB, ::sendProgressMessage)
        }
        dialogViewHolder.click(R.id.send30_button) {
            transferModel.transferDataTest(it.tag.toString().toLong() * MB, ::sendProgressMessage)
        }
        dialogViewHolder.click(R.id.send_custom_button) {
            it.context.numberInputDialog {
                hintInputString = "指定大小(MB)"
                maxInputLength = 3
                onInputResult = { dialog, inputText ->
                    inputText.toString().toLongOrNull()?.let {
                        transferModel.transferDataTest(it * MB, ::sendProgressMessage)
                    }
                    false
                }
            }
        }
    }

    private fun sendProgressMessage(message: CharSequence?) {
        message?.let {
            doMain {
                _dialogViewHolder?.tv(R.id.send_message_view)?.text = it
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

