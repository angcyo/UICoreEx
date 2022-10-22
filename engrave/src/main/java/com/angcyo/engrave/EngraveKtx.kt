package com.angcyo.engrave

import android.app.Activity
import android.app.Dialog
import android.content.Context
import androidx.activity.result.ActivityResultCaller
import androidx.fragment.app.Fragment
import androidx.lifecycle.LifecycleOwner
import com.angcyo.bluetooth.fsc.laserpacker.LaserPeckerHelper
import com.angcyo.bluetooth.fsc.laserpacker.command.DataCmd
import com.angcyo.canvas.utils.CanvasConstant
import com.angcyo.coroutine.launchLifecycle
import com.angcyo.coroutine.withBlock
import com.angcyo.dialog.hideLoading
import com.angcyo.dialog.loading2
import com.angcyo.drawable.loading.TGStrokeLoadingDrawable
import com.angcyo.library.L
import com.angcyo.library.ex.*
import com.angcyo.library.toastQQ
import java.util.concurrent.atomic.AtomicBoolean

/**
 * 扩展
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2022/06/20
 */


/**异步加载, 带loading dialog
 * [LifecycleOwner]
 * */
fun <T> LifecycleOwner.loadingAsync(block: () -> T?, action: (T?) -> Unit = {}) {
    val context = this
    if (context is ActivityResultCaller) {
        context.strokeLoading { cancel, loadEnd ->
            context.launchLifecycle {
                val result = withBlock { block() }
                action(result)
                loadEnd(result, null)
            }
        }
    } else {
        L.w("context is not ActivityResultCaller!")
    }
}

/**
 * TGStrokeLoadingDrawable 加载样式的loading
 * [cancel] 是否允许被取消*/
fun ActivityResultCaller.strokeLoading(
    cancel: Boolean = false,
    showErrorToast: Boolean = false,
    action: (isCancel: AtomicBoolean, loadEnd: (data: Any?, error: Throwable?) -> Unit) -> Unit
): Dialog? {
    return try {
        val activity = when (this) {
            is Fragment -> activity
            is Activity -> this
            is Context -> this
            else -> null
        } ?: return null
        activity.strokeLoading2(cancel, showErrorToast, action)
    } catch (e: Exception) {
        e.printStackTrace()
        return null
    }
}

/**扩展的对象不一样
 * [Context]*/
fun Context.strokeLoading2(
    cancel: Boolean = false,
    showErrorToast: Boolean = false,
    action: (isCancel: AtomicBoolean, loadEnd: (data: Any?, error: Throwable?) -> Unit) -> Unit
): Dialog? {
    val isCancel = AtomicBoolean(false)
    val dialog = loading2(layoutId = R.layout.engrave_loading_layout, config = {
        cancelable = cancel
        onDialogInitListener = { dialog, dialogViewHolder ->
            val loadingDrawable = TGStrokeLoadingDrawable().apply {
                loadingOffset = 6 * dp
                loadingWidth = 6 * dp
                indeterminateSweepAngle = 1f
                loadingBgColor = "#ffffff".toColorInt()
                loadingColor = loadingBgColor
            }
            dialogViewHolder.view(R.id.lib_loading_view)?.setBgDrawable(loadingDrawable)
        }
    }) { dialog ->
        isCancel.set(true)
        action(isCancel) { _, _ ->
            //no op
        }
    }

    isCancel.set(false)
    action(isCancel) { data, error ->
        if (error != null) {
            //失败
            if (showErrorToast) {
                toastQQ(error.message)
            }
            hideLoading(error.message)
        } else {
            hideLoading()
        }
    }

    return dialog
}

/**分:秒 的时间格式*/
fun Long?.toEngraveTime() = this?.toElapsedTime(
    pattern = intArrayOf(-1, 1, 1),
    units = arrayOf("", "", ":", ":", ":")
)

/**模式字符串*/
fun Int?.toModeString() = when (this) {
    CanvasConstant.DATA_MODE_PRINT -> _string(R.string.canvas_prints)
    CanvasConstant.DATA_MODE_GCODE -> _string(R.string.canvas_gcode)
    CanvasConstant.DATA_MODE_BLACK_WHITE -> _string(R.string.canvas_black_white)
    CanvasConstant.DATA_MODE_DITHERING -> _string(R.string.canvas_dithering)
    CanvasConstant.DATA_MODE_GREY -> _string(R.string.canvas_grey)
    CanvasConstant.DATA_MODE_SEAL -> _string(R.string.canvas_seal)
    else -> null
}

/**激光类型字符串*/
fun Byte?.toLaserTypeString() = when (this) {
    LaserPeckerHelper.LASER_TYPE_WHITE -> _string(R.string.laser_type_white)
    else -> _string(R.string.laser_type_blue)
}

/**将数据模式转换成雕刻类型
 * 数据模式:
 * [com.angcyo.canvas.utils.CanvasConstant.DATA_MODE_BLACK_WHITE]
 * [com.angcyo.canvas.utils.CanvasConstant.DATA_MODE_GCODE]
 * [com.angcyo.canvas.utils.CanvasConstant.DATA_MODE_DITHERING]
 * [com.angcyo.canvas.utils.CanvasConstant.DATA_MODE_GREY]
 *
 * 雕刻类型:
 * [com.angcyo.bluetooth.fsc.laserpacker.command.DataCmd.ENGRAVE_TYPE_BITMAP_DITHERING]
 * [com.angcyo.bluetooth.fsc.laserpacker.command.DataCmd.ENGRAVE_TYPE_GCODE]
 * [com.angcyo.bluetooth.fsc.laserpacker.command.DataCmd.ENGRAVE_TYPE_BITMAP_PATH]
 * [com.angcyo.bluetooth.fsc.laserpacker.command.DataCmd.ENGRAVE_TYPE_BITMAP]
 * */
fun Int.toEngraveTypeOfDataMode() = when (this) {
    CanvasConstant.DATA_MODE_BLACK_WHITE -> DataCmd.ENGRAVE_TYPE_BITMAP_PATH
    CanvasConstant.DATA_MODE_GCODE -> DataCmd.ENGRAVE_TYPE_GCODE
    CanvasConstant.DATA_MODE_GREY -> DataCmd.ENGRAVE_TYPE_BITMAP
    else -> DataCmd.ENGRAVE_TYPE_BITMAP_DITHERING
}

/**将雕刻类型字符串化*/
fun Int.toEngraveDataTypeStr() = when (this) {
    DataCmd.ENGRAVE_TYPE_BITMAP_DITHERING -> "抖动"
    DataCmd.ENGRAVE_TYPE_GCODE -> "GCode"
    DataCmd.ENGRAVE_TYPE_BITMAP_PATH -> "图片线段"
    DataCmd.ENGRAVE_TYPE_BITMAP -> "图片"
    else -> "未知"
}
