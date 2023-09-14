package com.angcyo.canvas2.laser.pecker.dialog

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.os.Build
import android.widget.ImageView
import androidx.appcompat.widget.AppCompatTextView
import com.angcyo.bitmap.handle.BitmapHandle
import com.angcyo.bluetooth.fsc.laserpacker.LaserPeckerHelper
import com.angcyo.bluetooth.fsc.laserpacker.LaserPeckerModel
import com.angcyo.canvas2.laser.pecker.R
import com.angcyo.core.vmApp
import com.angcyo.dialog.DslDialogConfig
import com.angcyo.dialog.configBottomDialog
import com.angcyo.library.annotation.DSL
import com.angcyo.library.annotation.Dp
import com.angcyo.library.component.hawk.LibHawkKeys
import com.angcyo.library.ex._color
import com.angcyo.library.ex.deleteSafe
import com.angcyo.library.ex.nowTimeString
import com.angcyo.library.ex.readText
import com.angcyo.library.ex.save
import com.angcyo.library.ex.shareFile
import com.angcyo.library.ex.zip
import com.angcyo.library.getAppVersionCode
import com.angcyo.library.libCacheFile
import com.angcyo.library.toastQQ
import com.angcyo.widget.DslViewHolder
import com.angcyo.widget.base.saveView
import com.angcyo.widget.base.setTextSizeDp
import com.angcyo.widget.base.string
import kotlin.math.roundToInt

/**
 * 字库生成对话框
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2023/09/13
 */
class FontLibraryHandleDialogConfig(context: Context? = null) : DslDialogConfig(context) {

    /**字库数据文件路径*/
    val fontLibraryDataPathList = mutableListOf<String>()

    init {
        dialogLayoutId = R.layout.dialog_font_library_handle_layout
        dialogTitle = "字库生成"
    }

    override fun initDialogView(dialog: Dialog, dialogViewHolder: DslViewHolder) {
        super.initDialogView(dialog, dialogViewHolder)

        dialogViewHolder.click(R.id.create_data_button) {
            createData(dialogViewHolder, false, true)
        }
        dialogViewHolder.click(R.id.share_data_button) {
            shareData()
        }
        dialogViewHolder.click(R.id.preview_button) {
            createData(dialogViewHolder, true, false)
        }
        dialogViewHolder.click(R.id.typeface_button) { anchor ->
            //字体选择
            anchor.context.canvasFontWindow(anchor) {
                onSelectedTypefaceAction = {
                    dialogViewHolder.tv(R.id.lib_content_edit_view)?.typeface = it
                }
            }
        }
    }

    val sizeStr: String
        get() = _dialogViewHolder?.tv(R.id.lib_edit_view)?.string(false) ?: ""

    val dpi: Float
        get() = vmApp<LaserPeckerModel>().productInfoData.value?.pxList?.find {
            it.px == 2.toByte() || (it.dpi / LaserPeckerHelper.DPI_254).roundToInt() == 2
        }?.dpi ?: LaserPeckerHelper.DPI_254

    /**创建数据*/
    private fun createData(
        dialogViewHolder: DslViewHolder,
        previewRaw: Boolean,
        previewLog: Boolean
    ) {
        val content = dialogViewHolder.tv(R.id.lib_content_edit_view)?.string(false) ?: ""
        if (content.isEmpty()) {
            toastQQ("请输入字库内容")
            return
        }

        @Dp
        val size = sizeStr.toFloatOrNull() ?: 8f

        val previewLayout = dialogViewHolder.group(R.id.lib_preview_layout)
        previewLayout?.removeAllViews()
        fontLibraryDataPathList.clear()

        //字库数据文件
        val dataFile = libCacheFile("data${sizeStr}_${dpi}.txt")
        dataFile.deleteSafe()
        //临时缓存文件
        val cacheFile = libCacheFile()
        val dataDir = vmApp<LaserPeckerModel>().dataDir()
        var isFirst = true

        for (char in content) {
            val textView = AppCompatTextView(dialogViewHolder.context)
            textView.setTextColor(Color.BLACK)
            textView.setTextSizeDp(size)
            textView.typeface = dialogViewHolder.tv(R.id.lib_content_edit_view)?.typeface
            val text = "$char"
            textView.text = text
            textView.saveView()?.let { bitmap ->

                val dpiBitmap =
                    LaserPeckerHelper.bitmapScale(bitmap, LaserPeckerHelper.LAYER_FILL, dpi)

                if (previewRaw) {
                    previewLayout?.addView(ImageView(dialogViewHolder.context).apply {
                        setBackgroundColor(_color(R.color.error_light))
                        setImageBitmap(dpiBitmap)
                    })
                }

                val bitmapPath = dpiBitmap.save(libCacheFile("${text}.png")).absolutePath
                fontLibraryDataPathList.add(bitmapPath)
                val lines = BitmapHandle.toBitmapPathFont(
                    dpiBitmap,
                    LibHawkKeys.grayThreshold,
                    LibHawkKeys.alphaThreshold,
                    cacheFile.absolutePath,
                    dataDir
                )
                cacheFile.readText()?.let {
                    if (!isFirst) {
                        dataFile.appendText("\n")
                    }
                    //高8位
                    val h = (lines shr 8).toByte()
                    //低8位
                    val l = (lines and 0xFF).toByte()
                    dataFile.appendText("const u16 dat_${text}[]={$h,$l$it};")
                    isFirst = false
                }
            }
        }
        fontLibraryDataPathList.add(dataFile.absolutePath)

        if (previewLog) {
            shareData()
        }
    }

    private fun shareData() {
        if (fontLibraryDataPathList.isEmpty()) {
            toastQQ("请先生成字库数据")
            return
        }
        fontLibraryDataPathList.zip(libCacheFile(buildString {
            append("LP_字库数据")
            append("_${sizeStr}DP_$dpi")
            append("_${getAppVersionCode()}")
            append("_${Build.MODEL}")
            append("_")
            append(nowTimeString("yyyy-MM-dd_HH-mm-ss"))
            append(".zip")
        }).absolutePath)?.shareFile()
    }
}

@DSL
fun Context.fontLibraryHandleDialogConfig(config: FontLibraryHandleDialogConfig.() -> Unit = {}) {
    return FontLibraryHandleDialogConfig(this).run {
        configBottomDialog(this@fontLibraryHandleDialogConfig)
        config()
        show()
    }
}

