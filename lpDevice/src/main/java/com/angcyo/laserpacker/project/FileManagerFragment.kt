package com.angcyo.laserpacker.project

import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import com.angcyo.core.fragment.BaseDslFragment
import com.angcyo.dialog.itemsDialog
import com.angcyo.item.component.initSearchAdapterFilter
import com.angcyo.laserpacker.device.R
import com.angcyo.laserpacker.project.dslitem.LpbFileItem
import com.angcyo.library.ex._string
import com.angcyo.library.ex.getColor

/**
 * 文件管理
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2023/08/03
 */
class FileManagerFragment : BaseDslFragment() {

    var currentFileType: Int = TYPE_SD

    companion object {
        const val TYPE_SD = 1
        const val TYPE_USB = 2
    }

    init {
        fragmentTitle = _string(R.string.file_manager_title)
        fragmentConfig.isLightStyle = true
        fragmentConfig.showTitleLineView = true
        fragmentConfig.fragmentBackgroundDrawable =
            ColorDrawable(getColor(R.color.lib_theme_white_color))

        enableRefresh = true

        titleLayoutId = R.layout.file_manager_title_layout
    }

    override fun initBaseView(savedInstanceState: Bundle?) {
        super.initBaseView(savedInstanceState)
        initSearchAdapterFilter(_string(R.string.ui_search))

        _vh.click(R.id.filter_button) {
            it.context.itemsDialog {
                addDialogItem {
                    itemText = _string(R.string.sd_card_file_title)
                    itemClick = {
                        if (currentFileType != TYPE_SD) {
                            currentFileType = TYPE_SD
                            renderSdListLayout()
                        }
                    }
                }
                addDialogItem {
                    itemText = _string(R.string.usb_file_title)
                    itemClick = {
                        if (currentFileType != TYPE_USB) {
                            currentFileType = TYPE_USB
                            renderUsbListLayout()
                        }
                    }
                }
            }
        }
    }

    override fun onLoadData() {
        super.onLoadData()
        if (currentFileType == TYPE_SD) {
            renderSdListLayout()
        } else if (currentFileType == TYPE_USB) {
            renderUsbListLayout()
        }
    }

    //---

    /**渲染sd卡文件列表*/
    private fun renderSdListLayout() {
        _vh.tv(R.id.lib_text_view)?.text = _string(R.string.sd_card_file_title)
        renderDslAdapter(true) {
            for (i in 0..10) {
                LpbFileItem()() {
                    itemFileName = "SD文件名$i"
                }
            }
        }
    }

    /**渲染U盘文件列表*/
    private fun renderUsbListLayout() {
        _vh.tv(R.id.lib_text_view)?.text = _string(R.string.usb_file_title)
        renderDslAdapter(true) {
            for (i in 0..10) {
                LpbFileItem()() {
                    itemFileName = "Usb文件名$i"
                }
            }
        }
    }

}