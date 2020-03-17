package com.angcyo.jsoup.html.dom

/**
 * 要获取的html选择结构Bean
 * Email:angcyo@126.com
 * @author angcyo
 * @date 2020/03/16
 * Copyright (c) 2019 ShenZhen O&M Cloud Co., Ltd. All rights reserved.
 */
data class HtmlElement(
    var title: String? = null,
    var summary: String? = null,
    var cover: String? = null,
    var targetUrl: String? = null
)