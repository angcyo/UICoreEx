package com.angcyo.agora.rtc

import io.agora.rtc.IRtcEngineEventHandlerEx
import io.agora.rtc.models.UserInfo

/**
 * 回调并非在主线程执行
 * Email:angcyo@126.com
 * @author angcyo
 * @date 2020/02/06
 */

open class RtcEngineEventHandler : IRtcEngineEventHandlerEx() {

    override fun onError(err: Int) {
        super.onError(err)
    }

    override fun onWarning(warn: Int) {
        super.onWarning(warn)
    }

    //<editor-fold desc="首帧回调">

    //本地音频第一帧解码
    override fun onFirstLocalAudioFrame(elapsed: Int) {
        super.onFirstLocalAudioFrame(elapsed)
    }

    //远程用户音频第一帧解码
    override fun onFirstRemoteAudioFrame(uid: Int, elapsed: Int) {
        super.onFirstRemoteAudioFrame(uid, elapsed)
    }

    override fun onFirstRemoteVideoFrame(uid: Int, width: Int, height: Int, elapsed: Int) {
        super.onFirstRemoteVideoFrame(uid, width, height, elapsed)
    }

    override fun onFirstLocalVideoFrame(width: Int, height: Int, elapsed: Int) {
        super.onFirstLocalVideoFrame(width, height, elapsed)
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
    }

    /**在网络状况不理想的情况下，客户端可能会与 Agora 的服务器失去连接；SDK 会自动尝试重连，重连成功后，本地会触发*/
    override fun onRejoinChannelSuccess(channel: String?, uid: Int, elapsed: Int) {
        super.onRejoinChannelSuccess(channel, uid, elapsed)
    }

    override fun onLeaveChannel(stats: RtcStats?) {
        super.onLeaveChannel(stats)
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
    }

    /**远端主播离开频道或掉线时，会触发该回调。*/
    override fun onUserOffline(uid: Int, reason: Int) {
        super.onUserOffline(uid, reason)
    }

    override fun onUserMuteAudio(uid: Int, muted: Boolean) {
        super.onUserMuteAudio(uid, muted)
    }

    override fun onUserEnableVideo(uid: Int, enabled: Boolean) {
        super.onUserEnableVideo(uid, enabled)
    }

    override fun onUserInfoUpdated(uid: Int, userInfo: UserInfo?) {
        super.onUserInfoUpdated(uid, userInfo)
    }

    //</editor-fold desc="用户事件回调">

    //<editor-fold desc="数据统计回调">

    override fun onLocalVideoStat(sentBitrate: Int, sentFrameRate: Int) {
        super.onLocalVideoStat(sentBitrate, sentFrameRate)
    }

    override fun onRemoteVideoStat(
        uid: Int,
        delay: Int,
        receivedBitrate: Int,
        receivedFrameRate: Int
    ) {
        super.onRemoteVideoStat(uid, delay, receivedBitrate, receivedFrameRate)
    }

    override fun onLocalAudioStats(stats: LocalAudioStats?) {
        super.onLocalAudioStats(stats)
    }

    override fun onRtcStats(stats: RtcStats?) {
        super.onRtcStats(stats)
    }

    override fun onRemoteAudioStats(stats: RemoteAudioStats?) {
        super.onRemoteAudioStats(stats)
    }

    override fun onLocalVideoStats(stats: LocalVideoStats?) {
        super.onLocalVideoStats(stats)
    }

    override fun onRemoteVideoStats(stats: RemoteVideoStats?) {
        super.onRemoteVideoStats(stats)
    }

    //</editor-fold desc="数据统计回调">

    //<editor-fold desc="质量回调">

    override fun onAudioQuality(uid: Int, quality: Int, delay: Short, lost: Short) {
        super.onAudioQuality(uid, quality, delay, lost)
    }

    override fun onLastmileQuality(quality: Int) {
        super.onLastmileQuality(quality)
    }

    override fun onVideoTransportQuality(uid: Int, bitrate: Int, delay: Short, lost: Short) {
        super.onVideoTransportQuality(uid, bitrate, delay, lost)
    }

    override fun onAudioTransportQuality(uid: Int, bitrate: Int, delay: Short, lost: Short) {
        super.onAudioTransportQuality(uid, bitrate, delay, lost)
    }

    override fun onNetworkQuality(uid: Int, txQuality: Int, rxQuality: Int) {
        super.onNetworkQuality(uid, txQuality, rxQuality)
    }

    //</editor-fold desc="质量回调">
}