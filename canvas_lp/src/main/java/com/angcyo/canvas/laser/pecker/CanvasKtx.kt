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
import com.angcyo.canvas.utils.CanvasDataHandleOperate
import com.angcyo.engrave.data.HawkEngraveKeys
import com.angcyo.engrave.engraveLoadingAsync
import com.angcyo.http.base.toJson
import com.angcyo.http.rx.doBack
import com.angcyo.library.L
import com.angcyo.library.ex.*
import com.angcyo.library.utils.writeTo
import java.io.File

/**
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2022/05/20
 */

//---状态存储和恢复---

/**保存实例数据, 实际就是保存工程数据
 * [name] 保存的工程文件名, 请包含后缀
 * [async] 是否异步保存
 * */
fun CanvasDelegate.saveInstanceState(name: String = ".temp", async: Boolean = true) {
    if (itemRendererCount <= 0) return
    val save = Runnable {
        val bean = getCanvasDataBean(null, HawkEngraveKeys.projectOutSize)
        val json = bean.toJson()
        json.writeTo(
            CanvasDataHandleOperate._defaultProjectOutputFile(name, false),
            false
        )
    }
    if (async) {
        doBack {
            save.run()
        }
    } else {
        //同步保存
        save.run()
    }
}

/**恢复实例数据
 * [saveInstanceState]*/
fun CanvasDelegate.restoreInstanceState(name: String = ".temp", async: Boolean = true) {
    val restore = Runnable {
        val file = CanvasDataHandleOperate._defaultProjectOutputFile(name, false)
        openCanvasFile(file, true)
    }
    if (async) {
        doBack {
            restore.run()
        }
    } else {
        //同步保存
        restore.run()
    }
}

//---打开文件---

/**异步加载, 带ui*/
fun CanvasDelegate.openCanvasFile(owner: LifecycleOwner, uri: Uri, clearOld: Boolean = true) {
    owner.engraveLoadingAsync({
        uri.readString()?.let { data ->
            openCanvasFile(data, clearOld)
        }
    })
}

/**异步加载, 带ui*/
fun CanvasDelegate.openCanvasFile(owner: LifecycleOwner, data: String, clearOld: Boolean = true) {
    owner.engraveLoadingAsync({
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
        owner.engraveLoadingAsync({
            openCanvasFile(dataBean, clearOld)
        })
    }
}


/**直接从文件中加载*/
fun CanvasDelegate.openCanvasFile(file: File?, clearOld: Boolean = true) =
    openCanvasFile(file?.readText(), clearOld)

/**直接从字符串中加载*/
fun CanvasDelegate.openCanvasFile(data: String?, clearOld: Boolean = true) =
    openCanvasFile(data?.toCanvasProjectBean(), clearOld)

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