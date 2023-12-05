package com.angcyo.canvas2.laser.pecker.dslitem

import com.angcyo.canvas2.laser.pecker.R
import com.angcyo.dsladapter.DslAdapterItem
import com.angcyo.item.DslSeekBarInfoItem
import com.angcyo.item.keyboard.numberKeyboardDialog
import com.angcyo.item.style.itemInfoText
import com.angcyo.library.component.RegionTouchDetector
import com.angcyo.library.ex._color
import com.angcyo.widget.DslViewHolder
import com.angcyo.widget.progress.DslSeekBar

/**
 * 画布滑块Item, 带气泡提示
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2023/03/09
 */
class CanvasSeekBarItem : DslSeekBarInfoItem() {

    /**键盘输入相关属性*/
    /**进度值的类型*/
    var itemSeekProgressType: Any? = null

    /**键盘输入的最小值*/
    var itemNumberMinValue: Any? = null

    /**键盘输入的最大值*/
    var itemNumberMaxValue: Any? = null

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

            if (itemSeekProgressType != null) {
                progressValueTouchAction = { touchType ->
                    if (touchType == RegionTouchDetector.TOUCH_TYPE_CLICK) {
                        //点击事件类型
                        itemHolder.context.numberKeyboardDialog {
                            dialogTitle = itemInfoText
                            numberValueType = itemSeekProgressType
                            numberMinValue = itemNumberMinValue
                            numberMaxValue = itemNumberMaxValue
                            numberValue = updateProgressValue(itemSeekProgress)
                            onNumberResultAction = {
                                it?.let {
                                    val value = getProgressValue() ?: itemSeekProgress
                                    itemSeekProgress = value

                                    val fraction = value / 100
                                    onItemSeekChanged(value, fraction, true)
                                    itemSeekTouchEnd(value, fraction)
                                    updateAdapterItem()
                                }
                                false
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onItemChangeListener(item: DslAdapterItem) {
        //super.onItemChangeListener(item)
    }

}