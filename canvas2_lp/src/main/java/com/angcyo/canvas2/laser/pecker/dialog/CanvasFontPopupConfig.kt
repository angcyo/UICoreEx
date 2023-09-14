package com.angcyo.canvas2.laser.pecker.dialog

import android.content.Context
import android.graphics.Typeface
import android.net.Uri
import android.view.View
import androidx.fragment.app.FragmentActivity
import com.angcyo.bluetooth.fsc.laserpacker.HawkEngraveKeys
import com.angcyo.bluetooth.fsc.laserpacker.writeBleLog
import com.angcyo.canvas.render.core.CanvasRenderDelegate
import com.angcyo.canvas.render.element.TextElement
import com.angcyo.canvas.render.renderer.BaseRenderer
import com.angcyo.canvas.render.util.element
import com.angcyo.canvas.render.util.renderElement
import com.angcyo.canvas2.laser.pecker.R
import com.angcyo.canvas2.laser.pecker.dslitem.ICanvasRendererItem
import com.angcyo.canvas2.laser.pecker.dslitem.control.TypefaceItem
import com.angcyo.component.getFiles
import com.angcyo.core.component.file.writeErrorLog
import com.angcyo.core.component.model.DataShareModel
import com.angcyo.core.vmApp
import com.angcyo.dialog.TargetWindow
import com.angcyo.dialog.popup.MenuPopupConfig
import com.angcyo.dialog.popup.actionPopupWindow
import com.angcyo.dsladapter.DslAdapter
import com.angcyo.dsladapter._dslAdapter
import com.angcyo.dsladapter.drawBottom
import com.angcyo.dsladapter.selectItem
import com.angcyo.laserpacker.device.engraveLoadingAsync
import com.angcyo.library._screenHeight
import com.angcyo.library._screenWidth
import com.angcyo.library.component.FontManager
import com.angcyo.library.component.pad.isInPadMode
import com.angcyo.library.ex._dimen
import com.angcyo.library.ex._string
import com.angcyo.library.ex.decode
import com.angcyo.library.ex.getShowName
import com.angcyo.library.ex.isFileExist
import com.angcyo.library.ex.nowTimeString
import com.angcyo.library.ex.shareFile
import com.angcyo.library.ex.zip
import com.angcyo.library.libCacheFile
import com.angcyo.library.model.TypefaceInfo
import com.angcyo.library.toast
import com.angcyo.library.utils.isChildClassOf
import com.angcyo.widget.DslViewHolder
import com.angcyo.widget.recycler.renderDslAdapter
import com.angcyo.widget.recycler.scrollToFirst
import com.angcyo.widget.tab
import kotlin.math.max

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

    /**选中字体的回调*/
    var onSelectedTypefaceAction: (Typeface?) -> Unit = {}

    override var itemRenderer: BaseRenderer? = null

    override var itemRenderDelegate: CanvasRenderDelegate? = null

    init {
        popupLayoutId = R.layout.dialog_font_layout

        if (isInPadMode()) {
            width = max(_screenWidth, _screenHeight) / 2
        }
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
                            val customFontList = FontManager.getCustomFontList()
                            renderAdapterFontList(customFontList)
                            checkShowBackupsView()
                        }

                        else -> {
                            //推荐
                            renderAdapterFontList(FontManager.getPrimaryFontList())
                        }
                    }
                }
            }
            //default
            renderAdapterFontList(FontManager.getPrimaryFontList())
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

        //备份字体
        viewHolder.click(R.id.export_view) {
            engraveLoadingAsync({
                val fontList = mutableListOf(FontManager.defaultCustomFontFolder)
                fontList.addAll(FontManager.customFontFolderList)
                fontList.zip(libCacheFile(buildString {
                    append("LP-${_string(R.string.canvas_font)}_")
                    append(nowTimeString("yyyy-MM-dd"))
                    append(FontManager.FONT_LIST_EXT)
                }).absolutePath)?.shareFile()
            }) {

            }
        }

        //监听同步更新的状态
        vmApp<DataShareModel>().shareUpdateAdapterItemOnceData.observe(this) {
            it?.let {
                if (it is Class<*> && it.isChildClassOf(TypefaceItem::class.java)) {
                    if (tabLayout?.currentItemIndex == 2) {
                        _recyclerView?._dslAdapter?.updateAllItem()
                    }
                }
            }
        }
    }

    /**检查是否要显示备份字体按钮*/
    fun checkShowBackupsView() {
        val viewHolder = _popupViewHolder ?: return
        val tabLayout = viewHolder.tab(R.id.lib_tab_layout)
        val customFontList = FontManager.getCustomFontList()
        viewHolder.visible(
            R.id.export_view,
            tabLayout?.currentItemIndex == 2 && customFontList.isNotEmpty()
        )
    }

    /**选择字体文件, 2023-5-8支持多选字体*/
    fun selectFont(context: FragmentActivity, viewHolder: DslViewHolder) {
        val tabLayout = viewHolder.tab(R.id.lib_tab_layout)
        context.supportFragmentManager.getFiles { list ->
            if (!list.isNullOrEmpty()) {
                val typefaceInfoList = mutableListOf<TypefaceInfo>()
                for (uri in list) {
                    val infoList = importFont(uri)
                    infoList?.let {
                        typefaceInfoList.addAll(it)

                        for (typefaceInfo in it) {
                            if (!typefaceInfo.isRepeat) {
                                if (tabLayout?.currentItemIndex == 2) {
                                    viewHolder.rv(R.id.lib_recycler_view)
                                        ?.renderDslAdapter(true, false) {
                                            typefaceItem(typefaceInfo, index = 0) //插入到第一个位置
                                            onDispatchUpdatesOnce {
                                                viewHolder.rv(R.id.lib_recycler_view)
                                                    ?.scrollToFirst()
                                            }
                                        }
                                }
                            } else {
                                toast(_string(R.string.canvas_font_exist))
                            }
                        }
                    }
                }
                if (typefaceInfoList.isEmpty()) {
                    toast(_string(R.string.canvas_invalid_font))
                } else {
                    if (tabLayout?.currentItemIndex != 2) {
                        tabLayout?.setCurrentItem(2)
                    }
                }
                checkShowBackupsView()
            }
        }
    }

    fun importFont(uri: Uri): List<TypefaceInfo>? {
        return try {
            "准备导入字体[${uri.getShowName()}]:${"$uri".decode()}".writeBleLog()
            FontManager.importCustomFont(uri)
        } catch (e: Exception) {
            e.printStackTrace()
            "导入字体失败[${"$uri".decode()}]:${e}".writeErrorLog()
            null
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
            previewText = HawkEngraveKeys.typefacePreviewText ?: _string(R.string.canvas_font_text)
            typeface = type
            itemIsSelected = if (element is TextElement) {
                element.paint.typeface == type
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
                    //action
                    onSelectedTypefaceAction(typeface)
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
                                    checkShowBackupsView()
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
        itemRenderer?.element<TextElement>()
            ?.updatePaintTypeface(typeface, renderer, itemRenderDelegate)
    }
}

/**Dsl*/
fun Context.canvasFontWindow(anchor: View?, config: CanvasFontPopupConfig.() -> Unit): Any {
    val popupConfig = CanvasFontPopupConfig()
    popupConfig.anchor = anchor
    popupConfig.config()
    return popupConfig.show(this)
}