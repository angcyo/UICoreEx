package com.angcyo.engrave.model

import com.angcyo.bluetooth.fsc.enqueue
import com.angcyo.bluetooth.fsc.laserpacker.command.FileModeCmd
import com.angcyo.bluetooth.fsc.laserpacker.parse.FileTransferParser
import com.angcyo.bluetooth.fsc.laserpacker.writeEngraveLog
import com.angcyo.bluetooth.fsc.parse
import com.angcyo.canvas.data.CanvasOpenDataType
import com.angcyo.canvas.data.CanvasProjectBean
import com.angcyo.canvas.data.CanvasProjectItemBean
import com.angcyo.canvas.data.toCanvasProjectItemList
import com.angcyo.core.component.file.writeErrorLog
import com.angcyo.core.lifecycle.LifecycleViewModel
import com.angcyo.core.vmApp
import com.angcyo.engrave.EngraveFlowDataHelper
import com.angcyo.engrave.data.TransferTaskStateData
import com.angcyo.engrave.model.TransferModel.Companion.calcTransferProgress
import com.angcyo.engrave.toEngraveDataTypeStr
import com.angcyo.engrave.transition.DataException
import com.angcyo.engrave.transition.EmptyException
import com.angcyo.engrave.transition.EngraveTransitionManager
import com.angcyo.engrave.transition.FailException
import com.angcyo.http.rx.doBack
import com.angcyo.http.rx.doMain
import com.angcyo.library.L
import com.angcyo.library.component.batchHandle
import com.angcyo.library.ex.uuid
import com.angcyo.objectbox.laser.pecker.entity.TransferDataEntity
import com.angcyo.objectbox.laser.pecker.lpSaveEntity
import com.angcyo.viewmodel.vmDataNull
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
    val autoEngraveTaskData = vmDataNull<AutoEngraveTask?>()

    /**任务状态提示*/
    val autoEngraveTaskOnceData = vmDataOnce<AutoEngraveTask?>()

    /**传输状态数据*/
    val autoTransferStateData = vmDataNull<TransferTaskStateData?>()

    val engraveModel = vmApp<EngraveModel>()

    /**需要自动雕刻的数据
     * 支持[com.angcyo.canvas.data.CanvasProjectItemBean]
     * 支持[com.angcyo.canvas.data.CanvasProjectBean]
     * */
    val engravePendingData = vmDataOnce<CanvasOpenDataType?>()

    //region---数据和传输---

    /**开始自动雕刻*/
    fun startAutoEngrave(projectBean: CanvasProjectBean): AutoEngraveTask {
        val task = AutoEngraveTask(uuid(), projectBean)
        val itemList = projectBean.data?.toCanvasProjectItemList()
        task._projectItemList = itemList
        if (itemList.isNullOrEmpty()) {
            task.isFinish = true
            task.error = EmptyException()//空数据异常
        } else {
            task.state = STATE_CREATE
            startCreateData(task.taskId, itemList) {
                task._transferDataList = it
                if (!task.isCancel) {
                    task.index = 0
                    task.count = it.size
                    startTransferData(task)
                }
            }
        }
        autoEngraveTaskData.postValue(task)
        autoEngraveTaskOnceData.postValue(task)
        return task
    }

    /**开始自动雕刻*/
    fun startAutoEngrave(itemList: List<CanvasProjectItemBean>?): AutoEngraveTask {
        val task = AutoEngraveTask(uuid(), null)
        task._projectItemList = itemList
        if (itemList.isNullOrEmpty()) {
            task.isFinish = true
            task.error = EmptyException()//空数据异常
        } else {
            startCreateData(task.taskId, itemList) {
                task._transferDataList = it
                if (!task.isCancel) {
                    task.index = 0
                    task.count = it.size
                    startTransferData(task)
                }
            }
        }
        autoEngraveTaskData.postValue(task)
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
            val transferConfig = EngraveFlowDataHelper.generateTransferConfig(taskId)
            val transferDataEntity = EngraveTransitionManager().transitionTransferData(
                itemBean,
                transferConfig
            )
            action(transferDataEntity)
        }
    }

    /**开始传输数据*/
    fun startTransferData(task: AutoEngraveTask) {
        task.state = STATE_TRANSFER
        autoEngraveTaskOnceData.postValue(task)
        (task._transferDataList ?: emptyList()).batchHandle({ data ->
            if (task.isCancel) {
                isCancel = true
                next()
            } else {
                //传输一条数据
                startTransferData(task, data) { throwable ->
                    if (throwable == null) {
                        task.index++
                    } else {
                        task.isCancel = true
                        error = throwable
                    }
                    next()
                }
            }
        }) {
            autoEngraveTaskOnceData.postValue(task)
            if (!task.isCancel) {
                startEngrave(task)
            }
        }
    }

    /**开始传输数据*/
    fun startTransferData(
        task: AutoEngraveTask?,
        transferDataEntity: TransferDataEntity,
        finishAction: (Throwable?) -> Unit = {}
    ) {
        L.i("开始传输数据:[${transferDataEntity.index}]")
        val taskId = task?.taskId ?: transferDataEntity.taskId
        val fileModeCmd = FileModeCmd(transferDataEntity.bytes()?.size ?: 0)
        fileModeCmd.enqueue { bean, error ->
            error?.let {
                it.toString().writeErrorLog()
                autoTransferStateData.postValue(
                    TransferTaskStateData(
                        taskId,
                        autoTransferStateData.value?.progress ?: 0,
                        FailException(error)
                    )
                )
            }
            if (task != null && (task.isFinish || task.isCancel)) {
                L.w("数据传输被取消:${taskId}")
                finishAction(null)
            } else {
                bean?.parse<FileTransferParser>()?.let {
                    if (it.isIntoFileMode()) {
                        //成功进入大数据模式
                        val dataCmd = TransferModel.getTransferDataCmd(transferDataEntity)
                        if (dataCmd == null) {
                            finishAction(null)
                        } else {
                            buildString {
                                append("开始传输:[${taskId}]")
                                append(" ${transferDataEntity.engraveDataType.toEngraveDataTypeStr()}")
                                append(" $transferDataEntity")
                            }.writeEngraveLog(true)

                            dataCmd.enqueue(progress = {
                                //进度
                                if (task != null) {
                                    val progress = calcTransferProgress(
                                        it.sendPacketPercentage,
                                        task.index,
                                        task.count
                                    )
                                    EngraveFlowDataHelper.updateTransferDataProgress(
                                        taskId,
                                        progress,
                                        it.sendSpeed
                                    )
                                    doMain {
                                        //及时回调
                                        autoTransferStateData.value =
                                            TransferTaskStateData(taskId, progress)
                                    }
                                }
                            }) { bean, error ->
                                val result = bean?.parse<FileTransferParser>()
                                L.w("传输结束:$result $error")
                                result?.let {
                                    if (result.isFileTransferSuccess()) {
                                        //文件传输完成
                                        transferDataEntity.isTransfer = true
                                        transferDataEntity.lpSaveEntity()
                                        finishAction(null)
                                    } else {
                                        "数据接收未完成".writeErrorLog()
                                        autoTransferStateData.postValue(
                                            TransferTaskStateData(
                                                taskId,
                                                autoTransferStateData.value?.progress
                                                    ?: 0,
                                                DataException()
                                            )
                                        )
                                        finishAction(DataException())
                                    }
                                }
                                if (result == null) {
                                    "发送数据失败".writeErrorLog()
                                    autoTransferStateData.postValue(
                                        TransferTaskStateData(
                                            taskId,
                                            autoTransferStateData.value?.progress ?: 0,
                                            FailException()
                                        )
                                    )
                                    finishAction(FailException())
                                }
                            }
                            //end data cmd
                        }
                        //end parse
                    } else {
                        "未成功进入数据传输模式".writeErrorLog()
                        autoTransferStateData.postValue(
                            TransferTaskStateData(
                                taskId,
                                autoTransferStateData.value?.progress ?: 0,
                                FailException()
                            )
                        )
                        finishAction(FailException())
                    }
                }
            }
            //end file mode cmd
        }
    }

    //endregion---数据和传输---

    //region---雕刻---

    /**开始雕刻*/
    fun startEngrave(task: AutoEngraveTask) {
        if (task.isCancel || task.isFinish) {
            return
        }
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

        //---

        //总共需要发送的数据量
        var count: Int = 0,

        //当前发送的索引
        var index: Int = 0,

        /**[projectBean]解析出来的数据*/
        var _projectItemList: List<CanvasProjectItemBean>? = null,

        /**[_projectItemList]生成需要传输的数据*/
        var _transferDataList: List<TransferDataEntity>? = null,

        //---

        /**任务是否被取消*/
        var isCancel: Boolean = false,
        /**任务是否完成*/
        var isFinish: Boolean = false,
        /**任务异常信息*/
        var error: Throwable? = null,
    )

}