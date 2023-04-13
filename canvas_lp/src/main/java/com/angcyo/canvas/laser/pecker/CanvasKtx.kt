package com.angcyo.canvas.laser.pecker

import android.graphics.Bitmap
import android.net.Uri
import androidx.annotation.AnyThread
import androidx.lifecycle.LifecycleOwner
import com.angcyo.canvas.CanvasDelegate
import com.angcyo.canvas.Strategy
import com.angcyo.canvas.graphics.GraphicsHelper
import com.angcyo.canvas.items.data.DataItemRenderer
import com.angcyo.http.base.toJson
import com.angcyo.http.rx.doBack
import com.angcyo.laserpacker.*
import com.angcyo.laserpacker.bean.LPElementBean
import com.angcyo.laserpacker.bean.LPProjectBean
import com.angcyo.laserpacker.device.DeviceHelper._defaultProjectOutputFile
import com.angcyo.bluetooth.fsc.laserpacker.HawkEngraveKeys
import com.angcyo.laserpacker.device.engraveLoadingAsync
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
fun CanvasDelegate.saveInstanceState(name: String = ".lp", async: Boolean = true): String {
    val file = _defaultProjectOutputFile(name, false)
    val save = Runnable {
        val bean = getCanvasDataBean(null, HawkEngraveKeys.projectOutSize)
        val json = bean.toJson()
        json.writeTo(file, false)
    }
    if (async) {
        doBack {
            save.run()
        }
    } else {
        //同步保存
        save.run()
    }
    return file.absolutePath
}

/**恢复实例数据, 可自定义线程加载
 * [saveInstanceState]*/
fun CanvasDelegate.restoreInstanceState(name: String = ".lp", async: Boolean = true): String {
    val file = _defaultProjectOutputFile(name, false)
    val restore = Runnable {
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
    return file.absolutePath
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
    dataBean: LPProjectBean?,
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
    openCanvasFile(data?.toProjectBean(), clearOld)

/**直接加载*/
@AnyThread
fun CanvasDelegate.openCanvasFile(dataBean: LPProjectBean?, clearOld: Boolean = true): Boolean {
    if (clearOld) {
        removeAllItemRenderer(Strategy.init)
        undoManager.clear()
    }
    projectName = dataBean?.file_name ?: projectName
    val result = dataBean?.data?.toElementBeanList()?.let { items ->
        /*items.forEach { itemData ->
            GraphicsHelper.renderItemDataBean(this, itemData, false, false, Strategy.init)
        }*/
        items.generateName()
        GraphicsHelper.renderItemDataBeanList(this, items, false, Strategy.init)
    } != null
    return result
}

//---

/**处理文件路径对应的数据, 解析成[LPElementBean]*/
fun String?.toCanvasProjectItemBeanOfFile(): LPElementBean? {
    val path = this ?: return null
    if (path.endsWith(LPDataConstant.GCODE_EXT)) {
        val text = path.file().readText()
        return text.toGCodeElementBean()
    } else if (path.endsWith(LPDataConstant.SVG_EXT)) {
        val text = path.file().readText()
        return text.toSvgElementBean()
    } else if (path.isImageType()) {
        val bitmap = path.toBitmap()
        return bitmap.toBlackWhiteBitmapItemData()
    } else {
        L.w("无法处理的文件路径:${path}")
    }
    return null
}

/**添加一个黑白算法处理过的图片*/
fun CanvasDelegate.addBlackWhiteBitmapRender(bitmap: Bitmap?): DataItemRenderer? {
    bitmap ?: return null
    val bean = bitmap.toBlackWhiteBitmapItemData() ?: return null
    return GraphicsHelper.addRenderItemDataBean(this, bean)
}