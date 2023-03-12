package com.angcyo.acc2.app.ui

import android.os.Bundle
import com.angcyo.acc2.app.AccAppDslFragment
import com.angcyo.acc2.app.R
import com.angcyo.acc2.app.component.Acc
import com.angcyo.acc2.app.component.AccWindow
import com.angcyo.acc2.app.component.Task
import com.angcyo.acc2.app.component.init
import com.angcyo.acc2.app.dslitem.AppTaskItem
import com.angcyo.acc2.app.dslitem.AppTextItem
import com.angcyo.acc2.app.dslitem.shareApk
import com.angcyo.acc2.app.helper.LogHelper
import com.angcyo.acc2.app.http.AccGitee
import com.angcyo.acc2.app.http.bean.FunctionBean
import com.angcyo.acc2.app.model.AccTaskModel
import com.angcyo.acc2.app.model.AdaptiveModel
import com.angcyo.acc2.app.model.GiteeModel
import com.angcyo.acc2.app.model.allApp
import com.angcyo.acc2.bean.TaskBean
import com.angcyo.acc2.parse.ConditionParse
import com.angcyo.component.hawkInstallAndRestore
import com.angcyo.core.vmApp
import com.angcyo.dialog.hideLoading
import com.angcyo.dialog.loadLoadingBottomCaller
import com.angcyo.dsladapter.filter.batchLoad
import com.angcyo.dsladapter.toLoading
import com.angcyo.library.L
import com.angcyo.library.ex.*
import com.angcyo.library.getAppVersionName
import com.angcyo.library.toastQQ
import com.angcyo.widget.base.appendDslItem
import com.angcyo.widget.base.string
import com.angcyo.widget.base.updateAllDslItem
import com.angcyo.widget.hawkTag
import com.angcyo.widget.span.span

/**
 *
 * Email:angcyo@126.com
 * @author angcyo
 * @date 2020/07/28
 * Copyright (c) 2020 ShenZhen Wayto Ltd. All rights reserved.
 */

class AccTaskTestFragment : AccAppDslFragment() {

    companion object {
        //剪切板中的文本
        var COPY_WORD: CharSequence? = null

        var NO_WORD = "无口令"

        //任务流控件
        var show_control_flow = true

        //视频流控件
        var show_video_flow = true

        //直播流控件
        var show_live_flow = true

        val KEY_ENABLE_ACTION = "enable_action"
    }

    val adaptiveModel: AdaptiveModel = vmApp()
    val accTaskModel: AccTaskModel = vmApp()
    val giteeModel: GiteeModel = vmApp()

    val appItemList = mutableListOf<AppTextItem>()

    init {
        fragmentTitle = span {
            append("本地任务测试页面")
            append(getAppVersionName()) {
                fontSize = 9 * dpi
            }
        }
        contentLayoutId = R.layout.fragment_task_test
        page.requestPageSize = Int.MAX_VALUE

        allApp.forEach {
            appItemList.add(AppTextItem().apply {
                packageName = it.packageName
                appName = it.label
            })
        }
    }

    override fun initBaseView(savedInstanceState: Bundle?) {
        super.initBaseView(savedInstanceState)

        //观察数据变化
        giteeModel.allTaskData.observe {
            L.w(it)
            hideLoading()
            updateList(it)
        }

        accTaskModel.taskData.observe {
            updateList()
        }

        //enable
        //_vh.ev(R.id.enable_action_edit)?.setInputText(KEY_ENABLE_ACTION.hawkGet())
        _vh.hawkTag(R.id.enable_action_edit, "enable_action_edit")
        _vh.hawkInstallAndRestore()

        //flow
        _vh.visible(R.id.task_control_layout, show_control_flow)
        _vh.visible(R.id.video_flow, show_video_flow)
        _vh.visible(R.id.live_flow, show_live_flow)

        //在线数据
        _vh.throttleClick(R.id.on_line_checkbox) {
            loadLoadingBottomCaller(showCloseView = false) { cancel, loadEnd ->
                if (!cancel.get()) {
                    /*checkModel.loadChecks((it as CompoundButton).isChecked)
                    taskModel.loadTasks(it.isChecked)
                    vmApp<ActionsModel>().loadActions(it.isChecked)*/
                    AccGitee.fetch(_vh.isChecked(R.id.on_line_checkbox), true)
                }
            }
        }

        //add check
        _vh.throttleClick(R.id.add_check_button) {
            loadLoadingBottomCaller { cancel, loadEnd ->
                /*checkModel.reset()
                if (!cancel) {
                    checkModel.uploadCheck { succeed, throwable ->
                        loadEnd(succeed, throwable)
                        _vh.tv(R.id.add_check_button)?.text = if (succeed) {
                            "Succeed"
                        } else {
                            throwable?.message
                        }
                    }
                }*/
                loadEnd(null, IllegalArgumentException("无接口"))
            }
        }

        //copy word
        _vh.check(R.id.word_copy_box, false) { _, isChecked ->
            if (isChecked) {
                _setCopyTip()
                _vh.check(R.id.skip_word_box, !isChecked)
            }
            _vh.visible(R.id.copy_tip_view, isChecked)
        }

        //skip word
        _vh.check(R.id.skip_word_box, false) { _, isChecked ->
            if (isChecked) {
                _vh.check(R.id.info_box, !isChecked)
                _vh.check(R.id.word_copy_box, !isChecked)
            }
        }

        //box
        _vh.check(R.id.all_box, false) { _, isChecked ->
            if (!_vh.isChecked(R.id.live_all_box)) {
                _vh.check(R.id.info_box, isChecked)
            }

            _vh.check(R.id.like_box, isChecked)
            _vh.check(R.id.comment_box, isChecked)
            _vh.check(R.id.attention_box, isChecked)
            _vh.check(R.id.user_box, isChecked)
            _vh.check(R.id.collect_box, isChecked)
            _vh.check(R.id.copy_box, isChecked)
            _vh.check(R.id.word_box, isChecked)

            if (isChecked) {
                _vh.check(R.id.live_comment_box, !isChecked)
                _vh.check(R.id.live_like_box, !isChecked)
                _vh.check(R.id.live_attention_box, !isChecked)
                _vh.check(R.id.live_shop_box, !isChecked)
                _vh.check(R.id.live_all_box, !isChecked)
            }
        }

        //live box
        _vh.check(R.id.live_all_box, false) { _, isChecked ->
            if (!_vh.isChecked(R.id.all_box)) {
                _vh.check(R.id.info_box, isChecked)
            }

            if (isChecked) {
                _vh.check(R.id.like_box, !isChecked)
                _vh.check(R.id.comment_box, !isChecked)
                _vh.check(R.id.attention_box, !isChecked)
                _vh.check(R.id.user_box, !isChecked)
                _vh.check(R.id.collect_box, !isChecked)
                _vh.check(R.id.copy_box, !isChecked)
                _vh.check(R.id.word_box, !isChecked)
                _vh.check(R.id.all_box, !isChecked)
            }

            _vh.check(R.id.live_comment_box, isChecked)
            _vh.check(R.id.live_like_box, isChecked)
            _vh.check(R.id.live_attention_box, isChecked)
            _vh.check(R.id.live_shop_box, isChecked)
        }

        //share apk
        _vh.throttleClick(R.id.share_app_button) {
            fContext().shareApk()
        }

        //share log
        _vh.throttleClick(R.id.share_log_button) {
            LogHelper.showLogShareDialog(fContext())
        }

        //app info
        _vh.group(R.id.task_app_tip_view)?.appendDslItem(appItemList, 0)
        _vh.visible(R.id.on_line_checkbox, isDebug() || adaptiveModel.isAdmin())
        _vh.visible(R.id.add_check_button, isDebugType())

        //loading color
        /*_adapter.dslAdapterStatusItem.onBindStateLayout = { itemHolder, state ->
            if (state == DslAdapterStatusItem.ADAPTER_STATUS_LOADING) {
                itemHolder.tv(R.id.lib_text_view)?.setTextColor(Color.WHITE)
            }
        }*/

        //test
        _vh.throttleClick(R.id.test_button) {
            //AccTouchTipLayer.showMove(0.3f, 0.3f, 0.3f, 0.8f)
        }

        //加载测试任务
        if (giteeModel.allTaskData.value.isNullOrEmpty()) {
            _vh.post {
                if (!_adapter.isAdapterStatus()) {
                    _adapter.toLoading()
                }
            }
        }
        //加载全部离线数据
        AccGitee.fetch(false)
    }

    //读取剪切板内容提示
    fun _setCopyTip() {
        val primaryClip = getPrimaryClip()
        if (!primaryClip.isNullOrEmpty()) {
            COPY_WORD = primaryClip
        }
        _vh.tv(R.id.copy_tip_view)?.text =
            if (COPY_WORD.isNullOrEmpty()) NO_WORD else COPY_WORD
    }

    override fun onFragmentFirstShow(bundle: Bundle?) {
        super.onFragmentFirstShow(bundle)
        AccWindow.show {
            reset()
            this.text = "^-^"
        }

        if (COPY_WORD.isNullOrEmpty()) {
            COPY_WORD = getPrimaryClip()
        }
    }

    override fun onFragmentShow(bundle: Bundle?) {
        super.onFragmentShow(bundle)

        //update
        _vh.group(R.id.task_app_tip_view)?.updateAllDslItem()

        //copy
        _vh.click(R.id.task_app_tip_view) {
            //UserModel.appListLogInfo().copy()
        }

        if (_vh.tv(R.id.copy_tip_view)?.text.isNullOrEmpty() ||
            _vh.tv(R.id.copy_tip_view)?.text == NO_WORD
        ) {
            _vh.post {
                _setCopyTip()
            }
        }

        //getText获取到的值
        _vh.tv(R.id.get_text_tip_view)?.text = Task.control.controlEndToLog()
    }

    fun updateList(list: List<TaskBean>? = giteeModel.allTaskData.value) {
        renderDslAdapter {
            loadDataEnd(AppTaskItem::class.java, list, null) { task ->
                val taskData = accTaskModel.taskData.value

                //是否正在运行任务
                val taskRun = if (taskData?.taskId != -1L) {
                    taskData?.taskId == task.taskId
                } else {
                    taskData.title == task.title
                }

                taskBean = task
                taskState = if (taskRun) 1 else 0

                startAction = {
                    if (startTask(this, it)) {
                        //刷新列表
                        updateList()
                    }
                }
            }

            if (fragmentShowCount < 1) {
                batchLoad()
            }
        }
    }

    fun startTask(item: AppTaskItem, task: TaskBean): Boolean {
        item.apply {

            if (!Acc.check(fContext(), false)) {
                return false
            }

            if (taskState > 0) {
                //停止任务
                Task.control.stop()
            } else {
                //替换自定义关键字
                if (customWordList != null) {
                    task.wordList = customWordList
                }

                //开始任务
                if (_vh.isChecked(R.id.word_copy_box) && !COPY_WORD.isNullOrEmpty()) {
                    //替换口令
                    task.wordList?.apply {
                        if (this is MutableList) {
                            try {
                                val taskId = task.taskId
                                if (taskId == 388L) {
                                    this[1] = COPY_WORD.str()
                                } else {
                                    this[0] = COPY_WORD.str()
                                }
                                "".copy()//清空剪切板
                            } catch (e: Exception) {
                                toastQQ("剪切板口令设置失败")
                            }
                        }
                    }
                }
                //去掉form
                //it.form = null

                //强制小屏浮窗
                if (_vh.isChecked(R.id.mini_checkbox)) {
                    task.fullscreen = false
                } else {
                    //否则使用task.json默认
                }

                val enableString = getEnableString(task)
                val disableString = getDisableString(task)

                Task.start(task.init(), enableString, disableString)
            }
        }
        return true
    }

    /**需要禁用的[Action]*/
    fun getDisableString(taskBean: TaskBean): String {
        return buildString {
            //跳过debug分组
            taskBean.disableAction = "${taskBean.disableAction ?: ""};debug;d1;"

            //跳过口令
            if (_vh.isChecked(R.id.skip_word_box)) {
                taskBean.disableAction = "${taskBean.disableAction ?: ""};a1;"
            }
        }
    }

    /**获取需要指定激活的[Action]*/
    fun getEnableString(taskBean: TaskBean): String {
        return buildString {

            val enableAction = _vh.ev(R.id.enable_action_edit).string()
            taskBean.enableAction =
                "${taskBean.enableAction ?: ""};$enableAction;e1;${ConditionParse.OR};"
            KEY_ENABLE_ACTION.hawkPut(enableAction)

            //-----------------------------↓ 视频相关-----------------------------

            //获取帐号
            if (_vh.isChecked(R.id.info_box)) {

            }
            //开启点赞
            if (_vh.isChecked(R.id.like_box)) {
                giteeModel.findFunction(FunctionBean.FUNCTION_LIKE)?.actions?.let {
                    append(it)
                }
            }
            //开启评论
            if (_vh.isChecked(R.id.comment_box)) {
                giteeModel.findFunction(FunctionBean.FUNCTION_COMMENT)?.actions?.let {
                    append(it)
                }
            }
            //开启关注
            if (_vh.isChecked(R.id.attention_box)) {
                giteeModel.findFunction(FunctionBean.FUNCTION_ATTENTION)?.actions?.let {
                    append(it)
                }
            }
            //开启进入个人主页
            if (_vh.isChecked(R.id.user_box)) {
                append("1410;1420;")
            }
            //开启收藏
            if (_vh.isChecked(R.id.collect_box)) {
                giteeModel.findFunction(FunctionBean.FUNCTION_COLLECT)?.actions?.let {
                    append(it)
                }
            }
            //开启复制链接
            if (_vh.isChecked(R.id.copy_box)) {

            }
            //开启复制口令/微信分享
            if (_vh.isChecked(R.id.word_box)) {
                giteeModel.findFunction(FunctionBean.FUNCTION_SHARE)?.actions?.let {
                    append(it)
                }
            }

            //跳过口令
            if (_vh.isChecked(R.id.skip_word_box)) {
                //跳过口令, 不需要激活action
            } else {
                //url
                if (_vh.isChecked(R.id.url_box)) {
                    //抖音直接打开url

                } else {
                    //抖音口令

                }
            }


            //-----------------------------↓ 直播相关-----------------------------


            //直播关注
            if (_vh.isChecked(R.id.live_attention_box)) {
            }
            //直播点赞
            if (_vh.isChecked(R.id.live_like_box)) {
            }
            //直播评论
            if (_vh.isChecked(R.id.live_comment_box)) {
            }
            //直播购物车
            if (_vh.isChecked(R.id.live_shop_box)) {
            }
        }
    }
}