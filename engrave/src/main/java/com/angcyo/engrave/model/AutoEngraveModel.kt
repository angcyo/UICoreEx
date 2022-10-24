package com.angcyo.engrave.model

import com.angcyo.bluetooth.fsc.enqueue
import com.angcyo.bluetooth.fsc.laserpacker.command.ExitCmd
import com.angcyo.canvas.data.CanvasOpenDataType
import com.angcyo.canvas.data.CanvasProjectBean
import com.angcyo.canvas.data.CanvasProjectItemBean
import com.angcyo.canvas.data.toCanvasProjectItemList
import com.angcyo.core.lifecycle.LifecycleViewModel
import com.angcyo.core.vmApp
import com.angcyo.engrave.EngraveFlowDataHelper
import com.angcyo.engrave.data.TransferState
import com.angcyo.engrave.transition.EmptyException
import com.angcyo.engrave.transition.EngraveTransitionManager
import com.angcyo.http.rx.doBack
import com.angcyo.library.component.batchHandle
import com.angcyo.objectbox.laser.pecker.entity.TransferDataEntity
import com.angcyo.viewmodel.observe
import com.angcyo.viewmodel.vmDataOnce

/**
 * 自动雕刻Model, 可以通过数据直接雕刻
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2022/10/21
 */
class AutoEngraveModel : LifecycleViewModel() {

    companion object {
        /**正常*/
        const val STATE_NORMAL = 0

        /**正在创建数据*/
        const val STATE_CREATE = 1

        /**正在传输数据*/
        const val STATE_TRANSFER = 2

        /**正在雕刻*/
        const val STATE_ENGRAVE = 3
    }

    /**自动雕刻任务*/
    var _autoEngraveTask: AutoEngraveTask? = null

    /**任务状态提示*/
    val autoEngraveTaskOnceData = vmDataOnce<AutoEngraveTask?>()

    val transferModel = vmApp<TransferModel>()

    val engraveModel = vmApp<EngraveModel>()

    /**需要自动雕刻的数据
     * 支持[com.angcyo.canvas.data.CanvasProjectItemBean]
     * 支持[com.angcyo.canvas.data.CanvasProjectBean]
     *
     * [com.angcyo.engrave.auto.AutoEngraveActivity] 监听处理
     * */
    val engravePendingData = vmDataOnce<CanvasOpenDataType?>()

    init {
        //传输监听
        transferModel.transferStateOnceData.observe(this, allowBackward = false) {
            val autoEngraveTask = _autoEngraveTask
            if (autoEngraveTask != null && it != null && it.taskId == autoEngraveTask.taskId) {
                //传输状态监听
                autoEngraveTask.progress = it.progress
                if (it.state == TransferState.TRANSFER_STATE_FINISH) {
                    //传输完成开始雕刻
                    ExitCmd().enqueue { bean, error ->
                        if (error == null) {
                            //进入空闲模式, 才能开始雕刻
                            startEngrave(autoEngraveTask)
                        } else {
                            autoEngraveTask.isFinish = it.error != null
                            autoEngraveTask.error = it.error
                            autoEngraveTaskOnceData.postValue(autoEngraveTask)
                        }
                    }
                } else {
                    autoEngraveTask.isFinish = it.error != null
                    autoEngraveTask.error = it.error
                    autoEngraveTaskOnceData.postValue(autoEngraveTask)
                }
            }
        }
        //雕刻监听
        engraveModel.engraveStateData.observe(this, allowBackward = false) {
            val autoEngraveTask = _autoEngraveTask
            if (autoEngraveTask != null && it != null && it.taskId == autoEngraveTask.taskId) {
                autoEngraveTask.progress = it.progress
                if (it.state == EngraveModel.ENGRAVE_STATE_FINISH) {
                    //雕刻完成
                    autoEngraveTask.progress = 100
                    autoEngraveTask.isFinish = true
                    autoEngraveTask.state = TransferState.TRANSFER_STATE_NORMAL
                    autoEngraveTaskOnceData.postValue(autoEngraveTask)
                }
                autoEngraveTaskOnceData.postValue(autoEngraveTask)
            }
        }
    }

    //region---数据和传输---

    /**开始自动雕刻*/
    fun startAutoEngrave(taskId: String, projectBean: CanvasProjectBean): AutoEngraveTask {
        val task = AutoEngraveTask(taskId, projectBean)
        _autoEngraveTask = task
        val itemList = projectBean.data?.toCanvasProjectItemList()
        if (itemList.isNullOrEmpty()) {
            task.isFinish = true
            task.error = EmptyException()//空数据异常
        } else {
            task.state = STATE_CREATE
            startCreateData(task.taskId, itemList) {
                task.state = STATE_TRANSFER
                autoEngraveTaskOnceData.postValue(task)
                transferModel.startTransferData(task.taskId)
            }
        }
        autoEngraveTaskOnceData.postValue(task)
        return task
    }

    /**开始自动雕刻*/
    fun startAutoEngrave(taskId: String, itemList: List<CanvasProjectItemBean>?): AutoEngraveTask {
        val task = AutoEngraveTask(taskId, null)
        _autoEngraveTask = task
        if (itemList.isNullOrEmpty()) {
            task.isFinish = true
            task.error = EmptyException()//空数据异常
        } else {
            startCreateData(task.taskId, itemList) {
                task.state = STATE_TRANSFER
                autoEngraveTaskOnceData.postValue(task)
                transferModel.startTransferData(task.taskId)
            }
        }
        autoEngraveTaskOnceData.postValue(task)
        return task
    }

    //endregion---数据和传输---

    //region---数据和传输---

    /**开始批量创建数据*/
    fun startCreateData(
        taskId: String,
        itemBeanList: List<CanvasProjectItemBean>,
        action: (List<TransferDataEntity>) -> Unit = {}
    ) {
        doBack {
            val result = mutableListOf<TransferDataEntity>()
            itemBeanList.batchHandle({ data ->
                startCreateData(taskId, data) {
                    it?.let { result.add(it) }
                    next()
                }
            }) {
                action(result)
            }
        }
    }

    /**开始创建数据*/
    fun startCreateData(
        taskId: String,
        itemBean: CanvasProjectItemBean,
        action: (TransferDataEntity?) -> Unit = {}
    ) {
        doBack(true) {
            val transferConfig = EngraveFlowDataHelper.generateTransferConfig(taskId)//创建数据配置
            itemBean._cacheBitmap = true//开启图片缓存
            val transferDataEntity = EngraveTransitionManager().transitionTransferData(
                itemBean,
                transferConfig
            )//入库传输的数据
            EngraveFlowDataHelper.generateEngraveConfig(taskId, itemBean)//创建雕刻参数信息
            action(transferDataEntity)
        }
    }

    //region---雕刻---

    /**开始雕刻*/
    fun startEngrave(task: AutoEngraveTask) {
        if (task.isCancel || task.isFinish) {
            return
        }
        task.progress = 0
        task.state = STATE_ENGRAVE
        autoEngraveTaskOnceData.postValue(task)
        engraveModel.startEngrave(task.taskId)
    }

    //endregion---雕刻---

    /**自动雕刻任务*/
    data class AutoEngraveTask(
        //任务id
        val taskId: String,
        /**需要雕刻的数据, 工程数据. 里面包含子数据*/
        val projectBean: CanvasProjectBean?,

        /**任务当前的状态
         * [STATE_NORMAL]
         * [STATE_CREATE]
         * [STATE_TRANSFER]
         * [STATE_ENGRAVE]
         * */
        var state: Int = STATE_NORMAL,

        /**当前状态的进度*/
        var progress: Int = -1,

        //---

        /**任务是否被取消*/
        var isCancel: Boolean = false,
        /**任务是否完成*/
        var isFinish: Boolean = false,
        /**任务异常信息*/
        var error: Throwable? = null,
    )

}