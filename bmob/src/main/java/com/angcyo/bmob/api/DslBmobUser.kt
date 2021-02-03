package com.angcyo.bmob.api

import cn.bmob.v3.BmobUser
import cn.bmob.v3.exception.BmobException
import cn.bmob.v3.listener.SaveListener
import cn.bmob.v3.listener.UpdateListener
import com.angcyo.library.L

/**
 * Email:angcyo@126.com
 * @author angcyo
 * @date 2021/01/19
 */

class DslBmobUser {

    /**登录回调*/
    var loginAction: ((user: BmobUser?, ex: BmobException?) -> Unit)? = null

    /**注册回调*/
    var signUpAction: ((user: BmobUser?, ex: BmobException?) -> Unit)? = null

    /**用户信息回调*/
    var userInfoAction: ((user: BmobUser?, ex: BmobException?) -> Unit)? = null

    /**更新用户信息*/
    var updateAction: ((ex: BmobException?) -> Unit)? = null
}

/**用户登录*/
inline fun <reified T : BmobUser> bmobLogin(user: T, action: DslBmobUser.() -> Unit) {
    val dslUser = DslBmobUser()
    dslUser.action()
    user.login<T>(object : SaveListener<T>() {

        override fun done(user: T?, ex: BmobException?) {
            dslUser.loginAction?.invoke(user, ex)
        }
    })
}

/**注册*/
fun <T : BmobUser> bmobSignUp(user: T, action: DslBmobUser.() -> Unit) {
    val dslUser = DslBmobUser()
    dslUser.action()
    user.signUp(object : SaveListener<T>() {
        override fun done(user: T?, ex: BmobException?) {
//            user.objectId = objectId
//            user.createdAt = createdAt
            dslUser.loginAction?.invoke(user, ex)
        }
    })
}

/**删除一行数据*/
inline fun <reified T : BmobUser> bmobDeleteUser(crossinline end: (ex: BmobException?) -> Unit) {
    if (!isBmobLogin()) {
        end(BmobException("请先登录"))
        return
    }
    getBmobCurrentUser<T>()?.delete(object : UpdateListener() {
        override fun done(ex: BmobException?) {
            end(ex)
        }
    })
}

///**
// * 通过短信验证码一键登录或注册 如果是新用户，则进行注册；且如果不设置username，则默认username将是手机号码，且默认mobilePhoneVerified将是true。 如果是旧用户，则自动登陆。
// * */
//fun <T : BmobUser> bmobSignUpOrLoginSmsCode(
//    user: T,
//    smsCode: String,
//    action: DslBmobUser.() -> Unit
//) {
//    val dslUser = DslBmobUser()
//    dslUser.action()
//    user.signUpOrLoginSmsCode(smsCode, object : SignUpOrLoginSmsCodeListener<T>() {
//        override fun onFailure(ex: BmobException?) {
//            dslUser.signUpAction?.invoke(null, ex)
//        }
//
//        override fun onSuccess(user: T) {
//            dslUser.signUpAction?.invoke(user, null)
//        }
//    })
//}

/**获取用户信息*/
inline fun <reified T : BmobUser> bmobUserInfo(objectId: String, action: DslBmobUser.() -> Unit) {
    val dslUser = DslBmobUser()
    dslUser.action()
    bmobGet<T>(objectId) {
        getAction = { data, ex ->
            dslUser.userInfoAction?.invoke(data, ex)
        }
    }
}

/**
 * 修改用户信息，需确保objectId不为空，确保用户已经登录。
 * 在更新用户信息时，若用户邮箱有变更并且在管理后台打开了邮箱验证选项，Bmob云后端会自动发送一封验证邮件给用户。
 * BmobUser.getInstance().getCurrentUser(TestUser.class)
 * BmobUser.getInstance().isLogin()
 *
 * 更新完用户信息之后, 请使用[getBmobCurrentUser]重新获取最新的用户信息
 */
fun <T : BmobUser> bmobUpdateUserInfo(user: T, action: DslBmobUser.() -> Unit) {
    if (!isBmobLogin()) {
        L.e("请先登录!")
        return
    }
    val dslUser = DslBmobUser()
    dslUser.action()
    bmobUpdate(user) {
        updateAction = { ex ->
            dslUser.updateAction?.invoke(ex)
        }
    }
}

///**检查登录是否失效*/
//inline fun <reified T : BmobUser> bmobCheckUserSession(crossinline end: (msg: String?, ex: BmobException?) -> Unit) {
//    getBmobCurrentUser<T>()?.checkUserSession(object : CheckUserSessionListener() {
//        override fun onFailure(ex: BmobException?) {
//            end(null, ex)
//        }
//
//        override fun onSuccess(msg: String?) {
//            end(msg, null)
//        }
//    })
//}
//




