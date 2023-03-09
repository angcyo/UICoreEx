package com.angcyo.canvas2.laser.pecker.dslitem

import com.angcyo.canvas2.laser.pecker.R
import com.angcyo.dsladapter.DslAdapterItem
import com.angcyo.item.DslSeekBarInfoItem
import com.angcyo.library.ex._color
import com.angcyo.widget.DslViewHolder
import com.angcyo.widget.progress.DslSeekBar

/**
 * 画布滑块Item, 带气泡提示
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2023/03/09
 */
class CanvasSeekBarItem : DslSeekBarInfoItem() {

    init {
        itemExtendLayoutId = R.layout.dsl_extent_seek_little_item
    }

    override fun onItemBind(
        itemHolder: DslViewHolder,
        itemPosition: Int,
        adapterItem: DslAdapterItem,
        payloads: List<Any>
    ) {
        super.onItemBind(itemHolder, itemPosition, adapterItem, payloads)

        itemHolder.v<DslSeekBar>(R.id.lib_seek_view)?.apply {
            val color = _color(R.color.canvas_primary)
            setBgGradientColors("${_color(R.color.canvas_line)}")
            setTrackGradientColors("$color")
            updateThumbColor(color)
        }
    }

    override fun onItemChangeListener(item: DslAdapterItem) {
        //super.onItemChangeListener(item)
    }

}