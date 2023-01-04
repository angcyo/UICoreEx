package com.angcyo.engrave.data

import com.angcyo.library.extend.IToText
import com.angcyo.library.getAppString

/**
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2023/01/04
 */
data class ZModel(val resKey: String) : IToText {
    override fun toText(): CharSequence? = getAppString(resKey)
}
