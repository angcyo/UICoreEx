package com.angcyo.bmob.api

import cn.bmob.v3.BmobSMS
import cn.bmob.v3.BmobUser
import cn.bmob.v3.datatype.BmobFile
import cn.bmob.v3.exception.BmobException
import cn.bmob.v3.listener.QueryListener
import cn.bmob.v3.listener.UpdateListener
import cn.bmob.v3.listener.UploadFileListener
import java.io.File


/**
 * Bmob的异步回调, 都在子线程处理. 请注意更新UI操作.
 *
 * http://doc.bmob.cn/other/error_code/#restapi
 *
 * Email:angcyo@126.com
 * @author angcyo
 * @date 2021/01/19
 */

fun isBmobLogin() = BmobUser.isLogin()

inline fun <reified T : BmobUser> getBmobCurrentUser(): T? =
    BmobUser.getCurrentUser(T::class.java)

/**验证短信验证码*/
fun bmobVerifySmsCode(
    phoneNumber: String,
    smsCode: String,
    verifySmsCodeListener: UpdateListener
) {
    BmobSMS.verifySmsCode(phoneNumber, smsCode, verifySmsCodeListener)
}

/**发送验证短信，如果使用默认模板，则设置template为空字符串或不设置*/
fun bmobSendSms(
    phoneNumber: String,
    template: String,
    sendSmsCodeListener: QueryListener<Int>
) {
    BmobSMS.requestSMSCode(phoneNumber, template, sendSmsCodeListener)
}

/**发送验证用户邮箱的邮件*/
fun bmobSendEmailForVerifyUserEmail(email: String, end: (ex: BmobException?) -> Unit) {
    BmobUser.requestEmailVerify(email, object : UpdateListener() {
        override fun done(ex: BmobException?) {
            end(ex)
        }
    })
}

/**发送重置密码的邮件*/
fun bmobSendEmailForResetPassword(email: String, end: (ex: BmobException?) -> Unit) {
    BmobUser.resetPasswordByEmail(email, object : UpdateListener() {
        override fun done(ex: BmobException?) {
            end(ex)
        }
    })
}

/**使用短信验证码重置密码*/
fun bmobResetPasswordBySmsCode(
    smsCode: String,
    newPassword: String,
    end: (ex: BmobException?) -> Unit
) {
    BmobUser.resetPasswordBySMSCode(smsCode, newPassword, object : UpdateListener() {
        override fun done(ex: BmobException?) {
            end(ex)
        }
    })
}

///**使用旧密码重置密码，需要用户登录*/
//inline fun <reified T : BmobUser> bmobResetPasswordByOldPassword(
//    oldPassword: String,
//    newPassword: String,
//    crossinline end: (msg: String?, ex: BmobException?) -> Unit
//) {
//    if (!isBmobLogin()) {
//        end(null, BmobException("请先登录"))
//        return
//    }
//    getBmobCurrentUser<T>()?.pass(
//        oldPassword,
//        newPassword,
//        object : ResetPasswordListener() {
//            override fun onFailure(ex: BmobException?) {
//                end(null, ex)
//            }
//
//            override fun onSuccess(msg: String?) {
//                end(msg, null)
//            }
//        })
//}

/**上传文件*/
fun bmobUploadFile(file: File, uploadListener: UploadFileListener) {
    val bmobFile = BmobFile(file)
    bmobFile.upload(uploadListener)
}

fun bmobDeleteFile(file: File, deleteFileListener: UpdateListener) {
    val bmobFile = BmobFile(file)
    bmobFile.delete(deleteFileListener)
}