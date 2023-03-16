package com.angcyo.acc2.app.dslitem

import com.angcyo.acc2.app.R
import com.angcyo.acc2.app.model.isAdaptive
import com.angcyo.acc2.app.model.isInstall
import com.angcyo.acc2.bean.TaskBean
import com.angcyo.dialog.inputDialog
import com.angcyo.dsladapter.DslAdapterItem
import com.angcyo.library.ex.*
import com.angcyo.widget.DslViewHolder
import com.angcyo.widget.span.span

/**
 *
 * Email:angcyo@126.com
 * @author angcyo
 * @date 2020/07/28
 * Copyright (c) 2020 ShenZhen Wayto Ltd. All rights reserved.
 */

class AppTaskItem : DslAdapterItem() {

    //任务数据
    var taskBean: TaskBean? = null

    //任务状态, >0 已开始
    var taskState: Int = 0

    /**启动任务回调*/
    var startAction: (TaskBean) -> Unit = {}

    /**自定义关键字列表*/
    var customWordList: List<String>? = null

    init {
        itemLayoutId = R.layout.app_task_item
    }

    override fun onItemBind(
        itemHolder: DslViewHolder,
        itemPosition: Int,
        adapterItem: DslAdapterItem,
        payloads: List<Any>
    ) {
        super.onItemBind(itemHolder, itemPosition, adapterItem, payloads)

        itemHolder.tv(R.id.task_id_view)?.text = span {
            val taskId = taskBean?.taskId ?: -1
            if (taskId > 0) {
                append("任务ID:${taskId}")
            } else {
                append("任务UUId:${taskBean?.uuid ?: "--"}")
            }
            taskBean?.type?.let {
                append(" type:[${it.or()}]")
            }
        }
        itemHolder.tv(R.id.task_name_view)?.text =
            "任务名:${taskBean?.title.or()}[${taskBean?.actionList.size()}]"
        itemHolder.tv(R.id.task_des_view)?.text = "描述:${taskBean?.des.or()}"
        itemHolder.tv(R.id.task_package_name_view)?.text = "${taskBean?.packageName.or()}"
        itemHolder.tv(R.id.task_word_list_view)?.text = span {
            (customWordList ?: taskBean?.wordList)?.forEachIndexed { index, s ->
                if (index > 0) {
                    appendln()
                }
                append("$index:")
                append(s)
            }.elseNull {
                append("--")
            }
        }

        val isInstall = taskBean?.packageName.isInstall()
        val isAdaptive = taskBean?.packageName.isAdaptive()

        itemHolder.tv(R.id.start_button)?.text = when {
            taskState > 0 -> "停止"
            !isInstall -> "未安装"
            taskBean?.adaptive ?: false && !isAdaptive -> "启动x"
            else -> "启动"
        }

        //event
        itemHolder.throttleClick(R.id.start_button) {
            if (isInstall) {
                taskBean?.let(startAction)
            } else {
                //no op
            }
        }

        //custom
        itemHolder.throttleClick(R.id.custom_button) {
            it.context.inputDialog {
                dialogTitle = "自定义关键字(回车分割)"
                canInputEmpty = false
                defaultInputString = (customWordList ?: taskBean?.wordList)?.connect("\n")
                inputViewHeight = 238 * dpi

                onInputResult = { dialog, inputText ->
                    customWordList = inputText.splitList("\n")
                    updateAdapterItem()
                    false
                }
            }
        }
    }
}