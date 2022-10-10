package com.angcyo.objectbox.laser.pecker.entity

import androidx.annotation.Keep
import androidx.annotation.Px
import io.objectbox.annotation.Entity
import io.objectbox.annotation.Id

/**
 * 预览配置信息实体
 *
 * 比如: 第三轴 圆柱/锥体/球体
 *
 * 周长/直径 上直径/小直径/高度 周长/长径/短径
 *
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2022/10/09
 */

@Keep
@Entity
data class PreviewConfigEntity(
    @Id var entityId: Long = 0L,

    /**当前雕刻任务的id*/
    var taskId: String? = null,

    //---L4专属---

    /**雕刻物体直径, 这里用像素作为单位
     * [com.angcyo.bluetooth.fsc.laserpacker.command.EngraveCmd.diameter]
     *
     * [com.angcyo.engrave.data.EngraveDataInfo.width]
     * [com.angcyo.engrave.data.EngraveDataInfo.height]
     * */
    @Px
    var diameterPixel: Float = -1f,
)