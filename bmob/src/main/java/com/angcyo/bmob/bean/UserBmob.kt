package com.angcyo.bmob.bean

import cn.bmob.v3.BmobUser
import com.angcyo.library.utils.Device

/**
 * 继承[BmobUser]的表, 会覆盖Bmob默认的[_User]表
 * Email:angcyo@126.com
 * @author angcyo
 * @date 2020/02/08
 */

data class UserBmob(
    var userId: Long? = null,
    var state: Int? = 1, // 状态小于0,被禁用
    var type: Int? = 1, //类型大于10 超级权限
    var likeName: String? = null,
    var showName: String? = null,
    var password2: String? = null,//明文密码
    var psuedoID: String? = Device.deviceId,
    var androidId: String? = Device.androidId,
    var json: String? = null
) : BmobUser()

fun UserBmob.showName(): String = likeName ?: showName ?: username ?: email ?: ""