package com.angcyo.engrave.ble.dslitem

import com.angcyo.bluetooth.fsc.laserpacker.LaserPeckerHelper
import com.angcyo.canvas.core.convertPixelToValueUnit
import com.angcyo.canvas.utils.CanvasConstant
import com.angcyo.dsladapter.DslAdapterItem
import com.angcyo.engrave.R
import com.angcyo.engrave.toModeString
import com.angcyo.engrave.toEngraveTime
import com.angcyo.glide.loadImage
import com.angcyo.library.ex._string
import com.angcyo.library.ex.or
import com.angcyo.objectbox.laser.pecker.entity.EngraveHistoryEntity
import com.angcyo.widget.DslViewHolder
import com.angcyo.widget.span.span

/**
 * 雕刻历史item
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2022/07/05
 */
class EngraveHistoryItem : DslAdapterItem() {

    var engraveHistoryEntity: EngraveHistoryEntity? = null

    val valueUnit = CanvasConstant.valueUnit

    init {
        itemLayoutId = R.layout.item_engrave_history_layout
    }

    override fun onItemBind(
        itemHolder: DslViewHolder,
        itemPosition: Int,
        adapterItem: DslAdapterItem,
        payloads: List<Any>
    ) {
        super.onItemBind(itemHolder, itemPosition, adapterItem, payloads)

        itemHolder.img(R.id.lib_image_view)?.loadImage(engraveHistoryEntity?.previewDataPath)

        itemHolder.tv(R.id.lib_text_view)?.text = span {
            append("${_string(R.string.file_name)} ${engraveHistoryEntity?.name.or()}")
            appendln()

            val mode = engraveHistoryEntity?.optionMode.toModeString()
            append("${_string(R.string.print_file_name)}: $mode")
            appendln()

            if ((engraveHistoryEntity?.duration ?: 0) > 0) {
                append("${_string(R.string.print_time)}: ${engraveHistoryEntity?.duration?.toEngraveTime()}")
                appendln()
            }

            append("${_string(R.string.print_range)}: ")
            append(
                "${_string(R.string.wide)}${
                    valueUnit.convertPixelToValueUnit(engraveHistoryEntity?.width?.toFloat())
                }"
            )
            append(
                " ${_string(R.string.high)}${
                    valueUnit.convertPixelToValueUnit(engraveHistoryEntity?.height?.toFloat())
                }"
            )
            appendln()

            append("${_string(R.string.custom_material)} ${engraveHistoryEntity?.material}")
            appendln()

            append("${_string(R.string.custom_power)} ${engraveHistoryEntity?.power}%")
            appendln()

            append("${_string(R.string.custom_speed)} ${engraveHistoryEntity?.depth}%")
            appendln()

            val pxDes = LaserPeckerHelper.findPxInfo(engraveHistoryEntity?.px)?.des
            append("${_string(R.string.tv_01)}: $pxDes")

            /*暂时不显示
            val zMode = engraveHistoryEntity?.zMode ?: -1
            if (zMode >= 0) {
                appendln()
                append(_string(R.string.device_setting_tips_fourteen_11))
                append(zMode.toZModeString())
            }*/
        }
    }

}