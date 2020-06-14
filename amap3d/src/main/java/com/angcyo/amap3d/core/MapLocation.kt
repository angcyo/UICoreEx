package com.angcyo.amap3d.core

import com.amap.api.maps.model.LatLng
import com.amap.api.services.core.LatLonPoint
import com.amap.api.services.core.PoiItem
import com.amap.api.services.geocoder.RegeocodeAddress
import com.amap.api.services.help.Tip
import com.angcyo.library.ex.orString
import com.autonavi.amap.mapcore.Inner_3dMap_location

/**
 * [Inner_3dMap_location] [AMapLocation]
 *
 * latitude=22.568021#longitude=114.066152
 * #province=广东省#city=深圳市#district=福田区#cityCode=0755#adCode=440304
 * #address=广东省深圳市福田区梅林路17号靠近彩梅立交#country=中国#road=梅林路
 * #poiName=#street=#streetNum=#aoiName=#poiid=#floor=#errorCode=0#errorInfo=success
 * #locationDetail=#id:YaDhoZ2U5M2diZjMzZjVlaDk3MmhoZzFjMTIyMzY3LFhCNFZYTnE5NktZREFPSnhyNGVuc3JCVg==
 * #csid:d83314dc0ed646398530b72d732e55a1#locationType=2
 * Email:angcyo@126.com
 * @author angcyo
 * @date 2020/06/13
 * Copyright (c) 2020 angcyo. All rights reserved.
 */

data class MapLocation(
    //广东省
    var province: String? = null,
    //深圳市
    var city: String? = null,
    //福田区
    var district: String? = null,
    //0755 POI的城市编码
    var cityCode: String? = null,
    //440304 POI的行政区划代码。
    var adCode: String? = null,
    //广东省深圳市福田区梅林路17号靠近彩梅立交
    var address: String? = null,
    //POI 搜索时, 返回的title
    var poiName: String? = null,
    //中国
    var country: String? = "中国",
    //梅林路
    var road: String? = null,
    var street: String? = null,
    //纬度
    var latitude: Double = 0.0,
    //经度
    var longitude: Double = 0.0
) {
    companion object {
        fun from(location: Inner_3dMap_location): MapLocation {
            return MapLocation().apply {
                province = location.province
                city = location.city
                district = location.district
                cityCode = location.cityCode
                adCode = location.adCode
                address = location.address
                country = location.country
                road = location.road
                street = location.street
                latitude = location.latitude
                longitude = location.longitude
            }
        }

        fun from(location: RegeocodeAddress, latLng: LatLng?): MapLocation {
            return MapLocation().apply {
                province = location.province
                city = location.city
                district = location.district
                cityCode = location.cityCode //0755
                adCode = location.adCode //440309
                address = location.formatAddress
                country = location.country
                //road = location.road
                street = location.township//民治街道
                location.towncode//440309002000
                latitude = latLng?.latitude ?: latitude
                longitude = latLng?.longitude ?: longitude
            }
        }

        fun from(location: PoiItem): MapLocation {
            return MapLocation().apply {
                province = location.provinceName
                city = location.cityName
                district = location.adName
                cityCode = location.cityCode
                adCode = location.adCode
                poiName = location.title
                street = location.snippet
                address = "${province.orString(
                    ""
                )}${city.orString(
                    ""
                )}${district.orString(
                    ""
                )}${street.orString("")}"
                //country = location.country
                //road = location.road
                location.businessArea //民治村 返回POI的所在商圈。
                latitude = location.latLonPoint?.latitude ?: latitude
                longitude = location.latLonPoint?.longitude ?: longitude
            }
        }

        fun from(location: Tip): MapLocation {
            return MapLocation().apply {
                //province = location.province
                //city = location.city
                district = location.district
                //cityCode = location.cityCode
                poiName = location.name
                adCode = location.adcode
                address = "${district.orString("")}${location.address.orString("")}"
                //country = location.country
                //road = location.road
                //street = location.street
                latitude = location.point?.latitude ?: latitude
                longitude = location.point?.longitude ?: longitude
            }
        }
    }
}

fun MapLocation.latLng() = LatLng(latitude, longitude)

fun MapLocation.latLngPoint() = LatLonPoint(latitude, longitude)