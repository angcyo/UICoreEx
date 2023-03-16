package com.angcyo.canvas2.laser.pecker.dialog

import android.content.Context
import android.graphics.Typeface
import android.view.View
import androidx.fragment.app.FragmentActivity
import com.angcyo.bluetooth.fsc.laserpacker.writeBleLog
import com.angcyo.canvas.render.core.CanvasRenderDelegate
import com.angcyo.canvas.render.element.TextElement
import com.angcyo.canvas.render.renderer.BaseRenderer
import com.angcyo.canvas.render.util.renderElement
import com.angcyo.canvas.render.util.textElement
import com.angcyo.canvas2.laser.pecker.R
import com.angcyo.canvas2.laser.pecker.dslitem.ICanvasRendererItem
import com.angcyo.canvas2.laser.pecker.dslitem.control.TypefaceItem
import com.angcyo.component.getFile
import com.angcyo.core.component.file.writeErrorLog
import com.angcyo.dialog.TargetWindow
import com.angcyo.dialog.popup.MenuPopupConfig
import com.angcyo.dialog.popup.actionPopupWindow
import com.angcyo.dsladapter.DslAdapter
import com.angcyo.dsladapter.drawBottom
import com.angcyo.dsladapter.selectItem
import com.angcyo.library.component.FontManager
import com.angcyo.library.ex.*
import com.angcyo.library.model.TypefaceInfo
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
class CanvasFontPopupConfig : MenuPopupConfig(), ICanvasRendererItem {

    override var itemRenderer: BaseRenderer? = null

    override var itemRenderDelegate: CanvasRenderDelegate? = null

    init {
        popupLayoutId = R.layout.dialog_font_layout
    }

    override fun initLayout(window: TargetWindow, viewHolder: DslViewHolder) {
        super.initLayout(window, viewHolder)

        val tabLayout = viewHolder.tab(R.id.lib_tab_layout)
        tabLayout?.apply {
            observeIndexChange { fromIndex, toIndex, reselect, fromUser ->
                if (!reselect) {
                    //viewHolder.visible(R.id.sync_font_button, toIndex == 2)
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
                selectFont(context, viewHolder)
            } else {
                toast(_string(R.string.canvas_cannot_import))
            }
        }

        /*
        //同步SD上的字体
        viewHolder.click(R.id.sync_font_button) {
            viewHolder.context.requestSdCardPermission {
                if (it) {
                    FontManager.customFontFolderList.firstOrNull()?.let { folder ->
                        FontManager.backupFontTo(folder)
                        FontManager.loadCustomFont(folder)
                        renderAdapterFontList(FontManager.getCustomFontList())

                        toast("Success")
                    }
                }
            }
        }*/
    }

    /**选择字体文件*/
    fun selectFont(context: FragmentActivity, viewHolder: DslViewHolder) {
        val tabLayout = viewHolder.tab(R.id.lib_tab_layout)
        context.supportFragmentManager.getFile("*/*") {
            if (it != null) {
                try {
                    "准备导入字体[${it.getDisplayName()}]:${"$it".decode()}".writeBleLog()
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
                    "导入字体失败[${"$it".decode()}]:${e}".writeErrorLog()
                    toast(_string(R.string.canvas_invalid_font))
                }
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

        val renderer = itemRenderer
        val element = itemRenderer?.renderElement
        TypefaceItem()(index) {
            itemData = info
            displayName = name
            previewText = _string(R.string.canvas_font_text)
            typeface = type
            itemIsSelected = if (element is TextElement) {
                element.textPaint.typeface == type
            } else {
                false
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
        val renderer = itemRenderer ?: return
        itemRenderer?.textElement?.updatePaintTypeface(typeface, renderer, itemRenderDelegate)
    }
}

/**Dsl*/
fun Context.canvasFontWindow(anchor: View?, config: CanvasFontPopupConfig.() -> Unit): Any {
    val popupConfig = CanvasFontPopupConfig()
    popupConfig.anchor = anchor
    popupConfig.config()
    return popupConfig.show(this)
}