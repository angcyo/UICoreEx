package com.angcyo.canvas.laser.pecker

import android.graphics.Bitmap
import android.net.Uri
import androidx.lifecycle.LifecycleOwner
import com.angcyo.canvas.CanvasDelegate
import com.angcyo.canvas.Strategy
import com.angcyo.canvas.data.CanvasProjectBean
import com.angcyo.canvas.data.CanvasProjectItemBean
import com.angcyo.canvas.data.toCanvasProjectBean
import com.angcyo.canvas.data.toCanvasProjectItemList
import com.angcyo.canvas.graphics.GraphicsHelper
import com.angcyo.canvas.graphics.toBitmapItemData
import com.angcyo.canvas.graphics.toGCodeItemData
import com.angcyo.canvas.graphics.toSvgItemData
import com.angcyo.canvas.items.data.DataItemRenderer
import com.angcyo.canvas.utils.CanvasConstant
import com.angcyo.engrave.data.HawkEngraveKeys
import com.angcyo.engrave.loadingAsync
import com.angcyo.library.L
import com.angcyo.library.ex.*

/**
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2022/05/20
 */

//---打开文件---

/**异步加载, 带ui*/
fun CanvasDelegate.openCanvasFile(owner: LifecycleOwner, uri: Uri, clearOld: Boolean = true) {
    owner.loadingAsync({
        uri.readString()?.let { data ->
            openCanvasFile(data, clearOld)
        }
    })
}

/**异步加载, 带ui*/
fun CanvasDelegate.openCanvasFile(owner: LifecycleOwner, data: String, clearOld: Boolean = true) {
    owner.loadingAsync({
        openCanvasFile(data, clearOld)
    })
}

/**异步加载, 带ui*/
fun CanvasDelegate.openCanvasFile(
    owner: LifecycleOwner,
    dataBean: CanvasProjectBean?,
    clearOld: Boolean = true
) {
    dataBean?.let {
        owner.loadingAsync({
            openCanvasFile(dataBean, clearOld)
        })
    }
}

/**直接加载*/
fun CanvasDelegate.openCanvasFile(data: String, clearOld: Boolean = true) =
    openCanvasFile(data.toCanvasProjectBean(), clearOld)

/**直接加载*/
fun CanvasDelegate.openCanvasFile(dataBean: CanvasProjectBean?, clearOld: Boolean = true): Boolean {
    if (clearOld) {
        removeAllItemRenderer(Strategy.preview)
        undoManager.clear()
    }
    val result = dataBean?.data?.toCanvasProjectItemList()?.let { items ->
        items.forEach { itemData ->
            GraphicsHelper.renderItemDataBean(this, itemData, false, false, Strategy.preview)
        }
    } != null
    return result
}

//---

/**处理文件路径对应的数据, 解析成[CanvasProjectItemBean]*/
fun String?.toCanvasProjectItemBeanOfFile(): CanvasProjectItemBean? {
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

/**转成黑白图片*/
fun Bitmap?.toBlackWhiteBitmap(bmpThreshold: Int, invert: Boolean = false): String? {
    this ?: return null
    /*return OpenCV.bitmapToBlackWhite(
        this,
        bmpThreshold,
        if (invert) 1 else 0
    ).toBase64Data()*/
    return toBlackWhiteHandle(bmpThreshold, invert).toBase64Data()
}

fun Bitmap?.toBlackWhiteBitmapItemData(): CanvasProjectItemBean? {
    val bitmap = this ?: return null
    return toBitmapItemData {
        imageFilter = CanvasConstant.DATA_MODE_BLACK_WHITE //默认黑白处理
        blackThreshold = HawkEngraveKeys.lastBWThreshold
        src = bitmap.toBlackWhiteBitmap(HawkEngraveKeys.lastBWThreshold.toInt())
    }
}

/**添加一个黑白算法处理过的图片*/
fun CanvasDelegate.addBlackWhiteBitmapRender(bitmap: Bitmap?): DataItemRenderer? {
    bitmap ?: return null
    val bean = bitmap.toBlackWhiteBitmapItemData() ?: return null
    return GraphicsHelper.addRenderItemDataBean(this, bean)
}