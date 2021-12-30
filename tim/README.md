# 2021-11-9

腾讯TIM 即时通信IM

https://cloud.tencent.com/product/im

## 最新版

https://github.com/tencentyun/TIMSDK/tree/master/Android/IMSDK

## API接口

https://im.sdk.qcloud.com/doc/zh-cn/md_introduction_Android%E6%A6%82%E8%A7%88.html

## 离线推送

https://cloud.tencent.com/document/product/269/44516

## 移动推送 TPNS

移动推送 TPNS（Tencent Push Notification Service）为应用提供合法合规、消息通道稳定、消息高效秒达、全球服务覆盖的消息推送服务，已稳定服务腾讯游戏、腾讯视频等超高日活应用；支持App推送、应用内消息、智能短信等多种消息类型，有效提升用户活跃度。

https://cloud.tencent.com/product/tpns

https://cloud.tencent.com/document/product/548

**离线推送（Android）**

https://cloud.tencent.com/document/product/269/44516

推送通道	|系统要求|条件说明
---|---|---
小米推送	|MIUI	|使用小米推送，添加依赖：implementation 'com.tencent.tpns:xiaomi:1.2.1.2-release'
华为推送	|EMUI	|使用华为推送，添加依赖：implementation 'com.tencent.tpns:huawei:1.2.1.2-release' 和 implementation 'com.huawei.hms:push:5.0.2.300'
Google FCM 推送|	Android 4.1 及以上	|手机端需安装 Google Play Services 且在中国大陆地区以外使用。添加依赖：implementation 'com.google.firebase:firebase-messaging:20.2.3'
魅族推送	|Flyme	|使用魅族推送，添加依赖：implementation 'com.tencent.tpns:meizu:1.2.1.2-release'
OPPO 推送	|ColorOS	|并非所有 OPPO 机型和版本都支持使用 OPPO 推送。使用 OPPO 推送，添加依赖：implementation 'com.tencent.tpns:oppo:1.2.1.2-release'
vivo 推送	|FuntouchOS	|并非所有 vivo 机型和版本都支持使用 vivo 推送。使用 vivo 推送，添加依赖：implementation 'com.tencent.tpns:vivo:1.2.1.2-release'