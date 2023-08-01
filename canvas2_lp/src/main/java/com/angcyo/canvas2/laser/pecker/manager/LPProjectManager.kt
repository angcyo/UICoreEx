package com.angcyo.canvas2.laser.pecker.manager

import android.graphics.Bitmap
import android.graphics.RectF
import android.net.Uri
import com.angcyo.bluetooth.fsc.laserpacker.DeviceStateModel
import com.angcyo.bluetooth.fsc.laserpacker.HawkEngraveKeys
import com.angcyo.bluetooth.fsc.laserpacker.LaserPeckerModel
import com.angcyo.canvas.render.core.CanvasRenderDelegate
import com.angcyo.canvas.render.renderer.BaseRenderer
import com.angcyo.canvas.render.renderer.CanvasGroupRenderer
import com.angcyo.canvas2.laser.pecker.R
import com.angcyo.canvas2.laser.pecker.engrave.LPEngraveHelper
import com.angcyo.canvas2.laser.pecker.manager.LPProjectAutoSaveManager.isSaveBoolean
import com.angcyo.canvas2.laser.pecker.util.LPRendererHelper
import com.angcyo.canvas2.laser.pecker.util.lpBitmapElement
import com.angcyo.canvas2.laser.pecker.util.lpElement
import com.angcyo.canvas2.laser.pecker.util.lpElementBean
import com.angcyo.core.vmApp
import com.angcyo.engrave2.EngraveFlowDataHelper
import com.angcyo.http.base.json
import com.angcyo.http.base.jsonArray
import com.angcyo.http.base.toJson
import com.angcyo.http.rx.doBack
import com.angcyo.laserpacker.CanvasOpenDataType
import com.angcyo.laserpacker.LPDataConstant
import com.angcyo.laserpacker.bean.LPElementBean
import com.angcyo.laserpacker.bean.LPLaserOptionsBean
import com.angcyo.laserpacker.bean.LPProjectBean
import com.angcyo.laserpacker.bean.toLaserOptionsBean
import com.angcyo.laserpacker.device.DeviceHelper
import com.angcyo.laserpacker.device.engraveStrokeLoading
import com.angcyo.laserpacker.device.exception.EmptyException
import com.angcyo.laserpacker.generateName
import com.angcyo.laserpacker.project.readProjectBean
import com.angcyo.laserpacker.toBitmapElementBeanV2
import com.angcyo.laserpacker.toElementBeanList
import com.angcyo.laserpacker.toGCodeElementBean
import com.angcyo.laserpacker.toProjectBean
import com.angcyo.laserpacker.toSvgElementBean
import com.angcyo.library.L
import com.angcyo.library.canvas.core.Reason
import com.angcyo.library.component.Strategy
import com.angcyo.library.component.hawk.LibHawkKeys
import com.angcyo.library.component.lastContext
import com.angcyo.library.ex.*
import com.angcyo.library.libCacheFile
import com.angcyo.library.toastQQ
import com.angcyo.library.unit.toMm
import com.angcyo.library.utils.BuildHelper
import com.angcyo.library.utils.fileType
import com.angcyo.library.utils.writeTo
import com.angcyo.library.utils.writeToFile
import com.angcyo.opencv.OpenCV.toBitmap
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
         * [com.angcyo.laserpacker.project.readProjectBean]
         * [File.readProjectBean]*/
        fun parseProjectBean(filePath: String?): LPProjectBean? {
            return filePath?.file()?.readProjectBean()
        }

        /**工程分享功能实现*/
        var onShareProjectAction: (bean: ShareProjectInfo) -> Unit = {
            it.projectFile.shareFile()
        }

        /**[LPLaserOptionsBean]*/
        fun getProjectLaserOptions(taskId: String?): List<LPLaserOptionsBean> {
            val result = mutableListOf<LPLaserOptionsBean>()
            EngraveFlowDataHelper.getTaskEngraveConfigList(taskId).forEach {
                result.add(it.toLaserOptionsBean())
            }
            return result
        }

        fun saveProjectLaserOptions(
            bean: LPProjectBean,
            taskId: String?,
            renderList: List<BaseRenderer>?
        ) {
            val list = getProjectLaserOptions(taskId)
            if (list.isNotEmpty()) {
                if (HawkEngraveKeys.saveAllProjectOptions) {
                    bean.laserOptions = list.toJson()
                } else {
                    //有数据的图层, 才保存
                    val result = mutableListOf<LPLaserOptionsBean>()
                    for (options in list) {
                        val layerId = options.layerId
                        renderList?.forEach { renderer ->
                            val element = renderer.lpElementBean()
                            if (element?._layerId == layerId) {
                                //有数据的图层, 才保存
                                result.add(options)
                            }
                        }
                    }
                    if (result.isNotEmpty()) {
                        bean.laserOptions = result.toJson()
                    }
                }
            }
        }

        /**保存工程对应的默认参数*/
        fun saveProjectLastParams(projectBean: LPProjectBean?) {
            projectBean ?: return
            //last
            projectBean.lastType = HawkEngraveKeys.lastType
            projectBean.lastPower = HawkEngraveKeys.lastPower
            projectBean.lastDepth = HawkEngraveKeys.lastDepth
            projectBean.lastLayerDpi = HawkEngraveKeys.lastDpiLayerJson
        }

        fun configProjectBean(
            bean: LPProjectBean,
            taskId: String?,
            renderList: List<BaseRenderer>?
        ) {
            val laserPeckerModel = vmApp<LaserPeckerModel>()
            val productInfo = laserPeckerModel.productInfoData.value
            bean.apply {
                create_time = nowTime()
                update_time = nowTime()
                version = 2
                swVersion = productInfo?.softwareVersion ?: swVersion
                hwVersion = productInfo?.hardwareVersion ?: hwVersion

                //可能不准, 需要覆盖
                width = HawkEngraveKeys.lastPreviewWidth
                height = HawkEngraveKeys.lastPreviewHeight

                if (!renderList.isNullOrEmpty()) {
                    CanvasGroupRenderer.getRendererListRenderProperty(renderList)
                        .getRenderBounds(RectF()).apply {
                            width = this.width().toMm()
                            height = this.height().toMm()
                        }
                }

                productName = productInfo?.name
                exDevice = laserPeckerModel.getExDevice()
                moduleState =
                    vmApp<DeviceStateModel>().deviceStateData.value?.moduleState ?: moduleState

                //options
                saveProjectLaserOptions(this, taskId, renderList)

                //last
                saveProjectLastParams(this)
            }
        }
    }

    /**[com.angcyo.laserpacker.bean.LPProjectBean.file_name]*/
    var projectName: String? = HawkEngraveKeys.lastTransferName

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
        if (!projectBean.lastLayerDpi.isNullOrEmpty()) {
            HawkEngraveKeys.lastDpiLayerJson = projectBean.lastLayerDpi
        }
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
        taskId: String?,
        delegate: CanvasRenderDelegate,
        renderList: List<BaseRenderer>? = delegate.renderManager.elementRendererList,
        overrideSize: Float? = HawkEngraveKeys.projectOutSize.toFloat()
    ): LPProjectBean? {
        if (renderList.isNullOrEmpty()) {
            return null
        }
        try {
            val result = LPProjectBean().apply {
                configProjectBean(this, taskId, renderList)
                version = 1
                file_name = projectName

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
    fun saveProjectV1To(taskId: String?, file: File, delegate: CanvasRenderDelegate): File? {
        val bean = getProjectBean(taskId, delegate) ?: return null
        bean.file_name = bean.file_name ?: file.name
        val json = bean.toJson()
        json.writeTo(file, false)
        return file
    }

    /**[saveProjectV1To]*/
    fun saveProjectV2To(
        taskId: String?,
        zipFile: File,
        delegate: CanvasRenderDelegate,
        renderList: List<BaseRenderer>? = delegate.renderManager.elementRendererList,
        overrideSize: Float? = HawkEngraveKeys.projectOutSize.toFloat(),
        previewFile: File? = null, /*预览图需要额外保存的路径*/
        resultBean: LPProjectBean = LPProjectBean() /*保存的工程数据*/
    ): File? {
        if (renderList.isNullOrEmpty()) {
            return null
        }
        zipFileWrite(zipFile.absolutePath) {
            //开始写入数据流

            val projectBean = resultBean.apply {
                configProjectBean(this, taskId, renderList)
                version = 2
                file_name = projectName ?: zipFile.name

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

                if (previewFile != null) {
                    preview?.save(previewFile)
                }
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
        taskId: String?,
        delegate: CanvasRenderDelegate,
        fileName: String = LPDataConstant.PROJECT_V1_TEMP_NAME,
        async: Boolean = true,
        result: (String, Exception?) -> Unit = { _, _ -> } /*成功与失败的回调*/
    ): String {
        isSaveBoolean.set(true)
        val file = DeviceHelper._defaultProjectOutputFile(fileName, false)
        val save = Runnable {
            try {
                saveProjectV1To(taskId, file, delegate)
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
     * [saveFile] 直接指定需要保存的目标文件全路径, 此时会忽略[fileName]参数
     * [previewFile] 预览图需要额外保存的路径
     * @return 返回zip保存的文件路径
     * */
    fun saveProjectV2(
        taskId: String?,
        delegate: CanvasRenderDelegate,
        fileName: String = LPDataConstant.PROJECT_V2_TEMP_NAME,
        renderList: List<BaseRenderer>? = delegate.renderManager.elementRendererList,
        overrideSize: Float? = HawkEngraveKeys.projectOutSize.toFloat(),
        async: Boolean = true,
        saveFile: File? = null, /*保存的文件路径*/
        previewFile: File? = null, /*预览图需要额外保存的路径*/
        resultBean: LPProjectBean = LPProjectBean(),
        result: (zipFilePath: String, error: Exception?) -> Unit = { _, _ -> } /*成功与失败的回调*/
    ): String {
        val file = saveFile ?: DeviceHelper._defaultProjectOutputFileV2(fileName, false)
        val tempFile = libCacheFile("temp_${file.name}")
        val zipFilePath = file.absolutePath
        if (renderList.isNullOrEmpty()) {
            zipFilePath.file().deleteSafe()
            result(zipFilePath, EmptyException())
            return zipFilePath
        }
        isSaveBoolean.set(true)
        val save = Runnable {
            try {
                val resultFile = saveProjectV2To(
                    taskId,
                    tempFile,
                    delegate,
                    renderList,
                    overrideSize,
                    previewFile,
                    resultBean
                )
                if (resultFile == null) {
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

    /**保存一个工程结构, 用来分享到社区*/
    fun saveProjectV2Share(delegate: CanvasRenderDelegate?, taskId: String?) {
        if (delegate == null || taskId.isNullOrBlank()) {
            toastQQ(_string(R.string.cannot_share_project))
            return
        }
        val rendererList = LPEngraveHelper.getLayerRendererList(delegate, null)
        if (rendererList.isEmpty()) {
            toastQQ(_string(R.string.cannot_share_project))
            return
        }

        lastContext.engraveStrokeLoading { isCancel, loadEnd ->
            doBack {
                val projectFile = libCacheFile("${taskId}${LPDataConstant.PROJECT_EXT2}")
                val previewFile = libCacheFile("${taskId}${LPDataConstant.EXT_PREVIEW}")
                val resultBean = LPProjectBean()
                saveProjectV2(
                    taskId,
                    delegate,
                    projectFile.name,
                    rendererList,
                    async = false,
                    saveFile = projectFile,
                    previewFile = previewFile,
                    resultBean = resultBean
                ) { zipFilePath, error ->
                    loadEnd(zipFilePath, error)
                    if (error == null) {
                        //保存成功
                        onShareProjectAction(
                            ShareProjectInfo(taskId, projectFile, previewFile, resultBean)
                        )
                    } else {
                        toastQQ(error.message)
                    }
                }
            }
        }
    }

    //endregion ---保存---

    //region ---temp---

    /**工程临时存储, 速度够快, 可以在主线程雕刻*/
    fun saveProjectV2Folder(
        taskId: String?,
        delegate: CanvasRenderDelegate,
        renderList: List<BaseRenderer>? = delegate.renderManager.elementRendererList,
        overrideSize: Float? = HawkEngraveKeys.projectOutSize.toFloat(),
        onlySaveProperty: Boolean = false /*是否仅保存属性信息*/
    ): LPProjectBean? {
        if (!onlySaveProperty) {
            DeviceHelper.deleteProjectFileV2Folder()
        }
        if (renderList.isNullOrEmpty()) {
            return null
        }
        val projectBean = LPProjectBean().apply {
            configProjectBean(this, taskId, renderList)
            file_name = projectName
            version = 2

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
                                    writeV2TempRes(uri, elementBean.data!!)
                                }

                                //原图
                                val imageOriginalBitmap = sub.lpBitmapElement()?.originBitmap
                                    ?: elementBean.imageOriginal?.toBitmapOfBase64()
                                if (imageOriginalBitmap != null) {
                                    if (BuildHelper.isCpu64 || imageOriginalBitmap.width * imageOriginalBitmap.height <= LibHawkKeys.maxBitmapSaveSize) {
                                        val uri =
                                            LPDataConstant.PROJECT_V2_BASE_URI + "${uuid()}.png"
                                        elementBean.imageOriginalUri = uri
                                        writeV2TempRes(uri, imageOriginalBitmap)
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
                                            writeV2TempRes(uri, srcBitmap)
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

            //last write, 临时存储, 不需要预览图 2023-6-14
            /*val preview =
                delegate.preview(overrideSize = overrideSize, rendererList = renderList)
            //preview_img = preview?.toBase64Data()
            //previewImgUri = LPDataConstant.PROJECT_V2_BASE_URI + "${uuid()}.png"
            writeV2TempRes(LPDataConstant.PROJECT_V2_PREVIEW_NAME, preview)*/
        }
        val json = projectBean.toJson()
        writeV2TempRes(LPDataConstant.PROJECT_V2_DEFAULT_NAME, json)
        return projectBean
    }

    /**从文件夹中恢复工程*/
    fun restoreProjectV2Folder(
        delegate: CanvasRenderDelegate?,
        clearOld: Boolean = true
    ): LPProjectBean? {
        var projectBean: LPProjectBean? = null
        val folder = DeviceHelper._defaultProjectOutputV2Folder()
        if (folder.exists() && folder.isFolder()) {
            try {
                projectBean =
                    File(folder, LPDataConstant.PROJECT_V2_DEFAULT_NAME).readText()?.toProjectBean()
                openProjectBeanV2Folder(delegate, folder, projectBean, clearOld)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        return projectBean
    }

    /**打开工程文件, 从文件夹中*/
    fun openProjectBeanV2Folder(
        delegate: CanvasRenderDelegate?,
        folder: File,
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
                    bean.data = File(folder, bean.dataUri!!).readText()
                }

                //原图
                if (!bean.imageOriginalUri.isNullOrEmpty()) {
                    bean._imageOriginalBitmap = File(folder, bean.imageOriginalUri!!).toBitmap()
                    /*bean.imageOriginal =
                        zipFile.readEntryBitmap(bean.imageOriginalUri!!)?.toBase64Data()*/
                }

                //滤镜后的图
                if (!bean.srcUri.isNullOrEmpty()) {
                    bean._srcBitmap = File(folder, bean.srcUri!!).toBitmap()
                    /*bean.src = zipFile.readEntryBitmap(bean.srcUri!!)?.toBase64Data()*/
                }
            }
            LPRendererHelper.renderElementList(delegate, beanList, false, Strategy.init)
        } != null
        return result
    }

    fun writeV2TempRes(res: String, text: String?) {
        val folder = DeviceHelper._defaultProjectOutputV2Folder()
        val file = File(folder, res)
        file.writeText(text ?: "", false)
    }

    fun writeV2TempRes(res: String, bitmap: Bitmap?) {
        val folder = DeviceHelper._defaultProjectOutputV2Folder()
        val file = File(folder, res)
        bitmap?.writeToFile(file)
    }

    //endregion ---temp---
}

/**[com.angcyo.canvas2.laser.pecker.manager.LPProjectManager.saveProjectV1]*/
@Deprecated("V1格式不支持大数据存储, 请使用V2格式")
fun CanvasRenderDelegate.saveProjectState(taskId: String?, async: Boolean = true) {
    if (isSaveBoolean.get()) {
        return
    }
    try {
        LPProjectManager().saveProjectV1(taskId, this, async = async)
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

/**[com.angcyo.canvas2.laser.pecker.manager.LPProjectManager.restoreProjectV1]*/
fun CanvasRenderDelegate.restoreProjectState() {
    LPProjectManager().restoreProjectV1(this)
}

/**[com.angcyo.canvas2.laser.pecker.manager.LPProjectManager.saveProjectV2]*/
fun CanvasRenderDelegate.saveProjectStateV2(taskId: String?, async: Boolean = true) {
    LPProjectAutoSaveManager.autoSave(taskId, this, async)
}

/**[com.angcyo.canvas2.laser.pecker.manager.LPProjectManager.restoreProjectV2]*/
fun CanvasRenderDelegate.restoreProjectStateV2() {
    LPProjectAutoSaveManager.autoRestore(this)
}

/**[com.angcyo.laserpacker.device.DeviceHelper.deleteProjectFileV2]*/
fun deleteProjectFileV2() {
    DeviceHelper.deleteProjectFileV2()
}

fun deleteProjectFileV2Folder() {
    DeviceHelper.deleteProjectFileV2Folder()
}

/**处理文件路径对应的数据, 解析成[LPElementBean]
 * [bmpThreshold] 不指定阈值时, 自动从图片中获取
 * */
fun String?.toElementBeanOfFile(bmpThreshold: Int? = null): CanvasOpenDataType? {
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
        return bitmap.toBitmapElementBeanV2(bmpThreshold)
    } else if (path.endsWith(LPDataConstant.PROJECT_EXT) || path.endsWith(LPDataConstant.PROJECT_EXT2)) {
        return LPProjectManager().openProjectFile(null, file)
    } else {
        L.w("无法处理的文件路径:${path}")
    }
    return null
}