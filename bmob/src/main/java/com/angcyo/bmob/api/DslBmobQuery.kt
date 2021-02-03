package com.angcyo.bmob.api

import cn.bmob.v3.BmobObject
import cn.bmob.v3.BmobQuery
import cn.bmob.v3.exception.BmobException
import cn.bmob.v3.listener.*


/**
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

    var saveAction: ((data: T?, ex: BmobException?) -> Unit)? = null
}

//<editor-fold desc="查询相关">

/**查询表*/
inline fun <reified T : BmobObject> bmobQuery(
    objectId: String,
    config: DslBmobQuery<T>.() -> Unit
) {
    val query = DslBmobQuery<T>()
    query.config()
    query.getObject(objectId, object : QueryListener<T>() {

        override fun done(obj: T?, ex: BmobException?) {
            query.getAction?.invoke(obj, ex)
        }
    })
}

inline fun <reified T : BmobObject> bmobGet(objectId: String, config: DslBmobQuery<T>.() -> Unit) {
    bmobQuery(objectId, config)
}

inline fun <reified T : BmobObject> bmobQueryList(config: DslBmobQuery<T>.() -> Unit) {
    val query = DslBmobQuery<T>()
    query.config()
    query.findObjects(object : FindListener<T>() {

        override fun done(list: MutableList<T>?, ex: BmobException?) {
            query.getsAction?.invoke(list, ex)
        }
    })
}

inline fun <reified T : BmobObject> bmobGets(config: DslBmobQuery<T>.() -> Unit) {
    bmobQueryList(config)
}

/**查询数量*/
inline fun <reified T : BmobObject> bmobCount(config: DslBmobQuery<T>.() -> Unit) {
    val query = DslBmobQuery<T>()
    query.config()
    query.count(T::class.java, object : CountListener() {
        override fun done(count: Int?, ex: BmobException?) {
            query.countAction?.invoke(count, ex)
        }
    })
}

//</editor-fold desc="查询相关">

//<editor-fold desc="删除相关">

fun <T : BmobObject> bmobDelete(bmobObj: T, config: DslBmobQuery<T>.() -> Unit) {
    val query = DslBmobQuery<T>()
    query.config()
    bmobObj.delete(object : UpdateListener() {
        override fun done(ex: BmobException?) {
            query.deleteAction?.invoke(ex)
        }
    })
}

//</editor-fold desc="删除相关">

//<editor-fold desc="更新相关">

/**更新一行数据*/
fun <T : BmobObject> bmobUpdate(bmobObj: T, config: DslBmobQuery<T>.() -> Unit) {
    val query = DslBmobQuery<T>()
    query.config()
//    val oldObjectId = bmobObj.objectId
//    val oldCreateAt = bmobObj.createdAt
//    val oldAcl = bmobObj.acl
    bmobObj.update(object : UpdateListener() {

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
fun <T : BmobObject> bmobSave(bmobObj: T, config: DslBmobQuery<T>.() -> Unit) {
    val query = DslBmobQuery<T>()
    query.config()
    bmobObj.save(object : SaveListener<String>() {

        override fun done(objectId: String?, ex: BmobException?) {
            query.saveAction?.invoke(bmobObj, ex)
        }
    })
}

//</editor-fold desc="保存相关">
