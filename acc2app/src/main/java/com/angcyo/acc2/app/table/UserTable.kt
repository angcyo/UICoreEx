package com.angcyo.acc2.app.table

/**
 * Email:angcyo@126.com
 * @author angcyo
 * @date 2021/01/19
 */
class UserTable {

    var username: String? = null

    var password: String? = null

    /**密码备份*/
    var passwordBackup: String? = null

    /**用于类型, 1:普通用于 10:高级用户, 负数:用户被禁用*/
    var type: Int = 1

    /**被禁用时的消息*/
    var typeMessage: String? = null
}