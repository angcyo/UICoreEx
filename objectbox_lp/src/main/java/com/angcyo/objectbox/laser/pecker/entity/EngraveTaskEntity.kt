package com.angcyo.objectbox.laser.pecker.entity

import androidx.annotation.Keep
import io.objectbox.annotation.Entity
import io.objectbox.annotation.Id

/**
 * 雕刻任务总管理
 *
 * 管理任务下, 所有的雕刻索引, 所有的耗时时长, 总进度等
 *
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2022/10/09
 */

@Keep
@Entity
data class EngraveTaskEntity(

    @Id var entityId: Long = 0L,

    /**当前雕刻任务的id*/
    var taskId: String? = null,

    /**蓝牙设备地址
     * DC:0D:30:00:1F:60*/
    var deviceAddress: String? = null,

    /**批量雕刻时的大索引, 如果支持批量雕刻才有值*/
    var bigIndex: Int? = null,

    /**当前正在雕刻的数据索引*/
    var currentIndex: Int = -1,

    /**当前雕刻的文件名*/
    var currentFileName: String? = null,

    /**当前雕刻索引的进度*/
    var currentProgress: Int = 0,

    /**任务需要雕刻的索引集合, 同时也是需要雕刻的文件名集合
     * 数据库只支持[List<String>]类型, 不支持[List<Int>]类型
     * */
    var dataList: List<String>? = null,

    /**是否激活了[com.angcyo.engrave.data.HawkEngraveKeys.enableItemEngraveParams]
     * 决定了雕刻参数的获取方式
     * [com.angcyo.engrave.EngraveFlowDataHelper.getEngraveConfig(java.lang.Integer)]*/
    var enableItemEngraveParams: Boolean = false,

    /**是否是文件名雕刻任务*/
    var isFileNameEngrave: Boolean = false,

    /**是否要忽略在历史界面显示*/
    var ignoreShow: Boolean = false,

    /**雕刻的索引数据是在SD还是USB
     * [com.angcyo.bluetooth.fsc.laserpacker.command.QueryCmd.TYPE_SD]
     * [com.angcyo.bluetooth.fsc.laserpacker.command.QueryCmd.TYPE_USB]
     * */
    var mount: Int = -1,

    //---

    /**
     * 雕刻任务的状态
     * [com.angcyo.engrave.model.EngraveModel.ENGRAVE_STATE_START]
     * [com.angcyo.engrave.model.EngraveModel.ENGRAVE_STATE_PAUSE]
     * [com.angcyo.engrave.model.EngraveModel.ENGRAVE_STATE_FINISH]
     * [com.angcyo.engrave.model.EngraveModel.ENGRAVE_STATE_ERROR]
     * 异常在: [com.angcyo.engrave.model.EngraveModel._lastEngraveCmdError]属性中
     * */
    var state: Int = 0,

    /**雕刻总进度[0~100]*/
    var progress: Int = 0,

    /**任务开始的时间, 毫秒*/
    var startTime: Long = -1,

    /**当前索引开始的时间, 毫秒
     * 当只有1个文件时, 等同于[startTime]*/
    var indexStartTime: Long = -1,

    /**当前索引第几次雕刻的开始的时间, 毫秒
     * 当只雕刻一次时, 等同于[indexStartTime]*/
    var indexPrintStartTime: Long = -1,

    /**任务完成的时间, 毫秒*/
    var finishTime: Long = -1,

    //---

    /**最后一次统计剩余时长时记录的进度, 只有在进度有变化时才重新计算剩余时长*/
    var lastDurationProgress: Int = -1,

    /**最后一次算出来的剩余时长, 毫秒*/
    var lastDuration: Long = -1,

    /**最后一次计算时长的时间, 13位时间戳*/
    var lastDurationTime: Long = -1,
)
