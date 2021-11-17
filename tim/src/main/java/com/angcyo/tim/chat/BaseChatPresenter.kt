package com.angcyo.tim.chat

import android.view.View
import android.widget.EditText
import androidx.recyclerview.widget.RecyclerView
import com.angcyo.core.vmApp
import com.angcyo.dsladapter.DslAdapter
import com.angcyo.dsladapter.DslAdapterItem
import com.angcyo.http.base.toJson
import com.angcyo.library.L
import com.angcyo.library.component._delay
import com.angcyo.library.ex.*
import com.angcyo.library.ex.string
import com.angcyo.library.model.loadPath
import com.angcyo.media.audio.record.RecordControl
import com.angcyo.media.video.record.recordVideo
import com.angcyo.picker.dslPickerImageVideo
import com.angcyo.tim.R
import com.angcyo.tim.TimMessage
import com.angcyo.tim.TimSdkException
import com.angcyo.tim.bean.*
import com.angcyo.tim.bean.MessageInfoBean.Companion.MSG_STATUS_REVOKE
import com.angcyo.tim.bean.MessageInfoBean.Companion.MSG_STATUS_SENDING
import com.angcyo.tim.bean.MessageInfoBean.Companion.MSG_STATUS_SEND_FAIL
import com.angcyo.tim.bean.MessageInfoBean.Companion.MSG_STATUS_SEND_SUCCESS
import com.angcyo.tim.dslitem.ChatEmojiItem
import com.angcyo.tim.dslitem.ChatLoadingItem
import com.angcyo.tim.dslitem.ChatMoreActionItem
import com.angcyo.tim.helper.*
import com.angcyo.tim.model.ChatModel
import com.angcyo.tim.ui.chat.BaseChatFragment
import com.angcyo.tim.util.FaceManager
import com.angcyo.tim.util.TimConfig
import com.angcyo.widget.DslViewHolder
import com.angcyo.widget.base.*
import com.angcyo.widget.layout.*
import com.angcyo.widget.recycler.*
import com.tencent.imsdk.v2.*
import com.tencent.imsdk.v2.V2TIMMessageListGetOption.V2TIM_GET_CLOUD_OLDER_MSG

/**
 * 聊天操作层
 * Email:angcyo@126.com
 * @author angcyo
 * @date 2021/11/11
 * Copyright (c) 2020 ShenZhen Wayto Ltd. All rights reserved.
 */
abstract class BaseChatPresenter {

    var chatFragment: BaseChatFragment? = null

    /**是否是语音输入*/
    var isVoiceInput: Boolean = false

    val _adapter: DslAdapter?
        get() = chatFragment?._adapter

    val _recycler: DslRecyclerView?
        get() = chatFragment?._recycler

    val _vh: DslViewHolder?
        get() = chatFragment?._vh

    val softInputLayout: DslSoftInputLayout?
        get() = _vh?.v<DslSoftInputLayout>(R.id.lib_soft_input_layout)

    val inputEditText: EditText?
        get() = _vh?.v<EditText>(R.id.chat_edit_text)

    val chatBean: ChatInfoBean?
        get() = chatFragment?.chatInfoBean

    /**消息管理*/
    val messageManager = V2TIMManager.getMessageManager()

    // 消息已读上报时间间隔
    val READ_REPORT_INTERVAL = 1000 // 单位： 毫秒

    var _lastReadReportTime = 0L
    var _canReadReport = true

    var _isLoading = false

    /**语音录制*/
    var recordControl: RecordControl = RecordControl().apply {
        recordUI.minRecordTime = 3
        recordUI.maxRecordTime = 60
    }

    //<editor-fold desc="初始化">

    /**初始化界面*/
    open fun initView(fragment: BaseChatFragment) {
        chatFragment = fragment

        //输入框
        inputEditText?.onTextChange {
            if (it.isEmpty()) {
                _vh?.gone(R.id.chat_send_button)
            } else {
                _vh?.visible(R.id.chat_send_button)
            }
        }
        //键盘
        softInputLayout?.onSoftInputChangeStart { action, height, oldHeight ->
            if (action.isSoftInputShowAction()) {
                _scrollToLast()
            } else if (action.isEmojiShowAction() && _recycler?.isLastItemVisibleCompleted() == true) {
                _scrollToLast()
            }
        }

        //发送按钮
        _vh?.click(R.id.chat_send_button) {
            sendInputMessage(inputEditText?.string())
            if (!isDebugType()) {
                //清空输入框
                inputEditText?.setInputText()
            }
        }
        //录制语音
        fragment.activity?.let {
            val voiceInput = _vh?.view(R.id.chat_voice_input)
            recordControl.wrap(_vh?.view(R.id.chat_voice_input), it, onTouch = {
                if (it.isTouchDown()) {
                    voiceInput?.setBackgroundResource(R.drawable.chat_audio_input_press_bg_shape)
                } else if (it.isTouchFinish()) {
                    voiceInput?.setBackgroundResource(R.drawable.chat_input_bg_shape)
                }
            }) { recordFile ->
                val file = recordControl.rename(recordFile)
                val duration = recordControl.recordUI.currentRecordTime / 1000 //秒
                L.i("录制结束:${file}")
                sendSoundMessage(file.absolutePath, duration.toInt())
            }
        }

        //语音
        fragment._vh.click(R.id.chat_voice_view) {
            softInputLayout?.hideEmojiLayout()
            _switchVoiceInput(!isVoiceInput)
        }
        //表情
        fragment._vh.click(R.id.chat_emoji_view) {
            softInputLayout?.showEmojiLayout()
            _showEmojiLayout()
            if (isVoiceInput) {
                _switchVoiceInput(false, false)
            }
        }
        //更多
        fragment._vh.click(R.id.chat_more_view) {
            softInputLayout?.showEmojiLayout()
            _showMoreLayout()
            if (isVoiceInput) {
                _switchVoiceInput(false, false)
            }
        }
        //recycler
        _recycler?.apply {
            onScrollStateChangedAction { recyclerView, newState ->
                if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                    //滚动停止后
                    if (recyclerView.isTopItemVisibleCompleted()) {
                        loadHistoryMessage()
                    }
                }
            }
            onScrolledAction { recyclerView, dx, dy ->
                L.i()
            }
        }
    }

    val moreActionList: MutableList<MoreActionBean> = mutableListOf()

    /**更多菜单item*/
    open fun initMoreAction() {
        moreActionList.clear()
        moreActionList.add(MoreActionBean().apply {
            title = "相册"
            iconResId = R.drawable.ic_image_type_item
            action = {
                chatFragment?.dslPickerImageVideo(
                    videoMinDuration = TimConfig.MIN_VIDEO_DURATION,
                    videoMaxDuration = TimConfig.MAX_VIDEO_DURATION
                ) {
                    it?.firstOrNull()?.let {
                        sendImageOrVideoMessage(it.loadPath())
                    }
                }
            }
        })
        moreActionList.add(MoreActionBean().apply {
            title = "拍摄"
            iconResId = R.drawable.ic_camera_type_item
            action = {
                chatFragment?.recordVideo(
                    maxRecordTime = (TimConfig.MAX_VIDEO_DURATION / 1000).toInt(),
                    minRecordTime = (TimConfig.MIN_VIDEO_DURATION / 1000).toInt()
                ) {
                    sendImageOrVideoMessage(it)
                }
            }
        })
        moreActionList.add(MoreActionBean().apply {
            title = "视频通话"
            iconResId = R.drawable.ic_video_type_item
        })
        moreActionList.add(MoreActionBean().apply {
            title = "位置"
            iconResId = R.drawable.ic_gps_type_item
        })
        moreActionList.add(MoreActionBean().apply {
            title = "文件"
            iconResId = R.drawable.ic_file_type_item
        })
    }

    //</editor-fold desc="初始化">

    //<editor-fold desc="操作">

    /**加载消息*/
    open fun loadMessage() {
        if (_isLoading) {
            return
        }
        showLoadingItem()

        chatBean?.let { chatBean ->
            getMessageList() { list, timSdkException ->
                timSdkException?.let {
                    L.w(timSdkException)
                }
                list?.let {
                    //消息已读回执
                    limitReadReport(chatBean.chatId, chatBean.isGroup)

                    showLoadingItem(false)
                    _addMessageItem(it.toDslAdapterItemList(chatFragment), true)
                }
            }
        }
    }

    open fun loadHistoryMessage(getType: Int = V2TIM_GET_CLOUD_OLDER_MSG) {
        if (_isLoading) {
            return
        }
        showLoadingItem()

        _delay(160) {
            chatBean?.let { chatBean ->
                getMessageList(_adapter?.firstMessageInfoBean()?.message) { list, timSdkException ->
                    timSdkException?.let {
                        L.w(timSdkException)
                    }
                    list?.let {
                        //消息已读回执
                        limitReadReport(chatBean.chatId, chatBean.isGroup)

                        showLoadingItem(false)
                        _addMessageItem(it.toDslAdapterItemList(chatFragment), false, 1)
                    }
                }
            }
        }
    }

    /**发送输入框的消息*/
    open fun sendInputMessage(text: CharSequence?) {
        if (text.isNullOrEmpty()) {
            return
        }
        sendMessage(TimMessage.textMessageBean(text.str()))
    }

    /**显示加载消息item*/
    fun showLoadingItem(loading: Boolean = true) {
        _isLoading = loading
        _adapter?.changeDataItems {
            val item = it.firstOrNull()
            if (item == null || item !is ChatLoadingItem) {
                it.add(0, ChatLoadingItem().apply {
                    isLoading = loading
                })
            } else {
                item.isLoading = loading
            }
        }
    }

    /**获取消息列表*/
    fun getMessageList(
        lastMsg: V2TIMMessage? = null,
        callback: (List<V2TIMMessage>?, TimSdkException?) -> Unit
    ) {
        chatBean?.let { chatBean ->
            if (chatBean.isGroup) {
                TimMessage.getGroupHistoryMessageList(
                    chatBean.chatId,
                    lastMsg,
                    callback = callback
                )
            } else {
                TimMessage.getC2CHistoryMessageList(
                    chatBean.chatId,
                    lastMsg,
                    callback = callback
                )
            }
        }
    }

    //</editor-fold desc="操作">

    //<editor-fold desc="消息">

    /**发送图片或视频消息*/
    fun sendImageOrVideoMessage(path: String?) {
        if (path.isNullOrEmpty() || !path.isFileExist()) {
            return
        }
        val type = path.mimeType()
        if (type.isNullOrEmpty()) {
            return
        }
        if (type.isImageMimeType()) {
            sendMessage(TimMessage.imageMessageBean(path))
        } else if (type.isVideoMimeType()) {
            sendMessage(TimMessage.videoMessageBean(path))
        }
    }

    fun sendSoundMessage(path: String?, duration: Int) {
        if (path.isNullOrEmpty() || !path.isFileExist()) {
            return
        }
        sendMessage(TimMessage.soundMessageBean(path, duration))
    }

    /**发送消息*/
    fun sendMessage(info: MessageInfoBean?, retry: Boolean = false) {
        chatFragment?.chatInfoBean?.let { chatBean ->
            sendMessage(chatBean, info, retry)
        }
    }

    /**发送消息
     * [retry] 是否是重新发送消息, 如果是重新发送的消息, 那么消息item, 会被移除并重新添加到尾部*/
    fun sendMessage(chatInfo: ChatInfoBean, info: MessageInfoBean?, retry: Boolean = false) {
        if (info == null || info.status == MSG_STATUS_SENDING || info.message == null) {
            //消息正在发送中
            return
        }

        //离线推送信息构建
        val containerBean = OfflineMessageContainerBean()
        val entity = OfflineMessageBean()
        entity.content = info.content
        entity.sender = info.fromUser
        entity.title = chatInfo.chatTitle
        entity.faceUrl = vmApp<ChatModel>().selfFaceUrlData.value //头像地址
        containerBean.entity = entity

        if (chatInfo.isGroup) {
            entity.chatType = V2TIMConversation.V2TIM_GROUP
            entity.sender = chatInfo.chatId
        }

        val v2TIMOfflinePushInfo = V2TIMOfflinePushInfo()
        v2TIMOfflinePushInfo.ext = containerBean.toJson()?.toByteArray()
        // OPPO必须设置ChannelID才可以收到推送消息，这个channelID需要和控制台一致
        v2TIMOfflinePushInfo.setAndroidOPPOChannelID("default")

        //发送消息
        info.messageId = sendMessage(
            chatInfo,
            info.message!!,
            offlinePushInfo = v2TIMOfflinePushInfo
        ) { v2TIMMessage, timSdkException, progress ->
            v2TIMMessage?.let {
                info.status = MSG_STATUS_SEND_SUCCESS
                info.timestamp = it.timestamp * 1000
                info.findDslAdapterItem(_adapter)?.updateAdapterItem()
            }
            timSdkException?.let {
                info.status = MSG_STATUS_SEND_FAIL
                info.findDslAdapterItem(_adapter)?.updateAdapterItem()
            }
        }

        //添加到界面
        if (info.msgType < V2TIMMessage.V2TIM_ELEM_TYPE_GROUP_TIPS || info.msgType > MSG_STATUS_REVOKE) {
            info.status = MSG_STATUS_SENDING //消息状态
            if (retry) {
                info.findDslAdapterItem(_adapter)?.let { item ->
                    item.messageInfoBean = info
                    _adapter?.changeDataItems {
                        it.moveToLast(item)
                    }
                }
            } else {
                _addMessageItem(info.toDslAdapterItem(chatFragment), true)
            }
        }
    }

    /**
     * 发送高级消息（高级版本：可以指定优先级，推送信息等特性）
     *
     *
     * [message]待发送的消息对象，需要通过对应的 createXXXMessage 接口进行创建。
     * [receiver]消息接收者的 userID, 如果是发送 C2C 单聊消息，只需要指定 receiver 即可。
     * [groupID]目标群组 ID，如果是发送群聊消息，只需要指定 groupID 即可。
     * [priority]消息优先级，仅针对群聊消息有效。请把重要消息设置为高优先级（比如红包、礼物消息），高频且不重要的消息设置为低优先级（比如点赞消息）。
     * [onlineUserOnly]是否只有在线用户才能收到，如果设置为 true ，接收方历史消息拉取不到，常被用于实现“对方正在输入”或群组里的非重要提示等弱提示功能，该字段不支持 AVChatRoom。
     * [offlinePushInfo]离线推送时携带的标题和内容。
     *
     * https://im.sdk.qcloud.com/doc/zh-cn/classcom_1_1tencent_1_1imsdk_1_1v2_1_1V2TIMMessageManager.html#a28e01403acd422e53e999f21ec064795
     *
     * @return 消息唯一标识
     * */
    fun sendMessage(
        chatInfo: ChatInfoBean,
        message: V2TIMMessage,
        priority: Int = V2TIMMessage.V2TIM_PRIORITY_DEFAULT,
        onlineUserOnly: Boolean = false,
        offlinePushInfo: V2TIMOfflinePushInfo? = null,
        callback: (V2TIMMessage?, TimSdkException?, Int) -> Unit
    ): String {
        //消息是否不计入会话未读数：默认为 false
        //message.isExcludedFromUnreadCount = false

        //获取消息是否不计入会话 lastMessage
        //message.isExcludedFromLastMessage = false

        val userId: String?
        val groupId: String?
        if (chatInfo.isGroup) {
            userId = null
            groupId = chatInfo.chatId
        } else {
            userId = chatInfo.chatId
            groupId = null
        }

        return messageManager.sendMessage(message,
            userId,
            groupId,
            priority,
            onlineUserOnly,
            offlinePushInfo,
            object : V2TIMSendCallback<V2TIMMessage> {

                override fun onSuccess(message: V2TIMMessage) {
                    L.d("消息发送成功:${message.msgID}")
                    callback(message, null, 100)
                }

                override fun onError(code: Int, desc: String?) {
                    L.d("消息发送失败:${code}:${desc}")
                    callback(null, TimSdkException(code, desc), -1)
                }

                override fun onProgress(progress: Int) {
                    L.d("消息发送中:${progress}")
                    callback(null, null, progress)
                }
            })
    }

    /**监听消息变化*/
    fun listenerMessage() {
        chatFragment?.let { fragment ->
            val chatBean = fragment.chatInfoBean
            if (chatBean?.chatId != null) {
                vmApp<ChatModel>().newMessageData.observe(fragment) { timMessage ->
                    if (timMessage != null) {
                        if ((chatBean.isGroup && timMessage.groupID == chatBean.chatId) || timMessage.userID == chatBean.chatId) {
                            //界面需要处理的消息
                            val message = timMessage.toMessageInfoBean()
                            if (fragment.isFragmentShow) {
                                //消息已读回执
                                limitReadReport(chatBean.chatId, chatBean.isGroup)
                            }
                            _addMessageItem(
                                message?.toDslAdapterItem(fragment),
                                _recycler?.isLastItemVisibleCompleted() == true
                            )
                        }
                    }
                }
            }
        }
    }

    /**
     * 收到消息上报已读加频率限制
     * @param chatId 如果是 C2C 消息， chatId 是 userId, 如果是 Group 消息 chatId 是 groupId
     * @param isGroup 是否为 Group 消息
     */
    fun limitReadReport(chatId: String?, isGroup: Boolean) {
        if (chatId.isNullOrEmpty()) {
            return
        }
        val currentTime = System.currentTimeMillis()
        val timeDifference: Long = currentTime - _lastReadReportTime
        if (timeDifference >= READ_REPORT_INTERVAL) {
            readReport(chatId, isGroup)
            _lastReadReportTime = currentTime
        } else {
            if (!_canReadReport) {
                return
            }
            val delay: Long = READ_REPORT_INTERVAL - timeDifference
            _canReadReport = false
            _delay(delay) {
                readReport(chatId, isGroup)
                _lastReadReportTime = System.currentTimeMillis()
                _canReadReport = true
            }
        }
    }

    fun readReport(
        chatId: String,
        isGroup: Boolean,
        callback: ((TimSdkException?) -> Unit)? = null
    ) {
        val listener = object : V2TIMCallback {
            override fun onError(code: Int, desc: String) {
                L.w("设置已读失败:$desc")
                callback?.invoke(TimSdkException(code, desc))
            }

            override fun onSuccess() {
                callback?.invoke(null)
            }
        }
        if (!isGroup) {
            messageManager.markC2CMessageAsRead(chatId, listener)
        } else {
            messageManager.markGroupMessageAsRead(chatId, listener)
        }
    }

    //</editor-fold desc="消息">

    //<editor-fold desc="内部操作">

    /**向界面中添加一个item,并且滚动到底部*/
    fun _addMessageItem(item: DslAdapterItem?, scrollToEnd: Boolean) {
        item?.let {
            _adapter?.apply {
                changeDataItems {
                    it.add(item)
                }
                if (scrollToEnd) {
                    onDispatchUpdatesOnce {
                        _scrollToLast()
                    }
                }
            }
        }
    }

    fun _addMessageItem(list: List<DslAdapterItem>?, scrollToEnd: Boolean, index: Int = -1) {
        if (list.isNullOrEmpty()) {
            return
        }
        _adapter?.apply {
            val filterParams = _defaultFilterParams()
            if (index != -1) {
                filterParams.onDispatchUpdatesTo = { diffResult, diffList ->
                    //如果使用dispatchUpdatesTo, 列表顶部会从0的位置开始布局, 并把原来的布局挤掉
                    //如果使用notifyItemRangeInserted, 则原来的布局不会动, 列表会从0的位置, 从上布局
                    notifyItemRangeInserted(0, list.size)
                }
            }
            changeDataItems(filterParams) {
                if (index == -1) {
                    //尾部追加
                    it.addAll(list)
                } else {
                    it.addAll(index, list)
                }
            }
            if (scrollToEnd) {
                onDispatchUpdatesOnce {
                    _scrollToLast()
                }
            }
        }
    }

    /**语音输入布局控制*/
    fun _switchVoiceInput(voiceInput: Boolean, showSoftInput: Boolean = true) {
        chatFragment?._vh?.apply {
            val editText = inputEditText

            if (voiceInput) {
                //切换到语音输入
                img(R.id.chat_voice_view)?.setImageResource(R.drawable.ic_chat_keyboard)
                visible(R.id.chat_voice_input)
                gone(R.id.chat_edit_text)
                gone(R.id.chat_send_button) //发送按钮

                editText?.hideSoftInput()
            } else {
                //切换到文本输入
                img(R.id.chat_voice_view)?.setImageResource(R.drawable.ic_chat_voice)
                gone(R.id.chat_voice_input)
                visible(R.id.chat_edit_text)
                visible(R.id.chat_send_button, inputEditText.string().isNotEmpty()) //发送按钮

                if (showSoftInput) {
                    editText?.setSelectionLast()
                    editText?.showSoftInput()
                }
            }
            isVoiceInput = voiceInput
        }
    }

    fun _scrollToLast() {
        _recycler?.scrollHelper?.lockScrollToLast {
            firstScrollAnim = false
            lockDuration = 200
        }
    }

    /**关闭表情布局*/
    fun onCloseEmojiLayout() {

    }

    /**点击表情时回调*/
    fun onEmojiClick(emoji: Emoji) {
        inputEditText?.apply {
            val index: Int = selectionStart
            val editable = text
            editable.insert(index, emoji.filter)
            FaceManager.handlerEmojiText(this, editable.toString(), true)
        }
    }

    /**表情上的删除按钮*/
    fun onEmojiDelete() {
        inputEditText?.apply {
            sendDelKey()
            /*val index: Int = selectionStart
            val editable = text
            var isFace = false
            if (index <= 0) {
                return
            }
            if (editable[index - 1] == ']') {
                for (i in index - 2 downTo 0) {
                    if (editable[i] == '[') {
                        val faceChar = editable.subSequence(i, index).toString()
                        if (FaceManager.isFaceChar(faceChar)) {
                            editable.delete(i, index)
                            isFace = true
                        }
                        break
                    }
                }
            }
            if (!isFace) {
                editable.delete(index - 1, index)
            }*/
        }
    }

    var emojiLayout: View? = null

    /**显示emoji布局*/
    fun _showEmojiLayout() {
        if (emojiLayout?.parent != null) {
            return
        }
        chatFragment?._vh?.group(R.id.lib_emoji_layout)?.apply {
            removeView(moreLayout)
            if (emojiLayout == null) {
                replace(R.layout.lib_chat_face_layout).apply {
                    emojiLayout = this
                    moreLayout = null

                    find<DslRecyclerView>(R.id.lib_recycler_view)?.initDslAdapter {
                        FaceManager.emojiList.forEach { emoji ->
                            ChatEmojiItem()() {
                                itemEmoji = emoji
                                itemClick = {
                                    onEmojiClick(emoji)
                                }
                            }
                        }
                    }
                    //删除
                    find<View>(R.id.lib_delete_view)?.clickIt {
                        onEmojiDelete()
                    }
                }
            } else {
                addView(emojiLayout)
            }
        }
    }

    var moreLayout: View? = null

    /**显示更多布局*/
    fun _showMoreLayout() {
        if (moreLayout?.parent != null) {
            return
        }
        chatFragment?._vh?.group(R.id.lib_emoji_layout)?.apply {
            removeView(emojiLayout)
            if (moreLayout == null) {
                replace(R.layout.lib_chat_more_layout).apply {
                    moreLayout = this
                    emojiLayout = null

                    find<DslRecyclerView>(R.id.lib_recycler_view)?.initDslAdapter {
                        moreActionList.forEach {
                            ChatMoreActionItem()() {
                                moreActionBean = it
                                itemClick = it.action
                            }
                        }
                    }
                }
            } else {
                addView(moreLayout)
            }
        }
    }

    //</editor-fold desc="内部操作">
}