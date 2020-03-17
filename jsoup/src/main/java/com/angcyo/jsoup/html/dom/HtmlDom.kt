package com.angcyo.jsoup.html.dom

/**
 *
 * Email:angcyo@126.com
 * @author angcyo
 * @date 2020/03/17
 */
data class HtmlDom(
    /**目标网页*/
    var htmlUrl: String? = null,

    /**目标网页所有分类的数据*/
    var htmlCategoryList: List<HtmlCategory>? = null
)