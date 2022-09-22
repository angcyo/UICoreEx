package com.angcyo.canvas.laser.pecker.dslitem

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import com.angcyo.canvas.graphics.addBitmapRender
import com.angcyo.canvas.laser.pecker.R
import com.angcyo.canvas.laser.pecker.loadingAsync
import com.angcyo.component.getPhoto
import com.angcyo.component.luban.luban
import com.angcyo.dsladapter.item.IFragmentItem
import com.angcyo.library.L
import com.angcyo.library.Library
import com.angcyo.library.ex._string
import com.angcyo.library.ex.isDebugType
import com.angcyo.library.ex.save
import com.angcyo.library.ex.toBitmap
import com.angcyo.library.libCacheFile
import com.angcyo.library.model.loadPath
import com.angcyo.library.utils.fileNameUUID
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
                            itemFragment?.loadingAsync({
                                media.loadPath()?.apply {
                                    //canvasView.addDrawableRenderer(toBitmap())
                                    //canvasView.addBitmapRenderer(toBitmap())
                                    itemCanvasDelegate?.addBitmapRender(toBitmap())
                                }
                            })
                        }
                    }
                } else {
                    it.context.getPhoto(this) { bitmap ->
                        bitmap?.let {
                            itemFragment?.loadingAsync({
                                val path = libCacheFile(fileNameUUID(".png")).absolutePath
                                bitmap.save(path)
                                val newPath = path.luban()
                                L.i("${path}->${newPath}")

                                //压缩后
                                itemCanvasDelegate?.addBitmapRender(newPath.toBitmap())
                            })
                        }
                    }
                }
            }
            UMEvent.CANVAS_IMAGE.umengEventValue()
        }
    }
}