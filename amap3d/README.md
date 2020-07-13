# amap3d
2020-6-11

高德3D地图 **需要绑定应用包名**

# Android Studio 配置工程

https://lbs.amap.com/api/android-sdk/guide/create-project/android-studio-create-project

# 获取Key

https://lbs.amap.com/api/android-sdk/guide/create-project/get-key

# Android 地图SDK 相关下载

https://lbs.amap.com/api/android-sdk/download/

# API文档

https://a.amap.com/lbs/static/unzip/Android_Map_Doc/index.html

# 注意

1、3D地图 SDK 和导航 SDK，5.0.0 版本以后全面支持多平台 so 库(armeabi、armeabi-v7a、arm64-v8a、x86、x86_64)，开发者可以根据需要选择。同时还需要注意的是：如果您涉及到新旧版本更替请移除旧版本的 so 库之后替换新版本 so 库到工程中。

2、navi导航SDK 5.0.0以后版本包含了3D地图SDK，所以请不要同时引入 map3d 和 navi SDK。

3、如果build失败提示com.amap.api:XXX:X.X.X 找不到，请确认拼写及版本号是否正确，如果访问不到jcenter可以切换为maven仓库尝试一下。

4、依照上述方法引入 SDK 以后，不需要在libs文件夹下导入对应SDK的 so 和 jar 包，会有冲突。

# 说明

1.AMap_3DMap_V3.0.jar包是AMap地图包，支持Android SDK2.2及以上.

2.libgdamapv4sdk736.so和libgdamapv4sdk736ex.so文件是AMap地图引擎，支持Android SDK2.2及以上；

3.Android_Location_V1.4.0.jar架包是AMap定位包，支持Android SDK2.1及以上，

4.MapApiServices.jar包是AMap搜索包，支持Android SDK1.6及以上，

5.如果遇到问题，可以上技术支持论坛http://bbs.amap.com/forum.php。
