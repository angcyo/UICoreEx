package com.angcyo.canvas.laser.pecker

import android.content.Context
import android.graphics.Typeface
import android.view.View
import androidx.fragment.app.FragmentActivity
import com.angcyo.canvas.TypefaceInfo
import com.angcyo.canvas.items.data.DataTextItem
import com.angcyo.canvas.items.renderer.BaseItemRenderer
import com.angcyo.canvas.items.data.DataItemRenderer
import com.angcyo.canvas.items.renderer.IItemRenderer
import com.angcyo.canvas.items.renderer.PictureTextItemRenderer
import com.angcyo.canvas.laser.pecker.dslitem.TypefaceItem
import com.angcyo.canvas.utils.FontManager
import com.angcyo.component.getFile
import com.angcyo.dialog.TargetWindow
import com.angcyo.dialog.popup.MenuPopupConfig
import com.angcyo.dialog.popup.actionPopupWindow
import com.angcyo.dsladapter.DslAdapter
import com.angcyo.dsladapter.drawBottom
import com.angcyo.dsladapter.selectItem
import com.angcyo.library.ex._dimen
import com.angcyo.library.ex._string
import com.angcyo.library.ex.isFileExist
import com.angcyo.library.toast
import com.angcyo.widget.DslViewHolder
import com.angcyo.widget.recycler.renderDslAdapter
import com.angcyo.widget.recycler.scrollToFirst
import com.angcyo.widget.tab

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
class CanvasFontPopupConfig : MenuPopupConfig() {

    /**操作的渲染项*/
    var itemRenderer: IItemRenderer<*>? = null

    init {
        popupLayoutId = R.layout.canvas_font_layout
    }

    override fun initLayout(window: TargetWindow, viewHolder: DslViewHolder) {
        super.initLayout(window, viewHolder)

        val tabLayout = viewHolder.tab(R.id.lib_tab_layout)
        tabLayout?.apply {
            observeIndexChange { fromIndex, toIndex, reselect, fromUser ->
                if (!reselect) {
                    when (toIndex) {
                        1 -> {
                            //系统字体
                            renderAdapterFontList(FontManager.getSystemFontList())
                        }
                        2 -> {
                            //自定义
                            renderAdapterFontList(FontManager.getCustomFontList())
                        }
                        else -> {
                            //推荐
                            renderAdapterFontList(FontManager.getPrimaryFontList())
                        }
                    }
                }
            }
            /*setCurrentItem(
                when (dataType) {
                    CanvasConstant.DATA_TYPE_QRCODE -> 1
                    CanvasConstant.DATA_TYPE_BARCODE -> 2
                    else -> 0
                }
            )*/
        }

        //导入字体
        viewHolder.click(R.id.import_view) {
            val context = viewHolder.context
            if (context is FragmentActivity) {
                context.supportFragmentManager.getFile("*/*") {
                    if (it != null) {
                        try {
                            val typefaceInfo: TypefaceInfo? = FontManager.importCustomFont(it)
                            if (typefaceInfo != null) {
                                //ui
                                if (!typefaceInfo.isRepeat) {
                                    if (tabLayout?.currentItemIndex == 2) {
                                        viewHolder.rv(R.id.lib_recycler_view)
                                            ?.renderDslAdapter(true, false) {
                                                typefaceItem(typefaceInfo, index = 0)
                                                onDispatchUpdatesOnce {
                                                    viewHolder.rv(R.id.lib_recycler_view)
                                                        ?.scrollToFirst()
                                                }
                                            }
                                    } else {
                                        //
                                        tabLayout?.setCurrentItem(2)
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

    fun renderAdapterFontList(list: List<TypefaceInfo>) {
        _recyclerView?.renderDslAdapter {
            list.forEach {
                typefaceItem(it)
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
            itemIsSelected = if (itemRenderer is DataItemRenderer) {
                (itemRenderer?.getRendererRenderItem() as? DataTextItem)?.textPaint?.typeface == type
            } else {
                (itemRenderer as? BaseItemRenderer)?.paint?.typeface == type
            }
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
            if (info.isCustom) {
                //自定义字体才支持删除
                itemLongClick = {
                    //长按删除字体
                    if (info.filePath.isFileExist()) {
                        it.context.actionPopupWindow(it) {
                            addAction(_string(R.string.canvas_delete_font)) { window, view ->
                                if (FontManager.deleteCustomFont(info)) {
                                    render {
                                        removeAdapterItem()
                                    }
                                }
                            }
                        }
                    }
                    true
                }
            }
        }
    }

    //更新字体
    fun updatePaintTypeface(typeface: Typeface?) {
        val renderer = itemRenderer
        if (renderer is PictureTextItemRenderer) {
            renderer.updateTextTypeface(typeface)
        } else if (renderer is DataItemRenderer) {
            renderer.dataTextItem?.updateTextTypeface(typeface, renderer)
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