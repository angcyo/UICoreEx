package com.angcyo.agora.rtc

import com.angcyo.library.L
import com.angcyo.library.ex.toElapsedTime
import io.agora.rtc.IRtcEngineEventHandlerEx
import io.agora.rtc.models.UserInfo
import java.util.concurrent.CopyOnWriteArrayList

/**
 * https://docs.agora.io/cn/Interactive%20Broadcast/API%20Reference/java/classio_1_1agora_1_1rtc_1_1_i_rtc_engine_event_handler.html
 *
 * 回调并非在主线程执行
 * Email:angcyo@126.com
 * @author angcyo
 * @date 2020/02/09
 */
class EngineEventDispatch : IRtcEngineEventHandlerEx() {

    companion object {
        private val listeners = CopyOnWriteArrayList<RtcEngineEventHandler>()

        fun addListener(listener: RtcEngineEventHandler) {
            listeners.add(listener)
        }

        fun removeListener(listener: RtcEngineEventHandler) {
            listeners.add(listener)
        }
    }

    /**
     * 错误码
     * https://docs.agora.io/cn/Interactive%20Broadcast/API%20Reference/java/classio_1_1agora_1_1rtc_1_1_i_rtc_engine_event_handler_1_1_error_code.html
     * */
    override fun onError(err: Int) {
        super.onError(err)
        L.e("声网错误码:", err)
        listeners.forEach {
            it.onError(err)
        }
    }

    /**发生警告回调。*/
    override fun onWarning(warn: Int) {
        super.onWarning(warn)
        L.w("声网警告:", warn)
        listeners.forEach {
            it.onWarning(warn)
        }
    }

    //<editor-fold desc="首帧回调">

    //本地音频第一帧解码
    override fun onFirstLocalAudioFrame(elapsed: Int) {
        super.onFirstLocalAudioFrame(elapsed)
        L.d("this...$elapsed")
        listeners.forEach {
            it.onFirstLocalAudioFrame(elapsed)
        }
    }

    /**
     * 远程用户音频第一帧解码
     * elapsed	从调用 joinChannel 方法直至该回调被触发的延迟（毫秒）
     */
    override fun onFirstRemoteAudioFrame(uid: Int, elapsed: Int) {
        super.onFirstRemoteAudioFrame(uid, elapsed)
        L.d("收到远程用户:$uid 的音频,耗时:$elapsed ms")
        listeners.forEach {
            it.onFirstRemoteAudioFrame(uid, elapsed)
        }
    }

    override fun onFirstRemoteVideoFrame(uid: Int, width: Int, height: Int, elapsed: Int) {
        super.onFirstRemoteVideoFrame(uid, width, height, elapsed)
        L.d("收到远程用户:$uid 的视频,${width}*${height} 耗时:$elapsed ms")
        listeners.forEach {
            it.onFirstRemoteVideoFrame(uid, width, height, elapsed)
        }
    }

    override fun onFirstLocalVideoFrame(width: Int, height: Int, elapsed: Int) {
        super.onFirstLocalVideoFrame(width, height, elapsed)
        L.d("this...$width $height $elapsed")
        listeners.forEach {
            it.onFirstLocalVideoFrame(width, height, elapsed)
        }
    }

    override fun onFirstRemoteAudioDecoded(uid: Int, elapsed: Int) {
        super.onFirstRemoteAudioDecoded(uid, elapsed)
    }

    override fun onFirstRemoteVideoDecoded(uid: Int, width: Int, height: Int, elapsed: Int) {
        super.onFirstRemoteVideoDecoded(uid, width, height, elapsed)
    }

    //</editor-fold desc="首帧回调">

    /**本地用户成功加入频道时，会触发该回调。*/
    override fun onJoinChannelSuccess(channel: String?, uid: Int, elapsed: Int) {
        super.onJoinChannelSuccess(channel, uid, elapsed)
        L.d("频道:", channel, " 用户:$uid 加入成功", " 耗时:", elapsed)
        listeners.forEach {
            it.onJoinChannelSuccess(channel, uid, elapsed)
        }
    }

    /**在网络状况不理想的情况下，客户端可能会与 Agora 的服务器失去连接；SDK 会自动尝试重连，重连成功后，本地会触发*/
    override fun onRejoinChannelSuccess(channel: String?, uid: Int, elapsed: Int) {
        super.onRejoinChannelSuccess(channel, uid, elapsed)
        L.d("频道:", channel, " 用户:$uid 重新加入成功", " 耗时:", elapsed)
        listeners.forEach {
            it.onRejoinChannelSuccess(channel, uid, elapsed)
        }
    }

    /**
     * 离开频道回调。
     * App 调用 leaveChannel 方法时，SDK 提示 App 离开频道成功。 在该回调方法中，App 可以得到此次通话的总通话时长、SDK 收发数据的流量等信息。
     * stats	通话相关的统计信息：RtcStats
     */
    override fun onLeaveChannel(stats: RtcStats) {
        super.onLeaveChannel(stats)
        L.d("离开频道:时长:${(stats.totalDuration * 1000L).toElapsedTime()}")
        listeners.forEach {
            it.onLeaveChannel(stats)
        }
    }

    //<editor-fold desc="用户事件回调">

    override fun onUserMuteVideo(uid: Int, muted: Boolean) {
        super.onUserMuteVideo(uid, muted)
    }

    override fun onUserEnableLocalVideo(uid: Int, enabled: Boolean) {
        super.onUserEnableLocalVideo(uid, enabled)
    }

    /**用户加入*/
    override fun onUserJoined(uid: Int, elapsed: Int) {
        super.onUserJoined(uid, elapsed)
        L.d("用户加入:", uid, " 耗时:", elapsed)
        listeners.forEach {
            it.onUserJoined(uid, elapsed)
        }
    }

    /**远端主播离开频道或掉线时，会触发该回调。*/
    override fun onUserOffline(uid: Int, reason: Int) {
        super.onUserOffline(uid, reason)
        L.d("用户离开:", uid, " 原因:", reason)
        listeners.forEach {
            it.onUserOffline(uid, reason)
        }
    }

    override fun onUserMuteAudio(uid: Int, muted: Boolean) {
        super.onUserMuteAudio(uid, muted)
        L.d("用户:$uid 静音:$muted")
    }

    override fun onUserEnableVideo(uid: Int, enabled: Boolean) {
        super.onUserEnableVideo(uid, enabled)
        L.d("用户:$uid 视频:$enabled")
    }

    override fun onUserInfoUpdated(uid: Int, userInfo: UserInfo?) {
        super.onUserInfoUpdated(uid, userInfo)
        L.d("用户更新:", uid, " ->", userInfo)
        listeners.forEach {
            it.onUserInfoUpdated(uid, userInfo)
        }
    }

    //</editor-fold desc="用户事件回调">

    //<editor-fold desc="数据统计回调">

    override fun onLocalVideoStat(sentBitrate: Int, sentFrameRate: Int) {
        super.onLocalVideoStat(sentBitrate, sentFrameRate)
        L.d("this...$sentBitrate $sentFrameRate")
    }

    override fun onRemoteVideoStat(
        uid: Int,
        delay: Int,
        receivedBitrate: Int,
        receivedFrameRate: Int
    ) {
        super.onRemoteVideoStat(uid, delay, receivedBitrate, receivedFrameRate)
        L.d("this...$uid $delay $receivedBitrate $receivedFrameRate")
    }

    override fun onLocalAudioStats(stats: LocalAudioStats) {
        super.onLocalAudioStats(stats)
        L.d("声道数:${stats.numChannels} ${stats.sentSampleRate}Hz ${stats.sentBitrate}Kbps")
    }

    /**当前通话统计回调。 该回调在通话中每两秒触发一次。*/
    override fun onRtcStats(stats: RtcStats) {
        super.onRtcStats(stats)
        L.v("${(stats.totalDuration * 1000L).toElapsedTime()} 发送:${stats.txBytes}bytes 接收:${stats.rxBytes}bytes")
    }

    override fun onRemoteAudioStats(stats: RemoteAudioStats) {
        super.onRemoteAudioStats(stats)
        L.v("远程音频统计 uid:${stats.uid} ${stats.quality.audioQualityStr()}")
    }

    /**
     * 通话中本地视频流的统计信息回调。
     * https://docs.agora.io/cn/Interactive%20Broadcast/API%20Reference/java/classio_1_1agora_1_1rtc_1_1_i_rtc_engine_event_handler_1_1_local_video_stats.html
     * */
    override fun onLocalVideoStats(stats: LocalVideoStats) {
        super.onLocalVideoStats(stats)
        L.d(buildString {
            append("${stats.sentBitrate}Kbps ")
            append("${stats.encoderOutputFrameRate}fps ")
            append("send:${stats.sentFrameRate}fps ")
            append("render:${stats.rendererOutputFrameRate}fps ")
            append("w:${stats.encodedFrameWidth} ")
            append("h:${stats.encodedFrameHeight} ")
            append("c:${stats.encodedFrameCount} ")
        })
    }

    override fun onRemoteVideoStats(stats: RemoteVideoStats) {
        super.onRemoteVideoStats(stats)
        L.d(buildString {
            append("uid:${stats.uid} ")
            append("${stats.width}*${stats.height} ")
            append("${stats.receivedBitrate} ")
            append("receive:${stats.decoderOutputFrameRate}fps ")
            append("render:${stats.rendererOutputFrameRate}fps ")
        })
    }

    //</editor-fold desc="数据统计回调">

    //<editor-fold desc="质量回调">

    override fun onAudioQuality(uid: Int, quality: Int, delay: Short, lost: Short) {
        super.onAudioQuality(uid, quality, delay, lost)
        L.d("this...$uid $delay")
    }

    override fun onLastmileQuality(quality: Int) {
        super.onLastmileQuality(quality)
        L.d("this...$quality")
    }

    override fun onVideoTransportQuality(uid: Int, bitrate: Int, delay: Short, lost: Short) {
        super.onVideoTransportQuality(uid, bitrate, delay, lost)
        L.d("this...$uid $delay")
    }

    override fun onAudioTransportQuality(uid: Int, bitrate: Int, delay: Short, lost: Short) {
        super.onAudioTransportQuality(uid, bitrate, delay, lost)
        L.d("this...$uid $delay")
    }

    /**
     *
     * uid	用户 ID。表示该回调报告的是持有该 ID 的用户的网络质量。当 uid 为 0 时，返回的是本地用户的网络质量。
     * txQuality	该用户的上行网络质量，基于上行视频的发送码率、上行丢包率、平均往返时延和网络抖动计算。该值代表当前的上行网络质量，帮助判断是否可以支持当前设置的视频编码属性。假设直播模式下上行码率是 1000 Kbps，那么支持 640 × 480 的分辨率、30 fps 的帧率没有问题，但是支持 1280 x 720 的分辨率就会有困难
     *      QUALITY_UNKNOWN(0)：质量未知
     *      QUALITY_EXCELLENT(1)：质量极好
     *      QUALITY_GOOD(2)：用户主观感觉和极好差不多，但码率可能略低于极好
     *      QUALITY_POOR(3)：用户主观感受有瑕疵但不影响沟通
     *      QUALITY_BAD(4)：勉强能沟通但不顺畅
     *      QUALITY_VBAD(5)：网络质量非常差，基本不能沟通
     *      QUALITY_DOWN(6)：网络连接断开，完全无法沟通
     *      QUALITY_DETECTING(8)：SDK 正在探测网络质量
     * rxQuality	该用户的下行网络质量，基于下行网络的丢包率、平均往返延时和网络抖动计算
     *      QUALITY_UNKNOWN(0)：质量未知
     *      QUALITY_EXCELLENT(1)：质量极好
     *      QUALITY_GOOD(2)：用户主观感觉和极好差不多，但码率可能略低于极好
     *      QUALITY_POOR(3)：用户主观感受有瑕疵但不影响沟通
     *      QUALITY_BAD(4)：勉强能沟通但不顺畅
     *      QUALITY_VBAD(5)：网络质量非常差，基本不能沟通
     *      QUALITY_DOWN(6)：网络连接断开，完全无法沟通
     *      QUALITY_DETECTING(8)：SDK 正在探测网络质量
     * */
    override fun onNetworkQuality(uid: Int, txQuality: Int, rxQuality: Int) {
        super.onNetworkQuality(uid, txQuality, rxQuality)
        L.v("网络质量:uid:$uid ${txQuality.txQualityStr()} ${rxQuality.rxQualityStr()}")
        listeners.forEach {
            it.onNetworkQuality(uid, txQuality, rxQuality)
        }
    }

    //</editor-fold desc="质量回调">
}

fun Int.txQualityStr(): String {
    return when (this) {
        1 -> "QUALITY_EXCELLENT(1)：质量极好"
        2 -> "QUALITY_GOOD(2)：用户主观感觉和极好差不多，但码率可能略低于极好"
        3 -> "QUALITY_POOR(3)：用户主观感受有瑕疵但不影响沟通"
        4 -> "QUALITY_BAD(4)：勉强能沟通但不顺畅"
        5 -> "QUALITY_VBAD(5)：网络质量非常差，基本不能沟通"
        6 -> "QUALITY_DOWN(6)：网络连接断开，完全无法沟通"
        8 -> "QUALITY_DETECTING(8)：SDK 正在探测网络质量"
        //" QUALITY_UNKNOWN(0)：质量未知"
        else -> "QUALITY_UNKNOWN(0)：质量未知"
    }
}

fun Int.rxQualityStr(): String {
    return when (this) {
        1 -> "QUALITY_EXCELLENT(1)：质量极好"
        2 -> "QUALITY_GOOD(2)：用户主观感觉和极好差不多，但码率可能略低于极好"
        3 -> "QUALITY_POOR(3)：用户主观感受有瑕疵但不影响沟通"
        4 -> "QUALITY_BAD(4)：勉强能沟通但不顺畅"
        5 -> "QUALITY_VBAD(5)：网络质量非常差，基本不能沟通"
        6 -> "QUALITY_DOWN(6)：网络连接断开，完全无法沟通"
        8 -> "QUALITY_DETECTING(8)：SDK 正在探测网络质量"
        //" QUALITY_UNKNOWN(0)：质量未知"
        else -> "QUALITY_UNKNOWN(0)：质量未知"
    }
}

fun Int.audioQualityStr(): String {
    return when (this) {
        1 -> "质量极好"
        2 -> "用户主观感觉和极好差不多 ，但码率可能略低于极好"
        3 -> "用户主观感受有瑕疵，但不影响沟通"
        4 -> "勉强能沟通但不顺畅"
        5 -> "网络质量非常差，基本不能沟通"
        6 -> "网络连接已断开，完全无法沟通"
        else -> "质量未知"
    }
}


fun RtcEngineEventHandler.addListener() {
    EngineEventDispatch.addListener(this)
}

fun RtcEngineEventHandler.removeListener() {
    EngineEventDispatch.removeListener(this)
}