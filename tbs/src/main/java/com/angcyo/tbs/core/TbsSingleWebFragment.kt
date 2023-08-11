package com.angcyo.tbs.core

import com.angcyo.web.R

/**
 * 纯净的TBS内核, 单页面, 无菜单, 无进度条, 无手势
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2023/08/11
 */
open class TbsSingleWebFragment : TbsWebFragment() {

    init {
        fragmentLayoutId = R.layout.layout_web_content
        enableSoftInput = true
    }

    override fun canSwipeBack(): Boolean {
        return false
    }

    override fun canFlingBack(): Boolean {
        return super.canFlingBack()
    }

}