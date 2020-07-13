# bdtrace
2020-7-13

百度鹰眼轨迹


需要申请 `service_id`

# 快速开发指南

http://lbsyun.baidu.com/index.php?title=android-yingyan/guide/introduction


# 鹰眼概念

http://lbsyun.baidu.com/index.php?title=android-yingyan/guide/concept

# 保活

http://lbsyun.baidu.com/index.php?title=android-yingyan/guide/tracelive

# 坐标系说明

目前中国主要有以下三种坐标系：

`WGS84`：为一种大地坐标系，也是目前广泛使用的GPS全球卫星定位系统使用的坐标系。

`GCJ02`：是由中国国家测绘局制订的地理信息系统的坐标系统。由WGS84坐标系经加密后的坐标系。

`BD09`：为百度坐标系，在GCJ02坐标系基础上再次加密。其中bd09ll表示百度经纬度坐标，bd09mc表示百度墨卡托米制坐标

非中国地区地图，统一使用WGS84坐标

# 使用流程

1. 配置`baidu_ak`

请在[gradle.properties]中配置[baidu_ak].

2. 初始化
```kotlin
com.angcyo.baidu.trace.DslBaiduTrace.initTrace
```


  
