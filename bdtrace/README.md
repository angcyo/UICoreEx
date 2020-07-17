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

http://lbs.baidu.com/index.php?title=android-yingyan/guide/coordtrans

目前中国主要有以下三种坐标系：

`WGS84`：为一种大地坐标系，也是目前广泛使用的GPS全球卫星定位系统使用的坐标系。
`GCJ02`：是由中国国家测绘局制订的地理信息系统的坐标系统。由WGS84坐标系经加密后的坐标系。
`BD09`：为百度坐标系，在GCJ02坐标系基础上再次加密。其中bd09ll表示百度经纬度坐标，bd09mc表示百度墨卡托米制坐标

非中国地区地图，统一使用WGS84坐标

> 鹰眼 Android SDK v3.0.0及以上版本的所有接口（除客户端地理围栏外），
> 输入参数均支持以上三种坐标系，开发者无需进行任何坐标转换，
> 只需通过输入参数"coord_type"指明所使用的坐标系即可。
> 而输出坐标，支持百度坐标（bd09ll）和国测局加密坐标（GCJ02）。

## 其他坐标转高德坐标

https://lbs.amap.com/api/javascript-api/guide/transform/convertfrom/

地球上同一个地理位置的经纬度，在不同的坐标系中，会有少许偏移，国内目前常见的坐标系主要分为三种：

`地球坐标系——WGS84`：常见于 GPS 设备，Google 地图等国际标准的坐标体系。
`火星坐标系——GCJ-02`：中国国内使用的被强制加密后的坐标体系，高德坐标就属于该种坐标体系。
`百度坐标系——BD-09`：百度地图所使用的坐标体系，是在火星坐标系的基础上又进行了一次加密处理。

# 使用流程

1. 配置`baidu_ak`

请在[gradle.properties]中配置[baidu_ak].

2. 初始化
```kotlin
com.angcyo.baidu.trace.DslBaiduTrace.initTrace
```


  
