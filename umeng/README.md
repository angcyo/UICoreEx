# umeng

友盟数据统计

>应用的安卓版和iOS版不能使用相同的AppKey，需要分开注册。
 【友盟+】的应用名与实际应用名和包名无关，若注册应用时，提示应用名称已存在，建议命名为应用名+平台（iOS/Android)。

https://developer.umeng.com/docs/119267/detail/118578

https://github.com/umeng/MultiFunctionAndroidMavenDemo-master

# 集成使用

https://developer.umeng.com/docs/119267/detail/118578

## 1.

```
buildscript {
  repositories {
    google()
    mavenCentral()
    maven { url 'https://dl.bintray.com/umsdk/release' }
  }
  dependencies {
    classpath 'com.android.tools.build:gradle:3.4.0'
    // NOTE: Do not place your application dependencies here; they belong
    // in the individual module build.gradle files
  }
}
allprojects {
  repositories {
    google()
    mavenCentral()
    maven { url 'https://dl.bintray.com/umsdk/release' }
  }
}
```

https://mobile.umeng.com/platform/config/apps/create

## 2.

```
// 友盟基础组件库（所有友盟业务SDK都依赖基础组件库）
implementation "com.umeng.umsdk:common:9.3.6" （必选）
implementation "com.umeng.umsdk:asms:1.2.0" // asms包依赖(必选)

// 下面各SDK根据宿主App是否使用相关业务按需引入。
implementation "com.umeng.umsdk:abtest:1.0.0" // ABTest功能依赖(可选)
implementation "com.umeng.umsdk:apm:1.1.1" // 应用性能监控SDK依赖(可选)
implementation "com.umeng.umsdk:game:9.2.0+G" // 游戏统计SDK依赖(可选)
更多可用组件，请参见可用maven列表
```

## 3.

```
接口

/**
* 注意: 即使您已经在AndroidManifest.xml中配置过appkey和channel值，也需要在App代码中调
* 用初始化接口（如需要使用AndroidManifest.xml中配置好的appkey和channel值，
* UMConfigure.init调用中appkey和channel参数请置为null）。
*/
UMConfigure.init(Context context, String appkey, String channel, int deviceType, String pushSecret);

代码实例

//初始化组件化基础库, 所有友盟业务SDK都必须调用此初始化接口。
//建议在宿主App的Application.onCreate函数中调用基础组件库初始化函数。
UMConfigure.init(this, "59892f08310c9307b60023d0", "Umeng", UMConfigure.DEVICE_TYPE_PHONE, "");
```

```
//选择AUTO页面采集模式，统计SDK基础指标无需手动埋点可自动采集。
//建议在宿主App的Application.onCreate函数中调用此函数。
MobclickAgent.setPageCollectionMode(MobclickAgent.PageMode.AUTO);
```