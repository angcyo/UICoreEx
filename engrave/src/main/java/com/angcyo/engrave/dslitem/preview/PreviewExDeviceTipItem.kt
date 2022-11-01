package com.angcyo.engrave.dslitem.preview

import com.angcyo.bluetooth.fsc.laserpacker.LaserPeckerModel
import com.angcyo.bluetooth.fsc.laserpacker.parse.QuerySettingParser
import com.angcyo.core.vmApp
import com.angcyo.dsladapter.DslAdapterItem
import com.angcyo.engrave.R
import com.angcyo.library.ex._color
import com.angcyo.library.ex._string
import com.angcyo.widget.DslViewHolder
import com.angcyo.widget.span.span
import kotlin.math.max

/**
 * Z/R/S轴提示
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2022/10/21
 */
class PreviewExDeviceTipItem : PreviewTipItem() {

    //产品模式
    val laserPeckerModel = vmApp<LaserPeckerModel>()

    init {
        itemTipTextColor = _color(R.color.text_sub_color)
    }

    override fun onItemBind(
        itemHolder: DslViewHolder,
        itemPosition: Int,
        adapterItem: DslAdapterItem,
        payloads: List<Any>
    ) {

        val isForward = laserPeckerModel.deviceSettingData.value?.dir == 1 //正转
        itemTip = when {
            //第三轴
            laserPeckerModel.isZOpen() -> span {
                append(_string(R.string.device_setting_tips_fourteen_2))
                val list = listOf(
                    _string(R.string.device_setting_tips_fourteen_8),
                    _string(R.string.device_setting_tips_fourteen_9),
                    _string(R.string.device_setting_tips_fourteen_10)
                )
                append(":")
                append(list[max(0, QuerySettingParser.Z_MODEL)])
            }
            //旋转轴
            laserPeckerModel.isROpen() -> span {
                append(_string(R.string.device_ex_r_label))
                append(":")
                append(
                    if (isForward) _string(R.string.device_direction_forward) else
                        _string(R.string.device_direction_reversal)
                )
            }
            //滑台
            laserPeckerModel.isSOpen() -> span {
                append(_string(R.string.device_ex_s_label))
                append(":")
                append(
                    if (isForward) _string(R.string.device_direction_forward) else
                        _string(R.string.device_direction_reversal)
                )
            }
            //滑台多文件雕刻模式
            laserPeckerModel.isSRepMode() -> span {
                append(_string(R.string.device_s_batch_engrave_label))
                append(":")
                append(
                    if (isForward) _string(R.string.device_direction_forward) else
                        _string(R.string.device_direction_reversal)
                )
            }
            //C1握笔模块
            laserPeckerModel.isPenMode() -> span {
                append(_string(R.string.device_ex_engrave_module))
                append(":")
                append(_string(R.string.device_ex_pen_engrave_label))
            }
            else -> null
        }

        super.onItemBind(itemHolder, itemPosition, adapterItem, payloads)
    }

}