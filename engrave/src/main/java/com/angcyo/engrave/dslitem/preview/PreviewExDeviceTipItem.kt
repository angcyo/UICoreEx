package com.angcyo.engrave.dslitem.preview

import com.angcyo.bluetooth.fsc.laserpacker.LaserPeckerHelper
import com.angcyo.bluetooth.fsc.laserpacker.LaserPeckerModel
import com.angcyo.bluetooth.fsc.laserpacker.parse.QuerySettingParser
import com.angcyo.core.vmApp
import com.angcyo.dsladapter.DslAdapterItem
import com.angcyo.engrave.R
import com.angcyo.engrave.ble.DeviceSettingFragment
import com.angcyo.library.ex._color
import com.angcyo.library.ex._string
import com.angcyo.library.ex.ensureInt
import com.angcyo.library.ex.size
import com.angcyo.objectbox.laser.pecker.entity.EngraveConfigEntity
import com.angcyo.widget.DslViewHolder
import com.angcyo.widget.span.span
import kotlin.math.max

/**
 * Z/R/S轴 光源 功率 焦距提示
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2022/10/21
 */
class PreviewExDeviceTipItem : PreviewTipItem() {

    //产品模式
    val laserPeckerModel = vmApp<LaserPeckerModel>()

    /**雕刻配置信息, 用来显示光源等信息*/
    var itemEngraveConfigEntity: EngraveConfigEntity? = null

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
                append(_string(R.string.device_ex_z_label))
                val list = DeviceSettingFragment.getZDirSegmentList()
                append(":")
                append(list[max(0, QuerySettingParser.Z_MODEL)])

                append(" ")
                append(
                    if (isForward) _string(R.string.device_direction_forward) else
                        _string(R.string.device_direction_reversal)
                )

                appendEngraveConfig()
            }
            //旋转轴
            laserPeckerModel.isROpen() -> span {
                append(_string(R.string.device_ex_r_label))
                appendEngraveConfig()
            }
            //滑台
            laserPeckerModel.isSOpen() -> span {
                append(_string(R.string.device_ex_s_label))
                appendEngraveConfig()
            }
            //滑台多文件雕刻模式
            laserPeckerModel.isSRepMode() -> span {
                append(_string(R.string.device_s_batch_engrave_label))
                appendEngraveConfig()
            }
            //C1握笔模块
            laserPeckerModel.isC1() -> {
                val moduleState = laserPeckerModel.deviceStateData.value?.moduleState
                if (moduleState == null) {
                    null
                } else {
                    span {
                        append(_string(R.string.device_ex_engrave_module))
                        append(":")
                        when (moduleState) {
                            //0 5W激光
                            0 -> append("5W 450nm")
                            //1 10W激光
                            1 -> append("10W 450nm")
                            //2 20W激光
                            2 -> append("20W 450nm")
                            //3 1064激光
                            3 -> append("2W 1064nm")
                            //4 单色笔模式
                            4 -> append(_string(R.string.engrave_module_single_pen))
                            //5 彩色笔模式
                            5 -> append(_string(R.string.engrave_module_color_pen))
                            //6 刀切割模式
                            6 -> append(_string(R.string.engrave_module_knife_cutting))
                            //7 CNC模式
                            7 -> append(_string(R.string.engrave_module_cnc))
                            else -> append("Unknown $moduleState")
                        }
                        appendFocalDistance()
                    }
                }
            }
            else -> span {
                appendEngraveConfig()
            }
        }

        super.onItemBind(itemHolder, itemPosition, adapterItem, payloads)
    }

    /**光源*/
    fun Appendable.appendEngraveConfig() {
        val laserTypeList = LaserPeckerHelper.findProductSupportLaserTypeList()
        if (laserTypeList.size() == 1) {
            //只有一种光,则直接显示
            laserTypeList.first().let {
                append(" ${it.wave}nm ${it.power.ensureInt()}w")
            }
        } else {
            itemEngraveConfigEntity?.apply {
                val laser = laserTypeList.find { it.type == type }
                laser?.let {
                    append(" ${it.wave}nm ${it.power.ensureInt()}w")
                }
            }
        }
        appendFocalDistance()
    }

    /**焦距信息*/
    fun Appendable.appendFocalDistance() {
        laserPeckerModel.productInfoData.value?.let {
            it.focalDistance?.let {
                append(" ${_string(R.string.focal_distance)}:${it}mm")
            }
        }
    }

}