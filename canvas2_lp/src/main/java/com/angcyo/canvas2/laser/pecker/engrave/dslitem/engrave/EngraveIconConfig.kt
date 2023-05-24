package com.angcyo.canvas2.laser.pecker.engrave.dslitem.engrave

import android.text.style.DynamicDrawableSpan
import androidx.annotation.DrawableRes
import com.angcyo.bluetooth.fsc.laserpacker.HawkEngraveKeys
import com.angcyo.canvas2.laser.pecker.R
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

fun DslSpan.appendDrawable(@DrawableRes resId: Int) {
    appendDrawable(
        _drawable(resId)?.setBounds(
            EngraveIconConfig.iconSize,
            EngraveIconConfig.iconSize
        ), EngraveIconConfig.iconAlignment
    )
}