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

    /**数据版本*/
    var version: Long = 0,

    /**[com.angcyo.acc2.app.http.Gitee.fetchFunctionList]*/
    var updateFunction: Boolean = false,

    /**[com.angcyo.acc2.app.http.Gitee.fetchAllAction]*/
    var updateAction: Boolean = false,

    /**[com.angcyo.acc2.app.http.Gitee.fetchAllBackAction]*/
    var updateBackAction: Boolean = false,

    /**[com.angcyo.acc2.app.http.Gitee.fetchAllCheck]*/
    var updateCheck: Boolean = false,

    /**[com.angcyo.acc2.app.http.Gitee.fetchAllTask]*/
    var updateTask: Boolean = false,

    /**是否是在线获取到的数据
     * [com.angcyo.acc2.app.http.Gitee.fetchMemoryConfig]*/
    var isOnlineData: Boolean = false,

    /**是否已经填充过*/
    @Transient
    var _isFill: Boolean = false,

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

    /**未适配时, 是否允许运行*/
    var allowNoAdapterRun: Boolean = false,

    //</editor-fold desc="功能配置">

    //<editor-fold desc="其他配置">

    /**拉取gitee数据的时间间隔, 秒*/
    var fetchInterval: Long = 10,

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

    /**完播最少时长 秒*/
    var minPlayTime: Long = 30,

    /**完播最大时长 秒*/
    var maxPlayTime: Long = 50,

    /**系统维护中*/
    var vindicate: String? = null,

    /**对应文件的文件名*/
    var file: FileConfigBean? = null,

    /**推广提返现示语*/
    var promoteDes: String? = null,

    /**邀请绑定现示语*/
    var inviteDes: String? = null,

    /**[com.angcyo.acc2.parse.AccParse.defTimeRandomFactor]*/
    var defTimeRandomFactor: Long? = null,

    /**需要选中主业务的数量*/
    var mainAppCount: Int = 1,

    var maxTextLength: Int? = null,
    var maxTextLines: Int? = null,

    var errorTip: String? = null,

    /**请求下一个任务的延迟时长, 毫秒*/
    var requestTaskDelay: Long? = null,

    /**熄屏时, 请求下一个任务的延迟时长, 毫秒*/
    var requestTaskDelayScreenOff: Long? = null,

    /**请求错误时, 请求下一个任务的延迟时长, 毫秒*/
    var requestTaskDelayOnError: Long? = null,

    /**日志上传行数限制,
     * 数据量太大的话... {"message":"500 Internal Server Error"}*/
    var uploadLogLineLimit: Int = 150,

    //</editor-fold desc="其他配置">

    //<editor-fold desc="2021-12-05">

    /**通用: 请求数据的时间间隔, 毫秒*/
    var refreshDataInterval: Long = 5000,

    /**通用: 提醒动画时长*/
    var remindAnimatorDuration: Long = 600,

    /**通用: 历史数据, 查询多少天内的数据*/
    var historyDataQueryDay: Int = 7,

    /**通用: 闪屏通知延迟, 毫秒*/
    var flashNotifyDelay: Long = 5000,

    /**通用: 倒计时, 毫秒*/
    var countDown: Long = 3 * 60 * 1000,

    /**通用: 倒计时, 第二阶段, 毫秒*/
    var countDownStep2: Long = 60 * 1000,

    //</editor-fold desc="2021-12-05">

    //<editor-fold desc="每个包名对应各自的配置">

    /**马甲包的包名, 对应的配置, 如果没有找到对应的配置, 则使用主配置*/
    var pks: Map<String, MemoryConfigBean>? = null

    //</editor-fold desc="每个包名对应各自的配置">
)