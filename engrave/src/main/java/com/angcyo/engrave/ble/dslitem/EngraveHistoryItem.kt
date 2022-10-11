package com.angcyo.engrave.ble.dslitem

import com.angcyo.canvas.utils.CanvasConstant
import com.angcyo.dsladapter.DslAdapterItem
import com.angcyo.engrave.R
import com.angcyo.engrave.toModeString
import com.angcyo.glide.loadImage
import com.angcyo.item.DslTagGroupItem
import com.angcyo.item.data.LabelDesData
import com.angcyo.library.ex._string
import com.angcyo.library.ex.isDebug
import com.angcyo.library.ex.or
import com.angcyo.library.unit.convertPixelToValueUnit
import com.angcyo.objectbox.laser.pecker.entity.EngraveHistoryEntity
import com.angcyo.widget.DslViewHolder

/**
 * 雕刻历史item
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2022/07/05
 */
class EngraveHistoryItem : DslTagGroupItem() {

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

        /*
        //暂时不显示
        val zMode = engraveHistoryEntity?.zMode ?: -1
        if (zMode >= 0) {
            appendln()
            append(_string(R.string.device_setting_tips_fourteen_11))
            append(zMode.toZModeString())
        } */

        /*itemHolder.tv(R.id.lib_text_view)?.text = span {
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
            append(" ${_string(R.string.custom_speed)} ${engraveHistoryEntity?.depth}%")
            appendln()

            val pxDes = engraveHistoryEntity?.px?.toPxDes()
            append("${_string(R.string.tv_01)}: $pxDes")

            *//*暂时不显示
            val zMode = engraveHistoryEntity?.zMode ?: -1
            if (zMode >= 0) {
                appendln()
                append(_string(R.string.device_setting_tips_fourteen_11))
                append(zMode.toZModeString())
            }*//*
        }*/
    }

    override fun initLabelDesList() {
        renderLabelDesList {

            if (isDebug()) {
                add(LabelDesData("编号", "${engraveHistoryEntity?.index}"))
            }

            add(LabelDesData(_string(R.string.file_name), engraveHistoryEntity?.name.or()))

            val mode = engraveHistoryEntity?.dataMode.toModeString()
            add(LabelDesData(_string(R.string.print_file_name), mode))

            add(
                LabelDesData(
                    _string(R.string.print_range),
                    "${valueUnit.convertPixelToValueUnit(engraveHistoryEntity?.width?.toFloat())}x${
                        valueUnit.convertPixelToValueUnit(engraveHistoryEntity?.height?.toFloat())
                    }"
                )
            )

            add(LabelDesData(_string(R.string.custom_material), engraveHistoryEntity?.material))

            add(LabelDesData(_string(R.string.custom_power), "${engraveHistoryEntity?.power}%"))

            add(LabelDesData(_string(R.string.custom_speed), "${engraveHistoryEntity?.depth}%"))

            /*val pxDes = engraveHistoryEntity?.px?.toPxDes()
            add(LabelDesData(_string(R.string.resolution_ratio), pxDes))

            if ((engraveHistoryEntity?.duration ?: 0) > 0) {
                add(
                    LabelDesData(
                        _string(R.string.print_time),
                        engraveHistoryEntity?.duration?.toEngraveTime()
                    )
                )
            }*/
        }
    }

}