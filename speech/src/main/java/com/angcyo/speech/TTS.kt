package com.angcyo.speech

import android.content.Context
import android.os.Environment
import android.util.Log
import com.iflytek.cloud.*
import com.iflytek.cloud.msc.util.log.DebugLog

/**
 * Text To Speech 文本转语音
 *
 * https://www.xfyun.cn/doc/tts/offline_tts/Android-SDK.html#_2%E3%80%81sdk%E9%9B%86%E6%88%90%E6%8C%87%E5%8D%97
 *
 * 1: init
 * 2: startSpeaking
 * ...end
 *
 * Email:angcyo@126.com
 * @author angcyo
 * @date 2020/06/17
 * Copyright (c) 2020 ShenZhen Wayto Ltd. All rights reserved.
 */

object TTS {

    const val TAG = "TTS"

    var isInitSuccess = false

    // 语音合成对象
    private var speechSynthesizer: SpeechSynthesizer? = null

    /**初始化入口*/
    fun init(context: Context, appId: String, debug: Boolean = BuildConfig.DEBUG) {
        Setting.setShowLog(debug)
        DebugLog.setShowLog(debug)

        SpeechUtility.createUtility(context, "${SpeechConstant.APPID}=${appId}")

        // 初始化合成对象
        speechSynthesizer = SpeechSynthesizer.createSynthesizer(context) { code ->
            Log.d(TAG, "InitListener init() code = $code")
            isInitSuccess = code == ErrorCode.SUCCESS
            if (!isInitSuccess) {
                Log.e(TAG, "初始化失败,错误码：$code,请点击网址https://www.xfyun.cn/document/error-code查询解决方案")
            } else {
                // 初始化成功，之后可以调用startSpeaking方法
                // 注：有的开发者在onCreate方法中创建完合成对象之后马上就调用startSpeaking进行合成，
                // 正确的做法是将onCreate中的startSpeaking调用移至这里
            }
        }
    }

    /**语音合成配置参数*/
    fun initParams(engineType: String = SpeechConstant.TYPE_CLOUD) {
        speechSynthesizer?.apply {
            // 清空参数
            setParameter(SpeechConstant.PARAMS, null)
            // 根据合成引擎设置相应参数
            if (engineType == SpeechConstant.TYPE_CLOUD) {
                //在线合成
                setParameter(SpeechConstant.ENGINE_TYPE, SpeechConstant.TYPE_CLOUD)
                //支持实时音频返回，仅在synthesizeToUri条件下支持
                setParameter(SpeechConstant.TTS_DATA_NOTIFY, "1")
                //	setParameter(SpeechConstant.TTS_BUFFER_TIME,"1");

                // 设置在线合成发音人
                setParameter(SpeechConstant.VOICE_NAME, "xiaoyan")
                //设置合成语速
                setParameter(SpeechConstant.SPEED, "50")
                //设置合成音调
                setParameter(SpeechConstant.PITCH, "50")
                //设置合成音量
                setParameter(SpeechConstant.VOLUME, "50")
            } else {
                //离线合成
                setParameter(SpeechConstant.ENGINE_TYPE, SpeechConstant.TYPE_LOCAL)
                setParameter(SpeechConstant.VOICE_NAME, "")
            }

            //设置播放器音频流类型
            setParameter(SpeechConstant.STREAM_TYPE, "3")
            //设置播放合成音频打断音乐播放，默认为true
            setParameter(SpeechConstant.KEY_REQUEST_FOCUS, "false")

            // 设置音频保存路径，保存音频格式支持pcm、wav，设置路径为sd卡请注意WRITE_EXTERNAL_STORAGE权限
            //通过此参数设置合成音频文件格式，可选：PCM、WAV，默认值：PCM
            setParameter(SpeechConstant.AUDIO_FORMAT, "pcm")
            setParameter(
                SpeechConstant.TTS_AUDIO_PATH,
                Environment.getExternalStorageDirectory().toString() + "/msc/tts.pcm"
            )
        }
    }

    /**开始播放文本转成的语音*/
    fun startSpeaking(text: String?, listener: SynthesizerListener? = null) {
        if (isInitSuccess && !text.isNullOrEmpty()) {
            speechSynthesizer?.startSpeaking(text, listener)
        }
    }

    /**合成语音文本*/
    fun synthesizeToUri(text: String?, path: String, listener: SynthesizerListener? = null): Int {
        return if (isInitSuccess && !text.isNullOrEmpty()) {
            speechSynthesizer?.synthesizeToUri(text, path, listener) ?: -1
        } else {
            -1
        }
    }
}