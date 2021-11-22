package com.angcyo.amap3d.location

import android.content.Context
import com.amap.api.location.AMapLocation
import com.amap.api.location.AMapLocationClient
import com.amap.api.location.AMapLocationClientOption
import com.amap.api.location.AMapLocationListener
import com.angcyo.library.L
import com.angcyo.library.app


/**
 * 高德定位sdk, 获取定位
 *
 * https://lbs.amap.com/api/android-location-sdk/guide/android-location/getlocation/
 *
 * 注意事项
 * ● 目前手机设备在长时间黑屏或锁屏时CPU会休眠，这导致定位SDK不能正常进行位置更新。若您有锁屏状态下获取位置的需求，您可以应用alarmManager实现1个可叫醒CPU的Timer，定时请求定位。
 * ● 使用定位SDK务必要注册GPS和网络的使用权限。
 * ● 在使用定位SDK时，请尽量保证网络畅通，如获取网络定位，地址信息等都需要设备可以正常接入网络。
 * ● 定位SDK在国内返回高德类型坐标，海外定位将返回GPS坐标。
 * ● V1.x版本定位SDK参考手册和错误码参考表可以点我获取。
 *
 *
 * Email:angcyo@126.com
 * @author angcyo
 * @date 2021/11/22
 * Copyright (c) 2020 ShenZhen Wayto Ltd. All rights reserved.
 */
class DslLocation : AMapLocationListener {

    //声明AMapLocationClient类对象
    lateinit var client: AMapLocationClient

    //声明AMapLocationClientOption对象
    var option: AMapLocationClientOption = AMapLocationClientOption()

    /**回调*/
    var onLocationChangedAction: ((location: AMapLocation) -> Unit)? = null

    /**1: 初始化*/
    fun initLocation(context: Context = app()) {
        client = AMapLocationClient(context)
        client.setLocationListener(this)

        /**
         * 设置定位场景，目前支持三种场景（签到、出行、运动，默认无场景）
         */
        option.locationPurpose = AMapLocationClientOption.AMapLocationPurpose.SignIn

        //获取一次定位结果：
        //该方法默认为false。
        option.isOnceLocation = false

        //获取最近3s内精度最高的一次定位结果：
        //设置setOnceLocationLatest(boolean b)接口为true，启动定位时SDK会返回最近3s内精度最高的一次定位结果。
        //如果设置其为true，setOnceLocation(boolean b)接口也会被设置为true，反之不会，默认为false。
        option.isOnceLocationLatest = false

        //设置定位模式为AMapLocationMode.Battery_Saving，低功耗模式。
        option.locationMode = AMapLocationClientOption.AMapLocationMode.Battery_Saving

        //设置定位间隔,单位毫秒,默认为2000ms，最低1000ms。
        option.interval = 1000

        //设置是否允许模拟位置,默认为true，允许模拟位置
        option.isMockEnable = true

        //关闭缓存机制
        option.isLocationCacheEnable = false
    }

    fun startLocation() {
        client.setLocationOption(option)
        //设置场景模式后最好调用一次stop，再调用start以保证场景模式生效
        client.stopLocation()
        client.startLocation()
    }

    fun stopLocation() {
        client.stopLocation()
    }

    //销毁定位客户端，同时销毁本地定位服务。
    fun release() {
        client.setLocationListener(null)
        client.onDestroy()
    }


    //amapLocation.getLocationType();//获取当前定位结果来源，如网络定位结果，详见定位类型表
    //amapLocation.getLatitude();//获取纬度
    //amapLocation.getLongitude();//获取经度
    //amapLocation.getAccuracy();//获取精度信息
    //amapLocation.getAddress();//地址，如果option中设置isNeedAddress为false，则没有此结果，网络定位结果中会有地址信息，GPS定位不返回地址信息。
    //amapLocation.getCountry();//国家信息
    //amapLocation.getProvince();//省信息
    //amapLocation.getCity();//城市信息
    //amapLocation.getDistrict();//城区信息
    //amapLocation.getStreet();//街道信息
    //amapLocation.getStreetNum();//街道门牌号信息
    //amapLocation.getCityCode();//城市编码
    //amapLocation.getAdCode();//地区编码
    //amapLocation.getAoiName();//获取当前定位点的AOI信息
    //amapLocation.getBuildingId();//获取当前室内定位的建筑物Id
    //amapLocation.getFloor();//获取当前室内定位的楼层
    //amapLocation.getGpsAccuracyStatus();//获取GPS的当前状态
    ////获取定位时间
    //SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    //Date date = new Date(amapLocation.getTime());
    //df.format(date);
    override fun onLocationChanged(location: AMapLocation?) {

        if (location != null) {
            if (location.errorCode == 0) {
                //可在其中解析amapLocation获取相应内容。
            } else {
                //定位失败时，可通过ErrCode（错误码）信息来确定失败的原因，errInfo是错误信息，详见错误码表。
                L.e(
                    "AmapError", "location Error, ErrCode:"
                            + location.errorCode + ", errInfo:"
                            + location.errorInfo
                )
            }
        }

        location?.let { onLocationChangedAction?.invoke(it) }
    }

}