package com.angcyo.jsoup.html.css

/**
 * 获取[Element]的属性css选择器
 * Email:angcyo@126.com
 * @author angcyo
 * @date 2020/03/16
 * Copyright (c) 2019 ShenZhen O&M Cloud Co., Ltd. All rights reserved.
 */

data class AttrSelect(
    /**在[Elements]内, 属性直接挂在的[Element]. 支持[CSS_SPILT]*/
    var attrTargetCss: String? = null,
    /**需要读取的属性key, 支持[CSS_SPILT], 有几个特殊属性.*/
    var attrKey: String? = null,
    /**获取到属性后, 进行的正则取值, 取到后终止*/
    var attrPatternList: List<String>? = null,
    /**属性类型, 是否是url属性, 如果是, 会进行[absUrl]操作*/
    var attrType: Int = ATTR_TYPE_NORMAL
) {
    companion object {
        //多个取值key, 分割符. 取到后终止
        const val CSS_SPILT = "|"

        //特殊属性key
        /**[org.jsoup.nodes.Element.text]*/
        const val ATTR_KEY_TEXT = "text" //element 的text()方法返回值

        /**[org.jsoup.nodes.Element.html]*/
        const val ATTR_KEY_HTML = "html" //html()方法返回值

        /**[org.jsoup.nodes.Element.outerHtml]*/
        const val ATTR_KEY_OUT_HTML = "outer_html" //outerHtml()方法返回值

        /**[org.jsoup.nodes.Element.val]*/
        const val ATTR_KEY_OUT_VALUE = "value" //val()方法返回值

        //属性类型

        const val ATTR_TYPE_NORMAL = 0 //正常
        const val ATTR_TYPE_URL = 1//属性url类型
    }
}