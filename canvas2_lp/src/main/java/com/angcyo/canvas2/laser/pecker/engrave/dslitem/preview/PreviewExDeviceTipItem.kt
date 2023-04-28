package com.angcyo.canvas2.laser.pecker.engrave.dslitem.preview

import com.angcyo.bluetooth.fsc.laserpacker.DeviceStateModel
import com.angcyo.bluetooth.fsc.laserpacker.HawkEngraveKeys
import com.angcyo.bluetooth.fsc.laserpacker.LaserPeckerHelper
import com.angcyo.bluetooth.fsc.laserpacker.LaserPeckerModel
import com.angcyo.bluetooth.fsc.laserpacker.parse.QuerySettingParser
import com.angcyo.canvas2.laser.pecker.R
import com.angcyo.core.vmApp
import com.angcyo.dsladapter.DslAdapterItem
import com.angcyo.library.ex._color
import com.angcyo.library.ex._string
import com.angcyo.library.ex.ensureInt
import com.angcyo.library.ex.size
import com.angcyo.library.getAppString
import com.angcyo.objectbox.laser.pecker.entity.EngraveConfigEntity
import com.angcyo.widget.DslViewHolder
import com.angcyo.widget.span.DslSpan
import com.angcyo.widget.span.span

/**
 * Z/R/S轴 光源 功率 焦距提示
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2022/10/21
 */
class PreviewExDeviceTipItem : PreviewTipItem() {

    //产品模式
    val laserPeckerModel = vmApp<LaserPeckerModel>()
    val deviceStateModel = vmApp<DeviceStateModel>()

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
            !laserPeckerModel.isCSeries() && laserPeckerModel.isZOpen() -> span {
                append(_string(R.string.device_ex_z_label))
                append(":")
                append(
                    getAppString(QuerySettingParser.Z_MODEL_STR)
                        ?: getAppString(QuerySettingParser.Z_MODEL_FLAT)
                )

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
            laserPeckerModel.isCSeries() -> {
                val moduleState = deviceStateModel.deviceStateData.value?.moduleState
                if (moduleState == null) {
                    null
                } else {
                    span {
                        append(_string(R.string.device_ex_engrave_module))
                        append(":")
                        append(deviceStateModel.getDeviceModuleLabel(moduleState))
                        appendFocalDistance()
                        appendSupportTip()
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
    fun DslSpan.appendEngraveConfig() {
        val laserTypeList = LaserPeckerHelper.findProductSupportLaserTypeList()
        if (laserTypeList.size() == 1) {
            //只有一种光,则直接显示
            laserTypeList.first().let {
                append(" ${it.wave}nm ${it.power.ensureInt()}w")
            }
        } else {
            (itemEngraveConfigEntity?.type ?: HawkEngraveKeys.lastType.toByte()).also { type ->
                //多种光,则显示当前选择的光
                val laser = laserTypeList.find { it.type == type }
                laser?.let {
                    append(" ${it.wave}nm ${it.power.ensureInt()}w")
                }
            }
        }
        appendFocalDistance()
        appendSupportTip()
    }

    /**焦距信息*/
    fun DslSpan.appendFocalDistance() {
        laserPeckerModel.productInfoData.value?.let {
            it.focalDistance?.let {
                append(" ${_string(R.string.focal_distance)}:${it}mm")
            }
        }
    }

    /**是否支持的固件版本*/
    fun DslSpan.appendSupportTip() {
        if (!LaserPeckerHelper.isSupportFirmware()) {
            append(" ${_string(R.string.not_support_tip)}") {
                foregroundColor = _color(R.color.error)
            }
        }
    }

}