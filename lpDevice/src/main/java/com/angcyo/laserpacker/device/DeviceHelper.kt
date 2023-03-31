package com.angcyo.laserpacker.device

import com.angcyo.bluetooth.fsc.laserpacker.LaserPeckerHelper
import com.angcyo.core.component.DslLayout
import com.angcyo.core.component.file.appFilePath
import com.angcyo.core.component.renderLayout
import com.angcyo.glide.loadImage
import com.angcyo.http.rx.runRx
import com.angcyo.library.Library
import com.angcyo.library.ex.*
import com.angcyo.library.libCacheFile
import com.angcyo.library.toastQQ
import com.angcyo.library.utils.logPath
import com.angcyo.objectbox.findLast
import com.angcyo.objectbox.laser.pecker.LPBox
import com.angcyo.objectbox.laser.pecker.entity.EngraveTaskEntity

/**
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2023/03/30
 */
object DeviceHelper {

    /**临时的雕刻日志文件路径集合, 在分享之后清空*/
    val tempEngraveLogPathList = mutableListOf<String>()

    /**分享最近的雕刻日志*/
    fun shareEngraveLog() {
        toastQQ(_string(R.string.create_log_tip))
        runRx({
            val logList = mutableListOf(logPath())
            Library.hawkPath?.let { logList.add(it) } //xml
            logList.addAll(getTaskEngraveLogFilePath())
            logList.addAll(tempEngraveLogPathList)

            tempEngraveLogPathList.clear()
            logList.zip(libCacheFile("LaserPecker-log-${nowTimeString("yyyy-MM-dd")}.zip").absolutePath)
                ?.shareFile()
        })
    }

    /**截图之后的意见反馈*/
    fun showEngraveScreenShotShare(path: String) {
        tempEngraveLogPathList.clear()
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
        task?.dataIndexList?.forEach {
            result.addAll(getIndexLogFilePath(it))
        }
        return result
    }

    /**通过雕刻索引, 获取对应的元素预览图片文件路径*/
    fun getEngravePreviewBitmapPath(index: Any?): String = appFilePath(
        "${index}${DeviceConstant.EXT_PREVIEW.ensureExtName()}",
        DeviceConstant.ENGRAVE_FILE_FOLDER
    )

    /**获取指定索引对应的雕刻日志文件*/
    fun getIndexLogFilePath(index: Any?): List<String> {
        val result = mutableListOf<String>()
        //.png
        var path = appFilePath(
            "$index${DeviceConstant.EXT_PREVIEW}",
            DeviceConstant.ENGRAVE_FILE_FOLDER
        )
        if (path.isFileExist()) {
            result.add(path)
        }
        //.p.png
        path = appFilePath(
            "$index${DeviceConstant.EXT_DATA_PREVIEW}",
            DeviceConstant.ENGRAVE_FILE_FOLDER
        )
        if (path.isFileExist()) {
            result.add(path)
        }
        //.bp
        path = appFilePath("$index${DeviceConstant.EXT_BP}", DeviceConstant.ENGRAVE_FILE_FOLDER)
        if (path.isFileExist()) {
            result.add(path)
        }
        //.dt
        path = appFilePath("$index${DeviceConstant.EXT_DT}", DeviceConstant.ENGRAVE_FILE_FOLDER)
        if (path.isFileExist()) {
            result.add(path)
        }
        //.gcode
        path =
            appFilePath("$index${DeviceConstant.EXT_GCODE}", DeviceConstant.ENGRAVE_FILE_FOLDER)
        if (path.isFileExist()) {
            result.add(path)
        }
        return result
    }

    /**获取推荐的激光类型*/
    fun getProductLaserType(): Byte {
        return LaserPeckerHelper.findProductSupportLaserTypeList()
            .find { it.type.toInt() == HawkEngraveKeys.lastType }?.type
            ?: LaserPeckerHelper.LASER_TYPE_BLUE
    }

}