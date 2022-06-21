package com.angcyo.engrave.canvas.dslitem

import com.angcyo.dsladapter.DslAdapterItem
import com.angcyo.engrave.R
import com.angcyo.item.style.IImageItem
import com.angcyo.item.style.ImageItemConfig
import com.angcyo.item.style.itemLoadImage

/**
 * 画布数字回退输入item
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2022/05/14
 */
class CanvasNumberImageItem : DslAdapterItem(), IImageItem {

    override var imageItemConfig: ImageItemConfig = ImageItemConfig()

    init {
        itemLayoutId = R.layout.canvas_number_image_item_layout
        itemClickThrottleInterval = 0
        itemLoadImage = R.drawable.canvas_backspace
    }
}