package com.angcyo.jsoup.html

import com.angcyo.jsoup.html.css.HtmlElementSelect

/**
 * 获取一组元素
 * Email:angcyo@126.com
 * @author angcyo
 * @date 2020/03/16
 * Copyright (c) 2019 ShenZhen O&M Cloud Co., Ltd. All rights reserved.
 */
data class HtmlCategory(
    /**目标网页*/
    var htmlUrl: String? = null,

    /**分类的名称*/
    var categoryName: String? = null,

    /**目标[Elements]选择器, 属性从这个元素中获取. 支持使用[CSS_SPILT]分割多个select*/
    var elementsCss: String? = null,

    /**数据结构, 各个属性的css选择器*/
    var htmlElementSelect: HtmlElementSelect? = null,

    /**数据结构解析后的数据集合*/
    var elements: MutableList<HtmlElement>? = null
)