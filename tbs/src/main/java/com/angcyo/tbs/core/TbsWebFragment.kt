package com.angcyo.tbs.core

import android.content.ComponentName
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.TextView
import androidx.coordinatorlayout.widget.CoordinatorLayout
import com.angcyo.base.dslAHelper
import com.angcyo.base.dslFHelper
import com.angcyo.behavior.HideTitleBarBehavior
import com.angcyo.core.component.fileSelector
import com.angcyo.core.fragment.BaseTitleFragment
import com.angcyo.dialog.configBottomDialog
import com.angcyo.dialog.dslDialog
import com.angcyo.download.download
import com.angcyo.download.downloadNotify
import com.angcyo.dsladapter.renderItemList
import com.angcyo.image.dslitem.DslSubSamplingImageItem
import com.angcyo.library.L
import com.angcyo.library.component.DslIntent
import com.angcyo.library.component.DslNotify
import com.angcyo.library.component.dslIntentShare
import com.angcyo.library.ex.*
import com.angcyo.library.model.loadUri
import com.angcyo.library.toastQQ
import com.angcyo.loader.singleImage
import com.angcyo.loader.singleVideo
import com.angcyo.media.dslitem.DslTextureVideoItem
import com.angcyo.picker.dslPicker
import com.angcyo.tablayout.screenWidth
import com.angcyo.tbs.DslTbs
import com.angcyo.tbs.R
import com.angcyo.tbs.core.dslitem.DslBaseWebMenuItem
import com.angcyo.tbs.core.inner.TbsWeb
import com.angcyo.tbs.core.inner.TbsWebView
import com.angcyo.widget.DslViewHolder
import com.angcyo.widget.bar
import com.angcyo.widget.base.*
import com.angcyo.widget.span.span
import com.tencent.smtt.sdk.QbSdk
import com.tencent.smtt.sdk.TbsReaderView

/**
 * file:///android_asset/webpage/fileChooser.html
 *
 * https://debugtbs.qq.com/
 *
 * Email:angcyo@126.com
 * @author angcyo
 * @date 2020/03/01
 */
open class TbsWebFragment : BaseTitleFragment() {
    companion object {
        const val KEY_CONFIG = "key_config"

        const val LOADING_TITLE = "加载中..."

        const val DEBUG_TBS_URL = "https://debugtbs.qq.com"
    }

    init {
        contentLayoutId = R.layout.layout_tbs_web_content
        contentOverlayLayoutId = R.layout.layout_tbs_web_content_overlay
        fragmentTitle = null
    }

    override fun onCreateBackItem(): View? {
        return super.onCreateBackItem()?.apply {
            find<TextView>(R.id.lib_text_view)?.run {
                text = span {
                    drawable {
                        backgroundDrawable =
                            loadDrawable(R.drawable.lib_back)
                                .colorFilter(fragmentConfig.titleItemIconColor)
                    }
                }
            }
        }
    }

    override fun onCreateBehavior(child: View): CoordinatorLayout.Behavior<*>? {
        return if (child.id == R.id.lib_title_wrap_layout) {
            HideTitleBarBehavior(fContext())
        } else {
            super.onCreateBehavior(child)
        }
    }

    var webConfig = TbsWebConfig()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        arguments?.getParcelable<TbsWebConfig>(KEY_CONFIG)?.run {
            webConfig = this
            if (title != null) {
                fragmentTitle = title
            }
        }
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        //开启硬件加速
        activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED)
    }

    //<editor-fold desc="根据不同的类型, 填充不同的布局">

    override fun onCreateViewAfter(savedInstanceState: Bundle?) {
        super.onCreateViewAfter(savedInstanceState)
        initTbsWebLayout()
    }

    open fun initTbsWebLayout() {
        _vh.tv(R.id.lib_title_text_view)?.run {
            setWidth(width = screenWidth - 180 * dpi)
            setSingleLineMode()
        }

        val wrapLayout = _vh.group(R.id.tbs_wrap_layout)
        val uri = webConfig.uri

        if (uri == null) {
            toastQQ("数据异常", R.drawable.lib_ic_error)
        } else if (wrapLayout == null) {
            toastQQ("布局异常", R.drawable.lib_ic_error)
        } else {
            val loadUrl = uri.loadUrl()
            val mimeType = loadUrl.mimeType() ?: webConfig.mimeType

            L.d("TBS:$uri $loadUrl $mimeType")

            if (mimeType.isVideoMimeType()) {
                if (DslTbs.canUseTbsPlayer()) {
                    DslTbs.openVideo(fContext(), loadUrl!!)
                    dslAHelper {
                        finish()
                    }
                } else {
                    attachVideoView(wrapLayout, uri)
                }
            } else if (uri.isHttpScheme() || mimeType.isHttpMimeType()) {
                //打开网页
                loadTbsWebTitleLayout()
                attachTbsWebView(wrapLayout, loadUrl!!)
            } else if (uri.isFileScheme()) {
                val fileExt = loadUrl!!.ext()
                fragmentTitle = loadUrl.file()!!.name

                val readerView =
                    TbsReaderView(
                        fContext(),
                        TbsReaderView.ReaderCallback { actionType, args, result ->
                            hideLoadingView()
                            L.d("Tbs type:$actionType args:$args result:$result")
                        })

                when {
                    //如果tbs支持打开文件, 一般是文档格式
                    DslTbs.canOpenFileTbs(readerView, fileExt) -> attachTbsReaderView(
                        readerView,
                        wrapLayout,
                        loadUrl
                    )
                    mimeType.isImageMimeType() -> attachImageView(wrapLayout, uri)
                    else -> showLoadingView("无法打开文件\n$uri")
                }
            } else if (mimeType.isTextMimeType()) {
                attachTextView(wrapLayout, uri)
            } else {
                //其他类型
                showLoadingView("不支持的类型\n$uri")
            }
        }
    }

    /**加载网页类型的标题栏*/
    open fun loadTbsWebTitleLayout() {
        //有些网页, 无法回退. 添加强制关闭按钮
        appendLeftItem(ico = R.drawable.tbs_ic_close, action = {
            id = R.id.lib_close_view
            visibility = View.GONE
            marginParams {
                leftMargin = -6 * dpi
            }
        }) {
            close()
        }

        if (webConfig.showRightMenu) {
            //更多
            appendRightItem(ico = R.drawable.tbs_ic_more) {
                fContext().tbsWebMenuDialog {
                    val url = _tbsWebView?._loadUrl
                    webHost = url?.toUri()?.host
                    line1Items = renderItemList {
                        DslBaseWebMenuItem()() {
                            menuText = "刷新"
                            menuIcon = R.drawable.tbs_ic_refresh
                            itemClick = {
                                _dialog?.dismiss()
                                _tbsWebView?.loadUrl(url)
                            }
                        }
                        DslBaseWebMenuItem()() {
                            menuText = "复制链接"
                            menuIcon = R.drawable.tbs_ic_copy
                            itemClick = {
                                _dialog?.dismiss()
                                url?.copy()
                            }
                        }
                        DslBaseWebMenuItem()() {
                            menuText = "分享"
                            menuIcon = R.drawable.tbs_ic_share
                            itemClick = {
                                _dialog?.dismiss()
                                dslIntentShare {
                                    shareTitle = _tbsWebView?.title
                                    shareText = url
                                }
                            }
                        }
                        DslBaseWebMenuItem()() {
                            menuText = "浏览器打开"
                            menuIcon = R.drawable.tbs_ic_browser
                            itemClick = {
                                _dialog?.dismiss()
                                DslIntent.openUrl(fContext(), url)
                            }
                        }

                        if (isDebug()) {
                            DslBaseWebMenuItem()() {
                                menuText = "X5内核测试"
                                menuIcon = R.drawable.tbs_ic_x5
                                itemClick = {
                                    _dialog?.dismiss()
                                    _tbsWebView?.loadUrl(DEBUG_TBS_URL)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    var _tbsWebView: TbsWebView? = null

    /**追加[TbsWebView], 用于打开网页*/
    open fun attachTbsWebView(parent: ViewGroup?, url: String?) {
        //host提示
        if (_vh.view(R.id.lib_host_tip_view) == null) {
            rootControl().group(R.id.lib_coordinator_wrap_layout)?.apply {
                addView(inflate(R.layout.tbs_host_tip_layout, false), 0)
            }
        }

        val webView = TbsWebView(fContext())
        webView.apply {

            id = R.id.tbs_web_view

            _tbsWebView = this

            //标题回调
            receivedTitleAction = {
                if (webConfig.title.isNullOrEmpty()) {
                    fragmentTitle = it
                }

                updateHost(_tbsWebView?.originalUrl)
            }

            //进度回调
            progressChangedAction = { url, progress ->
                // L.d("$url $progress")
                if (webConfig.showLoading) {
                    _vh.bar(R.id.lib_progress_bar)?.setProgress(progress)
                }
                //加载框

                if (progress == 0) {
                    if (fragmentTitle.isNullOrEmpty()) {
                        fragmentTitle = LOADING_TITLE
                    }
                } else if (progress == 100) {
                    if (webView.receivedTitle.isNullOrEmpty()) {
                        receivedTitleAction(webView.title)
                    }
                }

                checkCloseView()

                if (progress >= 80) {
                    hideLoadingView()
                }
            }

            //下载回调
            downloadAction = { url, userAgent, contentDisposition, mime, length ->
                fContext().dslDialog {
                    configBottomDialog()
                    dialogLayoutId = R.layout.dialog_tbs_file_download
                    onDialogInitListener = { dialog, dialogViewHolder ->
                        val fileName = TbsWeb.getFileName(url, contentDisposition)
                        dialogViewHolder.tv(R.id.target_url_view)?.text = url
                        dialogViewHolder.tv(R.id.file_name_view)?.text = fileName
                        dialogViewHolder.tv(R.id.file_size_view)?.text =
                            if (length > 0) length.fileSizeString() else "未知大小"
                        dialogViewHolder.tv(R.id.file_type_view)?.text = mime

                        dialogViewHolder.longClick(R.id.target_url_view) {
                            url.copy()
                            toastQQ("下载地址已复制")
                        }

                        dialogViewHolder.click(R.id.download_button) {
                            dialog.dismiss()
                            url.downloadNotify()
                            url.download {
                                onConfigTask = {
                                    it.setFilename(fileName)
                                }
                            }
                        }
                    }
                }
            }

            //打开其他应用回调
            openAppAction = { url, activityInfo, appBean ->
                fContext().dslDialog {
                    configBottomDialog()
                    dialogLayoutId = R.layout.dialog_tbs_open_app
                    onDialogInitListener = { dialog, dialogViewHolder ->
                        dialogViewHolder.tv(R.id.lib_text_view)?.text = appBean.appName
                        dialogViewHolder.tv(R.id.lib_sub_text_view)?.text = url
                        dialogViewHolder.img(R.id.lib_image_view)?.setImageDrawable(appBean.appIcon)
                        dialogViewHolder.click(R.id.lib_reject_button) {
                            dialog.dismiss()
                        }
                        dialogViewHolder.click(R.id.lib_open_button) {
                            dialog.dismiss()
                            dslAHelper {
                                start(
                                    url.urlIntent(
                                        ComponentName(
                                            activityInfo.packageName,
                                            activityInfo.name
                                        )
                                    )
                                )
                            }
                        }
                    }
                }
            }

            //选择文件回调
            fileChooseAction = {

                when {
                    it.mimeType.isNullOrEmpty() -> {
                        //选择文件
                        dslFHelper {
                            fileSelector {
                                it?.run {
                                    onFileChooseResult(arrayOf(fileUri))
                                } ?: onFileChooseResult(null)
                            }
                        }
                    }
                    it.mimeType.isImageMimeType() -> {
                        //image
                        dslPicker({
                            singleImage()
                            maxSelectorLimit = it.multiLimit
                        }) {
                            it?.let {
                                onFileChooseResultList(it.mapTo(ArrayList()) { it.loadUri() })
                            } ?: onFileChooseResult(null)
                        }
                    }
                    it.mimeType.isVideoMimeType() -> {
                        //video
                        dslPicker({
                            singleVideo()
                            maxSelectorLimit = it.multiLimit
                        }) {
                            it?.let {
                                onFileChooseResultList(it.mapTo(ArrayList()) { it.loadUri() })
                            } ?: onFileChooseResult(null)
                        }
                    }
                }
            }

            //加载url
            loadUrl(url)
        }

        parent?.addView(webView, -1, -1)
    }

    fun updateHost(url: String?) {
        val host = url?.toUri()?.host

        _vh.tv(R.id.lib_host_tip_view)?.text = span {
            if (!host.isNullOrEmpty()) {
                append("网页由 $host 提供")

                if (DslTbs.isX5Core) {
                    appendln()
                    drawable {
                        backgroundDrawable =
                            _drawable(DslNotify.DEFAULT_NOTIFY_ICON)?.setBounds(18 * dpi, 18 * dpi)
                    }
                    append("腾讯x5内核支持")
                }
            }
        }
    }

    var _tbsReaderView: TbsReaderView? = null

    /**追加[TbsReaderView], 用于打开文档格式*/
    open fun attachTbsReaderView(readerView: TbsReaderView, parent: ViewGroup, path: String) {
        val extName = path.ext()
        val param = Bundle()

        //param.putString("fileExt", extName)
        param.putString(TbsReaderView.KEY_FILE_PATH, path)
        param.putString(TbsReaderView.KEY_TEMP_PATH, context.externalCacheDir?.absolutePath)

        //默认不设置，是全屏dialog显示文件内容,
        //param.putInt("windowType",2);
        //设置windowType = 2，进入view显示文件内容, 文件内容会挂到设置的layout上。
        //FILE_READER_WINDOW_TYPE_DEFAULT 全屏样式, 自己的标题栏会无法显示.
        //param.putInt("windowType", TbsFileInterfaceImpl.FILE_READER_WINDOW_TYPE_DEFAULT)

        readerView.apply {
            _tbsReaderView = this
            if (preOpen(extName, false)) {
                openFile(param)
            }
        }

        parent.addView(readerView, -1, -1)

//        //新接口方式, 都会带有默认的标题栏
//        TbsFileInterfaceImpl.getInstance()
//            .openFileReader(fContext(), param, { actionType, args, result ->
//                hideLoadingView()
//
//                L.d("Tbs type:$actionType args:$args result:$result")
//
//                if (ITbsReader.OPEN_FILEREADER_PLUGIN_SUCCESS == actionType) {
//                    L.w("Tbs plugin success")
//                } else if (ITbsReader.OPEN_FILEREADER_PLUGIN_FAILED == actionType) {
//                    L.w("Tbs plugin failed")
//                }
//            }, parent as FrameLayout)
    }

    var _dslVideoHolder: DslViewHolder? = null
    var _dslVideoItem: DslTextureVideoItem? = null

    /**加载视频*/
    open fun attachVideoView(parent: ViewGroup, uri: Uri) {
        parent.setBackgroundColor(Color.BLACK)
        hideLoadingView()

        val dslVideoItem = DslTextureVideoItem().apply {
            _dslVideoItem = this

            itemData = uri
            itemVideoUri = uri

            itemDownloadStart = { itemHolder, task ->
                onDownloadStart(itemHolder, task)
                showLoadingView("下载中...")
            }

            itemDownloadFinish = { itemHolder, task, cause, error ->
                onDownloadFinish(itemHolder, task, cause, error)
                hideLoadingView()
            }
        }
        _dslVideoHolder = parent.appendDslItem(dslVideoItem)
    }

    var _dslSubSamplingItem: DslSubSamplingImageItem? = null

    /**加载大图*/
    open fun attachImageView(parent: ViewGroup, uri: Uri) {
        parent.setBackgroundColor(Color.BLACK)
        hideLoadingView()

        val dslSubSamplingItem = DslSubSamplingImageItem().apply {
            _dslSubSamplingItem = this

            itemData = uri
            itemLoadUri = uri

            itemDownloadStart = { itemHolder, task ->
                onDownloadStart(itemHolder, task)
                showLoadingView("下载中...")
            }

            itemDownloadFinish = { itemHolder, task, cause, error ->
                onDownloadFinish(itemHolder, task, cause, error)
                hideLoadingView()
            }
        }
        _dslVideoHolder = parent.appendDslItem(dslSubSamplingItem)
    }

    /**加载文本*/
    open fun attachTextView(parent: ViewGroup, uri: Uri) {
        hideLoadingView()
        parent.inflate(R.layout.tbs_text_layout).apply {
            find<TextView>(R.id.lib_text_view)?.text = uri.toString()
        }
    }

    //</editor-fold desc="根据不同的类型, 填充不同的布局">

    //<editor-fold desc="生命周期操作">

    override fun onFragmentShow(bundle: Bundle?) {
        super.onFragmentShow(bundle)
        _tbsWebView?.resumeTimers()
    }

    override fun onFragmentHide() {
        super.onFragmentHide()
        _tbsWebView?.pauseTimers()
        _dslVideoItem?.itemViewDetachedToWindow?.invoke(_dslVideoHolder!!, 0)
        _dslSubSamplingItem?.itemViewDetachedToWindow?.invoke(_dslVideoHolder!!, 0)
    }

    override fun onDestroy() {
        super.onDestroy()
        _tbsWebView?.destroy()
        _tbsReaderView?.onStop()
        _dslVideoItem?.itemViewRecycled?.invoke(_dslVideoHolder!!, 0)
        _dslSubSamplingItem?.itemViewRecycled?.invoke(_dslVideoHolder!!, 0)

        //TbsFileInterfaceImpl.getInstance().closeFileReader()
        QbSdk.closeFileReader(fContext())
        QbSdk.clear(fContext())
    }

    override fun onBackPressed(): Boolean {
        val webView = _tbsWebView
        if (webView != null && webView.canGoBack()) {
            webView.goBack()
            checkCloseView()
            return false
        }
        return true
    }

    //</editor-fold desc="生命周期操作">

    //<editor-fold desc="其他操作">

    /**动态判断是否要显示强制关闭按钮*/
    open fun checkCloseView() {
        val webView = _tbsWebView
        if (webView?.canGoBack() == true) {
            leftControl()?.run {
                visible(R.id.lib_close_view)
            }
        } else {
            leftControl()?.run {
                gone(R.id.lib_close_view)
            }
        }
    }

    open fun close() {
        dslFHelper {
            remove(this@TbsWebFragment)
        }
    }

    fun showLoadingView(tip: CharSequence? = null) {
        if (webConfig.showLoading) {
            _vh.visible(R.id.lib_arc_loading_view)
            _vh.visible(R.id.lib_tip_view, tip != null)
        }
        _vh.tv(R.id.lib_tip_view)?.text = tip
    }

    fun hideLoadingView() {
        _vh.gone(R.id.lib_arc_loading_view)
        _vh.gone(R.id.lib_tip_view)
    }

    //</editor-fold desc="其他操作">

}