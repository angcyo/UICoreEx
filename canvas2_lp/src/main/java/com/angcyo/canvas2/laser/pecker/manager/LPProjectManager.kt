package com.angcyo.canvas2.laser.pecker.manager

import android.net.Uri
import com.angcyo.canvas.render.core.CanvasRenderDelegate
import com.angcyo.canvas.render.core.Strategy
import com.angcyo.canvas.render.renderer.BaseRenderer
import com.angcyo.canvas2.laser.pecker.util.LPRendererHelper
import com.angcyo.canvas2.laser.pecker.util.lpElement
import com.angcyo.http.base.json
import com.angcyo.http.base.jsonArray
import com.angcyo.http.base.toJson
import com.angcyo.http.rx.doBack
import com.angcyo.laserpacker.LPDataConstant
import com.angcyo.laserpacker.bean.LPProjectBean
import com.angcyo.laserpacker.device.DeviceHelper
import com.angcyo.laserpacker.device.HawkEngraveKeys
import com.angcyo.laserpacker.generateName
import com.angcyo.laserpacker.toElementBeanList
import com.angcyo.laserpacker.toProjectBean
import com.angcyo.library.ex.*
import com.angcyo.library.utils.writeTo
import java.io.File
import java.util.zip.ZipFile

/**
 * 工程管理, 用来实现保存/读取工程
 * Email:angcyo@126.com
 * @author angcyo
 * @date 2023/04/12
 * Copyright (c) 2020 angcyo. All rights reserved.
 */
class LPProjectManager {

    //region ---打开---

    /**打开工程文件*/
    fun openProjectFile(
        delegate: CanvasRenderDelegate,
        file: File?,
        clearOld: Boolean = true
    ): LPProjectBean? {
        val projectBean = file?.readText()?.toProjectBean()
        openProjectBean(delegate, projectBean, clearOld)
        return projectBean
    }

    /**打开工程文件*/
    fun openProjectFileV2(
        delegate: CanvasRenderDelegate,
        zipFile: File?,
        clearOld: Boolean = true
    ): LPProjectBean? {
        zipFile ?: return null
        var projectBean: LPProjectBean? = null
        zipFileRead(zipFile.absolutePath) {
            projectBean = readEntryString(LPDataConstant.PROJECT_V2_DEFAULT_NAME)?.toProjectBean()
            openProjectBeanV2(delegate, this, projectBean, clearOld)
        }
        return projectBean
    }

    /**打开工程文件*/
    fun openProjectUri(
        delegate: CanvasRenderDelegate,
        uri: Uri?,
        clearOld: Boolean = true
    ): LPProjectBean? {
        val projectBean = uri?.readString()?.toProjectBean()
        openProjectBean(delegate, projectBean, clearOld)
        return projectBean
    }

    /**打开工程文件*/
    fun openProjectBean(
        delegate: CanvasRenderDelegate,
        projectBean: LPProjectBean?,
        clearOld: Boolean = true
    ): Boolean {
        if (clearOld) {
            delegate.renderManager.removeAllElementRenderer(Strategy.init)
            delegate.undoManager.clear()
        }
        val result = projectBean?.data?.toElementBeanList()?.let { beanList ->
            beanList.generateName()
            LPRendererHelper.renderElementList(delegate, beanList, false, Strategy.init)
        } != null
        return result
    }

    /**打开工程文件*/
    fun openProjectBeanV2(
        delegate: CanvasRenderDelegate,
        zipFile: ZipFile,
        projectBean: LPProjectBean?,
        clearOld: Boolean = true
    ): Boolean {
        if (clearOld) {
            delegate.renderManager.removeAllElementRenderer(Strategy.init)
            delegate.undoManager.clear()
        }
        val result = projectBean?.data?.toElementBeanList()?.let { beanList ->
            beanList.generateName()
            for (bean in beanList) {

                //gcode/svg
                if (!bean.dataUri.isNullOrEmpty()) {
                    bean.data = zipFile.readEntryString(bean.dataUri!!)
                }

                //原图
                if (!bean.imageOriginalUri.isNullOrEmpty()) {
                    bean.imageOriginal =
                        zipFile.readEntryBitmap(bean.imageOriginalUri!!)?.toBase64Data()
                }

                //滤镜后的图
                if (!bean.srcUri.isNullOrEmpty()) {
                    bean.src = zipFile.readEntryBitmap(bean.srcUri!!)?.toBase64Data()
                }
            }
            LPRendererHelper.renderElementList(delegate, beanList, false, Strategy.init)
        } != null
        return result
    }

    /**恢复实例数据, 可自定义线程加载
     * [saveProjectV1]
     * @return 返回打开的工程文件路径
     * */
    fun restoreProjectV1(
        delegate: CanvasRenderDelegate,
        fileName: String = ".temp",
        async: Boolean = true
    ): String {
        val file = DeviceHelper._defaultProjectOutputFile(fileName, false)
        val restore = Runnable {
            openProjectFile(delegate, file, true)
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

    /**第二版 恢复实例数据, 可自定义线程加载
     * [saveProjectV2]
     * [restoreProjectV1]
     *
     * [LPDataConstant.PROJECT_V2_BASE_URI]
     * @return 返回打开的工程文件路径
     * */
    fun restoreProjectV2(
        delegate: CanvasRenderDelegate,
        fileName: String = ".temp",
        async: Boolean = true
    ): String {
        val file = DeviceHelper._defaultProjectOutputFileV2(fileName, false)
        val restore = Runnable {
            openProjectFileV2(delegate, file, true)
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
    //endregion ---打开---

    //region ---保存---

    /**获取工程结构[LPProjectBean]
     *
     * [java.lang.OutOfMemoryError]*/
    fun getProjectBean(
        delegate: CanvasRenderDelegate,
        renderList: List<BaseRenderer>? = delegate.renderManager.elementRendererList,
        overrideSize: Float? = HawkEngraveKeys.projectOutSize.toFloat()
    ): LPProjectBean? {
        try {
            val result = LPProjectBean().apply {
                create_time = nowTime()
                update_time = nowTime()

                val preview =
                    delegate.preview(overrideSize = overrideSize, rendererList = renderList)
                preview_img = preview?.toBase64Data()

                data = jsonArray {
                    renderList?.forEach { renderer ->
                        val list = renderer.getSingleRendererList(false)
                        list.forEach { sub ->
                            try {
                                sub.lpElement()?.let { element ->
                                    element.updateBeanFromElement(sub)
                                    add(element.elementBean.toJson().json())
                                }
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }
                    }
                }.toString() //java.lang.OutOfMemoryError
            }
            return result
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }

    /**第一版
     * 保存实例数据, 实际就是保存工程数据
     * [fileName] 保存的工程文件名, 请包含后缀
     * [async] 是否异步保存
     * @return 返回保存的文件路径
     * */
    fun saveProjectV1(
        delegate: CanvasRenderDelegate,
        fileName: String = ".temp",
        async: Boolean = true,
        result: (String, Exception?) -> Unit = { _, _ -> } /*成功与失败的回调*/
    ): String {
        val file = DeviceHelper._defaultProjectOutputFile(fileName, false)
        val save = Runnable {
            try {
                val bean = getProjectBean(delegate)!!
                val json = bean.toJson()
                json.writeTo(file, false)
                result(file.absolutePath, null)
            } catch (e: Exception) {
                e.printStackTrace()
                result(file.absolutePath, e)
            }
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

    /**第二版, 使用zip格式保存数据
     * [saveProjectV1]
     * @return 返回zip保存的文件路径
     * */
    fun saveProjectV2(
        delegate: CanvasRenderDelegate,
        fileName: String = ".temp",
        renderList: List<BaseRenderer>? = delegate.renderManager.elementRendererList,
        overrideSize: Float? = HawkEngraveKeys.projectOutSize.toFloat(),
        async: Boolean = true,
        result: (String, Exception?) -> Unit = { _, _ -> } /*成功与失败的回调*/
    ): String {
        val file = DeviceHelper._defaultProjectOutputFileV2(fileName, false)
        val zipFilePath = file.absolutePath
        val save = Runnable {
            try {
                zipFileWrite(zipFilePath) {
                    //开始写入数据流

                    val projectBean = LPProjectBean().apply {
                        create_time = nowTime()
                        update_time = nowTime()

                        val preview =
                            delegate.preview(overrideSize = overrideSize, rendererList = renderList)
                        //preview_img = preview?.toBase64Data()
                        previewImgUri = LPDataConstant.PROJECT_V2_BASE_URI + "${uuid()}.png"
                        writeEntry(previewImgUri!!, preview)

                        data = jsonArray {
                            renderList?.forEach { renderer ->
                                val list = renderer.getSingleRendererList(false)
                                list.forEach { sub ->
                                    try {
                                        sub.lpElement()?.let { element ->
                                            element.updateBeanFromElement(sub)
                                            val elementBean = element.elementBean.copy()

                                            //gcode/svg
                                            if (!elementBean.data.isNullOrEmpty()) {
                                                val uri =
                                                    LPDataConstant.PROJECT_V2_BASE_URI + uuid()
                                                elementBean.dataUri = uri
                                                writeEntry(uri, elementBean.data!!)
                                            }

                                            //原图
                                            if (!elementBean.imageOriginal.isNullOrEmpty()) {
                                                val uri =
                                                    LPDataConstant.PROJECT_V2_BASE_URI + "${uuid()}.png"
                                                elementBean.imageOriginalUri = uri
                                                writeEntry(
                                                    uri,
                                                    elementBean.imageOriginal!!.toBitmapOfBase64()
                                                )
                                            }

                                            //滤镜后的图
                                            if (!elementBean.src.isNullOrEmpty()) {
                                                val uri =
                                                    LPDataConstant.PROJECT_V2_BASE_URI + "${uuid()}.png"
                                                elementBean.srcUri = uri
                                                writeEntry(
                                                    uri,
                                                    elementBean.src!!.toBitmapOfBase64()
                                                )
                                            }

                                            //清空数据, 使用uri代替
                                            elementBean.data = null
                                            elementBean.imageOriginal = null
                                            elementBean.src = null
                                            add(elementBean.toJson().json())
                                        }
                                    } catch (e: Exception) {
                                        e.printStackTrace()
                                    }
                                }
                            }
                        }.toString() //java.lang.OutOfMemoryError
                    }
                    val json = projectBean.toJson()
                    writeEntry(LPDataConstant.PROJECT_V2_DEFAULT_NAME, json)
                    result(file.absolutePath, null)
                }

            } catch (e: Exception) {
                e.printStackTrace()
                result(file.absolutePath, e)
            }
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

    //endregion ---保存---

}

fun CanvasRenderDelegate.saveProjectState() {
    LPProjectManager().saveProjectV1(this)
}

fun CanvasRenderDelegate.restoreProjectState() {
    LPProjectManager().restoreProjectV1(this)
}

fun CanvasRenderDelegate.saveProjectStateV2() {
    LPProjectManager().saveProjectV2(this)
}

fun CanvasRenderDelegate.restoreProjectStateV2() {
    LPProjectManager().restoreProjectV2(this)
}