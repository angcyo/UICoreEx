package com.angcyo.laserpacker.open

import android.graphics.Paint
import android.graphics.drawable.Drawable
import android.os.Bundle
import androidx.core.graphics.drawable.toDrawable
import com.angcyo.activity.BaseAppCompatActivity
import com.angcyo.base.dslFHelper
import com.angcyo.bluetooth.fsc.laserpacker.HawkEngraveKeys
import com.angcyo.core.component.manage.InnerFileManageModel
import com.angcyo.core.dslitem.DslFileSelectorItem
import com.angcyo.core.vmApp
import com.angcyo.dsladapter.DslAdapter
import com.angcyo.dsladapter.DslAdapterStatusItem
import com.angcyo.dsladapter.updateAdapterState
import com.angcyo.getData
import com.angcyo.http.base.fromJson
import com.angcyo.http.rx.doBack
import com.angcyo.http.rx.doMain
import com.angcyo.kabeja.library.Dxf
import com.angcyo.laserpacker.*
import com.angcyo.laserpacker.bean.LPElementBean
import com.angcyo.laserpacker.bean.XCSBean
import com.angcyo.laserpacker.bean.toElementBeanList
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
import com.angcyo.library.component.isFontListType
import com.angcyo.library.component.pdf.Pdf
import com.angcyo.library.ex.*
import com.angcyo.library.getAppIcon
import com.angcyo.library.libCacheFile
import com.angcyo.library.utils.isGCodeContent
import com.angcyo.library.utils.isSvgContent
import com.angcyo.putData
import com.angcyo.widget.recycler.renderDslAdapter
import com.hingin.umeng.UMEvent
import com.hingin.umeng.umengEventValue
import java.io.File

/**
 * 文件打开的预览界面
 * [com.angcyo.laserpacker.open.CanvasOpenActivity]
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2022/10/12
 */
class CanvasOpenPreviewActivity : BaseAppCompatActivity() {

    companion object {
        /**转换*/
        var convertElementBeanListToDrawable: ((list: List<LPElementBean>?) -> Drawable?)? = null
    }

    init {
        activityLayoutId = R.layout.activity_open_preview_layout
    }

    /**需要打开的文件路径*/
    var openFilePath: String? = null

    override fun onCreateAfter(savedInstanceState: Bundle?) {
        super.onCreateAfter(savedInstanceState)

        var adapter: DslAdapter? = null

        _vh.rv(R.id.lib_recycler_view)?.apply {
            setBackgroundColor(_color(R.color.lib_theme_white_bg_color))
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

    /**处理文件路径,
     *
     * 相册内选择文件
     * [com.angcyo.canvas2.laser.pecker.dslitem.item.AddBitmapItem.addUri]
     * [String.toElementBeanOfFile]
     * */
    @ThreadDes("耗时方法, 请在子线程中调用")
    @Throws(OutOfSizeException::class)
    fun handleFilePath(adapter: DslAdapter?, filePath: String): Boolean {
        val canvasOpenModel = vmApp<CanvasOpenModel>()
        val innerFileManageModel = vmApp<InnerFileManageModel>()
        var path = filePath
        val extName = path.extName()
        if (extName.isNotEmpty()) {
            UMEvent.OPEN_FILE.umengEventValue {
                put(UMEvent.KEY_FILE_EXT, extName)
            }
        }

        if (path.endsWith(LPDataConstant.DXF_EXT, true)) {
            //dxf文件, 将dxf转成svg文件
            val svgFile = libCacheFile("${filePath.lastName().noExtName()}.svg")
            path = svgFile.absolutePath
            Dxf.parse(filePath, path)
        }
        //是否是工程文件
        val isProjectFile = path.endsWith(LPDataConstant.PROJECT_EXT, true) ||
                path.endsWith(LPDataConstant.PROJECT_EXT2, true)
        val file = path.file()
        if (!isProjectFile && file.length() > HawkEngraveKeys.openFileDataSize) {
            //超过最大的文件大小限制
            throw OutOfSizeException()
        }
        //处理文件类型
        if (isProjectFile) {
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
        } else if (path.isGCodeType() ||
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
                    itemDrawable = text?.toGCodePathDrawable()

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
                    var beanList: List<LPElementBean>? = null
                    itemDrawable = if (HawkEngraveKeys.enableImportGroup ||
                        text?.lines().size() <= HawkEngraveKeys.autoEnableImportGroupLines ||
                        (text?.length ?: 0) <= HawkEngraveKeys.autoEnableImportGroupLength
                    ) {
                        val svgBoundsData = SvgBoundsData()
                        beanList = parseSvgElementList(text, svgBoundsData)
                        svgBoundsData.getBoundsScaleMatrix()?.let {
                            HandleKtx.onElementApplyMatrix?.invoke(beanList, it)
                        }
                        convertElementBeanListToDrawable?.invoke(beanList)
                    } else {
                        parseSvg(text)
                    }

                    openAction = {
                        if (beanList.isNullOrEmpty()) {
                            val itemData = text.toSvgElementBean()
                            canvasOpenModel.open(this@CanvasOpenPreviewActivity, itemData)
                        } else {
                            canvasOpenModel.open(this@CanvasOpenPreviewActivity, beanList)
                        }
                        finish()
                    }

                    cancelAction = {
                        finish()
                    }
                }
            }
            return true
        } else if (path.endsWith(LPDataConstant.TXT_EXT, true)) {
            //文本文件
            val text = file.readText()
            checkFileLimit(text)
            //text
            adapter?.render {
                clearAllItems()
                CanvasOpenPreviewItem()() {
                    itemFilePath = path

                    val elementBean = LPElementBean().apply {
                        mtype = LPDataConstant.DATA_TYPE_TEXT
                        this.text = text
                        paintStyle = Paint.Style.FILL.toPaintStyleInt()
                    }
                    val beanList = mutableListOf(elementBean)

                    itemDrawable = convertElementBeanListToDrawable?.invoke(beanList)

                    openAction = {
                        canvasOpenModel.open(this@CanvasOpenPreviewActivity, beanList)
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
        } else if (path.isFontType() || path.isFontListType()) {
            //字体
            adapter?.render {
                clearAllItems()
                CanvasOpenPreviewItem()() {
                    itemFilePath = path
                    if (path.isFontType()) {
                        itemTypeface = path.toTypeface()
                    } else {
                        itemDrawable = _drawable(R.drawable.core_file_icon_font)
                    }
                    itemIsFontType = true
                    //导入字体
                    openAction = {
                        if (itemFilePath != path) {
                            path.file().renameTo(File(itemFilePath!!))
                        }

                        val typefaceInfoList = FontManager.importCustomFont(itemFilePath)
                        if (typefaceInfoList.isEmpty()) {
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
        } else if (path.endsWith(LPDataConstant.LPBEAN_EXT, true)) {
            //LPElementBean
            val text = file.readText()
            if (text.isNullOrBlank()) {
                return false
            } else if (text.startsWith("[") || text.startsWith("{")) {

            } else {
                //无效的格式
                return false
            }
            adapter?.render {
                clearAllItems()
                CanvasOpenPreviewItem()() {
                    itemFilePath = path

                    val beanList = if (text.startsWith("[")) {
                        //List<LPElementBean>
                        text.toElementBeanList()
                    } else {
                        //LPElementBean
                        text.toElementBean()?.toListOf()
                    }

                    itemDrawable = convertElementBeanListToDrawable?.invoke(beanList)

                    openAction = {
                        canvasOpenModel.open(this@CanvasOpenPreviewActivity, beanList)
                        finish()
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
        } else if (path.endsWith(LPDataConstant.PDF_EXT, true)) {
            //pdf文件
            adapter?.render {
                clearAllItems()
                CanvasOpenPreviewItem()() {
                    val bitmapList = Pdf.readPdfToBitmap(file)
                    val bitmap = bitmapList?.firstOrNull()
                    itemFilePath = path
                    itemDrawable = bitmap?.toDrawable(resources)

                    openAction = {
                        this@CanvasOpenPreviewActivity.engraveLoadingAsync({
                            bitmapList.toBitmapElementBeanListV2()
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
        } else if (innerFileManageModel.isSupportImportFile(file)) {
            //支持导入的文件, 但不是创作支持的文件
            adapter?.render {
                clearAllItems()
                CanvasOpenPreviewItem()() {
                    itemFilePath = path
                    itemDrawable = _drawable(DslFileSelectorItem.getFileIconRes(path))
                    cancelAction = {
                        finish()
                    }
                }
            }
            return true
        } else if (path.endsWith(LPDataConstant.XCS_EXT, true)) {
            //xtool的文件格式

            val text = file.readText()
            val xcsBean = text.fromJson<XCSBean>()
            val beanList = xcsBean?.toElementBeanList()
            if (beanList.isNullOrEmpty()) {
                return false
            }

            adapter?.render {
                clearAllItems()
                CanvasOpenPreviewItem()() {
                    itemFilePath = path
                    itemDrawable = convertElementBeanListToDrawable?.invoke(beanList)
                    openAction = {
                        canvasOpenModel.open(this@CanvasOpenPreviewActivity, beanList)
                        finish()
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