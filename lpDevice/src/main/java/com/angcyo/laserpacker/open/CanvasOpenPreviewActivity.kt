package com.angcyo.laserpacker.open

import android.graphics.Color
import android.os.Bundle
import androidx.core.graphics.drawable.toDrawable
import com.angcyo.activity.BaseAppCompatActivity
import com.angcyo.base.dslFHelper
import com.angcyo.bluetooth.fsc.laserpacker.HawkEngraveKeys
import com.angcyo.core.vmApp
import com.angcyo.dsladapter.DslAdapter
import com.angcyo.dsladapter.DslAdapterStatusItem
import com.angcyo.dsladapter.updateAdapterState
import com.angcyo.getData
import com.angcyo.http.rx.doBack
import com.angcyo.http.rx.doMain
import com.angcyo.kabeja.library.Dxf
import com.angcyo.laserpacker.*
import com.angcyo.laserpacker.device.BuildConfig
import com.angcyo.laserpacker.device.R
import com.angcyo.laserpacker.device.engraveLoadingAsync
import com.angcyo.laserpacker.device.exception.OutOfSizeException
import com.angcyo.laserpacker.device.firmware.FirmwareUpdateFragment
import com.angcyo.laserpacker.project.readProjectBean
import com.angcyo.library.annotation.ThreadDes
import com.angcyo.library.component.FontManager
import com.angcyo.library.component.FontManager.toTypeface
import com.angcyo.library.component._delay
import com.angcyo.library.ex.*
import com.angcyo.library.getAppIcon
import com.angcyo.library.libCacheFile
import com.angcyo.library.utils.isGCodeContent
import com.angcyo.library.utils.isSvgContent
import com.angcyo.putData
import com.angcyo.widget.recycler.renderDslAdapter
import java.io.File

/**
 * 文件打开的预览界面
 * [com.angcyo.laserpacker.open.CanvasOpenActivity]
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2022/10/12
 */
class CanvasOpenPreviewActivity : BaseAppCompatActivity() {

    init {
        activityLayoutId = R.layout.activity_open_preview_layout
    }

    /**需要打开的文件路径*/
    var openFilePath: String? = null

    override fun onCreateAfter(savedInstanceState: Bundle?) {
        super.onCreateAfter(savedInstanceState)

        var adapter: DslAdapter? = null

        _vh.rv(R.id.lib_recycler_view)?.apply {
            setBackgroundColor(Color.WHITE)
            renderDslAdapter {
                adapter = this
                dslAdapterStatusItem.itemEnableRetry = false
                setAdapterStatus(DslAdapterStatusItem.ADAPTER_STATUS_LOADING)
            }
        }

        doBack {
            try {
                val path: String? = getData()
                openFilePath = path
                if (path != null && path.isFileExist()) {
                    if (!handleFilePath(adapter, path)) {
                        //不支持
                        openFilePath = null
                    }
                }

                //support
                if (openFilePath == null) {
                    adapter?.updateAdapterState(IllegalStateException("not support!\n${path?.lastName()}"))
                }
            } catch (e: Exception) {
                adapter?.updateAdapterState(e)
            }
        }
    }

    /**处理文件路径*/
    @ThreadDes("耗时方法, 请在子线程中调用")
    @Throws(OutOfSizeException::class)
    fun handleFilePath(adapter: DslAdapter?, filePath: String): Boolean {
        val canvasOpenModel = vmApp<CanvasOpenModel>()
        var path = filePath
        if (path.endsWith(LPDataConstant.DXF_EXT, true)) {
            //dxf文件, 将dxf转成svg文件
            val svgFile = libCacheFile("${filePath.lastName().noExtName()}.svg")
            path = svgFile.absolutePath
            Dxf.parse(filePath, path)
        }
        val file = path.file()
        if (file.length() > HawkEngraveKeys.openFileDataSize) {
            //超过最大的文件大小限制
            throw OutOfSizeException()
        }
        //
        if (path.endsWith(LPDataConstant.PROJECT_EXT, true) ||
            path.endsWith(LPDataConstant.PROJECT_EXT2, true)
        ) {
            //工程文件
            val projectBean = file.readProjectBean()

            if (projectBean != null) {
                adapter?.render {
                    clearAllItems()
                    CanvasOpenPreviewItem()() {
                        itemFilePath = path
                        itemShowName = projectBean.file_name
                        itemDrawable = projectBean._previewImgBitmap?.toDrawable(resources)
                            ?: projectBean.preview_img?.toBitmapOfBase64()?.toDrawable(resources)
                                    ?: getAppIcon()

                        openAction = {
                            canvasOpenModel.open(this@CanvasOpenPreviewActivity, projectBean)
                            finish()
                        }

                        cancelAction = {
                            finish()
                        }
                    }
                }
                return true
            }
        } else if (path.endsWith(LPDataConstant.GCODE_EXT, true) ||
            (path.endsWith(LPDataConstant.TXT_EXT, true) && file.readText()
                ?.isGCodeContent() == true)
        ) {
            val text = file.readText()
            checkFileLimit(text)
            //gcode
            adapter?.render {
                clearAllItems()
                CanvasOpenPreviewItem()() {
                    itemFilePath = path
                    itemDrawable = text?.toGCodePath()?.toDrawable()

                    openAction = {
                        val itemData = text.toGCodeElementBean()
                        canvasOpenModel.open(this@CanvasOpenPreviewActivity, itemData)
                        finish()
                    }

                    cancelAction = {
                        finish()
                    }
                }
            }
            return true
        } else if (path.endsWith(LPDataConstant.SVG_EXT, true) ||
            (path.endsWith(LPDataConstant.TXT_EXT, true) && file.readText()
                ?.isSvgContent() == true)
        ) {
            val text = file.readText()
            checkFileLimit(text)
            //svg
            adapter?.render {
                clearAllItems()
                CanvasOpenPreviewItem()() {
                    itemFilePath = path
                    itemDrawable = parseSvg(text)

                    openAction = {
                        val itemData = text.toSvgElementBean()
                        canvasOpenModel.open(this@CanvasOpenPreviewActivity, itemData)
                        finish()
                    }

                    cancelAction = {
                        finish()
                    }
                }
            }
            return true
        } else if (path.endsWith(FirmwareUpdateFragment.FIRMWARE_EXT, true) ||
            (isAppDebug() && path.endsWith(".bin", true))
        ) {
            //固件升级
            doMain {
                dslFHelper {
                    restore(FirmwareUpdateFragment::class) {
                        putData(path)
                    }
                }
            }
            return true
        } else if (path.isFontType()) {
            //字体
            adapter?.render {
                clearAllItems()
                CanvasOpenPreviewItem()() {
                    itemFilePath = path
                    itemTypeface = path.toTypeface()

                    //导入字体
                    openAction = {
                        if (itemFilePath != path) {
                            path.file().renameTo(File(itemFilePath!!))
                        }

                        val typefaceInfo = FontManager.importCustomFont(itemFilePath)
                        if (typefaceInfo == null) {
                            //导入失败
                            updateAdapterState(IllegalStateException(_string(R.string.font_import_fail)))
                        } else {
                            dslAdapterStatusItem.onBindStateLayout = { itemHolder, state ->
                                if (state == DslAdapterStatusItem.ADAPTER_STATUS_ERROR) {
                                    itemHolder.img(R.id.lib_image_view)?.apply {
                                        setImageResource(R.drawable.lib_ic_succeed)
                                        setWidthHeight(80 * dpi, 80 * dpi)
                                    }
                                }
                            }
                            //导入成功
                            updateAdapterState(IllegalStateException(_string(R.string.font_import_success)))
                            _delay(600) {
                                finish()
                            }
                        }
                    }

                    cancelAction = {
                        finish()
                    }
                }
            }
            return true
        } else if (path.isImageType()) {
            //图片, 导入的图片不进行压缩处理
            adapter?.render {
                clearAllItems()
                CanvasOpenPreviewItem()() {
                    val bitmap = path.toBitmap()
                    itemFilePath = path
                    itemDrawable = bitmap?.toDrawable(resources)

                    openAction = {
                        this@CanvasOpenPreviewActivity.engraveLoadingAsync({
                            bitmap.toBitmapElementBeanV2()
                        }) {
                            canvasOpenModel.open(this@CanvasOpenPreviewActivity, it)
                            finish()
                        }
                    }

                    cancelAction = {
                        finish()
                    }
                }
            }
            return true
        }
        return false
    }

    /**检查文件的行数是否超过了限制, 文本的数据量大小*/
    @Throws(OutOfSizeException::class)
    fun checkFileLimit(text: String?) {
        if (BuildConfig.DEBUG) {
            return
        }
        //2023-4-12 使用Jni解析文件, 速度快, 取消限制
        /*if ((text?.byteSize() ?: 0) > HawkEngraveKeys.openFileByteCount) {
            //超过了字节限制
            throw OutOfSizeException()
        }
        if ((text?.lines()?.size() ?: 0) > HawkEngraveKeys.openFileLineCount) {
            //超过了行数限制
            throw OutOfSizeException()
        }*/
    }

}