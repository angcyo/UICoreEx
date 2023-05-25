package com.angcyo.canvas2.laser.pecker.manager

import android.net.Uri
import com.angcyo.bluetooth.fsc.laserpacker.HawkEngraveKeys
import com.angcyo.bluetooth.fsc.laserpacker.LaserPeckerModel
import com.angcyo.canvas.render.core.CanvasRenderDelegate
import com.angcyo.canvas.render.core.Reason
import com.angcyo.canvas.render.core.Strategy
import com.angcyo.canvas.render.renderer.BaseRenderer
import com.angcyo.canvas2.laser.pecker.manager.LPProjectAutoSaveManager.isSaveBoolean
import com.angcyo.canvas2.laser.pecker.util.LPRendererHelper
import com.angcyo.canvas2.laser.pecker.util.lpBitmapElement
import com.angcyo.canvas2.laser.pecker.util.lpElement
import com.angcyo.core.vmApp
import com.angcyo.http.base.json
import com.angcyo.http.base.jsonArray
import com.angcyo.http.base.toJson
import com.angcyo.http.rx.doBack
import com.angcyo.laserpacker.CanvasOpenDataType
import com.angcyo.laserpacker.LPDataConstant
import com.angcyo.laserpacker.bean.LPElementBean
import com.angcyo.laserpacker.bean.LPProjectBean
import com.angcyo.laserpacker.device.DeviceHelper
import com.angcyo.laserpacker.device.exception.EmptyException
import com.angcyo.laserpacker.generateName
import com.angcyo.laserpacker.project.readProjectBean
import com.angcyo.laserpacker.toBlackWhiteBitmapItemData
import com.angcyo.laserpacker.toElementBeanList
import com.angcyo.laserpacker.toGCodeElementBean
import com.angcyo.laserpacker.toProjectBean
import com.angcyo.laserpacker.toSvgElementBean
import com.angcyo.library.L
import com.angcyo.library.component.hawk.LibHawkKeys
import com.angcyo.library.ex.*
import com.angcyo.library.utils.BuildHelper
import com.angcyo.library.utils.fileType
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

    companion object {

        /**从文件路径中, 解析出[LPProjectBean]
         *
         * [com.angcyo.laserpacker.project.LPProjectHelperKt.readProjectBean]
         * [File.readProjectBean]*/
        fun parseProjectBean(filePath: String?): LPProjectBean? {
            return filePath?.file()?.readProjectBean()
        }
    }

    /**[com.angcyo.laserpacker.bean.LPProjectBean.file_name]*/
    var projectName: String? = null

    /**恢复工程对应的默认参数*/
    fun restoreProjectLastParams(projectBean: LPProjectBean?) {
        projectBean ?: return
        //last
        if (projectBean.lastType > 0) {
            HawkEngraveKeys.lastType = projectBean.lastType
        }
        if (projectBean.lastPower > 0) {
            HawkEngraveKeys.lastPower = projectBean.lastPower
        }
        if (projectBean.lastDepth > 0) {
            HawkEngraveKeys.lastDepth = projectBean.lastDepth
        }
        if (projectBean.lastDpi > 0) {
            HawkEngraveKeys.lastDpi = projectBean.lastDpi
        }
    }

    /**保存工程对应的默认参数*/
    fun saveProjectLastParams(projectBean: LPProjectBean?) {
        projectBean ?: return
        //last
        projectBean.lastType = HawkEngraveKeys.lastType
        projectBean.lastPower = HawkEngraveKeys.lastPower
        projectBean.lastDepth = HawkEngraveKeys.lastDepth
        projectBean.lastDpi = HawkEngraveKeys.lastDpi
    }

    //region ---打开---

    /**打开工程文件*/
    fun openProjectFile(
        delegate: CanvasRenderDelegate?,
        file: File?,
        clearOld: Boolean = true
    ): LPProjectBean? {
        if (file?.name?.endsWith(LPDataConstant.PROJECT_EXT2, true) == true) {
            //V2 工程结构
            return openProjectFileV2(delegate, file, clearOld)
        }
        val projectBean = file?.readText()?.toProjectBean()
        openProjectBean(delegate, projectBean, clearOld)
        return projectBean
    }

    /**打开工程文件*/
    fun openProjectFileV2(
        delegate: CanvasRenderDelegate?,
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
        var projectBean: LPProjectBean? = null
        val name = uri?.getShowName()
        if (name?.endsWith(LPDataConstant.PROJECT_EXT2, true) == true) {
            //V2 工程结构
            projectBean = openProjectFileV2(delegate, uri.saveTo().file(), clearOld)
        } else {
            //V1 工程结构
            projectBean = uri?.readString()?.toProjectBean()
            openProjectBean(delegate, projectBean, clearOld)
        }
        return projectBean
    }

    /**打开工程文件*/
    fun openProjectBean(
        delegate: CanvasRenderDelegate?,
        projectBean: LPProjectBean?,
        clearOld: Boolean = true
    ): Boolean {
        delegate ?: return false
        projectBean ?: return false
        if (projectBean.version == 2) {
            //V2版本
            val zipFile = projectBean._filePath?.file() ?: return false
            var result = false
            zipFileRead(zipFile.absolutePath) {
                result = openProjectBeanV2(delegate, this, projectBean, clearOld)
            }
            return result
        } else {
            if (clearOld) {
                delegate.renderManager.removeAllElementRenderer(Reason.init, Strategy.init)
                delegate.undoManager.clear()
            }
            val result = projectBean.data?.toElementBeanList()?.let { beanList ->
                beanList.generateName()
                LPRendererHelper.renderElementList(delegate, beanList, false, Strategy.init)
            } != null
            return result
        }
    }

    /**打开工程文件*/
    fun openProjectBeanV2(
        delegate: CanvasRenderDelegate?,
        zipFile: ZipFile,
        projectBean: LPProjectBean?,
        clearOld: Boolean = true
    ): Boolean {
        projectBean ?: return false

        if (delegate != null) {
            restoreProjectLastParams(projectBean)

            if (clearOld) {
                delegate.renderManager.removeAllElementRenderer(Reason.init, Strategy.init)
                delegate.undoManager.clear()
            }
        }

        val result = projectBean.data?.toElementBeanList()?.let { beanList ->
            beanList.generateName()
            for (bean in beanList) {

                //gcode/svg
                if (!bean.dataUri.isNullOrEmpty()) {
                    bean.data = zipFile.readEntryString(bean.dataUri!!)
                }

                //原图
                if (!bean.imageOriginalUri.isNullOrEmpty()) {
                    bean._imageOriginalBitmap = zipFile.readEntryBitmap(bean.imageOriginalUri!!)
                    /*bean.imageOriginal =
                        zipFile.readEntryBitmap(bean.imageOriginalUri!!)?.toBase64Data()*/
                }

                //滤镜后的图
                if (!bean.srcUri.isNullOrEmpty()) {
                    bean._srcBitmap = zipFile.readEntryBitmap(bean.srcUri!!)
                    /*bean.src = zipFile.readEntryBitmap(bean.srcUri!!)?.toBase64Data()*/
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
        fileName: String = LPDataConstant.PROJECT_V1_TEMP_NAME,
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
        fileName: String = LPDataConstant.PROJECT_V2_TEMP_NAME,
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

    /**打开一个[LPElementBean]元素*/
    fun openElementBean(
        delegate: CanvasRenderDelegate,
        bean: LPElementBean?,
        assignLocation: Boolean
    ): Boolean {
        bean ?: return false
        return LPRendererHelper.parseElementRenderer(bean, assignLocation)?.apply {
            delegate.renderManager.addElementRenderer(this, true, Reason.user, Strategy.normal)
            LPRendererHelper.generateName(delegate)
        } != null
    }

    /**打开一组[LPElementBean]元素*/
    fun openElementBeanList(
        delegate: CanvasRenderDelegate,
        beanList: List<LPElementBean>?,
        assignLocation: Boolean
    ): Boolean {
        beanList ?: return false
        return LPRendererHelper.parseElementRendererList(beanList, assignLocation).apply {
            delegate.renderManager.addElementRenderer(this, true, Reason.user, Strategy.normal)
            LPRendererHelper.generateName(delegate)
        }.isNotEmpty()
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
        if (renderList.isNullOrEmpty()) {
            return null
        }
        try {
            val result = LPProjectBean().apply {
                val productInfo = vmApp<LaserPeckerModel>().productInfoData.value

                create_time = nowTime()
                update_time = nowTime()
                file_name = projectName
                version = 1
                swVersion = productInfo?.softwareVersion ?: swVersion
                hwVersion = productInfo?.hardwareVersion ?: hwVersion

                //last
                saveProjectLastParams(this)

                val preview =
                    delegate.preview(overrideSize = overrideSize, rendererList = renderList)
                preview_img = preview?.toBase64Data()

                data = jsonArray {
                    renderList.forEach { renderer ->
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

    /**将工程保存到指定文件[file]*/
    fun saveProjectV1To(file: File, delegate: CanvasRenderDelegate): File? {
        val bean = getProjectBean(delegate) ?: return null
        bean.file_name = bean.file_name ?: file.name
        val json = bean.toJson()
        json.writeTo(file, false)
        return file
    }

    /**[saveProjectV1To]*/
    fun saveProjectV2To(
        zipFile: File,
        delegate: CanvasRenderDelegate,
        renderList: List<BaseRenderer>? = delegate.renderManager.elementRendererList,
        overrideSize: Float? = HawkEngraveKeys.projectOutSize.toFloat(),
    ): File? {
        if (renderList.isNullOrEmpty()) {
            return null
        }
        zipFileWrite(zipFile.absolutePath) {
            //开始写入数据流

            val projectBean = LPProjectBean().apply {
                val productInfo = vmApp<LaserPeckerModel>().productInfoData.value
                create_time = nowTime()
                update_time = nowTime()
                file_name = projectName ?: zipFile.name
                version = 2
                swVersion = productInfo?.softwareVersion ?: swVersion
                hwVersion = productInfo?.hardwareVersion ?: hwVersion

                //last
                saveProjectLastParams(this)

                data = jsonArray {
                    renderList.forEach { renderer ->
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
                                    val imageOriginalBitmap = sub.lpBitmapElement()?.originBitmap
                                        ?: elementBean.imageOriginal?.toBitmapOfBase64()
                                    if (imageOriginalBitmap != null) {
                                        if (BuildHelper.isCpu64 || imageOriginalBitmap.width * imageOriginalBitmap.height <= LibHawkKeys.maxBitmapSaveSize) {
                                            val uri =
                                                LPDataConstant.PROJECT_V2_BASE_URI + "${uuid()}.png"
                                            elementBean.imageOriginalUri = uri
                                            writeEntry(uri, imageOriginalBitmap)
                                        }
                                    }

                                    //滤镜后的图
                                    if (HawkEngraveKeys.saveFilterBitmap) {
                                        val srcBitmap = sub.lpBitmapElement()?.renderBitmap
                                            ?: elementBean.src?.toBitmapOfBase64()
                                        if (srcBitmap != null) {
                                            if (BuildHelper.isCpu64 || srcBitmap.width * srcBitmap.height <= LibHawkKeys.maxBitmapSaveSize) {
                                                val uri =
                                                    LPDataConstant.PROJECT_V2_BASE_URI + "${uuid()}.png"
                                                elementBean.srcUri = uri
                                                writeEntry(uri, srcBitmap)
                                            }
                                        }
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

                //last write

                val preview =
                    delegate.preview(overrideSize = overrideSize, rendererList = renderList)
                //preview_img = preview?.toBase64Data()
                //previewImgUri = LPDataConstant.PROJECT_V2_BASE_URI + "${uuid()}.png"
                writeEntry(LPDataConstant.PROJECT_V2_PREVIEW_NAME, preview)
            }
            val json = projectBean.toJson()
            writeEntry(LPDataConstant.PROJECT_V2_DEFAULT_NAME, json)
        }
        return zipFile
    }

    /**第一版
     * 保存实例数据, 实际就是保存工程数据
     * [fileName] 保存的工程文件名, 请包含后缀
     * [async] 是否异步保存
     * @return 返回保存的文件路径
     * */
    fun saveProjectV1(
        delegate: CanvasRenderDelegate,
        fileName: String = LPDataConstant.PROJECT_V1_TEMP_NAME,
        async: Boolean = true,
        result: (String, Exception?) -> Unit = { _, _ -> } /*成功与失败的回调*/
    ): String {
        isSaveBoolean.set(true)
        val file = DeviceHelper._defaultProjectOutputFile(fileName, false)
        val save = Runnable {
            try {
                saveProjectV1To(file, delegate)
            } catch (e: Exception) {
                e.printStackTrace()
                result(file.absolutePath, e)
            }
            isSaveBoolean.set(false)
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
        fileName: String = LPDataConstant.PROJECT_V2_TEMP_NAME,
        renderList: List<BaseRenderer>? = delegate.renderManager.elementRendererList,
        overrideSize: Float? = HawkEngraveKeys.projectOutSize.toFloat(),
        async: Boolean = true,
        result: (zipFilePath: String, error: Exception?) -> Unit = { _, _ -> } /*成功与失败的回调*/
    ): String {
        isSaveBoolean.set(true)
        val file = DeviceHelper._defaultProjectOutputFileV2(fileName, false)
        val tempFile = DeviceHelper._defaultProjectOutputFileV2("${fileName}.${nowTime()}", false)
        val zipFilePath = file.absolutePath
        val save = Runnable {
            try {
                val saveFile = saveProjectV2To(tempFile, delegate, renderList, overrideSize)
                if (saveFile == null) {
                    //保存失败
                    result(zipFilePath, EmptyException())
                } else {
                    file.delete()
                    tempFile.renameTo(file)
                    tempFile.delete()
                    result(zipFilePath, null)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                result(zipFilePath, e)
            }
            isSaveBoolean.set(false)
        }
        if (async) {
            doBack {
                save.run()
            }
        } else {
            //同步保存
            save.run()
        }
        return zipFilePath
    }

    //endregion ---保存---
}

/**[com.angcyo.canvas2.laser.pecker.manager.LPProjectManager.saveProjectV1]*/
@Deprecated("V1格式不支持大数据存储, 请使用V2格式")
fun CanvasRenderDelegate.saveProjectState(async: Boolean = true) {
    if (isSaveBoolean.get()) {
        return
    }
    try {
        LPProjectManager().saveProjectV1(this, async = async)
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

/**[com.angcyo.canvas2.laser.pecker.manager.LPProjectManager.restoreProjectV1]*/
fun CanvasRenderDelegate.restoreProjectState() {
    LPProjectManager().restoreProjectV1(this)
}

/**[com.angcyo.canvas2.laser.pecker.manager.LPProjectManager.saveProjectV2]*/
fun CanvasRenderDelegate.saveProjectStateV2(async: Boolean = true) {
    LPProjectAutoSaveManager.autoSave(this, async)
}

/**[com.angcyo.canvas2.laser.pecker.manager.LPProjectManager.restoreProjectV2]*/
fun CanvasRenderDelegate.restoreProjectStateV2() {
    try {
        LPProjectManager().restoreProjectV2(this)
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

/**[com.angcyo.laserpacker.device.DeviceHelper.deleteProjectFileV2]*/
fun deleteProjectFileV2() {
    DeviceHelper.deleteProjectFileV2()
}

/**处理文件路径对应的数据, 解析成[LPElementBean]*/
fun String?.toElementBeanOfFile(): CanvasOpenDataType? {
    val path = this?.lowercase() ?: return null
    val file = path.file()
    if (path.endsWith(LPDataConstant.GCODE_EXT)) {
        val text = file.readText()
        return text.toGCodeElementBean()
    } else if (path.endsWith(LPDataConstant.SVG_EXT)) {
        val text = file.readText()
        return text.toSvgElementBean()
    } else if (path.isImageType() || file.fileType().isImageType()) {
        val bitmap = path.toBitmap()
        return bitmap.toBlackWhiteBitmapItemData()
    } else if (path.endsWith(LPDataConstant.PROJECT_EXT) || path.endsWith(LPDataConstant.PROJECT_EXT2)) {
        return LPProjectManager().openProjectFile(null, file)
    } else {
        L.w("无法处理的文件路径:${path}")
    }
    return null
}