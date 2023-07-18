package com.angcyo.laserpacker.device

import android.app.Activity
import android.app.Dialog
import android.content.Context
import androidx.activity.result.ActivityResultCaller
import androidx.fragment.app.Fragment
import androidx.lifecycle.LifecycleOwner
import com.angcyo.bluetooth.fsc.laserpacker.LaserPeckerHelper
import com.angcyo.core.loadingAsyncTimeout
import com.angcyo.coroutine.launchLifecycle
import com.angcyo.coroutine.withBlock
import com.angcyo.dialog.LoadingDialog
import com.angcyo.dialog.hideLoading
import com.angcyo.dialog.loading
import com.angcyo.drawable.loading.TGStrokeLoadingDrawable
import com.angcyo.library.IActivityProvider
import com.angcyo.library.L
import com.angcyo.library.ex._string
import com.angcyo.library.ex.dp
import com.angcyo.library.ex.setBgDrawable
import com.angcyo.library.ex.toColorInt
import com.angcyo.library.ex.toMinuteTime
import com.angcyo.library.toastQQ
import java.util.concurrent.atomic.AtomicBoolean

/**
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2023/03/28
 */

/**异步加载, 带loading dialog
 * [LifecycleOwner]
 * */
fun <T> LifecycleOwner.engraveLoadingAsync(block: () -> T?, action: (T?) -> Unit = {}) {
    val context = this
    if (context is ActivityResultCaller) {
        context.engraveStrokeLoadingCaller { cancel, loadEnd ->
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
 * 当异步执行多久之后, 仍未返回结果时, 则自动显示loading
 * [timeout] 异步执行超时时长, 毫秒
 * */
fun <T> LifecycleOwner.engraveLoadingAsyncTimeout(
    block: () -> T?,
    timeout: Long = LoadingDialog.LOADING_TIMEOUT,
    action: (T?) -> Unit = {}
) {
    loadingAsyncTimeout(block, { context ->
        if (context is ActivityResultCaller) {
            context.engraveStrokeLoadingCaller { cancel, loadEnd ->
                //no op
            }
        } else {
            L.w("context is not ActivityResultCaller!")
            null
        }
    }, timeout, action)
}

/**
 * TGStrokeLoadingDrawable 加载样式的loading
 * [cancel] 是否允许被取消*/
fun ActivityResultCaller.engraveStrokeLoadingCaller(
    cancel: Boolean = false,
    showErrorToast: Boolean = false,
    action: (isCancel: AtomicBoolean, loadEnd: (data: Any?, error: Throwable?) -> Unit) -> Unit
): Dialog? {
    return try {
        val activity = when (this) {
            is Fragment -> activity
            is Activity -> this
            is Context -> this
            is IActivityProvider -> getActivityContext()
            else -> null
        } ?: return null
        activity.engraveStrokeLoading(cancel, showErrorToast, action)
    } catch (e: Exception) {
        e.printStackTrace()
        return null
    }
}

/**扩展的对象不一样
 * [Context]*/
fun Context.engraveStrokeLoading(
    cancel: Boolean = false,
    showErrorToast: Boolean = false,
    action: (isCancel: AtomicBoolean, loadEnd: (data: Any?, error: Throwable?) -> Unit) -> Unit
): Dialog? {
    val isCancel = AtomicBoolean(false)
    val dialog = loading(layoutId = R.layout.engrave_loading_layout, config = {
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

/**激光类型字符串
 * [com.angcyo.bluetooth.fsc.laserpacker.data.LaserTypeInfo]*/
fun Byte?.toLaserTypeString(includeWave: Boolean = false) = buildString {
    val laserType = this@toLaserTypeString
    append(
        when (laserType) {
            LaserPeckerHelper.LASER_TYPE_WHITE -> _string(R.string.laser_type_white)
            else -> _string(R.string.laser_type_blue)
        }
    )
    if (includeWave) {
        append(laserType.toLaserWave())
    }
}

/**转换成波长*/
fun Byte?.toLaserWave() = when (this) {
    LaserPeckerHelper.LASER_TYPE_WHITE -> 1064
    else -> 450
}

fun Int?.toLaserTypeString(includeWave: Boolean = false) =
    this?.toByte()?.toLaserTypeString(includeWave)

/**分:秒 的时间格式*/
fun Long?.toEngraveTime() = this?.toMinuteTime()
