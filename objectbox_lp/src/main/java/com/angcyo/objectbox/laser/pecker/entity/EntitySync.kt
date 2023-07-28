package com.angcyo.objectbox.laser.pecker.entity

import com.angcyo.library.component.FontManager
import com.angcyo.library.component.hawk.HawkPropertyValue
import com.angcyo.library.ex.file
import com.angcyo.library.ex.fileMd5
import com.angcyo.library.ex.nowTime
import com.angcyo.library.model.TypefaceInfo
import com.angcyo.library.utils.folderPath
import com.angcyo.objectbox.findAll
import com.angcyo.objectbox.findLast
import com.angcyo.objectbox.laser.pecker.LPBox
import com.angcyo.objectbox.laser.pecker.lpSaveAllEntity
import com.angcyo.objectbox.laser.pecker.lpSaveEntity
import io.objectbox.query.QueryBuilder

/**
 * 同步
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2023/07/26
 */
object EntitySync {

    /**正常*/
    const val SYNC_STATE_NORMAL = 0x00

    /**同步中*/
    const val SYNC_STATE_ING = 0x01

    /**同步成功*/
    const val SYNC_STATE_SUCCESS = SYNC_STATE_ING shl 1

    /**同步失败, 则会在下次同步周期时恢复成
     * [SYNC_STATE_NORMAL]*/
    const val SYNC_STATE_ERROR = SYNC_STATE_SUCCESS shl 1

    /**当前登录的用户id*/
    var userId: String? by HawkPropertyValue<Any, String?>(null)

    /**更新当前用户的id*/
    fun updateUserId(userId: String?) {
        EntitySync.userId = userId
        //切换对应的字体目录
        if (userId.isNullOrBlank()) {
            FontManager.defaultCustomFontFolder =
                folderPath("${FontManager.DEFAULT_FONT_FOLDER_NAME}/custom")
        } else {
            //登录成功
            FontManager.defaultCustomFontFolder =
                folderPath("${FontManager.DEFAULT_FONT_FOLDER_NAME}/custom/$userId")
        }
        FontManager.reloadCustomFontList()
    }

    //region ---字体同步相关---

    /**保存一个字体, 用于同步*/
    fun saveFontSyncEntity(typefaceInfo: TypefaceInfo) {
        FontSyncEntity().apply {
            userId = "${EntitySync.userId}"
            name = typefaceInfo.filePath?.file()?.name ?: typefaceInfo.name
            filePath = typefaceInfo.filePath
            fileMd5 = filePath.fileMd5()
            localDataVersion = 1
            dataVersion = localDataVersion
        }.lpSaveEntity()
    }

    /**删除一个字体*/
    fun deleteFontSyncEntity(typefaceInfo: TypefaceInfo) {
        FontSyncEntity::class.findAll(LPBox.PACKAGE_NAME) {
            apply(
                FontSyncEntity_.userId.equal("$userId")
                    .and(FontSyncEntity_.filePath.equal("${typefaceInfo.filePath}"))
            )
        }.apply {
            forEach {
                it.syncState = SYNC_STATE_NORMAL
                it.isDelete = true
                it.localDataVersion++
                it.updateTime = nowTime()
            }
            lpSaveAllEntity()
        }
    }

    /**获取所有需要同步的字体*/
    fun getFontSyncEntityList(
        query: QueryBuilder<FontSyncEntity>.() -> Unit = {
            apply(
                FontSyncEntity_.userId.equal("$userId")
                    .and(FontSyncEntity_.isDelete.equal(false))
            )
        }
    ): List<FontSyncEntity> = FontSyncEntity::class.findAll(LPBox.PACKAGE_NAME) {
        query()
    }

    //endregion ---字体同步相关---

    //region ---工程同步相关---

    /**保存一个工程, 用于同步*/
    fun saveProjectSyncEntity(projectName: String?, projectFilePath: String?) {
        ProjectSyncEntity().apply {
            userId = "${EntitySync.userId}"
            name = projectName
            filePath = projectFilePath
            fileMd5 = filePath.fileMd5()
            localDataVersion = 1
            dataVersion = localDataVersion
        }.lpSaveEntity()
    }

    /**删除一个工程*/
    fun deleteProjectSyncEntity(entityId: Long?) {
        ProjectSyncEntity::class.findAll(LPBox.PACKAGE_NAME) {
            apply(ProjectSyncEntity_.entityId.equal(entityId ?: -1))
        }.apply {
            forEach {
                it.syncState = SYNC_STATE_NORMAL
                it.isDelete = true
                it.localDataVersion++
                it.updateTime = nowTime()
            }
            lpSaveAllEntity()
        }
    }

    fun updateProjectSyncEntity(entityId: Long?, action: ProjectSyncEntity.() -> Unit) {
        ProjectSyncEntity::class.findLast(LPBox.PACKAGE_NAME) {
            apply(ProjectSyncEntity_.entityId.equal(entityId ?: -1))
        }?.let {
            it.apply(action)
            updateProjectSyncEntity(it)
        }
    }

    /**更新一个工程*/
    fun updateProjectSyncEntity(projectSyncEntity: ProjectSyncEntity) {
        projectSyncEntity.apply {
            syncState = SYNC_STATE_NORMAL
            isDelete = false
            updateTime = nowTime()
            localDataVersion++
        }.lpSaveEntity()
    }

    /**获取所有需要同步的工程*/
    fun getProjectSyncEntityList(
        query: QueryBuilder<ProjectSyncEntity>.() -> Unit = {
            apply(
                ProjectSyncEntity_.userId.equal("$userId")
                    .and(ProjectSyncEntity_.isDelete.equal(false))
            )
        }
    ): List<ProjectSyncEntity> = ProjectSyncEntity::class.findAll(LPBox.PACKAGE_NAME) {
        query()
    }

    //endregion ---工程同步相关---

    //region ---材质同步相关---

    /**获取所有需要同步的工程*/
    fun getMaterialSyncEntityList(
        query: QueryBuilder<MaterialEntity>.() -> Unit = {
            apply(
                MaterialEntity_.userId.equal("$userId")
                    .and(MaterialEntity_.isDelete.equal(false))
            )
        }
    ): List<MaterialEntity> = MaterialEntity::class.findAll(LPBox.PACKAGE_NAME) {
        query()
    }

    //endregion ---材质同步相关---
}