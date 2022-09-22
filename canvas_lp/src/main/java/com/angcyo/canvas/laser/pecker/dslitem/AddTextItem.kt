package com.angcyo.canvas.laser.pecker.dslitem

import androidx.fragment.app.Fragment
import com.angcyo.canvas.CanvasView
import com.angcyo.canvas.graphics.addBarTextRender
import com.angcyo.canvas.graphics.addQRTextRender
import com.angcyo.canvas.graphics.addTextRender
import com.angcyo.canvas.items.PictureBitmapItem
import com.angcyo.canvas.items.renderer.PictureItemRenderer
import com.angcyo.canvas.laser.pecker.R
import com.angcyo.canvas.laser.pecker.addTextDialog
import com.angcyo.canvas.utils.CanvasConstant
import com.angcyo.dsladapter.item.IFragmentItem
import com.angcyo.library.ex._string
import com.hingin.umeng.UMEvent
import com.hingin.umeng.umengEventValue

/**
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2022/04/18
 */
class AddTextItem : CanvasControlItem2(), IFragmentItem {

    companion object {

        /**输入条码*/
        fun inputBarcode(
            canvasView: CanvasView?,
            itemRenderer: PictureItemRenderer<PictureBitmapItem>?
        ) {
            /*fragment.context?.inputDialog {
                dialogTitle = _string(R.string.canvas_barcode)
                inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS
                digits = _string(R.string.lib_barcode_digits)
                maxInputLength = AddTextItem.MAX_INPUT_LENGTH
                defaultInputString = itemRenderer?.rendererItem?.data as CharSequence?
                onInputResult = { dialog, inputText ->
                    if (itemRenderer == null) {
                        //添加条码
                        if (inputText.isNotEmpty()) {
                            inputText.createBarCode()?.let {
                                canvasView?.canvasDelegate?.addPictureBitmapRenderer(it)?.apply {
                                    dataType = CanvasConstant.DATA_TYPE_BARCODE
                                    data = inputText
                                }
                            }
                        }
                    } else {
                        //修改条码
                        val renderItem = itemRenderer.rendererItem
                        if (inputText.isNotEmpty()) {
                            inputText.createBarCode()?.let {
                                if (renderItem is PictureBitmapItem) {
                                    renderItem.originBitmap = it
                                }
                                renderItem?.data = inputText
                                itemRenderer.requestRendererItemUpdate()
                            }
                        }
                    }

                    false
                }
            }*/
        }

        /**输入二维码*/
        fun inputQrCode(
            canvasView: CanvasView?,
            itemRenderer: PictureItemRenderer<PictureBitmapItem>?
        ) {
            /*fragment.context?.inputDialog {
                dialogTitle = _string(R.string.canvas_qrcode)
                maxInputLength = AddTextItem.MAX_INPUT_LENGTH
                defaultInputString = itemRenderer?.rendererItem?.data as CharSequence?
                onInputResult = { dialog, inputText ->
                    if (itemRenderer == null) {
                        if (inputText.isNotEmpty()) {
                            inputText.createQRCode()?.let {
                                canvasView?.canvasDelegate?.addPictureBitmapRenderer(it)?.apply {
                                    dataType = CanvasConstant.DATA_TYPE_QRCODE
                                    data = inputText
                                }
                            }
                        }
                    } else {
                        val renderItem = itemRenderer.rendererItem
                        if (inputText.isNotEmpty()) {
                            inputText.createQRCode()?.let {
                                if (renderItem is PictureBitmapItem) {
                                    renderItem.originBitmap = it
                                }
                                renderItem?.data = inputText
                                itemRenderer.requestRendererItemUpdate()
                            }
                        }
                    }
                    false
                }
            }*/

        }
    }

    override var itemFragment: Fragment? = null

    init {
        itemIco = R.drawable.canvas_text_ico
        itemText = _string(R.string.canvas_text)

        itemClick = {
            itemFragment?.context?.addTextDialog {
                /*onInputResult = { dialog, inputText ->
                    if (inputText.isNotEmpty()) {
                        //canvasView.addTextRenderer("$inputText")
                        //canvasView.addPictureTextRenderer("$inputText")
                        canvasView.canvasDelegate.addPictureTextRender("$inputText")
                        UMEvent.CANVAS_TEXT.umengEventValue()
                    }
                    false
                }*/
                onAddTextAction = { inputText, type ->
                    when (type) {
                        CanvasConstant.DATA_TYPE_QRCODE -> {
                            itemCanvasDelegate?.addQRTextRender(inputText)
                        }
                        CanvasConstant.DATA_TYPE_BARCODE -> {
                            itemCanvasDelegate?.addBarTextRender(inputText)
                        }
                        else -> {
                            itemCanvasDelegate?.addTextRender(inputText)
                            UMEvent.CANVAS_TEXT.umengEventValue()
                        }
                    }
                }
            }
        }
    }
}