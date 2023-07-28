package com.angcyo.objectbox.laser.pecker.entity

import androidx.annotation.Keep
import com.angcyo.library.component.sync.ISyncEntity
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

    /**云端数据库的id*/
    var dataId: String? = null,

    /**服务器数据的版本
     * 版本更高的数据会覆盖低版本的数据,
     * 暂时不做冲突解决
     * */
    var dataVersion: Long = 0,

    /**本地数据版本
     * 每次更新后,版本数据要+1
     * */
    var localDataVersion: Long = 0,

    /**数据显示的名称*/
    var name: String? = null,

    /**数据对应的本地路径, 用于上传*/
    var filePath: String? = null,

    /**数据对应的md5值*/
    var fileMd5: String? = null,

    /**数据远程地址, 用于下载*/
    var fileUrl: String? = null
) : ISyncEntity {
    override fun isSync(): Boolean {
        return syncState == EntitySync.SYNC_STATE_SUCCESS
    }
}
