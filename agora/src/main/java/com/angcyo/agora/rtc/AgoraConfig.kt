package com.angcyo.agora.rtc

import com.angcyo.library.utils.FileUtils
import com.angcyo.library.utils.fileName
import io.agora.rtc.Constants
import io.agora.rtc.RtcEngine
import io.agora.rtc.video.BeautyOptions
import io.agora.rtc.video.VideoEncoderConfiguration
import io.agora.rtm.RtmClient

/**
 * 声网配置数据类
 * Email:angcyo@126.com
 * @author angcyo
 * @date 2020/02/06
 */
data class AgoraConfig(

    //Wayto 声网 默认appid https://dashboard.agora.io/projects
    //APP 证书 9f944e8c86e****fa413c22001b95d4c****0428
    var appId: String? = "960644f3218845e5aef0ab9166256473",

    //本地日志文件
    var logFilePath: String? = FileUtils.appRootExternalFolderFile(
        folder = "agora",
        name = "agora" + fileName("yyyy-MM-dd", ".log")
    )?.absolutePath,

    //日志文件大小
    var logFileSize: Int = 10 * 1024, /*10mb*/

    //日志级别
    var logFileFilter: Int = Constants.LOG_FILTER_INFO,

    //激活美颜
    var enableBeauty: Boolean = true,

    //美颜参数
    var beautyOptions: BeautyOptions = BeautyOptions(
        1,
        0.7f,
        0.5f,
        0.1f
    )
)

fun AgoraConfig.initRtc(rtcEngine: RtcEngine) {
    rtcEngine.apply {
        //https://docs.agora.io/cn/Interactive%20Broadcast/APIReference/java/classio_1_1agora_1_1rtc_1_1_rtc_engine.html#a1bfb76eb4365b8b97648c3d1b69f2bd6
        //通讯模式:CHANNEL_PROFILE_COMMUNICATION
        //直播模式:CHANNEL_PROFILE_LIVE_BROADCASTING
        setChannelProfile(Constants.CHANNEL_PROFILE_COMMUNICATION)

        //一个 RtcEngine 只能使用一种频道模式。如果想切换为其他模式，需要先调用 destroy 方法释放当前的 RtcEngine 实例，然后使用 create 方法创建一个新实例，再调用 setChannelProfile 设置新的频道模式。

        setLogFile(logFilePath)
        setLogFileSize(logFileSize)
        setLogFilter(logFileFilter)

        //美颜
        setBeautyEffectOptions(enableBeauty, beautyOptions)

        //激活视频模块, 但是可以关闭视频流.
        //enableVideo()
        //enableAudio()

        //视频编码信息
        setVideoEncoderConfiguration(
            VideoEncoderConfiguration(
                VideoEncoderConfiguration.VD_1280x720,
                VideoEncoderConfiguration.FRAME_RATE.FRAME_RATE_FPS_24,
                VideoEncoderConfiguration.STANDARD_BITRATE,
                VideoEncoderConfiguration.ORIENTATION_MODE.ORIENTATION_MODE_FIXED_PORTRAIT
            )
        )
    }
}

fun AgoraConfig.initRtm(rtmClient: RtmClient) {
    rtmClient.apply {
        setLogFile(logFilePath)
        setLogFileSize(logFileSize)
        setLogFilter(logFileFilter)
    }
}