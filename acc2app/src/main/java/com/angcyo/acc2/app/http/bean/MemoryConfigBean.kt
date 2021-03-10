package com.angcyo.acc2.app.http.bean

/**
 * Email:angcyo@126.com
 * @author angcyo
 * @date 2021/01/21
 */
data class MemoryConfigBean(

    //<editor-fold desc="检查配置">

    /**是否要检查app开了代理无法使用*/
    var checkApp: Boolean = true,

    /**指定状态, 才kill*/
    var checkState: List<Int>? = null,

    /**[checkApp]忽略白名单*/
    var checkIgnoreAndroidId: List<String>? = null,

    //</editor-fold desc="检查配置">

    //<editor-fold desc="界面配置">

    /**功能区提示*/
    var functionDes: String? = null,

    /**操作区提示*/
    var funStartDes: String? = null,

    /**操作区底部提示*/
    var funStartBottomDes: String? = null,

    var functionTitle: String? = "功能区♪ ",

    var functionStartTitle: String? = "操作区♪ ",

    //</editor-fold desc="界面配置">

    //<editor-fold desc="功能配置">

    /**显示手势提示框*/
    var showTouchTip: Boolean = true,

    /**显示节点提示框*/
    var showNodeTip: Boolean = true,

    /**是否要显示分享日志按钮*/
    var showShareLog: Boolean = false,

    //</editor-fold desc="功能配置">

    //<editor-fold desc="其他配置">

    /**技术支持qq*/
    var qq: String? = null,

    /**启动时, 需要打开的url*/
    var startUrl: String? = null,

    /**帮助页*/
    var helpUrl: String? = null,

    /**提现帮助页*/
    var promoteHelpUrl: String? = null,

    /**轮询间隔时长, 秒*/
    var pollTime: Long = 10,

    /**首次登录赠送时长(天)*/
    var giveServiceTime: Int = 3,

    /**填写推广码, 赠送时长 天*/
    var promoteGiveServiceTime: Int = 3,

    /**签到赠送时长(分钟)*/
    var signGiveServiceTime: Int = 30,

    /**系统维护中*/
    var vindicate: String? = null,

    /**对应文件的文件名*/
    var file: FileConfigBean? = null,

    /**推广提示语*/
    var promoteDes: String? = null,

    //</editor-fold desc="其他配置">
)