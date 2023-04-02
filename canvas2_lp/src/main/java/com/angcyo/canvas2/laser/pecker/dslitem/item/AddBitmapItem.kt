package com.angcyo.canvas2.laser.pecker.dslitem.item

import androidx.fragment.app.Fragment
import com.angcyo.canvas2.laser.pecker.R
import com.angcyo.canvas2.laser.pecker.dslitem.CanvasIconItem
import com.angcyo.canvas2.laser.pecker.util.LPElementHelper
import com.angcyo.component.getFile
import com.angcyo.dsladapter.item.IFragmentItem
import com.angcyo.laserpacker.LPDataConstant
import com.angcyo.library.L
import com.angcyo.library.component.ROpenFileHelper
import com.angcyo.library.ex.*
import com.angcyo.library.toastQQ
import com.angcyo.library.utils.isGCodeContent
import com.angcyo.library.utils.isSvgContent
import com.hingin.umeng.UMEvent
import com.hingin.umeng.umengEventValue

/**
 * 添加图片, 支持svg/gcode/等支持的文件格式
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2023/03/03
 */
class AddBitmapItem : CanvasIconItem(), IFragmentItem {

    override var itemFragment: Fragment? = null

    init {
        itemIco = R.drawable.canvas_image_ico
        itemText = _string(R.string.canvas_photo)

        itemClick = {
            itemFragment?.parentFragmentManager?.getFile { uri ->
                val filePath = ROpenFileHelper.parseData(uri)
                L.i("选择文件:$filePath")
                filePath?.let { path ->
                    val isSvgExt = path.endsWith(LPDataConstant.SVG_EXT, true)
                    val isGCodeExt = path.endsWith(LPDataConstant.GCODE_EXT, true)
                    if (isSvgExt) {
                        //.svg后缀
                        val text = path.file().readText()
                        LPElementHelper.addPathElement(
                            itemRenderDelegate,
                            LPDataConstant.DATA_TYPE_SVG,
                            text,
                            null
                        )
                    } else if (isGCodeExt) {
                        //.gcode后缀
                        val text = path.file().readText()
                        LPElementHelper.addPathElement(
                            itemRenderDelegate,
                            LPDataConstant.DATA_TYPE_GCODE,
                            text,
                            null
                        )
                    } else {
                        val isTxtExt = path.endsWith(LPDataConstant.TXT_EXT, true)
                        if (isTxtExt) {
                            //.txt后缀
                            val text = path.file().readText()
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
                                toastQQ(_string(R.string.not_support))
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
                            toastQQ(_string(R.string.not_support))
                        }
                    }
                }
            }
        }
    }
}