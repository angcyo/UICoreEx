package com.angcyo.objectbox.laser.pecker

import android.content.Context
import com.angcyo.objectbox.DslBox
import com.angcyo.objectbox.boxStoreOf
import io.objectbox.BoxStore

/**
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2022/06/29
 */
object LPBox {

    /**数据库初始化*/
    fun init(context: Context) {
        DslBox.init(context, BuildConfig.LIBRARY_PACKAGE_NAME, "LaserPecker")
    }
}

/**快速获取[BoxStore]*/
fun lpBoxStoreOf(action: BoxStore.() -> Unit = {}): BoxStore {
    return boxStoreOf(BuildConfig.LIBRARY_PACKAGE_NAME, action)
}