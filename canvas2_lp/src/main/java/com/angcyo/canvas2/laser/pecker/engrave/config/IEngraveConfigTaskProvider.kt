package com.angcyo.canvas2.laser.pecker.engrave.config

import com.angcyo.laserpacker.bean.LPProjectBean

/**
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2023/09/18
 */
interface IEngraveConfigTaskProvider {

    /**当前配置的任务id*/
    var engraveConfigTaskId: String?

    /**当前配置的项目bean, 如果有*/
    var engraveConfigProjectBean: LPProjectBean?
}