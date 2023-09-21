package com.angcyo.canvas2.laser.pecker.engrave.dslitem.engrave

import android.text.style.DynamicDrawableSpan
import androidx.annotation.DrawableRes
import com.angcyo.bluetooth.fsc.laserpacker.DeviceStateModel
import com.angcyo.bluetooth.fsc.laserpacker.HawkEngraveKeys
import com.angcyo.canvas2.laser.pecker.R
import com.angcyo.canvas2.laser.pecker.util.LPConstant
import com.angcyo.canvas2.laser.pecker.util.mmToRenderUnitValue
import com.angcyo.core.component.model.NightModel
import com.angcyo.core.vmApp
import com.angcyo.item.data.LabelDesData
import com.angcyo.library.ex._drawable
import com.angcyo.library.ex._string
import com.angcyo.library.ex.dpi
import com.angcyo.library.ex.setBounds
import com.angcyo.widget.span.DslSpan
import com.angcyo.widget.span.span

/**
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2023/05/08
 */

object EngraveIconConfig {
    val iconSize = 20 * dpi
    val iconAlignment = DynamicDrawableSpan.ALIGN_BOTTOM
}

/**材质*/
fun materialData(des: CharSequence?) = LabelDesData(
    span {
        if (HawkEngraveKeys.enableConfigIcon) {
            appendDrawable(R.drawable.engrave_config_material_svg)
        } else {
            append(_string(R.string.custom_material))
        }
    },
    des,
    _string(R.string.custom_material)
)

/**分辨率*/
fun resolutionData(des: CharSequence?) = LabelDesData(span {
    if (HawkEngraveKeys.enableConfigIcon) {
        appendDrawable(R.drawable.engrave_config_dpi_svg)
    } else {
        append(_string(R.string.resolution_ratio))
    }
}, des, _string(R.string.resolution_ratio))

/**加速级别*/
fun precisionData(des: CharSequence?) = LabelDesData(span {
    if (HawkEngraveKeys.enableConfigIcon) {
        appendDrawable(R.drawable.engrave_config_precision_svg)
    } else {
        append(_string(R.string.engrave_precision))
    }
}, des, _string(R.string.engrave_precision))

/**雕刻速度*/
fun velocityData(des: CharSequence?) = LabelDesData(span {
    if (HawkEngraveKeys.enableConfigIcon) {
        appendDrawable(R.drawable.engrave_config_velocity_svg)
    } else {
        append(_string(R.string.engrave_speed))
    }
}, des, _string(R.string.engrave_speed))

/**功率*/
fun powerData(des: Any?) = LabelDesData(
    span {
        if (HawkEngraveKeys.enableConfigIcon) {
            appendDrawable(R.drawable.engrave_config_power_svg)
        } else {
            append(_string(R.string.custom_power))
        }
    }, "${des}%", _string(R.string.custom_power)
)

/**深度*/
fun depthData(des: Any?) = LabelDesData(
    span {
        if (HawkEngraveKeys.enableConfigIcon) {
            appendDrawable(R.drawable.engrave_config_depth_svg)
        } else {
            append(_string(R.string.custom_speed))
        }
    }, "${des}%", _string(R.string.custom_speed)
)

/**次数*/
fun timesData(printTimes: Any?, times: Any?) = LabelDesData(
    span {
        if (HawkEngraveKeys.enableConfigIcon) {
            appendDrawable(R.drawable.engrave_config_times_svg)
        } else {
            append(_string(R.string.print_times))
        }
    }, "${printTimes}/${times}", _string(R.string.print_times)
)

/**加工时长*/
fun workTimeData(des: CharSequence?) = LabelDesData(
    span {
        if (HawkEngraveKeys.enableConfigIcon) {
            appendDrawable(R.drawable.engrave_config_work_time_svg)
        } else {
            append(_string(R.string.work_time))
        }
    }, des, _string(R.string.work_time)
)

/**剩余时长*/
fun remainingTimesData(des: CharSequence?) = LabelDesData(
    span {
        if (HawkEngraveKeys.enableConfigIcon) {
            appendDrawable(R.drawable.engrave_config_remaining_time_svg)
        } else {
            append(_string(R.string.remaining_time))
        }
    }, des, _string(R.string.remaining_time)
)

/**[sizeData]*/
fun widthHeightData(width: Float, height: Float, isMm: Boolean = true): LabelDesData {
    val valueUnit = LPConstant.renderUnit
    val w = valueUnit.formatValue(
        if (isMm) width.mmToRenderUnitValue() else valueUnit.convertPixelToValue(width),
        true,
        false
    )
    val h = valueUnit.formatValue(
        if (isMm) height.mmToRenderUnitValue() else valueUnit.convertPixelToValue(height),
        true,
        false
    )
    return sizeData(buildString {
        append(w)
        append("×")
        append(h)
        append(valueUnit.getUnit())
    })
}

/**镭雕尺寸*/
fun sizeData(des: CharSequence?) = LabelDesData(
    span {
        if (HawkEngraveKeys.enableConfigIcon) {
            appendDrawable(R.drawable.engrave_config_size_svg)
        } else {
            append(_string(R.string.print_range))
        }
    }, des, _string(R.string.print_range)
)

/**机型-产品*/
fun productNameData(des: CharSequence?) = LabelDesData(
    span {
        if (HawkEngraveKeys.enableConfigIcon) {
            appendDrawable(R.drawable.engrave_config_product_svg)
        } else {
            append(_string(R.string.device_models))
        }
    }, des, _string(R.string.device_models)
)

/**机型-雕刻模块*/
fun moduleData(type: Byte, moduleState: Int?): LabelDesData {
    val laserInfo = vmApp<DeviceStateModel>().getDeviceLaserModule(type, moduleState)
    val label = laserInfo?.toLabel() ?: "$type"
    return LabelDesData(
        span {
            if (HawkEngraveKeys.enableConfigIcon) {
                appendDrawable(R.drawable.engrave_config_module_svg)
            } else {
                append(_string(R.string.laser_type))
            }
        }, label, _string(R.string.laser_type)
    )
}

fun DslSpan.appendDrawable(@DrawableRes resId: Int, size: Int = EngraveIconConfig.iconSize) {
    val nightModel = vmApp<NightModel>()
    appendDrawable(
        nightModel.tintDrawableNight(_drawable(resId))?.setBounds(size, size),
        EngraveIconConfig.iconAlignment
    )
}