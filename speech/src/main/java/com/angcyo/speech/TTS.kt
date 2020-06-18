package com.angcyo.speech

import android.content.Context
import android.media.AudioManager
import android.media.AudioManager.OnAudioFocusChangeListener
import android.util.Log
import com.tencent.qcloudtts.LongTextTTS.LongTextTtsController
import com.tencent.qcloudtts.callback.QCloudPlayerCallback
import com.tencent.qcloudtts.callback.TtsExceptionHandler
import com.tencent.qcloudtts.exception.TtsNotInitializedException

/**
 * 腾讯tts
 *
 * https://cloud.tencent.com/document/product/1073/37929
 *
 * Email:angcyo@126.com
 * @author angcyo
 * @date 2020/06/17
 * Copyright (c) 2020 ShenZhen Wayto Ltd. All rights reserved.
 */

object TTS {

    const val TAG = "TTS"

    var isInitSuccess = false

    private var longTextTtsController: LongTextTtsController? = null
    private var listener: OnAudioFocusChangeListener? = null

    private val ttsExceptionHandler = TtsExceptionHandler { e ->
        Log.e(TAG, "tts onRequestException :" + e.message)
        //网络出错的时候
        longTextTtsController?.pause()
    }

    /**初始化入口*/
    fun init(
        context: Context,
        appId: Long,
        secretId: String,
        secretKey: String,
        debug: Boolean = BuildConfig.DEBUG
    ) {

        if (isInitSuccess) {
            return
        }

        //构造LongTextTtsController，支持长文本播放，可暂停/恢复播放。非流式api，故建议文本中第一句话不要设的太长
        longTextTtsController = LongTextTtsController()

        /* 在使用云API之前，请前往 腾讯云API密钥页面 申请安全凭证。 安全凭证包括 SecretId 和 SecretKey：
         * SecretId 用于标识 API 调用者身份
         * SecretKey 用于加密签名字符串和服务器端验证签名字符串的密钥。
         */

        //注意：这里只是示例，请根据用户实际申请的 SecretId 和 SecretKey 进行后续操作！
        longTextTtsController!!.init(context.applicationContext, appId, secretId, secretKey)

        initParams()

        requestAudioFocus(context.applicationContext)

        isInitSuccess = true
    }

    private fun requestAudioFocus(context: Context) {
        //初始化audio mananger
        listener = OnAudioFocusChangeListener { focusChange ->
            if (focusChange == AudioManager.AUDIOFOCUS_LOSS) {
                //丢失焦点，直接
                longTextTtsController?.stop()
            } else if (focusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT) {
                //丢失焦点，但是马上又能恢复
            } else if (focusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK) {
                //降低音量
            } else if (focusChange == AudioManager.AUDIOFOCUS_GAIN) {
                //获得了音频焦点
            }
        }

        //设置listener
        val am = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager?
        am?.requestAudioFocus(listener, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN)
    }

    private fun abandonAudioFocus(context: Context) {
        val am = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager?
        am?.abandonAudioFocus(listener)
    }

    fun initParams() {
        configParams {
            //设置语速
            setVoiceSpeed(0)
            //设置音色
            setVoiceType(5)
            //设置语言
            setVoiceLanguage(1)
            //设置ProjectId
            setProjectId(0)
        }
    }

    /**语音合成配置参数
     * https://cloud.tencent.com/document/product/1073/37995
     * */
    fun configParams(action: LongTextTtsController.() -> Unit = {}) {
        longTextTtsController?.apply {
            action()
        }
    }

    /**开始播放文本转成的语音*/
    fun startSpeaking(text: String?) {
        if (isInitSuccess && !text.isNullOrEmpty()) {
            try {
                longTextTtsController?.startTts(text, ttsExceptionHandler,
                    object : QCloudPlayerCallback {
                        //播放开始
                        override fun onTTSPlayStart() {
                            Log.d(TAG, "onPlayStart")
                        }

                        //音频缓冲中
                        override fun onTTSPlayWait() {
                            Log.d(TAG, "onPlayWait")
                        }

                        //缓冲完成，继续播放
                        override fun onTTSPlayResume() {
                            Log.d(TAG, "onPlayResume")
                        }

                        //连续播放下一句
                        override fun onTTSPlayNext() {
                            Log.d(TAG, "onPlayNext")
                        }

                        //播放中止
                        override fun onTTSPlayStop() {
                            Log.d(TAG, "onPlayStop")
                        }

                        //播放结束
                        override fun onTTSPlayEnd() {
                            Log.d(TAG, "onPlayEnd")
                        }

                        //当前播放的字符,当前播放的字符在所在的句子中的下标.
                        override fun onTTSPlayProgress(
                            currentWord: String,
                            currentIndex: Int
                        ) {
                            Log.d(TAG, "onTTSPlayProgress$currentWord$currentIndex")
                        }
                    })
            } catch (e: TtsNotInitializedException) {
                Log.e(TAG, "TtsNotInitializedException e:" + e.message)
            }
        }
    }

//    /**合成语音文本*/
//    fun synthesizeToUri(text: String?, path: String, listener: SynthesizerListener? = null): Int {
//        return if (isInitSuccess && !text.isNullOrEmpty()) {
//            speechSynthesizer?.synthesizeToUri(text, path, listener) ?: -1
//        } else {
//            -1
//        }
//    }
}