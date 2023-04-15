package com.angcyo.canvas2.laser.pecker.dialog

import android.app.Dialog
import android.content.Context
import android.view.View
import com.angcyo.bluetooth.fsc.laserpacker.LaserPeckerHelper
import com.angcyo.canvas2.laser.pecker.R
import com.angcyo.canvas2.laser.pecker.dslitem.CanvasIconItem
import com.angcyo.canvas2.laser.pecker.element.ILaserPeckerElement
import com.angcyo.canvas2.laser.pecker.engrave.dslitem.transfer.TransferDataPxItem
import com.angcyo.core.component.model.DataShareModel
import com.angcyo.core.vmApp
import com.angcyo.dialog.DslDialogConfig
import com.angcyo.dialog.configBottomDialog
import com.angcyo.dsladapter.DslAdapterItem
import com.angcyo.engrave2.data.TransitionParam
import com.angcyo.engrave2.transition.EngraveTransitionHelper
import com.angcyo.item.style.itemCurrentIndex
import com.angcyo.laserpacker.device.EngraveHelper
import com.angcyo.laserpacker.device.engraveLoadingAsync
import com.angcyo.library.annotation.DSL
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
        val messageBuilder = StringBuilder()
        vmApp<DataShareModel>().shareTextOnceData.observe(this) { message ->
            message?.let {
                if (messageBuilder.isNotBlank()) {
                    messageBuilder.appendLine()
                }
                messageBuilder.append(it)
                dialogViewHolder.tv(R.id.dialog_message_view)?.text = messageBuilder
            }
        }

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
            itemText = "转普通图片算法"
            itemClick = {
                wrapLoading {
                    EngraveTransitionHelper.transitionToBitmap(renderElement, transferConfigEntity)
                }
            }
        })
        itemList.add(CanvasIconItem().apply {
            itemIco = R.drawable.canvas_bitmap_black_white
            itemText = "转线段图片算法"
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
            itemText = "转抖动图片算法"
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
            itemText = "转GCode算法"
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

    private fun wrapLoading(action: () -> Unit) {
        engraveLoadingAsync({
            action()
        })
    }
}

/**算法处理对话框*/
@DSL
fun Context.arithmeticHandleDialogConfig(
    renderElement: ILaserPeckerElement,
    config: ArithmeticHandleDialogConfig.() -> Unit = {}
) {
    return ArithmeticHandleDialogConfig(this).run {
        this.renderElement = renderElement
        configBottomDialog(this@arithmeticHandleDialogConfig)
        config()
        show()
    }
}

