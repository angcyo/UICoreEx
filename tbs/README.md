# tbs

## 2020-7-14

```kotlin
DslTbs.init(content)
```

### 快速接入

https://x5.tencent.com/docs/access.html

### 平台适配

Android版本： Android 4.0-Android 10.0
CPU架构：armeabi、armeabi-v7a、arm64-v8a

## 2020-03-01

腾讯tbs浏览器服务

https://x5.tencent.com/tbs/guide.html

https://x5.tencent.com/tbs/sdk.html

https://x5.tencent.com/tbs/guide/sdkInit.html

TBS只支持32位运行:
64位手机无法加载x5(libmttwebview.so is 32-bit instead of 64-bit)
https://x5.tencent.com/tbs/technical.html#/detail/sdk/1/34cf1488-7dc2-41ca-a77f-0014112bcab7

TBS 5.0正式发布
纯咨询的问题可以加入官群：434421502 ，（腾讯浏览服务前端开发），在群里 @438204121或私聊
https://x5.tencent.com/tbs/history.html#/detail/24

腾讯说不需要加混淆, 但实际上必须加混淆规则.
否则会崩溃.
http://res.imtt.qq.com/TES/proguard.zip

---

Android SDK（完整版，含文件打开能力）
大小：456KB
版本：43697
更新日期：2019-08-08
说明：修复部分稳定性问题
Android Studio用户可在module的build.gradle文件的dependencies中添加 api 'com.tencent.tbs.tbssdk:sdk:43697'直接接入

---

新版TBS文件服务支持64位:

ndk{ abiFilters 'armeabi', 'armeabi-v7a', 'arm64-v8a' }

TBS文件静态集成方案(体验版)
大小：1.8M
版本：1.0
更新日期：2020-03-06
说明：支持密码文件打开,支持arm64/armv7,参考附件demo，详情见doc/demo_readme.docx。如接入遇到问题，请联系QQ：11060456