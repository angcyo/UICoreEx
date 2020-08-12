package com.angcyo.bmob.bean

import cn.bmob.v3.BmobObject
import cn.bmob.v3.BmobUser
import com.angcyo.library.app
import com.angcyo.library.utils.Device

/**
 * 继承[BmobUser]的表, 会覆盖Bmob默认的[_User]表
 * Email:angcyo@126.com
 * @author angcyo
 * @date 2020/02/08
 */

data class UserInfoBmob(
    var userId: Long? = null,
    var state: Int? = 1, // 状态小于0,被禁用
    var type: Int? = 1, //类型大于10 超级权限
    var username: String? = null,
    var likeName: String? = null,
    var showName: String? = null,
    var password: String? = null,
    var psuedoID: String? = Device.deviceId,
    var androidId: String? = Device.androidId,
    var packageName: String? = app().packageName,
    var appInfo: String? = null,
    var json: String? = null
) : BmobObject() /*: BmobUser()*/ //不能继承BmobUser, update 操作是失败

fun UserInfoBmob.showName(): String = showName ?: likeName ?: username ?: ""