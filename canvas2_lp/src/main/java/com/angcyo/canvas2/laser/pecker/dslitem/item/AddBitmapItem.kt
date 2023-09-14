package com.angcyo.canvas2.laser.pecker.dslitem.item

import android.net.Uri
import androidx.fragment.app.Fragment
import com.angcyo.bluetooth.fsc.laserpacker.HawkEngraveKeys
import com.angcyo.canvas2.laser.pecker.R
import com.angcyo.canvas2.laser.pecker.dslitem.CanvasIconItem
import com.angcyo.canvas2.laser.pecker.util.LPElementHelper
import com.angcyo.component.getFiles
import com.angcyo.dsladapter.item.IFragmentItem
import com.angcyo.laserpacker.LPDataConstant
import com.angcyo.laserpacker.isGCodeType
import com.angcyo.laserpacker.parseSvgElementList
import com.angcyo.laserpacker.toBitmapElementBeanListV2
import com.angcyo.laserpacker.toElementBean
import com.angcyo.laserpacker.toElementBeanList
import com.angcyo.library.L
import com.angcyo.library.component.ROpenFileHelper
import com.angcyo.library.component.pdf.Pdf
import com.angcyo.library.ex._string
import com.angcyo.library.ex.extName
import com.angcyo.library.ex.file
import com.angcyo.library.ex.isImageType
import com.angcyo.library.ex.readText
import com.angcyo.library.ex.size
import com.angcyo.library.ex.toBitmap
import com.angcyo.library.ex.toListOf
import com.angcyo.library.toastQQ
import com.angcyo.library.utils.isGCodeContent
import com.angcyo.library.utils.isSvgContent
import com.hingin.umeng.UMEvent
import com.hingin.umeng.umengEventValue

/**
 * 添加图片, 支持svg/gcode/等支持的文件格式
 *
 * [com.angcyo.laserpacker.open.CanvasOpenPreviewActivity.handleFilePath]
 *
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2023/03/03
 */
class AddBitmapItem : CanvasIconItem(), IFragmentItem {

    override var itemFragment: Fragment? = null

    init {
        itemIco = R.drawable.canvas_image_ico
        itemText = _string(R.string.canvas_photo)

        itemClick = {
            itemFragment?.parentFragmentManager?.getFiles(HawkEngraveKeys.maxSelectorPhotoCount) { uriList ->
                if (!uriList.isNullOrEmpty()) {
                    var haveNotSupport = false
                    for (uri in uriList) {
                        haveNotSupport = haveNotSupport || !addUri(uri)
                    }
                    if (haveNotSupport) {
                        if (uriList.size() > 1) {
                            toastQQ(_string(R.string.not_support_part))
                        } else {
                            toastQQ(_string(R.string.not_support))
                        }
                    }
                }
            }
        }
    }

    /**直接打开文件
     * [com.angcyo.laserpacker.open.CanvasOpenPreviewActivity.handleFilePath]
     * */
    private fun addUri(uri: Uri?): Boolean {
        val filePath = ROpenFileHelper.parseData(uri)

        val extName = filePath?.extName() ?: ""
        if (extName.isNotEmpty()) {
            UMEvent.OPEN_FILE.umengEventValue {
                put(UMEvent.KEY_FILE_EXT, extName)
            }
        }

        L.i("选择文件:$filePath")
        filePath?.let { path ->
            val file = path.file()
            val isSvgExt = path.endsWith(LPDataConstant.SVG_EXT, true)
            val isGCodeExt = path.isGCodeType()
            if (isSvgExt) {
                //.svg后缀
                val text = file.readText()

                if (HawkEngraveKeys.enableImportGroup ||
                    (text?.length ?: 0) <= HawkEngraveKeys.autoEnableImportGroupLength
                ) {
                    val elementList = parseSvgElementList(text)
                    if (elementList.isNullOrEmpty()) {
                        //no op
                        return false
                    } else {
                        LPElementHelper.addElementList(itemRenderDelegate, elementList)
                    }
                } else {
                    LPElementHelper.addPathElement(
                        itemRenderDelegate,
                        LPDataConstant.DATA_TYPE_SVG,
                        text,
                        null
                    )
                }
            } else if (isGCodeExt) {
                //.gcode后缀
                val text = file.readText()
                LPElementHelper.addPathElement(
                    itemRenderDelegate,
                    LPDataConstant.DATA_TYPE_GCODE,
                    text,
                    null
                )
            } else if (path.endsWith(LPDataConstant.LPBEAN_EXT, true)) {
                //.lpbean后缀
                val text = file.readText()
                if (text.isNullOrBlank()) {
                    return false
                }
                val beanList = if (text.startsWith("[")) {
                    //List<LPElementBean>
                    text.toElementBeanList()
                } else {
                    //LPElementBean
                    text.toElementBean()?.toListOf()
                }
                LPElementHelper.addElementList(itemRenderDelegate, beanList)
            } else if (path.endsWith(LPDataConstant.PDF_EXT, true)) {
                //pdf文件
                val bitmapList = Pdf.readPdfToBitmap(file)
                val beanList = bitmapList.toBitmapElementBeanListV2()
                LPElementHelper.addElementList(itemRenderDelegate, beanList)
            } else {
                val isTxtExt = path.endsWith(LPDataConstant.TXT_EXT, true)
                if (isTxtExt) {
                    //.txt后缀
                    val text = file.readText()
                    if (text?.isSvgContent() == true) {
                        //svg内容
                        LPElementHelper.addPathElement(
                            itemRenderDelegate,
                            LPDataConstant.DATA_TYPE_SVG,
                            text,
                            null
                        )
                    } else if (text?.isGCodeContent() == true) {
                        //gcode内容
                        LPElementHelper.addPathElement(
                            itemRenderDelegate,
                            LPDataConstant.DATA_TYPE_GCODE,
                            text,
                            null
                        )
                    } else {
                        //文本内容
                        LPElementHelper.addTextElement(itemRenderDelegate, text)
                    }
                } else if (path.isImageType()) {
                    //图片
                    /*itemFragment?.engraveLoadingAsync({
                        *//*val newPath = path.luban()
                                L.i("${path}->${newPath}")
                                //压缩后
                                val newBitmap = newPath.toBitmap() ?: return@engraveLoadingAsync
                                //itemCanvasDelegate?.addBlackWhiteBitmapRender(newBitmap)
                                newBitmap.recycle()*//*
                            })*/
                    LPElementHelper.addBitmapElement(
                        itemRenderDelegate,
                        filePath.toBitmap()
                    )
                    UMEvent.CANVAS_IMAGE.umengEventValue()
                } else {
                    return false
                }
            }
        }
        return true
    }
}