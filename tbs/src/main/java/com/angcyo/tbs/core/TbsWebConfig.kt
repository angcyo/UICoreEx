package com.angcyo.tbs.core

import android.net.Uri
import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

/**
 * 启动[TbsWebActivity]附带的参数信息
 * Email:angcyo@126.com
 * @author angcyo
 * @date 2020/03/01
 */

@Parcelize
data class TbsWebConfig(
    /*需要打开的网页, 或者本地文件路径, 也可以通过Intent.setData()设置*/
    var uri: Uri? = null,
    var mimeType: String? = null, /*当无法从uri中获取mimeType时, 使用此mimeType*/
    var targetClass: Class<out TbsWebFragment> = TbsWebFragment::class.java,
    /**指定界面的标题*/
    var title: CharSequence? = null,
    /**是否需要显示右边的菜单*/
    var showRightMenu: Boolean = true
) : Parcelable