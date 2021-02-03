package com.angcyo.bmob

import android.content.Context
import cn.bmob.v3.Bmob
import cn.bmob.v3.BmobConfig
import cn.bmob.v3.BmobUser
import cn.bmob.v3.exception.BmobException
import cn.bmob.v3.listener.LogInListener
import com.angcyo.library.L
import io.reactivex.disposables.Disposable
import io.reactivex.disposables.Disposables

/**
 * http://doc.bmob.cn/data/android/
 *
 * 由于Bmob Sdk设计的, 所有二次封装的Listener都会崩溃, 所以封装只能用于参考, 不能用于实际操作.
 *
 * 错误码
 * http://doc.bmob.cn/other/error_code/#androidsdk
 * Email:angcyo@126.com
 * @author angcyo
 * @date 2020/02/07
 */
object DslBmob {
    var context: Context? = null
    var appId: String = ""

    /**初始化*/
    fun initBmob(context: Context, appId: String) {
        this.context = context.applicationContext
        this.appId = appId

        //第二：自v3.4.7版本开始,设置BmobConfig,允许设置请求超时时间、文件分片上传时每片的大小、文件的过期时间(单位为秒)，
        val config: BmobConfig = BmobConfig.Builder(context)
            //设置appkey
            .setApplicationId(appId)
            //请求超时时间（单位为秒）：默认15s
            .setConnectTimeout(10)
            //文件分片上传时每片的大小（单位字节），默认512*1024
            .setUploadBlockSize(1024 * 1024)
            //文件的过期时间(单位为秒)：默认1800s
            .setFileExpiration(2500)
            .build()
        Bmob.initialize(config)
    }

    /**突出登录*/
    fun logout() {
        BmobUser.logOut()
    }

    /**获取当前已经登录的用户信息*/
    inline fun <reified User : BmobUser> loginUser(): User? {
        var user: User? = null
        if (BmobUser.isLogin()) {
            user = BmobUser.getCurrentUser(User::class.java)
        }
        return user
    }

    /**登录, 用BmobUser子类会崩溃*/
    private fun login(
        username: String,
        password: String,
        onResult: (result: BmobUser?, error: BmobException?) -> Unit = { result, error ->
            L.d("$result $error")
        }
    ): Disposable {
        return BmobUser.loginByAccount(username, password, object : LogInListener<BmobUser>() {
            override fun done(result: BmobUser?, e: BmobException?) {
                onResult(result, e)
            }
        })
    }

    /** 同步控制台数据到缓存中 */
    private fun fetchUserInfo(
        onResult: (result: BmobUser?, error: BmobException?) -> Unit = { result, error ->
            L.d(result, error)
        }
    ) {
        //BmobUser.fetchUserInfo(FetchUserInfoListenerHandler(onResult))
    }

    /**修改密码*/
    private fun updatePassword(
        oldPw: String,
        newPwd: String,
        onResult: (error: BmobException?) -> Unit = { error ->
            L.d(error)
        }
    ): Disposable {
        return Disposables.empty()// BmobUser.updateCurrentUserPassword(oldPw, newPwd, UpdateListenerHandler(onResult))
    }

    /**发送邮箱验证邮件¶*/
    private fun emailVerify(
        email: String,
        onResult: (error: BmobException?) -> Unit = { error ->
            L.d(error)
        }
    ): Disposable {
        return Disposables.empty()//BmobUser.requestEmailVerify(email, UpdateListenerHandler(onResult))
    }

    /** 邮箱重置密码 */
    private fun resetPasswordByEmail(
        email: String,
        onResult: (error: BmobException?) -> Unit = { error ->
            L.d(error)
        }
    ): Disposable {
        return Disposables.empty()//BmobUser.resetPasswordByEmail(email, UpdateListenerHandler(onResult))
    }
}