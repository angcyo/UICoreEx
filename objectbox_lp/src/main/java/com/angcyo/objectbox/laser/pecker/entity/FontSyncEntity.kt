package com.angcyo.objectbox.laser.pecker.entity

import androidx.annotation.Keep
import com.angcyo.library.ex.nowTime
import io.objectbox.annotation.Entity
import io.objectbox.annotation.Id

/**
 * 字体同步的结构
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2023/07/26
 */
@Keep
@Entity
data class FontSyncEntity(
    @Id var entityId: Long = 0L,

    /**是否被删除*/
    var isDelete: Boolean = false,

    /**当前的同步状态
     * [EntitySync.SYNC_STATE_NORMAL]
     * [EntitySync.SYNC_STATE_ING]
     * [EntitySync.SYNC_STATE_SUCCESS]
     * */
    var syncState: Int = EntitySync.SYNC_STATE_NORMAL,

    /**创建时间*/
    var createTime: Long = nowTime(),

    /**更新时间*/
    var updateTime: Long = nowTime(),

    //---

    /**用户id*/
    var userId: String? = null,

    /**数据显示的名称*/
    var name: String? = null,

    /**数据对应的本地路径*/
    var filePath: String? = null,

    /**数据对应的md5值*/
    var fileMd5: String? = null,
)
