package com.angcyo.engrave.auto

import com.angcyo.server.file.FileWebConfig

/**
 * 自动雕刻
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2022/10/22
 */
object AutoEngraveHelper {

    /**初始化*/
    fun init() {
        FileWebConfig.fileWebsiteList.add(AutoEngraveWebsite())
    }

}