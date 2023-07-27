package com.angcyo.objectbox.laser.pecker

import android.content.Context
import com.angcyo.objectbox.*
import io.objectbox.Box
import io.objectbox.BoxStore
import io.objectbox.query.QueryBuilder
import kotlin.reflect.KClass

/**
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2022/06/29
 */
object LPBox {

    /**数据库文件夹名*/
    const val DB_NAME = "LaserPecker"

    /**数据库的包名, [BoxStore]存储的地方*/
    const val PACKAGE_NAME = BuildConfig.LIBRARY_PACKAGE_NAME

    /**数据库初始化*/
    fun init(context: Context) {
        DslBox.init(context, PACKAGE_NAME, DB_NAME, false)
    }
}

inline fun <reified T> T.lpSaveEntity(): Long {
    return saveEntity(LPBox.PACKAGE_NAME)
}

inline fun <reified T> T.lpRemoveEntity() = deleteEntity(LPBox.PACKAGE_NAME)

/**批量保存或者更新
 * id不为0时, 就是更新*/
inline fun <reified T> Collection<T>.lpSaveAllEntity() {
    boxOf(T::class.java, LPBox.PACKAGE_NAME).put(this)
}

inline fun <reified T> Collection<T>.lpRemoveAllEntity() {
    boxOf(T::class.java, LPBox.PACKAGE_NAME).remove(this)
}

/**快速获取[BoxStore]
 * [com.angcyo.objectbox.boxStoreOf]*/
fun lpBoxStoreOf(action: BoxStore.() -> Unit = {}): BoxStore {
    return boxStoreOf(LPBox.PACKAGE_NAME, action)
}

/**[com.angcyo.objectbox.boxOf]*/
fun <T> lpBoxOf(entityClass: Class<T>, action: Box<T>.() -> Unit = {}): Box<T> {
    return boxOf(entityClass, LPBox.PACKAGE_NAME, action)
}

fun <T : Any> lpBoxOf(entityClass: KClass<T>, action: Box<T>.() -> Unit = {}): Box<T> {
    return boxOf(entityClass, LPBox.PACKAGE_NAME, action)
}

inline fun <reified T : Any> KClass<T>.lpUpdateOrCreateEntity(
    noinline query: QueryBuilder<T>.() -> Unit,
    update: T.() -> Unit
): Long {
    return updateOrCreateEntity(LPBox.PACKAGE_NAME, query, update)
}