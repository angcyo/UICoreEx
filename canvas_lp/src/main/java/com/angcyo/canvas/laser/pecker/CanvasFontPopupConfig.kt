package com.angcyo.canvas.laser.pecker

import android.content.Context
import android.graphics.Typeface
import android.net.Uri
import android.view.View
import androidx.fragment.app.FragmentActivity
import com.angcyo.canvas.TypefaceInfo
import com.angcyo.canvas.items.PictureTextItem
import com.angcyo.canvas.items.renderer.BaseItemRenderer
import com.angcyo.canvas.items.renderer.IItemRenderer
import com.angcyo.canvas.items.renderer.PictureTextItemRenderer
import com.angcyo.canvas.laser.pecker.dslitem.TypefaceItem
import com.angcyo.component.getFile
import com.angcyo.dialog.TargetWindow
import com.angcyo.dialog.popup.ShadowAnchorPopupConfig
import com.angcyo.dialog.popup.actionPopupWindow
import com.angcyo.dsladapter.DslAdapter
import com.angcyo.dsladapter.drawBottom
import com.angcyo.dsladapter.selectItem
import com.angcyo.library.app
import com.angcyo.library.ex.*
import com.angcyo.library.toast
import com.angcyo.library.utils.filePath
import com.angcyo.library.utils.folderPath
import com.angcyo.widget.DslViewHolder
import com.angcyo.widget.recycler.renderDslAdapter
import com.angcyo.widget.recycler.scrollToFirst
import java.io.File

/**
 * 画图字体选择
 *
 * ```
 * collection    font/collection    [RFC8081]
 * otf           font/otf           [RFC8081]
 * sfnt          font/sfnt          [RFC8081]
 * ttf           font/ttf           [RFC8081]
 * woff          font/woff          [RFC8081]
 * woff2         font/woff2         [RFC8081]
 * ```
 * res/font/filename.ttf （.ttf、.ttc、.otf 或 .xml）
 * https://developer.android.com/guide/topics/resources/font-resource?hl=zh-cn
 *
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2022/05/16
 */
class CanvasFontPopupConfig : ShadowAnchorPopupConfig() {

    companion object {

        /**默认的字体文件夹名称*/
        const val DEFAULT_FONT_FOLDER_NAME = "fonts"

        /**字体列表*/
        val fontList = mutableListOf<TypefaceInfo>()

        /**初始化字体列表*/
        fun initFontList() {
            fontList.clear()

            //字体文件夹
            val fontFolder = folderPath(DEFAULT_FONT_FOLDER_NAME)

            //自定义的字体
            fontFolder.file().eachFile { file ->
                try {
                    if (file.name.isFontType()) {
                        val typeface = Typeface.createFromFile(file)
                        fontList.add(
                            TypefaceInfo(file.name.noExtName(), typeface, file.absolutePath)
                        )
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            //旧版本字体目录
            val oldFontFolder = File(app().getExternalFilesDir("laserpecker"), "font")
            oldFontFolder.eachFile { file ->
                try {
                    if (file.name.isFontType()) {
                        val typeface = Typeface.createFromFile(file)
                        fontList.add(
                            TypefaceInfo(file.name.noExtName(), typeface, file.absolutePath)
                        )
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            //默认的字体列表
            fontList.addAll(PictureTextItem.DEFAULT_TYPEFACE_LIST)
        }

        /**导入字体*/
        fun importFont(uri: Uri?): TypefaceInfo? {
            if (uri == null) {
                return null
            }
            return importFont(uri.saveToFolder())
        }

        /**导入字体*/
        fun importFont(path: String?): TypefaceInfo? {
            try {
                if (fontList.isEmpty()) {
                    initFontList()
                }
                if (path.isFontType()) {
                    val file = File(path!!)

                    val typeface = Typeface.createFromFile(file)
                    file.copyTo(filePath(DEFAULT_FONT_FOLDER_NAME, file.name))

                    val typefaceInfo =
                        TypefaceInfo(file.name.noExtName(), typeface, file.absolutePath)

                    val find = fontList.find { it.name == typefaceInfo.name }
                    if (find == null) {
                        fontList.add(0, typefaceInfo)
                    } else {
                        //字体已存在
                        typefaceInfo.isRepeat = true
                    }

                    return typefaceInfo
                }
                return null
            } catch (e: Exception) {
                e.printStackTrace()
                return null
            }
        }
    }

    /**操作的渲染项*/
    var itemRenderer: IItemRenderer<*>? = null

    init {
        contentLayoutId = R.layout.canvas_font_layout
        triangleMinMargin = 24 * dpi
        yoff = -10 * dpi
    }

    override fun initContentLayout(window: TargetWindow, viewHolder: DslViewHolder) {
        super.initContentLayout(window, viewHolder)

        if (fontList.isEmpty()) {
            initFontList()
        }

        //字体列表
        viewHolder.rv(R.id.lib_recycler_view)?.renderDslAdapter {
            fontList.forEach {
                typefaceItem(it)
            }
        }
        //导入字体
        viewHolder.click(R.id.import_view) {
            val context = viewHolder.context
            if (context is FragmentActivity) {
                context.supportFragmentManager.getFile("*/*") {
                    if (it != null) {
                        try {
                            val typefaceInfo: TypefaceInfo? = importFont(it)
                            if (typefaceInfo != null) {
                                //ui
                                if (!typefaceInfo.isRepeat) {
                                    viewHolder.rv(R.id.lib_recycler_view)
                                        ?.renderDslAdapter(true, false) {
                                            typefaceItem(typefaceInfo, index = 0)
                                            onDispatchUpdatesOnce {
                                                viewHolder.rv(R.id.lib_recycler_view)
                                                    ?.scrollToFirst()
                                            }
                                        }
                                } else {
                                    toast(_string(R.string.canvas_font_exist))
                                }
                            } else {
                                error("is not font.")
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                            toast(_string(R.string.canvas_invalid_font))
                        }
                    }
                }
            } else {
                toast(_string(R.string.canvas_cannot_import))
            }
        }
    }

    fun DslAdapter.typefaceItem(info: TypefaceInfo, line: Boolean = true, index: Int = -1) {
        val name = info.name
        val type = info.typeface
        TypefaceItem()(index) {
            itemData = info
            displayName = name
            previewText = _string(R.string.canvas_font_text)
            typeface = type
            itemIsSelected = (itemRenderer as? BaseItemRenderer)?.paint?.typeface == type
            if (line) {
                drawBottom(_dimen(R.dimen.lib_line_px), 0, 0)
            }
            itemClick = {
                if (!itemIsSelected) {
                    selectItem(false) { true }
                    itemIsSelected = true
                    updatePaintTypeface(typeface)
                    updateAdapterItem()
                }
            }
            itemLongClick = {
                //长按删除字体
                if (info.filePath.isFileExist()) {
                    it.context.actionPopupWindow(it) {
                        addAction(_string(R.string.canvas_delete_font)) { window, view ->
                            fontList.remove(info)
                            info.filePath?.file()?.delete()
                            render {
                                removeAdapterItem()
                            }
                        }
                    }
                }
                true
            }
        }
    }

    //更新字体
    fun updatePaintTypeface(typeface: Typeface?) {
        val renderer = itemRenderer
        if (renderer is PictureTextItemRenderer) {
            renderer.updateTextTypeface(typeface)
        }
    }
}

/**Dsl*/
fun Context.canvasFontWindow(anchor: View?, config: CanvasFontPopupConfig.() -> Unit): Any {
    val popupConfig = CanvasFontPopupConfig()
    popupConfig.anchor = anchor
    popupConfig.config()
    return popupConfig.show(this)
}