package com.angcyo.canvas2.laser.pecker.manager

import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.ViewGroup
import com.angcyo.canvas.render.core.CanvasRenderDelegate
import com.angcyo.canvas2.laser.pecker.BuildConfig
import com.angcyo.canvas2.laser.pecker.IEngraveRenderFragment
import com.angcyo.canvas2.laser.pecker.R
import com.angcyo.canvas2.laser.pecker.engrave.BaseFlowLayoutHelper
import com.angcyo.canvas2.laser.pecker.engrave.SingleFlowLayoutHelper
import com.angcyo.canvas2.laser.pecker.manager.dslitem.LpbFileItem
import com.angcyo.core.fragment.BaseDslFragment
import com.angcyo.core.showIn
import com.angcyo.dialog.itemsDialog
import com.angcyo.fragment.AbsLifecycleFragment
import com.angcyo.item.component.initSearchAdapterFilter
import com.angcyo.library.ex._string
import com.angcyo.library.ex.getColor
import com.angcyo.library.ex.isDebugType

/**
 * 文件管理
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2023/08/03
 */
class FileManagerFragment : BaseDslFragment(), IEngraveRenderFragment {

    var currentFileType: Int = TYPE_SD

    companion object {

        /**
         * ```
         * 当mount=0时查询U盘列表。
         * 当mount=1时查询SD卡文件列表
         * ```
         * */
        const val TYPE_USB = 0
        const val TYPE_SD = 1
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
        _vh.tv(R.id.filter_text_view)?.text = _string(R.string.sd_card_file_title)
        renderDslAdapter(true) {
            for (i in 0..10) {
                LpbFileItem()() {
                    itemFileName = "SD文件名$i"
                    itemPreviewAction = {
                        startPreview(itemFileName, TYPE_SD)
                    }
                    itemEngraveAction = {
                        startEngrave(itemFileName, TYPE_SD)
                    }
                }
            }
        }
    }

    /**渲染U盘文件列表*/
    private fun renderUsbListLayout() {
        _vh.tv(R.id.filter_text_view)?.text = _string(R.string.usb_file_title)
        renderDslAdapter(true) {
            for (i in 0..10) {
                LpbFileItem()() {
                    itemFileName = "Usb文件名$i"
                    itemPreviewAction = {
                        startPreview(itemFileName, TYPE_USB)
                    }
                    itemEngraveAction = {
                        startEngrave(itemFileName, TYPE_USB)
                    }
                }
            }
        }
    }

    private fun startPreview(fileName: String?, mount: Int) {
        fileName ?: return
        if (BuildConfig.BUILD_TYPE.isDebugType()) {
            flowLayoutHelper._startPreview(this)
        } else {
            flowLayoutHelper.startPreview(this)
        }
    }

    private fun startEngrave(fileName: String?, mount: Int) {
        fileName ?: return
        flowLayoutHelper.engraveFlow = BaseFlowLayoutHelper.ENGRAVE_FLOW_BEFORE_CONFIG
        flowLayoutHelper.showIn(this, flowLayoutContainer)
    }

    //<editor-fold desc="IEngraveCanvasFragment">

    private val _flowLayoutHelper: SingleFlowLayoutHelper by lazy {
        SingleFlowLayoutHelper()
    }

    override val fragment: AbsLifecycleFragment
        get() = this

    override val flowLayoutHelper: SingleFlowLayoutHelper
        get() = _flowLayoutHelper.apply {
            engraveCanvasFragment = this@FileManagerFragment
        }

    override val renderDelegate: CanvasRenderDelegate?
        get() = null

    override val flowLayoutContainer: ViewGroup?
        get() = _vh.group(R.id.lib_content_overlay_wrap_layout) ?: _vh.itemView as ViewGroup

    override val dangerLayoutContainer: ViewGroup?
        get() = flowLayoutContainer

    //</editor-fold desc="IEngraveCanvasFragment">

}