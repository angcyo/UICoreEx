package com.angcyo.canvas2.laser.pecker.engrave.dslitem

import com.angcyo.item.DslLineItem
import com.angcyo.library.ex.dpi

/**
 * 分割线
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2022/09/27
 */
class EngraveDividerItem : DslLineItem() {
    init {
        itemHeight = 30 * dpi
    }
}