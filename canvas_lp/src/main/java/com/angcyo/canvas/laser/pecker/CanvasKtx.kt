package com.angcyo.canvas.laser.pecker

import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.result.ActivityResultCaller
import androidx.fragment.app.Fragment
import androidx.lifecycle.LifecycleOwner
import com.angcyo.canvas.CanvasDelegate
import com.angcyo.canvas.data.CanvasDataBean
import com.angcyo.canvas.data.ItemDataBean
import com.angcyo.canvas.data.ItemDataBean.Companion.DEFAULT_THRESHOLD_SPACE
import com.angcyo.canvas.graphics.GraphicsHelper
import com.angcyo.canvas.graphics.toBitmapItemData
import com.angcyo.canvas.graphics.toGCodeItemData
import com.angcyo.canvas.graphics.toSvgItemData
import com.angcyo.canvas.items.data.DataItemRenderer
import com.angcyo.canvas.utils.CanvasConstant
import com.angcyo.coroutine.launchLifecycle
import com.angcyo.coroutine.withBlock
import com.angcyo.dialog.hideLoading
import com.angcyo.dialog.loading2
import com.angcyo.drawable.loading.TGStrokeLoadingDrawable
import com.angcyo.http.base.fromJson
import com.angcyo.http.base.listType
import com.angcyo.library.L
import com.angcyo.library.ex.*
import com.angcyo.library.toastQQ
import com.angcyo.opencv.OpenCV
import java.util.concurrent.atomic.AtomicBoolean

/**
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2022/05/20
 */

/**异步加载, 带loading dialog*/
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
    val dialog = loading2(layoutId = R.layout.canvas_loading_layout, config = {
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

//---打开文件---

/**异步加载, 带ui*/
fun CanvasDelegate.openCanvasFile(owner: LifecycleOwner, uri: Uri) {
    owner.loadingAsync({
        uri.readString()?.let { data ->
            openCanvasFile(data)
        }
    })
}

/**异步加载, 带ui*/
fun CanvasDelegate.openCanvasFile(owner: LifecycleOwner, data: String) {
    owner.loadingAsync({
        openCanvasFile(data)
    })
}

/**直接加载*/
fun CanvasDelegate.openCanvasFile(data: String) {
    val bean = data.fromJson<CanvasDataBean>()
    bean?.data?.fromJson<List<ItemDataBean>>(listType(ItemDataBean::class.java))?.let { items ->
        items.forEach { itemData ->
            GraphicsHelper.renderItemDataBean(this, itemData, false)
        }
    }
}

/**处理路径对应的数据, 解析成[ItemDataBean]*/
fun String?.toItemDataBean(): ItemDataBean? {
    val path = this ?: return null
    if (path.endsWith(CanvasConstant.GCODE_EXT)) {
        val text = path.file().readText()
        return text.toGCodeItemData()
    } else if (path.endsWith(CanvasConstant.SVG_EXT)) {
        val text = path.file().readText()
        return text.toSvgItemData()
    } else if (path.isImageType()) {
        val bitmap = path.toBitmap()
        return bitmap.toBlackWhiteBitmapItemData()
    } else {
        L.w("无法处理的文件路径:${path}")
    }
    return null
}

fun Bitmap?.toBlackWhiteBitmapItemData(): ItemDataBean? {
    return toBitmapItemData {
        imageFilter = CanvasConstant.DATA_MODE_BLACK_WHITE //默认黑白处理
        src = OpenCV.bitmapToBlackWhite(
            this@toBlackWhiteBitmapItemData!!,
            DEFAULT_THRESHOLD_SPACE.toInt(),
            0
        ).toBase64Data()
    }
}

/**添加一个黑白算法处理过的图片*/
fun CanvasDelegate.addBlackWhiteBitmapRender(bitmap: Bitmap?): DataItemRenderer? {
    bitmap ?: return null
    val bean = bitmap.toBlackWhiteBitmapItemData() ?: return null
    return GraphicsHelper.addRenderItemDataBean(this, bean)
}