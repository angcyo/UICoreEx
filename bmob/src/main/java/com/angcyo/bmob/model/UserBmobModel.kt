package com.angcyo.bmob.model

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import cn.bmob.v3.BmobQuery
import cn.bmob.v3.exception.BmobException
import cn.bmob.v3.listener.FindListener
import com.angcyo.bmob.DslBmob
import com.angcyo.bmob.bean.UserInfoBmob
import com.angcyo.library.L

/**
 *
 * Email:angcyo@126.com
 * @author angcyo
 * @date 2020/02/08
 */

open class UserBmobModel : ViewModel() {

    companion object {
        /**用户id开始的偏移量*/
        const val USER_ID_OFFSET = 100_00L

        val allUserInfo = mutableListOf<UserInfoBmob>()

        /**拉取所有用户*/
        fun fetchAllUser() {
            val query = BmobQuery<UserInfoBmob>()
            query.findObjects(object : FindListener<UserInfoBmob>() {
                override fun done(results: MutableList<UserInfoBmob>?, error: BmobException?) {
                    results?.run {
                        this.forEach {
                            updateUserInfo(it)
                        }
                    }
                }
            })
        }

        /**拉取指定用户信息*/
        fun fetchUser(userId: Long) {
            val query = BmobQuery<UserInfoBmob>()
            query.addWhereEqualTo("userId", userId)
            query.findObjects(object : FindListener<UserInfoBmob>() {
                override fun done(results: MutableList<UserInfoBmob>?, error: BmobException?) {
                    results?.run {
                        this.forEach {
                            updateUserInfo(it)
                        }
                    }
                }
            })
        }

        /**更新内存缓存用户的信息*/
        fun updateUserInfo(info: UserInfoBmob) {
            var find = false

            for (i in allUserInfo.indices) {
                val user = allUserInfo[i]
                if (user.userId == info.userId) {
                    find = true

                    allUserInfo[i] = info
                }
            }

            if (!find) {
                allUserInfo.add(info)
            }
        }

        /**返回已经存在的用户信息, 并更新*/
        fun fetchAndGetUserBmob(userId: Long): UserInfoBmob? {
            var result: UserInfoBmob? = null

            for (i in allUserInfo.indices) {
                val user = allUserInfo[i]
                if (user.userId == userId) {
                    result = user
                }
            }
            fetchUser(userId)
            return result
        }
    }

    //登录的用户信息
    val userBmob = MutableLiveData<UserInfoBmob>()

    override fun onCleared() {
        super.onCleared()
        L.i("UserModel is onCleared!")
    }

    /**登出*/
    open fun logout() {
        userBmob.value = null
        DslBmob.logout()
        onLogout()
    }

    /**登录成功*/
    open fun onLoginSucceed(user: UserInfoBmob) {

    }

    /**登出*/
    open fun onLogout() {

    }

//    /**登录*/
//    fun login(
//        username: String,
//        password: String,
//        onResult: (result: UserBmob?, error: BmobException?) -> Unit = { result, error ->
//            L.d("$result $error")
//        }
//    ): Disposable {
//        val user = UserBmob()
//        user.username = username
//        user.setPassword(password)
//
//        return user.login(object : SaveListener<UserBmob>() {
//            override fun done(result: UserBmob?, error: BmobException?) {
//                if (error == null) {
//                    //登录成功
//                    userBmob.value = result
//                    hideLoading("欢迎:${result?.showName()}")
//                    onLoginSucceed(result!!)
//                } else {
//                    hideLoading(error.message ?: "error")
//                }
//                onResult(result, error)
//            }
//        })
//    }
//
//    /**注册*/
//    fun register(
//        username: String,
//        password: String,
//        login: Boolean = true,
//        onResult: (result: UserBmob?, error: BmobException?) -> Unit = { result, error ->
//            L.d("$result $error")
//        }
//    ): Disposable {
//        val user = UserBmob()
//        user.username = username
//        user.setPassword(password)
//
//        val bmobQuery = BmobQuery<UserBmob>()
//        return bmobQuery.count(UserBmob::class.java, object : CountListener() {
//            override fun done(result: Int?, error: BmobException?) {
//                if (error == null) {
//                    user.userId = USER_ID_OFFSET + (result ?: 0) + 1
//                    user.signUp(object : SaveListener<UserBmob>() {
//                        override fun done(result: UserBmob?, error: BmobException?) {
//                            if (error == null) {
//                                //注册成功
//                                if (login) {
//                                    userBmob.value = result
//                                    onLoginSucceed(result!!)
//                                }
//                                hideLoading("注册成功")
//                            } else {
//                                hideLoading(error.message ?: "error")
//                            }
//                            onResult(result, error)
//                        }
//                    })
//                } else {
//                    hideLoading(error.message ?: "error")
//                    onResult(null, error)
//                }
//            }
//        })
//    }
}