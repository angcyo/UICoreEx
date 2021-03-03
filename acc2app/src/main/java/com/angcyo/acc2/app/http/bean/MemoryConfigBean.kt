package com.angcyo.acc2.app.http.bean

/**
 * Email:angcyo@126.com
 * @author angcyo
 * @date 2021/01/21
 */
data class MemoryConfigBean(

    /**是否要检查app开了代理无法使用*/
    var checkApp: Boolean = true,

    /**技术支持qq*/
    var qq: String? = null,

    /**启动时, 需要打开的url*/
    var startUrl: String? = null,

    /**帮助页*/
    var helpUrl: String? = null,

    /**轮询间隔时长, 秒*/
    var pollTime: Long = 10,

    /**功能区提示*/
    var functionDes: String? = null,

    /**操作区提示*/
    var funStartDes: String? = null,

    /**操作区底部提示*/
    var funStartBottomDes: String? = null,

    /**显示手势提示框*/
    var showTouchTip: Boolean = true,

    /**显示节点提示框*/
    var showNodeTip: Boolean = true,

    /**首次登录赠送时长(天)*/
    var giveServiceTime: Int = 1,

    /**签到赠送时长(分钟)*/
    var signGiveServiceTime: Int = 30,

    /**系统维护中*/
    var vindicate: String? = null,

    var functionTitle: String? = "功能区♪ ",

    var functionStartTitle: String? = "操作区♪ ",

    /**对应文件的文件名*/
    var file: FileConfigBean? = null
)