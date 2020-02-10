package com.angcyo.agora.rtc

import android.content.Context
import android.view.View
import android.view.ViewGroup
import com.angcyo.agora.Agora
import io.agora.rtc.RtcEngine
import io.agora.rtc.video.VideoCanvas


/**
 *
 * 1.初始化声网引擎
 * [com.angcyo.agora.rtc.DslAgoraRtc.initRtc]
 *
 * 2.检查权限
 * [com.angcyo.agora.AgoraPermission.requestCheckPermission]
 *
 * 3.加入频道
 * [com.angcyo.agora.rtc.DslAgoraRtc.joinChannel]
 *
 * 4.显示视频(本地和远程)
 * [com.angcyo.agora.rtc.DslAgoraRtc.showLocalVideoTo]
 * [com.angcyo.agora.rtc.DslAgoraRtc.showRemoteVideoTo]
 *
 * 5.释放资源
 * [com.angcyo.agora.rtc.DslAgoraRtc.leaveChannel]
 * [com.angcyo.agora.rtc.DslAgoraRtc.release]
 *
 * 声网 音视频通话, 互动直播
 * https://docs.agora.io/cn
 *
 * Email:angcyo@126.com
 * @author angcyo
 * @date 2020/02/06
 */
object DslAgoraRtc {

    //声网引擎
    var _rtcEngine: RtcEngine? = null

    /**初始化声网*/
    fun initRtc(
        context: Context,
        appId: String,
        agoraConfig: AgoraConfig = AgoraConfig(),
        config: RtcEngine.() -> Unit = {}
    ) {
        release()
        _rtcEngine =
            RtcEngine.create(
                context.applicationContext,
                appId,
                EngineEventDispatch()
            )
        _rtcEngine?.apply {
            agoraConfig.initRtc(this)
            config()
        }
        Agora.appId = appId
        Agora.context = context.applicationContext
    }

    /**离开频道*/
    fun leaveChannel() {
        _rtcEngine?.leaveChannel()
    }

    /**释放资源*/
    fun release() {
        leaveChannel()
        RtcEngine.destroy()
        _rtcEngine = null
    }

    //<editor-fold desc="常用操作">

    /**显示本地视频流*/
    fun showLocalVideoTo(viewGroup: ViewGroup, userId: Int) {
        _rtcEngine?.run {
            val findViewWithTag = viewGroup.findViewWithTag<View>(userId)
            if (findViewWithTag == null) {
                val surfaceView = RtcEngine.CreateRendererView(viewGroup.context)
                surfaceView.setZOrderMediaOverlay(true)
                viewGroup.addView(surfaceView, -1, -1)
                surfaceView.tag = userId
                setupLocalVideo(VideoCanvas(surfaceView, VideoCanvas.RENDER_MODE_FIT, userId))
                startPreview()
            }
        }
    }

    /**显示远程视频流*/
    fun showRemoteVideoTo(viewGroup: ViewGroup, userId: Int) {
        _rtcEngine?.run {
            val findViewWithTag = viewGroup.findViewWithTag<View>(userId)
            if (findViewWithTag == null) {
                val surfaceView = RtcEngine.CreateRendererView(viewGroup.context)
                surfaceView.setZOrderMediaOverlay(true)
                viewGroup.addView(surfaceView, -1, -1)
                surfaceView.tag = userId
                setupRemoteVideo(VideoCanvas(surfaceView, VideoCanvas.RENDER_MODE_FIT, userId))
            }
        }
    }

    /**
     * 加入频道, 如果是[CHANNEL_PROFILE_LIVE_BROADCASTING]直播模式,
     * 主播在加入频道之前需要设置角色[setClientRole(CLIENT_ROLE_BROADCASTER)]
     * */
    fun joinChannel(
        userId: Int,
        channelName: String,
        token: String? = null,
        optionalInfo: String? = null
    ) {
        // 如果频道中有 Web SDK，调用该方法开启 Native SDK 和 Web SDK 互通。
        //_rtcEngine?.enableWebSdkInteroperability(true)

        //相同appId的相同channelName的用户, 就会进行通讯.
        _rtcEngine?.apply {
            //val uid = registerLocalUserAccount(appId, userAccount)

            /*
            （非必选项）用户 ID，32 位无符号整数。建议设置范围：1 到 (2^32-1)，并保证唯一性。
            如果不指定（即设为 0），SDK 会自动分配一个，并在 onJoinChannelSuccess 回调方法中返回，
            App 层必须记住该返回值并维护，SDK 不对该返回值进行维护。uid 在 SDK 内部用 32 位无符号整数表示，
            由于 Java 不支持无符号整数，uid 被当成 32 位有符号整数处理，
            对于过大的整数，Java 会表示为负数，如有需要可以用 (uid&0xffffffffL)
            转换成 64 位整数*/
            joinChannel(token, channelName, optionalInfo, userId)
        }

    }

    //</editor-fold desc="常用操作">


//    fun test() {
//        //通道 直播
//        _rtcEngine?.setChannelProfile(Constants.CHANNEL_PROFILE_LIVE_BROADCASTING)
//        //进入频道, 客户端的身份
//        _rtcEngine?.setClientRole(Constants.CLIENT_ROLE_AUDIENCE)//观众
//        _rtcEngine?.setClientRole(Constants.CLIENT_ROLE_BROADCASTER)//主播
//    }
}