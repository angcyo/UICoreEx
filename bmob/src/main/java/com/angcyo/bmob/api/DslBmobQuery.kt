package com.angcyo.bmob.api

import cn.bmob.v3.BmobBatch
import cn.bmob.v3.BmobObject
import cn.bmob.v3.BmobQuery
import cn.bmob.v3.datatype.BatchResult
import cn.bmob.v3.exception.BmobException
import cn.bmob.v3.listener.*
import com.angcyo.http.rx.doBack
import com.angcyo.library.ex.safe
import com.angcyo.library.ex.size
import com.angcyo.library.ex.sync
import com.angcyo.library.model.Page
import io.reactivex.disposables.Disposable


/**
 * http://doc.bmob.cn/data/android/develop_doc/#4
 * Email:angcyo@126.com
 * @author angcyo
 * @date 2021/01/19
 */

class DslBmobQuery<T> : BmobQuery<T>() {

    var getAction: ((data: T?, ex: BmobException?) -> Unit)? = null

    var getsAction: ((dataList: List<T>?, ex: BmobException?) -> Unit)? = null

    var countAction: ((count: Int?, ex: BmobException?) -> Unit)? = null

    var deleteAction: ((ex: BmobException?) -> Unit)? = null

    var updateAction: ((ex: BmobException?) -> Unit)? = null

    var saveAction: ((objectId: String?, ex: BmobException?) -> Unit)? = null

    var batchAction: ((results: List<BatchResult>?, ex: BmobException?) -> Unit)? = null

    /**更新已存在的数据*/
    var existBmobAction: ((existBmobObj: T) -> Unit)? = null

    //http://doc.bmob.cn/data/android/develop_doc/#_24

    /**升序*/
    fun asc(field: String) {
        order(field)
    }

    /**降序*/
    fun desc(field: String) {
        val _field = StringBuilder()
        field.split(",").forEach {
            _field.append("-$it,")
        }
        _field.safe(',')
        order(_field.toString())
    }
}

//<editor-fold desc="查询相关">

/**查询表*/
inline fun <reified T : BmobObject> bmobQuery(
    objectId: String,
    config: DslBmobQuery<T>.() -> Unit
): Disposable {
    val query = DslBmobQuery<T>()
    query.config()
    return query.getObject(objectId, object : QueryListener<T>() {

        override fun done(obj: T?, ex: BmobException?) {
            query.getAction?.invoke(obj, ex)
        }
    })
}

inline fun <reified T : BmobObject> bmobGet(
    objectId: String,
    config: DslBmobQuery<T>.() -> Unit
): Disposable {
    return bmobQuery(objectId, config)
}

inline fun <reified T : BmobObject> bmobQueryList(
    page: Page? = null,
    config: DslBmobQuery<T>.() -> Unit
): Disposable {
    val query = DslBmobQuery<T>()
    page?.let {
        query.setLimit(it.requestPageSize)
        query.setSkip((it.requestPageIndex - 1) * it.requestPageSize)
    }
    query.config()
    return query.findObjects(object : FindListener<T>() {

        override fun done(list: MutableList<T>?, ex: BmobException?) {
            query.getsAction?.invoke(list, ex)
        }
    })
}

/**拉表中的所有数据*/
inline fun <reified T : BmobObject> bmobQueryAll(config: DslBmobQuery<T>.() -> Unit) {
    val query = DslBmobQuery<T>()
    query.config()
    val result = mutableListOf<T>()
    val page = Page()

    doBack {
        while (true) {
            val syncResult = sync<Throwable> { countDownLatch, atomicReference ->
                bmobQueryList<T>(page) {
                    getsAction = { dataList, ex ->
                        dataList?.let {
                            result.addAll(it)
                            if (it.size() < page.requestPageSize) {
                                //请求结束了
                                atomicReference.set(InterruptedException("finsih"))
                            }
                        }
                        ex?.let {
                            query.getsAction?.invoke(result, it)
                            atomicReference.set(it)
                        }
                        countDownLatch.countDown()
                    }
                }
            }
            //是否继续
            if (syncResult == null) {
                //继续
                page.pageLoadEnd()
                page.pageLoadMore()
            } else {
                if (syncResult is InterruptedException) {
                    //完成
                    query.getsAction?.invoke(result, null)
                } else {
                    //异常
                }
                break
            }
        }
    }
}

inline fun <reified T : BmobObject> bmobGets(
    page: Page? = null,
    config: DslBmobQuery<T>.() -> Unit
): Disposable {
    return bmobQueryList(page, config)
}

inline fun <reified T : BmobObject> bmobGetAll(
    config: DslBmobQuery<T>.() -> Unit
) {
    bmobQueryAll<T>(config)
}

/**查询数量*/
inline fun <reified T : BmobObject> bmobCount(config: DslBmobQuery<T>.() -> Unit): Disposable {
    val query = DslBmobQuery<T>()
    query.config()
    return query.count(T::class.java, object : CountListener() {
        override fun done(count: Int?, ex: BmobException?) {
            query.countAction?.invoke(count, ex)
        }
    })
}

//</editor-fold desc="查询相关">

//<editor-fold desc="删除相关">

fun <T : BmobObject> bmobDelete(bmobObj: T, config: DslBmobQuery<T>.() -> Unit): Disposable {
    val query = DslBmobQuery<T>()
    query.config()
    return bmobObj.delete(object : UpdateListener() {
        override fun done(ex: BmobException?) {
            query.deleteAction?.invoke(ex)
        }
    })
}

//</editor-fold desc="删除相关">

//<editor-fold desc="更新相关">

/**更新一行数据*/
fun <T : BmobObject> bmobUpdate(bmobObj: T, config: DslBmobQuery<T>.() -> Unit): Disposable {
    val query = DslBmobQuery<T>()
    query.config()
//    val oldObjectId = bmobObj.objectId
//    val oldCreateAt = bmobObj.createdAt
//    val oldAcl = bmobObj.acl
    return bmobObj.update(object : UpdateListener() {

        override fun done(ex: BmobException?) {
//            bmobObj.objectId = oldObjectId
//            bmobObj.createdAt = oldCreateAt
//            bmobObj.acl = oldAcl
//            bmobObj.updatedAt = updatedAt
            query.updateAction?.invoke(ex)
        }
    })
}

//</editor-fold desc="更新相关">

//<editor-fold desc="保存相关">

/**保存一行数据*/
fun <T : BmobObject> bmobSave(bmobObj: T, config: DslBmobQuery<T>.() -> Unit): Disposable {
    val query = DslBmobQuery<T>()
    query.config()
    return bmobObj.save(object : SaveListener<String>() {

        override fun done(objectId: String?, ex: BmobException?) {
            query.saveAction?.invoke(objectId, ex)
        }
    })
}

/**更新或者保存一行数据
 * [updateAction] 回调*/
inline fun <reified T : BmobObject> bmobUpdateOrSave(
    bmobObj: T,
    config: DslBmobQuery<T>.() -> Unit
): Disposable {
    val query = DslBmobQuery<T>()
    query.config()

    return bmobGets<T> {
        config()
        getsAction = { dataList, ex ->
            if (dataList.isNullOrEmpty() || ex?.errorCode == 101) {
                //查询的 对象或Class 不存在 或者 登录接口的用户名或密码不正确
                //没找到, 保存对象
                bmobSave(bmobObj) {
                    saveAction = { objectId, ex ->
                        query.updateAction?.invoke(ex)
                    }
                }
            } else if (ex == null) {
                //找到了, 更新对象
                val existBmobObj = dataList.first()
                bmobObj.objectId = existBmobObj.objectId

                //已存在的对象
                query.existBmobAction?.invoke(existBmobObj)

                bmobUpdate(bmobObj) {
                    updateAction = { ex ->
                        query.updateAction?.invoke(ex)
                    }
                }
            } else {
                query.updateAction?.invoke(ex)
            }
        }
    }
}

//</editor-fold desc="保存相关">

//<editor-fold desc="批量操作">

/**http://doc.bmob.cn/data/android/develop_doc/#161*/
fun <T : BmobObject> bmobBatchSave(list: List<T>, config: DslBmobQuery<T>.() -> Unit): Disposable {
    val query = DslBmobQuery<T>()
    query.config()

    return BmobBatch().insertBatch(list).doBatch(object : QueryListListener<BatchResult>() {
        override fun done(results: List<BatchResult>?, ex: BmobException?) {
            query.batchAction?.invoke(results, ex)
            /*for (i in 0 until results.size()) {
                val result: BatchResult = results!![i]
                val error = result.error
                if (error == null) {
                    //第几个成功
                } else {
                    //第几个失败
                }
            }*/
        }
    })
}

/**需要对象有objectId*/
fun <T : BmobObject> bmobBatchUpdate(
    list: List<T>,
    config: DslBmobQuery<T>.() -> Unit
): Disposable {
    val query = DslBmobQuery<T>()
    query.config()

    return BmobBatch().updateBatch(list).doBatch(object : QueryListListener<BatchResult>() {
        override fun done(results: List<BatchResult>?, ex: BmobException?) {
            query.batchAction?.invoke(results, ex)
        }
    })
}

/**需要对象有objectId*/
fun <T : BmobObject> bmobBatchDelete(
    list: List<T>,
    config: DslBmobQuery<T>.() -> Unit
): Disposable {
    val query = DslBmobQuery<T>()
    query.config()

    return BmobBatch().deleteBatch(list).doBatch(object : QueryListListener<BatchResult>() {
        override fun done(results: List<BatchResult>?, ex: BmobException?) {
            query.batchAction?.invoke(results, ex)
        }
    })
}

/**混合批量*/
fun bmobBatch(
    config: BmobBatch.() -> Unit,
    end: (results: List<BatchResult>?, ex: BmobException?) -> Unit
): Disposable {
    val bmobBatch = BmobBatch().apply(config)

    //bmobBatch.insertBatch()
    //bmobBatch.updateBatch()
    //bmobBatch.deleteBatch()

    return bmobBatch.doBatch(object : QueryListListener<BatchResult>() {
        override fun done(results: List<BatchResult>?, ex: BmobException?) {
            end.invoke(results, ex)
        }
    })
}

//</editor-fold desc="批量操作">
