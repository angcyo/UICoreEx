package com.angcyo.bmob.model

import androidx.lifecycle.ViewModel
import cn.bmob.v3.BmobQuery
import cn.bmob.v3.exception.BmobException
import cn.bmob.v3.listener.FindListener
import cn.bmob.v3.listener.SaveListener
import cn.bmob.v3.listener.UpdateListener
import com.angcyo.bmob.bean.DeviceBmob
import com.angcyo.library.L
import io.reactivex.disposables.Disposable

/**
 *
 * Email:angcyo@126.com
 * @author angcyo
 * @date 2020/02/09
 */
open class DeviceBmobModel : ViewModel() {

    /**更新或者创建数据*/
    fun updateOrCreate(userId: String? = null): Disposable {
        val bmob = DeviceBmob.get(userId)
        val query = BmobQuery<DeviceBmob>()
        query.addWhereEqualTo("psuedoID", bmob.psuedoID)
        return query.findObjects(object : FindListener<DeviceBmob>() {
            override fun done(results: MutableList<DeviceBmob>?, error: BmobException?) {
                if (error == null) {
                    if (results.isNullOrEmpty()) {
                        //不存在旧的
                        bmob.save(object : SaveListener<String>() {
                            override fun done(result: String?, error: BmobException?) {
                                L.d("$result $error")
                            }
                        })
                    } else {
                        //更新
                        bmob.update(
                            results.firstOrNull()?.objectId,
                            object : UpdateListener() {
                                override fun done(error: BmobException?) {
                                    L.d(" $error")
                                }
                            })
                    }
                } else {
                    //错误.
                    L.d("$results $error")
                }
            }

        })
    }
}