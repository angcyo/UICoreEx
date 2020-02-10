package com.angcyo.bmob.bean

import cn.bmob.v3.BmobUser

/**
 *
 * Email:angcyo@126.com
 * @author angcyo
 * @date 2020/02/08
 */

data class UserBmob(
    var userId: Long? = null,
    var likeName: String? = null
) : BmobUser()

fun UserBmob.showName(): String = likeName ?: username ?: email ?: ""