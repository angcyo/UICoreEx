package com.angcyo.laserpacker.open

import android.graphics.Typeface
import android.graphics.drawable.Drawable
import androidx.core.view.isVisible
import com.angcyo.bluetooth.fsc.laserpacker.HawkEngraveKeys
import com.angcyo.dialog.inputDialog
import com.angcyo.dsladapter.DslAdapterItem
import com.angcyo.glide.glide
import com.angcyo.laserpacker.device.R
import com.angcyo.library.ex.ClickAction
import com.angcyo.library.ex._string
import com.angcyo.library.ex.extName
import com.angcyo.library.ex.lastName
import com.angcyo.library.ex.noExtName
import com.angcyo.widget.DslViewHolder
import java.io.File

/**
 * 预览打开的文件item
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2022/10/12
 */

class CanvasOpenPreviewItem : DslAdapterItem() {

    /**需要显示的名字, 如果为null, 则从[itemFilePath]中获取*/
    var itemShowName: String? = null

    /**文件路径*/
    var itemFilePath: String? = null

    /**是否是字体类型*/
    var itemIsFontType: Boolean = false

    /**需要预览字体, 设置字体后, 同时会开启文件名编辑功能*/
    var itemTypeface: Typeface? = null
        set(value) {
            field = value
            if (value != null) {
                itemIsFontType = true
            }
        }

    /**需要预览图片*/
    var itemDrawable: Drawable? = null

    /**点击回调, 如果是字体, 则应该是回调*/
    var openAction: ClickAction = {}

    var cancelAction: ClickAction = {}

    init {
        itemLayoutId = R.layout.item_canvas_open_preview_layout
    }

    override fun onItemBind(
        itemHolder: DslViewHolder,
        itemPosition: Int,
        adapterItem: DslAdapterItem,
        payloads: List<Any>
    ) {
        super.onItemBind(itemHolder, itemPosition, adapterItem, payloads)

        val fileName = itemFilePath?.lastName()
        itemHolder.tv(R.id.file_name_view)?.text = itemShowName ?: fileName

        //
        itemTypeface?.let {
            itemHolder.tv(R.id.typeface_text_view)?.apply {
                text = HawkEngraveKeys.typefacePreviewText ?: _string(R.string.canvas_font_text)
                isVisible = true
                this.typeface = it
            }
        }
        if (itemIsFontType) {
            itemHolder.tv(R.id.open_button)?.text = _string(R.string.canvas_import_font)
        }
        //
        itemDrawable?.let { drawable ->
            itemHolder.img(R.id.image_view)?.apply {
                isVisible = true
                glide {
                    load(drawable)
                }
            }
        }

        //
        itemHolder.click(R.id.open_button, openAction)
        itemHolder.click(R.id.cancel_button, cancelAction)

        //edit
        itemHolder.visible(R.id.lib_edit_view, itemTypeface != null)
        itemHolder.click(R.id.lib_edit_view) {
            //编辑文件名
            it.context.inputDialog {
                defaultInputString = fileName?.noExtName()
                maxInputLength = 30
                dialogTitle = _string(R.string.canvas_rename)
                canInputEmpty = false
                onInputResult = { dialog, inputText ->
                    val file =
                        File(File(itemFilePath!!).parentFile, "${inputText}.${fileName?.extName()}")
                    itemFilePath = file.absolutePath
                    updateAdapterItem()
                    false
                }
            }
        }

    }

}