package com.angcyo.acc2.app.http

import com.angcyo.acc2.app.app
import com.angcyo.acc2.app.table.UserTable
import com.angcyo.http.rx.doMain
import com.angcyo.library.ex.killCurrentProcess
import com.angcyo.library.ex.sleep
import com.angcyo.library.utils.RUtils
import kotlin.concurrent.thread

/**
 * Email:angcyo@126.com
 * @author angcyo
 * @date 2021/01/19
 */
object UserHelper {

    var userTable: UserTable? = null

    /**是否是管理员帐号*/
    fun isAdmin() = userTable?.type ?: 0 >= 10

    /**超级管理员*/
    fun isSuperAdmin() = userTable?.type ?: 0 >= 100

    fun login(user: UserTable) {
        update(user)
    }

    /**检查用户是否有效*/
    fun checkUser(user: UserTable): Boolean {
        return if (user.type < 0) {
            doMain {
                logout()
                //alertError(user.typeMessage ?: "无法登录,请联系管理员!")
            }
            false
        } else {
            true
        }
    }

    fun update(user: UserTable) {
        userTable = user
    }

    fun logout() {
        userTable = null
    }

    fun chartQQ() {
        RUtils.chatQQ(app(), app().memoryConfigBean.qq ?: "664738095")
    }

    /**更新用户信息*/
    fun updateUserInfo(end: (user: UserTable?, ex: Exception?) -> Unit) {
        if (userTable == null) {
            end(null, IllegalStateException("请先登录"))
        } else {
            /*userBmob?.let {
                bmobUserInfo<UserBmob>(it.objectId) {
                    userInfoAction = { user, ex ->
                        user?.let {
                            userBmob = user as UserBmob
                            end(userBmob, ex)
                            if (checkUser(user)) {
                                doMain {
                                }
                            }
                        }
                        ex?.let {
                            end(null, ex)
                        }
                    }
                }
            }*/
        }
    }

    /**强制退出*/
    fun exit() {
        logout()
        thread {
            sleep(3_000) {
                killCurrentProcess()
            }
        }
    }
}