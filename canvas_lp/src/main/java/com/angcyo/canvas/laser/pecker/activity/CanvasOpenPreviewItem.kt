package com.angcyo.canvas.laser.pecker.activity

import android.graphics.Typeface
import android.graphics.drawable.Drawable
import androidx.core.view.isVisible
import com.angcyo.canvas.laser.pecker.R
import com.angcyo.dsladapter.DslAdapterItem
import com.angcyo.glide.glide
import com.angcyo.library.ex.ClickAction
import com.angcyo.library.ex._string
import com.angcyo.library.ex.lastName
import com.angcyo.widget.DslViewHolder

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

    /**需要预览字体*/
    var itemTypeface: Typeface? = null

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

        itemHolder.tv(R.id.file_name_view)?.text = itemShowName ?: itemFilePath?.lastName()

        //
        itemTypeface?.let {
            itemHolder.tv(R.id.typeface_text_view)?.apply {
                isVisible = true
                this.typeface = it
            }
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
    }

}