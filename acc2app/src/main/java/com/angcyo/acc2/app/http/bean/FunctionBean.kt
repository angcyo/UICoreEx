package com.angcyo.acc2.app.http.bean

import com.angcyo.acc2.app.model.GiteeModel
import com.angcyo.core.vmApp

/**
 *
 * Email:angcyo@126.com
 * @author angcyo
 * @date 2021/01/29
 * Copyright (c) 2020 ShenZhen Wayto Ltd. All rights reserved.
 */
data class FunctionBean(
    var type: Int = 0,
    var title: String? = null,
    var des: String? = null,
    /**需要激活的[ActionBean]id, 多个用;号隔开*/
    var actions: String? = null,
    /**激活*/
    var enable: Boolean = true,
    /**随机激活, 不管用户有没有选中. 只做提示使用,功能控制在json中*/
    var random: Boolean = false,
    /**是否需要必选*/
    var force: Boolean = false
) {
    companion object {
        //功能点, 完播
        const val FUNCTION_AFTER_PLAY = 0x01

        //功能点, 收藏
        const val FUNCTION_COLLECT = 0x02

        //功能点, 点赞
        const val FUNCTION_LIKE = 0x04

        //功能点, 关注
        const val FUNCTION_ATTENTION = 0x08

        //转发/分享
        const val FUNCTION_SHARE = 0x10

        //评论视频
        const val FUNCTION_COMMENT = 0x20

        //私信
        const val FUNCTION_MESSAGE = 0x40

        /**根据[type]查找对应的[FunctionBean]*/
        fun findFunBean(type: String?): FunctionBean? {
            if (type.isNullOrEmpty()) {
                return null
            }
            val _type = type.toIntOrNull() ?: return null
            return vmApp<GiteeModel>().allFunctionData.value?.find {
                it.type == _type
            }
        }
    }
}