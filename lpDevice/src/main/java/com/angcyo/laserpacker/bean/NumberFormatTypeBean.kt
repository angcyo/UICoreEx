package com.angcyo.laserpacker.bean

import com.angcyo.library.extend.IToText

/**
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2023/09/05
 */
data class NumberFormatTypeBean(
    /**
     * [com.angcyo.laserpacker.bean.LPVariableBean.NUMBER_TYPE_DEC]
     * [com.angcyo.laserpacker.bean.LPVariableBean.NUMBER_TYPE_HEX]
     * [com.angcyo.laserpacker.bean.LPVariableBean.NUMBER_TYPE_HEX_LOWER]
     * */
    val formatType: String,

    /**显示在界面上的文本*/
    val label: String
) : IToText {
    
    override fun toText(): CharSequence? = label

}
