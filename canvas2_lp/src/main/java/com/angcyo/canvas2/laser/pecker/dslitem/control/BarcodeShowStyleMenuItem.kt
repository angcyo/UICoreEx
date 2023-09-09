package com.angcyo.canvas2.laser.pecker.dslitem.control

import com.angcyo.canvas2.laser.pecker.R
import com.angcyo.canvas2.laser.pecker.dialog.canvasMenuPopupWindow
import com.angcyo.dsladapter.updateItemSelected
import com.angcyo.laserpacker.LPDataConstant
import com.angcyo.library.ex._string
import com.hingin.umeng.UMEvent
import com.hingin.umeng.umengEventValue

/**
 * 1D条形码显示类型菜单item
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2023/09/09
 */
class BarcodeShowStyleMenuItem : BaseTextControlItem() {

    companion object {
        var isShowStyleMenuPopup = false
    }

    init {
        itemIsSelected = isShowStyleMenuPopup
        itemIco = R.drawable.variable_show_style_svg
        itemText = _string(R.string.variable_barcode_show_style)
        itemClick = { anchor ->
            updateItemSelected(!itemIsSelected)

            if (itemIsSelected) {
                UMEvent.CANVAS_VARIABLE_SHOW_STYLE.umengEventValue()
                anchor.context.canvasMenuPopupWindow(anchor) {
                    isShowStyleMenuPopup = true
                    renderAdapterAction = {
                        BarcodeShowStyleSelectItem()() {
                            initItem()
                            itemIco = R.drawable.variable_show_style_top_svg
                            itemText = _string(R.string.variable_barcode_show_top)
                            itemShowStyle = LPDataConstant.TEXT_SHOW_STYLE_TOP
                        }
                        BarcodeShowStyleSelectItem()() {
                            initItem()
                            itemIco = R.drawable.variable_show_style_bottom_svg
                            itemText = _string(R.string.variable_barcode_show_bottom)
                            itemShowStyle = LPDataConstant.TEXT_SHOW_STYLE_BOTTOM
                        }
                        BarcodeShowStyleSelectItem()() {
                            initItem()
                            itemIco = R.drawable.variable_show_style_svg
                            itemText = _string(R.string.variable_barcode_show_none)
                            itemShowStyle = LPDataConstant.TEXT_SHOW_STYLE_NONE
                        }
                    }
                    onDismiss = {
                        isShowStyleMenuPopup = false
                        updateItemSelected(false)
                        false
                    }
                }
            }
        }
    }
}