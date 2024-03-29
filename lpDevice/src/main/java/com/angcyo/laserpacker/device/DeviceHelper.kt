package com.angcyo.laserpacker.device

import android.graphics.Color
import android.os.Build
import com.angcyo.bluetooth.fsc.laserpacker.HawkEngraveKeys
import com.angcyo.bluetooth.fsc.laserpacker.LaserPeckerHelper
import com.angcyo.bluetooth.fsc.laserpacker.LaserPeckerModel
import com.angcyo.bluetooth.fsc.laserpacker._deviceSettingBean
import com.angcyo.core.component.DslLayout
import com.angcyo.core.component.ScreenShotModel
import com.angcyo.core.component.file.appFilePath
import com.angcyo.core.component.renderLayout
import com.angcyo.core.vmApp
import com.angcyo.glide.loadImage
import com.angcyo.http.rx.runRx
import com.angcyo.laserpacker.LPDataConstant
import com.angcyo.library.ex._string
import com.angcyo.library.ex.deleteRecursivelySafe
import com.angcyo.library.ex.eachFile
import com.angcyo.library.ex.ensureExtName
import com.angcyo.library.ex.ensureName
import com.angcyo.library.ex.file
import com.angcyo.library.ex.isFileExist
import com.angcyo.library.ex.nowTimeString
import com.angcyo.library.ex.shareFile
import com.angcyo.library.ex.zip
import com.angcyo.library.getAppVersionCode
import com.angcyo.library.libCacheFile
import com.angcyo.library.toastQQ
import com.angcyo.library.utils.appFolderPath
import com.angcyo.library.utils.fileNameTime
import com.angcyo.library.utils.filePath
import com.angcyo.library.utils.folderPath
import com.angcyo.objectbox.findLast
import com.angcyo.objectbox.laser.pecker.LPBox
import com.angcyo.objectbox.laser.pecker.entity.EngraveTaskEntity
import java.io.File

/**
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2023/03/30
 */
object DeviceHelper {

    //region---常量属性---

    /**预览的提示颜色和蚂蚁线的颜色*/
    const val PREVIEW_COLOR = Color.BLUE

    /**雕刻颜色*/
    const val ENGRAVE_COLOR = Color.RED

    /**批量雕刻指令支持的固件范围*/
    val batchEngraveSupportFirmware = HawkEngraveKeys.batchEngraveSupportFirmware
        ?: _deviceSettingBean?.lpBatchEngraveFirmware

    //endregion---常量属性---

    /**临时的雕刻日志文件路径集合, 在分享之后清空*/
    val tempEngraveLogPathList = mutableListOf<String>()

    /**获取所有日志路径*/
    fun getEngraveLogPathList() = mutableListOf<String>().apply {
        addAll(ScreenShotModel.getBaseLogShareList())

        //xml
        addAll(getTaskEngraveLogFilePath())
        addAll(tempEngraveLogPathList)
    }

    /**分享最近的雕刻日志*/
    fun shareEngraveLog() {
        toastQQ(_string(R.string.create_log_tip))
        runRx({
            val logList = getEngraveLogPathList()

            tempEngraveLogPathList.clear()
            val version = vmApp<LaserPeckerModel>().productInfoData.value?.softwareVersion ?: -1
            val versionStr = if (version != -1) "_$version" else ""
            logList.zip(libCacheFile(buildString {
                append("LP")
                append("_${getAppVersionCode()}")
                append(versionStr)
                append("_${Build.MODEL}")
                append("_")
                append(nowTimeString("yyyy-MM-dd_HH-mm-ss"))
                append(".zip")
            }).absolutePath)?.shareFile()
        })
    }

    /**截图之后的意见反馈*/
    fun showEngraveScreenShotShare(path: String) {
        tempEngraveLogPathList.add(path)
        renderLayout(R.layout.layout_engrave_screen_shot_share) {
            renderLayoutAction = {
                img(R.id.lib_image_view)?.loadImage(path)
                click(R.id.lib_close_view) {
                    DslLayout.hide(this@renderLayout)
                }
                clickItem {
                    DslLayout.hide(this@renderLayout)
                    shareEngraveLog()
                }
            }
        }
    }

    /**最后一次雕刻任务的数据日志文件路径*/
    fun getTaskEngraveLogFilePath(): List<String> {
        val result = mutableListOf<String>()
        val task = EngraveTaskEntity::class.findLast(LPBox.PACKAGE_NAME) {
            //no op
        }
        task?.apply {
            val path = appFilePath(
                "$taskId${LPDataConstant.EXT_DATA_PREVIEW}",
                LPDataConstant.ENGRAVE_FILE_FOLDER
            )
            if (path.isFileExist()) {
                //鸟瞰图
                result.add(path)
            }
            dataList?.forEach {
                result.addAll(getIndexLogFilePath(it))
            }
        }
        //task
        return result
    }

    /**通过雕刻索引, 获取对应的元素预览图片文件路径*/
    fun getEngravePreviewBitmapPath(index: Any?): String = appFilePath(
        "${index}${LPDataConstant.EXT_PREVIEW.ensureExtName()}",
        LPDataConstant.ENGRAVE_FILE_FOLDER
    )

    /**获取指定索引对应的雕刻日志文件*/
    fun getIndexLogFilePath(index: Any?): List<String> {
        val result = mutableListOf<String>()
        //.png
        var path = appFilePath(
            "$index${LPDataConstant.EXT_PREVIEW}",
            LPDataConstant.ENGRAVE_FILE_FOLDER
        )
        if (path.isFileExist()) {
            result.add(path)
        }
        //.p.png
        path = appFilePath(
            "$index${LPDataConstant.EXT_DATA_PREVIEW}",
            LPDataConstant.ENGRAVE_FILE_FOLDER
        )
        if (path.isFileExist()) {
            result.add(path)
        }
        //.bp
        path = appFilePath("$index${LPDataConstant.EXT_BP}", LPDataConstant.ENGRAVE_FILE_FOLDER)
        if (path.isFileExist()) {
            result.add(path)
        }
        //.dt
        path = appFilePath("$index${LPDataConstant.EXT_DT}", LPDataConstant.ENGRAVE_FILE_FOLDER)
        if (path.isFileExist()) {
            result.add(path)
        }
        //.gcode
        path =
            appFilePath("$index${LPDataConstant.EXT_GCODE}", LPDataConstant.ENGRAVE_FILE_FOLDER)
        if (path.isFileExist()) {
            result.add(path)
        }
        //.path
        path =
            appFilePath("$index${LPDataConstant.EXT_PATH}", LPDataConstant.ENGRAVE_FILE_FOLDER)
        if (path.isFileExist()) {
            result.add(path)
        }
        return result
    }

    /**获取推荐的激光类型*/
    fun getProductLaserType(): Byte {
        val typeList = LaserPeckerHelper.findProductSupportLaserTypeList()

        //1:如果找到了上一次使用的激光类型, 则使用上一次的激光类型
        typeList.find { it.type.toInt() == HawkEngraveKeys.lastType }?.type?.let {
            return it
        }

        if (vmApp<LaserPeckerModel>().isCSeries()) return -1 else {
            //默认使用第一个激光类型
            val firstType = typeList.firstOrNull()?.type ?: -1
            return if (firstType.toInt() != -1) firstType else LaserPeckerHelper.LASER_TYPE_BLUE
        }
    }

    //region ---文件输出信息---

    /**项目的保存文件夹全路径*/
    val projectFolderPath: String
        get() = appFolderPath(LPDataConstant.PROJECT_FILE_FOLDER)

    /**枚举项目目录下的所有文件*/
    fun eachProjectFolderFile(block: (File) -> Unit) {
        projectFolderPath.file().eachFile(block = block)
    }

    /**gcode文件输出*/
    fun _defaultGCodeOutputFile() =
        filePath(
            LPDataConstant.VECTOR_FILE_FOLDER,
            fileNameTime(suffix = LPDataConstant.EXT_GCODE)
        ).file()

    /**svg文件输出*/
    fun _defaultSvgOutputFile() =
        filePath(
            LPDataConstant.VECTOR_FILE_FOLDER,
            fileNameTime(suffix = LPDataConstant.EXT_SVG)
        ).file()

    /**工程文件输出
     * [ensureExt] 是否要保证后缀为[LPDataConstant.PROJECT_EXT]*/
    fun _defaultProjectOutputFile(name: String, ensureExt: Boolean = true) = filePath(
        LPDataConstant.PROJECT_FILE_FOLDER,
        if (ensureExt) name.ensureName(LPDataConstant.PROJECT_EXT) else name
    ).file()

    /**工程文件输出
     * [ensureExt] 是否要保证后缀为[LPDataConstant.PROJECT_EXT]*/
    fun _defaultProjectOutputFileV2(name: String, ensureExt: Boolean = true) = filePath(
        LPDataConstant.PROJECT_FILE_FOLDER,
        if (ensureExt) name.ensureName(LPDataConstant.PROJECT_EXT2) else name
    ).file()

    /**V2临时存储的文件夹*/
    fun _defaultProjectOutputV2Folder() = folderPath(
        LPDataConstant.PROJECT_FILE_FOLDER + "/" + LPDataConstant.PROJECT_V2_TEMP_FOLDER,
    ).file()

    /**删除项目文件*/
    fun deleteProjectFile(name: String = LPDataConstant.PROJECT_V1_TEMP_NAME): Boolean {
        val file = _defaultProjectOutputFile(name, false)
        return file.delete()
    }

    /**删除项目文件*/
    fun deleteProjectFileV2(name: String = LPDataConstant.PROJECT_V2_TEMP_NAME): Boolean {
        val file = _defaultProjectOutputFileV2(name, false)
        return file.delete()
    }

    fun haveProjectFile(name: String = LPDataConstant.PROJECT_V1_TEMP_NAME): Boolean {
        val file = _defaultProjectOutputFile(name, false)
        return file.exists()
    }

    fun haveProjectFileV2(name: String = LPDataConstant.PROJECT_V2_TEMP_NAME): Boolean {
        val file = _defaultProjectOutputFileV2(name, false)
        return file.exists()
    }

    /**删除项目文件目录, 递归删除*/
    fun deleteProjectFileV2Folder(): Boolean {
        return _defaultProjectOutputV2Folder().deleteRecursivelySafe()
    }

    /**是否有工程文件的临时存放文件夹*/
    fun haveProjectFileV2Folder(): Boolean {
        return File(
            _defaultProjectOutputV2Folder(),
            LPDataConstant.PROJECT_V2_DEFAULT_NAME
        ).exists()
    }

    //endregion ---文件输出信息---

}