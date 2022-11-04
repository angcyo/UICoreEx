package com.angcyo.engrave.firmware

/**
 * 固件升级助手, 版本匹配规则验证
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2022/11/04
 */
object FirmwareUpdateHelper {

    /**解析范围, 格式 [xxx~xxx xxx~xxx]*/
    fun parseRange(config: String?): List<VersionRange> {
        val rangeStringList = config?.split(" ")
        val list = mutableListOf<VersionRange>()
        rangeStringList?.forEach {
            val rangeString = it.split("~")
            rangeString.firstOrNull()?.toIntOrNull()?.let { min ->
                if (rangeString.size >= 2) {
                    list.add(
                        VersionRange(
                            min,
                            rangeString.getOrNull(1)?.toIntOrNull() ?: Int.MAX_VALUE
                        )
                    )
                } else {
                    list.add(VersionRange(min, Int.MAX_VALUE))
                }
            }
        }
        return list
    }

    /**当前的版本[version]适配满足配置的规则[min~max]*/
    fun matches(version: Int, config: String?): Boolean {
        val versionRangeList = parseRange(config)
        if (versionRangeList.isEmpty()) {
            //无规则, 则通过
            return true
        }

        var targetRange: VersionRange? = null //匹配到的固件版本范围
        for (range in versionRangeList) {
            if (version in range.min..range.max) {
                targetRange = range
                break
            }
        }
        if (targetRange != null) {
            if (version in (targetRange.min..targetRange.max)) {
                return true
            }
        }
        return false
    }

    /**版本规则数据结构[min~max]*/
    data class VersionRange(val min: Int, val max: Int)
}