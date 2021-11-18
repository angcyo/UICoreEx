package com.angcyo.tim.dslitem

import android.graphics.drawable.AnimationDrawable
import android.view.Gravity
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import com.angcyo.library.L
import com.angcyo.library.ex.dpi
import com.angcyo.library.ex.isFileExist
import com.angcyo.library.toastQQ
import com.angcyo.media.audio.AudioPlayerHelper
import com.angcyo.tim.R
import com.angcyo.tim.bean.MessageInfoBean
import com.angcyo.tim.bean.isSelf
import com.angcyo.tim.bean.soundElem
import com.angcyo.tim.util.TimConfig
import com.angcyo.tim.util.TimConfig.AUDIO_MESSAGE_MAX_WIDTH
import com.angcyo.tim.util.TimConfig.AUDIO_MESSAGE_MIN_WIDTH
import com.angcyo.widget.DslViewHolder
import com.angcyo.widget.base.adjustOrder
import com.angcyo.widget.base.setWidth
import com.tencent.imsdk.v2.V2TIMDownloadCallback
import com.tencent.imsdk.v2.V2TIMElem
import com.tencent.imsdk.v2.V2TIMSoundElem
import kotlin.math.max
import kotlin.math.min

/**
 * 语音消息item
 * Email:angcyo@126.com
 * @author angcyo
 * @date 2021/11/17
 * Copyright (c) 2020 ShenZhen Wayto Ltd. All rights reserved.
 */
class MsgAudioItem : BaseChatMsgItem() {

    companion object {
        //音频未读
        const val UNREAD = 0

        //音频已读
        const val READ = 1
    }

    init {
        msgContentLayoutId = R.layout.msg_audio_layout
    }

    override fun bindMsgContent(itemHolder: DslViewHolder, itemPosition: Int, payloads: List<Any>) {
        super.bindMsgContent(itemHolder, itemPosition, payloads)

        messageInfoBean?.let { bean ->
            val audioLayout = itemHolder.v<LinearLayout>(R.id.msg_audio_content_layout)
            val audioTimeView = itemHolder.v<TextView>(R.id.msg_audio_time_view)
            val audioPlayView = itemHolder.v<ImageView>(R.id.msg_audio_play_view)

            //音频路径
            val soundPath = bean.dataUri

            if (!AudioPlayerHelper.isPlaying(soundPath)) {
                audioPlayView?.setImageResource(R.mipmap.voice_msg_playing_3)
            }

            if (bean.isSelf) {
                audioPlayView?.rotation = 180f
                audioLayout?.gravity = Gravity.RIGHT
                audioLayout?.adjustOrder(audioTimeView, audioPlayView)
            } else {
                audioLayout?.gravity = Gravity.LEFT
                audioPlayView?.rotation = 0f
                audioLayout?.adjustOrder(audioPlayView, audioTimeView)

                //音频未读提示
                itemHolder.visible(
                    R.id.msg_audio_unread_view,
                    bean.message?.localCustomInt == UNREAD
                )
            }

            //音频
            bean.soundElem?.let { element ->
                val duration = max(element.duration, 1)

                audioTimeView?.text = "$duration″"

                //布局的宽度
                val width =
                    min(AUDIO_MESSAGE_MIN_WIDTH + (duration * 6) * dpi, AUDIO_MESSAGE_MAX_WIDTH)
                audioLayout?.setWidth(width)

                //下载文件
                downloadSound(bean, element)

                itemHolder.click(R.id.msg_content_layout) {
                    //消息内容点击, 播放音频
                    if (soundPath.isNullOrEmpty()) {
                        toastQQ("音频异常, 无法播放")
                    } else if (AudioPlayerHelper.isPlaying()) {
                        AudioPlayerHelper.stop()
                    } else {
                        audioPlayView?.apply {
                            setImageResource(R.drawable.play_voice_message)
                            (drawable as? AnimationDrawable)?.start()
                        }
                        bean.message?.localCustomInt = READ //已读
                        itemHolder.gone(R.id.msg_audio_unread_view)

                        //播放结束
                        AudioPlayerHelper.onPlayEnd { i, playerException ->
                            audioPlayView?.apply {
                                //恢复图标
                                (drawable as? AnimationDrawable)?.stop()
                                setImageResource(R.mipmap.voice_msg_playing_3)
                            }
                        }
                        //开始播放
                        AudioPlayerHelper.play(soundPath)
                    }
                }
            }
        }
    }

    override fun onItemViewRecycled(itemHolder: DslViewHolder, itemPosition: Int) {
        super.onItemViewRecycled(itemHolder, itemPosition)
        AudioPlayerHelper.stop()
        AudioPlayerHelper.clearListener()
    }

    fun downloadSound(bean: MessageInfoBean, element: V2TIMSoundElem) {
        if (element.uuid.isNullOrEmpty()) {
            return
        }
        val path: String = TimConfig.getRecordDownloadDir(element.uuid)
        if (!path.isFileExist()) {
            element.downloadSound(path, object : V2TIMDownloadCallback {
                override fun onProgress(progressInfo: V2TIMElem.V2ProgressInfo) {
                    val currentSize = progressInfo.currentSize
                    val totalSize = progressInfo.totalSize
                    var progress = 0
                    if (totalSize > 0) {
                        progress = (100 * currentSize / totalSize).toInt()
                    }
                    if (progress > 100) {
                        progress = 100
                    }
                    L.i("下载音频progress:$progress")
                }

                override fun onError(code: Int, desc: String) {
                    L.e("下载音频失败:$code:$desc")
                }

                override fun onSuccess() {
                    bean.dataPath = path
                    bean.dataUri = path
                }
            })
        } else {
            bean.dataPath = path
            bean.dataUri = path
        }
    }
}