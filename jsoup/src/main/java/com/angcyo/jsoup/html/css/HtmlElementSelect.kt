package com.angcyo.jsoup.html.css

/**
 * 套获取数据结构的选择属性选择器
 * Email:angcyo@126.com
 * @author angcyo
 * @date 2020/03/16
 * Copyright (c) 2019 ShenZhen O&M Cloud Co., Ltd. All rights reserved.
 */
data class HtmlElementSelect(
    //元素显示的标题
    var titleSelect: AttrSelect? = null,
    //概要说明
    var summarySelect: AttrSelect? = null,
    //封面图片
    var coverSelect: AttrSelect? = null,
    //点击的目标url
    var targetUrlSelect: AttrSelect? = null
)