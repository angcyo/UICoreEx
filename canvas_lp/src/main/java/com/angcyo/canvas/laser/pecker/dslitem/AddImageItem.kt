package com.angcyo.canvas.laser.pecker.dslitem

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import com.angcyo.canvas.graphics.addGCodeRender
import com.angcyo.canvas.graphics.addSvgRender
import com.angcyo.canvas.laser.pecker.R
import com.angcyo.canvas.laser.pecker.addBlackWhiteBitmapRender
import com.angcyo.canvas.utils.CanvasConstant
import com.angcyo.component.getFile
import com.angcyo.component.luban.luban
import com.angcyo.dsladapter.item.IFragmentItem
import com.angcyo.engrave.engraveLoadingAsync
import com.angcyo.library.L
import com.angcyo.library.Library
import com.angcyo.library.component.ROpenFileHelper
import com.angcyo.library.ex.*
import com.angcyo.library.model.loadPath
import com.angcyo.library.toastQQ
import com.angcyo.library.utils.isGCodeContent
import com.angcyo.library.utils.isSvgContent
import com.angcyo.picker.dslSinglePickerImage
import com.hingin.umeng.UMEvent
import com.hingin.umeng.umengEventValue

/**
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2022/04/18
 */
class AddImageItem : CanvasControlItem2(), IFragmentItem {

    override var itemFragment: Fragment? = null

    var itemFragmentManager: FragmentManager? = null

    init {
        itemIco = R.drawable.canvas_image_ico
        itemText = _string(R.string.canvas_photo)

        itemClick = {
            (itemFragmentManager ?: itemFragment?.parentFragmentManager)?.apply {
                if (isDebugType() && Library.CLICK_COUNT++ % 2 == 0) {
                    it.context.dslSinglePickerImage(this) {
                        it?.firstOrNull()?.let { media ->
                            itemFragment?.engraveLoadingAsync({
                                media.loadPath()?.apply {
                                    //canvasView.addDrawableRenderer(toBitmap())
                                    //canvasView.addBitmapRenderer(toBitmap())
                                    itemCanvasDelegate?.addBlackWhiteBitmapRender(toBitmap())
                                }
                            })
                        }
                    }
                } else {
                    /*it.context.getPhoto(this) { bitmap ->
                        bitmap?.let {
                            itemFragment?.engraveLoadingAsync({
                                val path = libCacheFile(fileNameUUID(".png")).absolutePath
                                bitmap.save(path)
                                bitmap.recycle()
                                val newPath = path.luban()
                                L.i("${path}->${newPath}")

                                //压缩后
                                val newBitmap = newPath.toBitmap() ?: return@engraveLoadingAsync
                                itemCanvasDelegate?.addBlackWhiteBitmapRender(newBitmap)
                                newBitmap.recycle()
                            })
                        }
                    }*/

                    getFile { uri ->
                        val filePath = ROpenFileHelper.parseData(uri)
                        filePath?.let { path ->
                            if (path.endsWith(CanvasConstant.SVG_EXT, true) || (path.endsWith(
                                    CanvasConstant.TXT_EXT,
                                    true
                                ) && path.file().readText()
                                    ?.isSvgContent() == true)
                            ) {
                                //svg
                                itemCanvasDelegate?.addSvgRender(path.file().readText())
                            } else if (path.endsWith(CanvasConstant.GCODE_EXT, true) ||
                                (path.endsWith(CanvasConstant.TXT_EXT, true) && path.file()
                                    .readText()
                                    ?.isGCodeContent() == true)
                            ) {
                                //gcode
                                itemCanvasDelegate?.addGCodeRender(path.file().readText())
                            } else if (path.isImageType()) {
                                //图片
                                itemFragment?.engraveLoadingAsync({
                                    val newPath = path.luban()
                                    L.i("${path}->${newPath}")

                                    //压缩后
                                    val newBitmap = newPath.toBitmap() ?: return@engraveLoadingAsync
                                    itemCanvasDelegate?.addBlackWhiteBitmapRender(newBitmap)
                                    newBitmap.recycle()
                                })
                            } else {
                                toastQQ("not support!")
                            }
                        }
                    }
                }
            }
            UMEvent.CANVAS_IMAGE.umengEventValue()
        }
    }
}