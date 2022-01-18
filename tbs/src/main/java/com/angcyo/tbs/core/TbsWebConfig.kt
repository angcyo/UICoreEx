package com.angcyo.tbs.core

import android.net.Uri
import android.os.Parcelable
import com.angcyo.tbs.DslTbs
import kotlinx.android.parcel.Parcelize

/**
 * 启动[TbsWebActivity]附带的参数信息
 * Email:angcyo@126.com
 * @author angcyo
 * @date 2020/03/01
 */

@Parcelize
data class TbsWebConfig(
    /**需要打开的网页, 或者本地文件路径, 也可以通过Intent.setData()设置*/
    var uri: Uri? = null,
    /**需要直接加载的数据
     * [com.angcyo.tbs.core.inner.TbsWebView.loadDataWithBaseURL2]*/
    var data: String? = null,
    var mimeType: String? = null, /*当无法从uri中获取mimeType时, 使用此mimeType*/
    var targetClass: Class<out TbsWebFragment> = DslTbs.DEF_TBS_FRAGMENT
        ?: TbsWebFragment::class.java,
    /**指定界面的标题*/
    var title: CharSequence? = null,
    /**是否需要显示右边的菜单*/
    var showRightMenu: Boolean = true,
    /**是否需要loading提示, 包括顶部和中间*/
    var showLoading: Boolean = true,
    var enableTitleBarHideBehavior: Boolean = true,
) : Parcelable