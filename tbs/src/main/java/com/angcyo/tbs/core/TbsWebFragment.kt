package com.angcyo.tbs.core

import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.angcyo.base.dslFHelper
import com.angcyo.core.fragment.BaseTitleFragment
import com.angcyo.dialog.configBottomDialog
import com.angcyo.dialog.dslDialog
import com.angcyo.download.download
import com.angcyo.download.downloadNotify
import com.angcyo.library.ex.*
import com.angcyo.library.toastQQ
import com.angcyo.tablayout.screenWidth
import com.angcyo.tbs.R
import com.angcyo.tbs.core.inner.TbsWeb
import com.angcyo.tbs.core.inner.TbsWebView
import com.angcyo.widget.bar
import com.angcyo.widget.base.find
import com.angcyo.widget.base.getDrawable
import com.angcyo.widget.base.setSingleLineMode
import com.angcyo.widget.base.setWidth
import com.angcyo.widget.span.span

/**
 * file:///android_asset/webpage/fileChooser.html
 * Email:angcyo@126.com
 * @author angcyo
 * @date 2020/03/01
 */
open class TbsWebFragment : BaseTitleFragment() {
    companion object {
        const val KEY_CONFIG = "key_config"
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
                            getDrawable(com.angcyo.core.R.drawable.lib_back)
                                .colorFilter(fragmentConfig.titleItemIconColor)
                    }
                }
            }
        }
    }

    var webConfig = TbsWebConfig()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        arguments?.getParcelable<TbsWebConfig>(TbsWebFragment.KEY_CONFIG)?.run {
            webConfig = this
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        _vh.tv(R.id.lib_title_text_view)?.run {
            setWidth(width = screenWidth - 160 * dpi)
            setSingleLineMode()
        }

        //有些网页, 无法回退. 添加强制关闭按钮
        appendLeftItem(ico = R.drawable.tbs_ic_close, action = {
            id = R.id.lib_close_view
            visibility = View.GONE
        }) {
            close()
        }

        val uri = webConfig.uri

        if (uri == null) {
            toastQQ("数据异常")
        } else {
            if (uri.isHttpScheme()) {
                //打开网页
                val url = uri.toString()
                _vh.group(R.id.tbs_wrap_layout)?.run {
                    attachTbsWebView(this, url)
                }
            } else {
                //其他类型
            }
        }
    }

    //<editor-fold desc="根据不同的类型, 填充不同的布局">

    open fun attachTbsWebView(parent: ViewGroup, url: String) {
        parent.addView(TbsWebView(fContext()).apply {
            id = R.id.tbs_web_view

            //标题
            onReceivedTitle = {
                fragmentTitle = it
            }

            //进度
            onProgressChanged = { url, progress ->
                // L.d("$url $progress")
                _vh.bar(R.id.lib_progress_bar)?.setProgress(progress)
                //加载框

                if (progress == 0) {
                    if (fragmentTitle.isNullOrEmpty()) {
                        fragmentTitle = "加载中..."
                    }
                }

                checkCloseView()

                if (progress >= 80) {
                    _vh.gone(R.id.lib_arc_loading_view)
                }
            }

            //下载
            onDownloadListener = { url, userAgent, contentDisposition, mime, length ->
                fContext().dslDialog {
                    configBottomDialog()
                    dialogLayoutId = R.layout.dialog_tbs_file_download
                    onInitListener = { dialog, dialogViewHolder ->
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

            //打开其他应用

            loadUrl(url)
        }, -1, -1)
    }


    //</editor-fold desc="根据不同的类型, 填充不同的布局">

    override fun onFragmentShow(bundle: Bundle?) {
        super.onFragmentShow(bundle)
    }

    override fun onBackPressed(): Boolean {
        val webView = _vh.v<TbsWebView>(R.id.tbs_web_view)
        if (webView != null && webView.canGoBack()) {
            webView.goBack()
            checkCloseView()
            return false
        }
        return true
    }

    /**动态判断是否要显示强制关闭按钮*/
    open fun checkCloseView() {
        val webView = _vh.v<TbsWebView>(R.id.tbs_web_view)
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
}