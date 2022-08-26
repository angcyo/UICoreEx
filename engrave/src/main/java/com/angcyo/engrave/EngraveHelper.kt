package com.angcyo.engrave

import androidx.annotation.MainThread
import com.angcyo.bluetooth.fsc.laserpacker.LaserPeckerHelper
import com.angcyo.bluetooth.fsc.laserpacker.LaserPeckerModel
import com.angcyo.bluetooth.fsc.laserpacker.parse.QuerySettingParser
import com.angcyo.canvas.core.MmValueUnit
import com.angcyo.canvas.items.renderer.BaseItemRenderer
import com.angcyo.canvas.utils.CanvasConstant
import com.angcyo.core.vmApp
import com.angcyo.engrave.model.EngraveModel
import com.angcyo.library.component.HawkPropertyValue
import com.angcyo.library.ex.toHexInt
import com.angcyo.objectbox.laser.pecker.entity.MaterialEntity

/**
 * 雕刻助手
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2022/06/21
 */
object EngraveHelper {

    /**最后一次预览光功率设置 [0~1f]*/
    var lastPwrProgress: Float by HawkPropertyValue<Any, Float>(0.5f)

    /**最后一次功率*/
    var lastPower: Int by HawkPropertyValue<Any, Int>(100)

    /**最后一次深度*/
    var lastDepth: Int by HawkPropertyValue<Any, Int>(10)

    /**最后一次的物理尺寸, 像素*/
    var lastDiameterPixel: Float by HawkPropertyValue<Any, Float>(300f)

    fun findOptionIndex(list: List<Any>?, value: Byte?): Int {
        return list?.indexOfFirst { it.toString().toInt() == value?.toHexInt() } ?: -1
    }

    /**获取物理尺寸的值*/
    fun getDiameter(): Int {
        val laserPeckerModel = vmApp<LaserPeckerModel>()
        val engraveModel = vmApp<EngraveModel>()

        //物理尺寸
        val diameter = if (!laserPeckerModel.haveExDevice()) {
            0
        } else {
            val diameterPixel = engraveModel.engraveOptionInfoData.value!!.diameterPixel
            val mmValueUnit = MmValueUnit()
            val mm = mmValueUnit.convertPixelToValue(diameterPixel)
            (mm * 100).toInt()
        }
        return diameter
    }

    /**发送预览范围指令
     * [itemRenderer] 需要预览的*/
    @MainThread
    fun sendPreviewRange(
        itemRenderer: BaseItemRenderer<*>,
        updateState: Boolean,
        async: Boolean,
        zPause: Boolean = false
    ) {
        val laserPeckerModel = vmApp<LaserPeckerModel>()
        val engraveModel = vmApp<EngraveModel>()

        engraveModel.apply {
            //先执行
            updateEngravePreviewInfo {
                itemUuid = itemRenderer.getRendererItem()?.uuid

                if (QuerySettingParser.USE_FOUR_POINTS_PREVIEW //开启了4点预览
                    && !laserPeckerModel.haveExDevice() //没有外置设备连接
                ) {
                    rotate = itemRenderer.rotate
                } else {
                    rotate = null
                }
            }
            //后执行
            updateEngravePreviewUuid(itemRenderer.getRendererItem()?.uuid)
        }

        val diameter = getDiameter()

        laserPeckerModel.sendUpdatePreviewRange(
            itemRenderer.getBounds(),
            itemRenderer.getRotateBounds(),
            itemRenderer.rotate,
            lastPwrProgress,
            updateState, async, zPause,
            diameter
        )
    }

    //<editor-fold desc="material">

    /**材质列表*/
    val materialList = mutableListOf<MaterialEntity>()
    const val MATERIAL_SPLIT = ","

    /**L1设备推荐参数*/
    fun initL1MaterialList(materialList: MutableList<MaterialEntity> = EngraveHelper.materialList) {
        materialList.clear()
        val L1 = buildString {
            append(LaserPeckerHelper.LI)
            append(MATERIAL_SPLIT)
            append(LaserPeckerHelper.LI_Z)
            append(MATERIAL_SPLIT)
            append(LaserPeckerHelper.LI_PRO)
            append(MATERIAL_SPLIT)
            append(LaserPeckerHelper.LI_Z_PRO)
        }
        //环保纸
        materialList.add(MaterialEntity().apply {
            resId = R.string.material_hbz
            product = L1
            dataMode = CanvasConstant.DATA_MODE_BLACK_WHITE
            px = LaserPeckerHelper.PX_1K.toHexInt()
            type = LaserPeckerHelper.LASER_TYPE_BLUE.toHexInt()
            power = 100
            depth = 50
        })
        materialList.add(MaterialEntity().apply {
            resId = R.string.material_hbz
            product = L1
            dataMode = CanvasConstant.DATA_MODE_BLACK_WHITE
            px = LaserPeckerHelper.PX_1_3K.toHexInt()
            type = LaserPeckerHelper.LASER_TYPE_BLUE.toHexInt()
            power = 100
            depth = 30
        })
        materialList.add(MaterialEntity().apply {
            resId = R.string.material_hbz
            product = L1
            dataMode = CanvasConstant.DATA_MODE_BLACK_WHITE
            px = LaserPeckerHelper.PX_2K.toHexInt()
            type = LaserPeckerHelper.LASER_TYPE_BLUE.toHexInt()
            power = 100
            depth = 15
        })

        materialList.add(MaterialEntity().apply {
            resId = R.string.material_hbz
            product = L1
            dataMode = CanvasConstant.DATA_MODE_GREY
            px = LaserPeckerHelper.PX_1K.toHexInt()
            type = LaserPeckerHelper.LASER_TYPE_BLUE.toHexInt()
            power = 100
            depth = 80
        })
        materialList.add(MaterialEntity().apply {
            resId = R.string.material_hbz
            product = L1
            dataMode = CanvasConstant.DATA_MODE_GREY
            px = LaserPeckerHelper.PX_1_3K.toHexInt()
            type = LaserPeckerHelper.LASER_TYPE_BLUE.toHexInt()
            power = 100
            depth = 65
        })
        materialList.add(MaterialEntity().apply {
            resId = R.string.material_hbz
            product = L1
            dataMode = CanvasConstant.DATA_MODE_GREY
            px = LaserPeckerHelper.PX_2K.toHexInt()
            type = LaserPeckerHelper.LASER_TYPE_BLUE.toHexInt()
            power = 100
            depth = 30
        })

        //瓦楞纸
        materialList.add(MaterialEntity().apply {
            resId = R.string.material_wlz
            product = L1
            dataMode = CanvasConstant.DATA_MODE_BLACK_WHITE
            px = LaserPeckerHelper.PX_1K.toHexInt()
            type = LaserPeckerHelper.LASER_TYPE_BLUE.toHexInt()
            power = 100
            depth = 50
        })
        materialList.add(MaterialEntity().apply {
            resId = R.string.material_wlz
            product = L1
            dataMode = CanvasConstant.DATA_MODE_BLACK_WHITE
            px = LaserPeckerHelper.PX_1_3K.toHexInt()
            type = LaserPeckerHelper.LASER_TYPE_BLUE.toHexInt()
            power = 100
            depth = 30
        })
        materialList.add(MaterialEntity().apply {
            resId = R.string.material_wlz
            product = L1
            dataMode = CanvasConstant.DATA_MODE_BLACK_WHITE
            px = LaserPeckerHelper.PX_2K.toHexInt()
            type = LaserPeckerHelper.LASER_TYPE_BLUE.toHexInt()
            power = 100
            depth = 15
        })

        materialList.add(MaterialEntity().apply {
            resId = R.string.material_wlz
            product = L1
            dataMode = CanvasConstant.DATA_MODE_GREY
            px = LaserPeckerHelper.PX_1K.toHexInt()
            type = LaserPeckerHelper.LASER_TYPE_BLUE.toHexInt()
            power = 100
            depth = 90
        })
        materialList.add(MaterialEntity().apply {
            resId = R.string.material_wlz
            product = L1
            dataMode = CanvasConstant.DATA_MODE_GREY
            px = LaserPeckerHelper.PX_1_3K.toHexInt()
            type = LaserPeckerHelper.LASER_TYPE_BLUE.toHexInt()
            power = 100
            depth = 75
        })
        materialList.add(MaterialEntity().apply {
            resId = R.string.material_wlz
            product = L1
            dataMode = CanvasConstant.DATA_MODE_GREY
            px = LaserPeckerHelper.PX_2K.toHexInt()
            type = LaserPeckerHelper.LASER_TYPE_BLUE.toHexInt()
            power = 100
            depth = 45
        })

        //皮革
        materialList.add(MaterialEntity().apply {
            resId = R.string.material_pg
            product = L1
            dataMode = CanvasConstant.DATA_MODE_BLACK_WHITE
            px = LaserPeckerHelper.PX_1K.toHexInt()
            type = LaserPeckerHelper.LASER_TYPE_BLUE.toHexInt()
            power = 70
            depth = 70
        })
        materialList.add(MaterialEntity().apply {
            resId = R.string.material_pg
            product = L1
            dataMode = CanvasConstant.DATA_MODE_BLACK_WHITE
            px = LaserPeckerHelper.PX_1_3K.toHexInt()
            type = LaserPeckerHelper.LASER_TYPE_BLUE.toHexInt()
            power = 70
            depth = 50
        })
        materialList.add(MaterialEntity().apply {
            resId = R.string.material_pg
            product = L1
            dataMode = CanvasConstant.DATA_MODE_BLACK_WHITE
            px = LaserPeckerHelper.PX_2K.toHexInt()
            type = LaserPeckerHelper.LASER_TYPE_BLUE.toHexInt()
            power = 70
            depth = 30
        })

        materialList.add(MaterialEntity().apply {
            resId = R.string.material_pg
            product = L1
            dataMode = CanvasConstant.DATA_MODE_GREY
            px = LaserPeckerHelper.PX_1K.toHexInt()
            type = LaserPeckerHelper.LASER_TYPE_BLUE.toHexInt()
            power = 100
            depth = 90
        })
        materialList.add(MaterialEntity().apply {
            resId = R.string.material_pg
            product = L1
            dataMode = CanvasConstant.DATA_MODE_GREY
            px = LaserPeckerHelper.PX_1_3K.toHexInt()
            type = LaserPeckerHelper.LASER_TYPE_BLUE.toHexInt()
            power = 100
            depth = 60
        })
        materialList.add(MaterialEntity().apply {
            resId = R.string.material_pg
            product = L1
            dataMode = CanvasConstant.DATA_MODE_GREY
            px = LaserPeckerHelper.PX_2K.toHexInt()
            type = LaserPeckerHelper.LASER_TYPE_BLUE.toHexInt()
            power = 100
            depth = 30
        })

        //竹质
        materialList.add(MaterialEntity().apply {
            resId = R.string.material_zz
            product = L1
            dataMode = CanvasConstant.DATA_MODE_BLACK_WHITE
            px = LaserPeckerHelper.PX_1K.toHexInt()
            type = LaserPeckerHelper.LASER_TYPE_BLUE.toHexInt()
            power = 100
            depth = 80
        })
        materialList.add(MaterialEntity().apply {
            resId = R.string.material_zz
            product = L1
            dataMode = CanvasConstant.DATA_MODE_BLACK_WHITE
            px = LaserPeckerHelper.PX_1_3K.toHexInt()
            type = LaserPeckerHelper.LASER_TYPE_BLUE.toHexInt()
            power = 100
            depth = 60
        })
        materialList.add(MaterialEntity().apply {
            resId = R.string.material_zz
            product = L1
            dataMode = CanvasConstant.DATA_MODE_BLACK_WHITE
            px = LaserPeckerHelper.PX_2K.toHexInt()
            type = LaserPeckerHelper.LASER_TYPE_BLUE.toHexInt()
            power = 100
            depth = 30
        })

        materialList.add(MaterialEntity().apply {
            resId = R.string.material_zz
            product = L1
            dataMode = CanvasConstant.DATA_MODE_GREY
            px = LaserPeckerHelper.PX_1K.toHexInt()
            type = LaserPeckerHelper.LASER_TYPE_BLUE.toHexInt()
            power = 70
            depth = 80
        })
        materialList.add(MaterialEntity().apply {
            resId = R.string.material_zz
            product = L1
            dataMode = CanvasConstant.DATA_MODE_GREY
            px = LaserPeckerHelper.PX_1_3K.toHexInt()
            type = LaserPeckerHelper.LASER_TYPE_BLUE.toHexInt()
            power = 70
            depth = 50
        })
        materialList.add(MaterialEntity().apply {
            resId = R.string.material_zz
            product = L1
            dataMode = CanvasConstant.DATA_MODE_GREY
            px = LaserPeckerHelper.PX_2K.toHexInt()
            type = LaserPeckerHelper.LASER_TYPE_BLUE.toHexInt()
            power = 60
            depth = 40
        })

        //木质
        materialList.add(MaterialEntity().apply {
            resId = R.string.material_mz
            product = L1
            dataMode = CanvasConstant.DATA_MODE_BLACK_WHITE
            px = LaserPeckerHelper.PX_1K.toHexInt()
            type = LaserPeckerHelper.LASER_TYPE_BLUE.toHexInt()
            power = 100
            depth = 80
        })
        materialList.add(MaterialEntity().apply {
            resId = R.string.material_mz
            product = L1
            dataMode = CanvasConstant.DATA_MODE_BLACK_WHITE
            px = LaserPeckerHelper.PX_1_3K.toHexInt()
            type = LaserPeckerHelper.LASER_TYPE_BLUE.toHexInt()
            power = 100
            depth = 60
        })
        materialList.add(MaterialEntity().apply {
            resId = R.string.material_mz
            product = L1
            dataMode = CanvasConstant.DATA_MODE_BLACK_WHITE
            px = LaserPeckerHelper.PX_2K.toHexInt()
            type = LaserPeckerHelper.LASER_TYPE_BLUE.toHexInt()
            power = 100
            depth = 30
        })

        materialList.add(MaterialEntity().apply {
            resId = R.string.material_mz
            product = L1
            dataMode = CanvasConstant.DATA_MODE_GREY
            px = LaserPeckerHelper.PX_1K.toHexInt()
            type = LaserPeckerHelper.LASER_TYPE_BLUE.toHexInt()
            power = 100
            depth = 90
        })
        materialList.add(MaterialEntity().apply {
            resId = R.string.material_mz
            product = L1
            dataMode = CanvasConstant.DATA_MODE_GREY
            px = LaserPeckerHelper.PX_1_3K.toHexInt()
            type = LaserPeckerHelper.LASER_TYPE_BLUE.toHexInt()
            power = 100
            depth = 60
        })
        materialList.add(MaterialEntity().apply {
            resId = R.string.material_mz
            product = L1
            dataMode = CanvasConstant.DATA_MODE_GREY
            px = LaserPeckerHelper.PX_2K.toHexInt()
            type = LaserPeckerHelper.LASER_TYPE_BLUE.toHexInt()
            power = 100
            depth = 40
        })

        //软木
        materialList.add(MaterialEntity().apply {
            resId = R.string.material_rm
            product = L1
            dataMode = CanvasConstant.DATA_MODE_BLACK_WHITE
            px = LaserPeckerHelper.PX_1K.toHexInt()
            type = LaserPeckerHelper.LASER_TYPE_BLUE.toHexInt()
            power = 40
            depth = 40
        })
        materialList.add(MaterialEntity().apply {
            resId = R.string.material_rm
            product = L1
            dataMode = CanvasConstant.DATA_MODE_BLACK_WHITE
            px = LaserPeckerHelper.PX_1_3K.toHexInt()
            type = LaserPeckerHelper.LASER_TYPE_BLUE.toHexInt()
            power = 40
            depth = 30
        })
        materialList.add(MaterialEntity().apply {
            resId = R.string.material_rm
            product = L1
            dataMode = CanvasConstant.DATA_MODE_BLACK_WHITE
            px = LaserPeckerHelper.PX_2K.toHexInt()
            type = LaserPeckerHelper.LASER_TYPE_BLUE.toHexInt()
            power = 40
            depth = 20
        })

        materialList.add(MaterialEntity().apply {
            resId = R.string.material_rm
            product = L1
            dataMode = CanvasConstant.DATA_MODE_GREY
            px = LaserPeckerHelper.PX_1K.toHexInt()
            type = LaserPeckerHelper.LASER_TYPE_BLUE.toHexInt()
            power = 50
            depth = 30
        })
        materialList.add(MaterialEntity().apply {
            resId = R.string.material_rm
            product = L1
            dataMode = CanvasConstant.DATA_MODE_GREY
            px = LaserPeckerHelper.PX_1_3K.toHexInt()
            type = LaserPeckerHelper.LASER_TYPE_BLUE.toHexInt()
            power = 50
            depth = 20
        })
        materialList.add(MaterialEntity().apply {
            resId = R.string.material_rm
            product = L1
            dataMode = CanvasConstant.DATA_MODE_GREY
            px = LaserPeckerHelper.PX_2K.toHexInt()
            type = LaserPeckerHelper.LASER_TYPE_BLUE.toHexInt()
            power = 50
            depth = 15
        })

        //塑料
        materialList.add(MaterialEntity().apply {
            resId = R.string.material_sl
            product = L1
            dataMode = CanvasConstant.DATA_MODE_BLACK_WHITE
            px = LaserPeckerHelper.PX_1K.toHexInt()
            type = LaserPeckerHelper.LASER_TYPE_BLUE.toHexInt()
            power = 50
            depth = 60
        })
        materialList.add(MaterialEntity().apply {
            resId = R.string.material_sl
            product = L1
            dataMode = CanvasConstant.DATA_MODE_BLACK_WHITE
            px = LaserPeckerHelper.PX_1_3K.toHexInt()
            type = LaserPeckerHelper.LASER_TYPE_BLUE.toHexInt()
            power = 50
            depth = 50
        })
        materialList.add(MaterialEntity().apply {
            resId = R.string.material_sl
            product = L1
            dataMode = CanvasConstant.DATA_MODE_BLACK_WHITE
            px = LaserPeckerHelper.PX_2K.toHexInt()
            type = LaserPeckerHelper.LASER_TYPE_BLUE.toHexInt()
            power = 50
            depth = 40
        })

        materialList.add(MaterialEntity().apply {
            resId = R.string.material_sl
            product = L1
            dataMode = CanvasConstant.DATA_MODE_GREY
            px = LaserPeckerHelper.PX_1K.toHexInt()
            type = LaserPeckerHelper.LASER_TYPE_BLUE.toHexInt()
            power = 60
            depth = 70
        })
        materialList.add(MaterialEntity().apply {
            resId = R.string.material_sl
            product = L1
            dataMode = CanvasConstant.DATA_MODE_GREY
            px = LaserPeckerHelper.PX_1_3K.toHexInt()
            type = LaserPeckerHelper.LASER_TYPE_BLUE.toHexInt()
            power = 60
            depth = 50
        })
        materialList.add(MaterialEntity().apply {
            resId = R.string.material_sl
            product = L1
            dataMode = CanvasConstant.DATA_MODE_GREY
            px = LaserPeckerHelper.PX_2K.toHexInt()
            type = LaserPeckerHelper.LASER_TYPE_BLUE.toHexInt()
            power = 60
            depth = 30
        })

        //布料
        materialList.add(MaterialEntity().apply {
            resId = R.string.material_bl
            product = L1
            dataMode = CanvasConstant.DATA_MODE_BLACK_WHITE
            px = LaserPeckerHelper.PX_1K.toHexInt()
            type = LaserPeckerHelper.LASER_TYPE_BLUE.toHexInt()
            power = 90
            depth = 70
        })
        materialList.add(MaterialEntity().apply {
            resId = R.string.material_bl
            product = L1
            dataMode = CanvasConstant.DATA_MODE_BLACK_WHITE
            px = LaserPeckerHelper.PX_1_3K.toHexInt()
            type = LaserPeckerHelper.LASER_TYPE_BLUE.toHexInt()
            power = 90
            depth = 50
        })
        materialList.add(MaterialEntity().apply {
            resId = R.string.material_bl
            product = L1
            dataMode = CanvasConstant.DATA_MODE_BLACK_WHITE
            px = LaserPeckerHelper.PX_2K.toHexInt()
            type = LaserPeckerHelper.LASER_TYPE_BLUE.toHexInt()
            power = 90
            depth = 30
        })

        materialList.add(MaterialEntity().apply {
            resId = R.string.material_bl
            product = L1
            dataMode = CanvasConstant.DATA_MODE_GREY
            px = LaserPeckerHelper.PX_1K.toHexInt()
            type = LaserPeckerHelper.LASER_TYPE_BLUE.toHexInt()
            power = 100
            depth = 100
        })
        materialList.add(MaterialEntity().apply {
            resId = R.string.material_bl
            product = L1
            dataMode = CanvasConstant.DATA_MODE_GREY
            px = LaserPeckerHelper.PX_1_3K.toHexInt()
            type = LaserPeckerHelper.LASER_TYPE_BLUE.toHexInt()
            power = 100
            depth = 100
        })
        materialList.add(MaterialEntity().apply {
            resId = R.string.material_bl
            product = L1
            dataMode = CanvasConstant.DATA_MODE_GREY
            px = LaserPeckerHelper.PX_2K.toHexInt()
            type = LaserPeckerHelper.LASER_TYPE_BLUE.toHexInt()
            power = 100
            depth = 100
        })

        //毛毡布
        materialList.add(MaterialEntity().apply {
            resId = R.string.material_mzb
            product = L1
            dataMode = CanvasConstant.DATA_MODE_BLACK_WHITE
            px = LaserPeckerHelper.PX_1K.toHexInt()
            type = LaserPeckerHelper.LASER_TYPE_BLUE.toHexInt()
            power = 80
            depth = 20
        })
        materialList.add(MaterialEntity().apply {
            resId = R.string.material_mzb
            product = L1
            dataMode = CanvasConstant.DATA_MODE_BLACK_WHITE
            px = LaserPeckerHelper.PX_1_3K.toHexInt()
            type = LaserPeckerHelper.LASER_TYPE_BLUE.toHexInt()
            power = 80
            depth = 15
        })
        materialList.add(MaterialEntity().apply {
            resId = R.string.material_mzb
            product = L1
            dataMode = CanvasConstant.DATA_MODE_BLACK_WHITE
            px = LaserPeckerHelper.PX_2K.toHexInt()
            type = LaserPeckerHelper.LASER_TYPE_BLUE.toHexInt()
            power = 70
            depth = 10
        })

        materialList.add(MaterialEntity().apply {
            resId = R.string.material_mzb
            product = L1
            dataMode = CanvasConstant.DATA_MODE_GREY
            px = LaserPeckerHelper.PX_1K.toHexInt()
            type = LaserPeckerHelper.LASER_TYPE_BLUE.toHexInt()
            power = 80
            depth = 45
        })
        materialList.add(MaterialEntity().apply {
            resId = R.string.material_mzb
            product = L1
            dataMode = CanvasConstant.DATA_MODE_GREY
            px = LaserPeckerHelper.PX_1_3K.toHexInt()
            type = LaserPeckerHelper.LASER_TYPE_BLUE.toHexInt()
            power = 80
            depth = 35
        })
        materialList.add(MaterialEntity().apply {
            resId = R.string.material_mzb
            product = L1
            dataMode = CanvasConstant.DATA_MODE_GREY
            px = LaserPeckerHelper.PX_2K.toHexInt()
            type = LaserPeckerHelper.LASER_TYPE_BLUE.toHexInt()
            power = 80
            depth = 20
        })

        //不透明亚克力
        materialList.add(MaterialEntity().apply {
            resId = R.string.material_btmykl
            product = L1
            dataMode = CanvasConstant.DATA_MODE_BLACK_WHITE
            px = LaserPeckerHelper.PX_1K.toHexInt()
            type = LaserPeckerHelper.LASER_TYPE_BLUE.toHexInt()
            power = 80
            depth = 80
        })
        materialList.add(MaterialEntity().apply {
            resId = R.string.material_btmykl
            product = L1
            dataMode = CanvasConstant.DATA_MODE_BLACK_WHITE
            px = LaserPeckerHelper.PX_1_3K.toHexInt()
            type = LaserPeckerHelper.LASER_TYPE_BLUE.toHexInt()
            power = 80
            depth = 50
        })
        materialList.add(MaterialEntity().apply {
            resId = R.string.material_btmykl
            product = L1
            dataMode = CanvasConstant.DATA_MODE_BLACK_WHITE
            px = LaserPeckerHelper.PX_2K.toHexInt()
            type = LaserPeckerHelper.LASER_TYPE_BLUE.toHexInt()
            power = 80
            depth = 40
        })

        materialList.add(MaterialEntity().apply {
            resId = R.string.material_btmykl
            product = L1
            dataMode = CanvasConstant.DATA_MODE_GREY
            px = LaserPeckerHelper.PX_1K.toHexInt()
            type = LaserPeckerHelper.LASER_TYPE_BLUE.toHexInt()
            power = 100
            depth = 100
        })
        materialList.add(MaterialEntity().apply {
            resId = R.string.material_btmykl
            product = L1
            dataMode = CanvasConstant.DATA_MODE_GREY
            px = LaserPeckerHelper.PX_1_3K.toHexInt()
            type = LaserPeckerHelper.LASER_TYPE_BLUE.toHexInt()
            power = 90
            depth = 90
        })
        materialList.add(MaterialEntity().apply {
            resId = R.string.material_btmykl
            product = L1
            dataMode = CanvasConstant.DATA_MODE_GREY
            px = LaserPeckerHelper.PX_2K.toHexInt()
            type = LaserPeckerHelper.LASER_TYPE_BLUE.toHexInt()
            power = 90
            depth = 60
        })

        //光敏印章
        materialList.add(MaterialEntity().apply {
            resId = R.string.material_gmyz
            product = L1
            dataMode = CanvasConstant.DATA_MODE_BLACK_WHITE
            px = LaserPeckerHelper.PX_1K.toHexInt()
            type = LaserPeckerHelper.LASER_TYPE_BLUE.toHexInt()
            power = 50
            depth = 10
        })
        materialList.add(MaterialEntity().apply {
            resId = R.string.material_gmyz
            product = L1
            dataMode = CanvasConstant.DATA_MODE_BLACK_WHITE
            px = LaserPeckerHelper.PX_1_3K.toHexInt()
            type = LaserPeckerHelper.LASER_TYPE_BLUE.toHexInt()
            power = 60
            depth = 5
        })
        materialList.add(MaterialEntity().apply {
            resId = R.string.material_gmyz
            product = L1
            dataMode = CanvasConstant.DATA_MODE_BLACK_WHITE
            px = LaserPeckerHelper.PX_2K.toHexInt()
            type = LaserPeckerHelper.LASER_TYPE_BLUE.toHexInt()
            power = 60
            depth = 3
        })

        materialList.add(MaterialEntity().apply {
            resId = R.string.material_gmyz
            product = L1
            dataMode = CanvasConstant.DATA_MODE_GREY
            px = LaserPeckerHelper.PX_1K.toHexInt()
            type = LaserPeckerHelper.LASER_TYPE_BLUE.toHexInt()
            power = 60
            depth = 20
        })
        materialList.add(MaterialEntity().apply {
            resId = R.string.material_gmyz
            product = L1
            dataMode = CanvasConstant.DATA_MODE_GREY
            px = LaserPeckerHelper.PX_1_3K.toHexInt()
            type = LaserPeckerHelper.LASER_TYPE_BLUE.toHexInt()
            power = 60
            depth = 15
        })
        materialList.add(MaterialEntity().apply {
            resId = R.string.material_gmyz
            product = L1
            dataMode = CanvasConstant.DATA_MODE_GREY
            px = LaserPeckerHelper.PX_2K.toHexInt()
            type = LaserPeckerHelper.LASER_TYPE_BLUE.toHexInt()
            power = 60
            depth = 10
        })

        //果皮
        materialList.add(MaterialEntity().apply {
            resId = R.string.material_gp
            product = L1
            dataMode = CanvasConstant.DATA_MODE_BLACK_WHITE
            px = LaserPeckerHelper.PX_1K.toHexInt()
            type = LaserPeckerHelper.LASER_TYPE_BLUE.toHexInt()
            power = 100
            depth = 90
        })
        materialList.add(MaterialEntity().apply {
            resId = R.string.material_gp
            product = L1
            dataMode = CanvasConstant.DATA_MODE_BLACK_WHITE
            px = LaserPeckerHelper.PX_1_3K.toHexInt()
            type = LaserPeckerHelper.LASER_TYPE_BLUE.toHexInt()
            power = 100
            depth = 80
        })
        materialList.add(MaterialEntity().apply {
            resId = R.string.material_gp
            product = L1
            dataMode = CanvasConstant.DATA_MODE_BLACK_WHITE
            px = LaserPeckerHelper.PX_2K.toHexInt()
            type = LaserPeckerHelper.LASER_TYPE_BLUE.toHexInt()
            power = 100
            depth = 60
        })

        materialList.add(MaterialEntity().apply {
            resId = R.string.material_gp
            product = L1
            dataMode = CanvasConstant.DATA_MODE_GREY
            px = LaserPeckerHelper.PX_1K.toHexInt()
            type = LaserPeckerHelper.LASER_TYPE_BLUE.toHexInt()
            power = 100
            depth = 90
        })
        materialList.add(MaterialEntity().apply {
            resId = R.string.material_gp
            product = L1
            dataMode = CanvasConstant.DATA_MODE_GREY
            px = LaserPeckerHelper.PX_1_3K.toHexInt()
            type = LaserPeckerHelper.LASER_TYPE_BLUE.toHexInt()
            power = 100
            depth = 90
        })
        materialList.add(MaterialEntity().apply {
            resId = R.string.material_gp
            product = L1
            dataMode = CanvasConstant.DATA_MODE_GREY
            px = LaserPeckerHelper.PX_2K.toHexInt()
            type = LaserPeckerHelper.LASER_TYPE_BLUE.toHexInt()
            power = 100
            depth = 80
        })

    }

    /**L2设备推荐参数*/
    fun initL2MaterialList(materialList: MutableList<MaterialEntity> = EngraveHelper.materialList) {
        materialList.clear()
        val L2 = buildString {
            append(LaserPeckerHelper.LII)
        }
        //环保纸
        materialList.add(MaterialEntity().apply {
            resId = R.string.material_hbz
            product = L2
            dataMode = CanvasConstant.DATA_MODE_BLACK_WHITE
            px = LaserPeckerHelper.PX_1K.toHexInt()
            type = LaserPeckerHelper.LASER_TYPE_BLUE.toHexInt()
            power = 100
            depth = 3
        })
        materialList.add(MaterialEntity().apply {
            resId = R.string.material_hbz
            product = L2
            dataMode = CanvasConstant.DATA_MODE_BLACK_WHITE
            px = LaserPeckerHelper.PX_1_3K.toHexInt()
            type = LaserPeckerHelper.LASER_TYPE_BLUE.toHexInt()
            power = 90
            depth = 2
        })
        materialList.add(MaterialEntity().apply {
            resId = R.string.material_hbz
            product = L2
            dataMode = CanvasConstant.DATA_MODE_BLACK_WHITE
            px = LaserPeckerHelper.PX_2K.toHexInt()
            type = LaserPeckerHelper.LASER_TYPE_BLUE.toHexInt()
            power = 100
            depth = 1
        })

        materialList.add(MaterialEntity().apply {
            resId = R.string.material_hbz
            product = L2
            dataMode = CanvasConstant.DATA_MODE_GREY
            px = LaserPeckerHelper.PX_1K.toHexInt()
            type = LaserPeckerHelper.LASER_TYPE_BLUE.toHexInt()
            power = 100
            depth = 30
        })
        materialList.add(MaterialEntity().apply {
            resId = R.string.material_hbz
            product = L2
            dataMode = CanvasConstant.DATA_MODE_GREY
            px = LaserPeckerHelper.PX_1_3K.toHexInt()
            type = LaserPeckerHelper.LASER_TYPE_BLUE.toHexInt()
            power = 100
            depth = 15
        })
        materialList.add(MaterialEntity().apply {
            resId = R.string.material_hbz
            product = L2
            dataMode = CanvasConstant.DATA_MODE_GREY
            px = LaserPeckerHelper.PX_2K.toHexInt()
            type = LaserPeckerHelper.LASER_TYPE_BLUE.toHexInt()
            power = 100
            depth = 5
        })

        //瓦楞纸
        materialList.add(MaterialEntity().apply {
            resId = R.string.material_wlz
            product = L2
            dataMode = CanvasConstant.DATA_MODE_BLACK_WHITE
            px = LaserPeckerHelper.PX_1K.toHexInt()
            type = LaserPeckerHelper.LASER_TYPE_BLUE.toHexInt()
            power = 100
            depth = 3
        })
        materialList.add(MaterialEntity().apply {
            resId = R.string.material_wlz
            product = L2
            dataMode = CanvasConstant.DATA_MODE_BLACK_WHITE
            px = LaserPeckerHelper.PX_1_3K.toHexInt()
            type = LaserPeckerHelper.LASER_TYPE_BLUE.toHexInt()
            power = 90
            depth = 2
        })
        materialList.add(MaterialEntity().apply {
            resId = R.string.material_wlz
            product = L2
            dataMode = CanvasConstant.DATA_MODE_BLACK_WHITE
            px = LaserPeckerHelper.PX_2K.toHexInt()
            type = LaserPeckerHelper.LASER_TYPE_BLUE.toHexInt()
            power = 100
            depth = 1
        })

        materialList.add(MaterialEntity().apply {
            resId = R.string.material_wlz
            product = L2
            dataMode = CanvasConstant.DATA_MODE_GREY
            px = LaserPeckerHelper.PX_1K.toHexInt()
            type = LaserPeckerHelper.LASER_TYPE_BLUE.toHexInt()
            power = 100
            depth = 30
        })
        materialList.add(MaterialEntity().apply {
            resId = R.string.material_wlz
            product = L2
            dataMode = CanvasConstant.DATA_MODE_GREY
            px = LaserPeckerHelper.PX_1_3K.toHexInt()
            type = LaserPeckerHelper.LASER_TYPE_BLUE.toHexInt()
            power = 100
            depth = 15
        })
        materialList.add(MaterialEntity().apply {
            resId = R.string.material_wlz
            product = L2
            dataMode = CanvasConstant.DATA_MODE_GREY
            px = LaserPeckerHelper.PX_2K.toHexInt()
            type = LaserPeckerHelper.LASER_TYPE_BLUE.toHexInt()
            power = 100
            depth = 5
        })

        //皮革
        materialList.add(MaterialEntity().apply {
            resId = R.string.material_pg
            product = L2
            dataMode = CanvasConstant.DATA_MODE_BLACK_WHITE
            px = LaserPeckerHelper.PX_1K.toHexInt()
            type = LaserPeckerHelper.LASER_TYPE_BLUE.toHexInt()
            power = 100
            depth = 3
        })
        materialList.add(MaterialEntity().apply {
            resId = R.string.material_pg
            product = L2
            dataMode = CanvasConstant.DATA_MODE_BLACK_WHITE
            px = LaserPeckerHelper.PX_1_3K.toHexInt()
            type = LaserPeckerHelper.LASER_TYPE_BLUE.toHexInt()
            power = 90
            depth = 2
        })
        materialList.add(MaterialEntity().apply {
            resId = R.string.material_pg
            product = L2
            dataMode = CanvasConstant.DATA_MODE_BLACK_WHITE
            px = LaserPeckerHelper.PX_2K.toHexInt()
            type = LaserPeckerHelper.LASER_TYPE_BLUE.toHexInt()
            power = 100
            depth = 1
        })

        materialList.add(MaterialEntity().apply {
            resId = R.string.material_pg
            product = L2
            dataMode = CanvasConstant.DATA_MODE_GREY
            px = LaserPeckerHelper.PX_1K.toHexInt()
            type = LaserPeckerHelper.LASER_TYPE_BLUE.toHexInt()
            power = 30
            depth = 5
        })
        materialList.add(MaterialEntity().apply {
            resId = R.string.material_pg
            product = L2
            dataMode = CanvasConstant.DATA_MODE_GREY
            px = LaserPeckerHelper.PX_1_3K.toHexInt()
            type = LaserPeckerHelper.LASER_TYPE_BLUE.toHexInt()
            power = 30
            depth = 5
        })
        materialList.add(MaterialEntity().apply {
            resId = R.string.material_pg
            product = L2
            dataMode = CanvasConstant.DATA_MODE_GREY
            px = LaserPeckerHelper.PX_2K.toHexInt()
            type = LaserPeckerHelper.LASER_TYPE_BLUE.toHexInt()
            power = 30
            depth = 5
        })

        //竹质
        materialList.add(MaterialEntity().apply {
            resId = R.string.material_zz
            product = L2
            dataMode = CanvasConstant.DATA_MODE_BLACK_WHITE
            px = LaserPeckerHelper.PX_1K.toHexInt()
            type = LaserPeckerHelper.LASER_TYPE_BLUE.toHexInt()
            power = 100
            depth = 10
        })
        materialList.add(MaterialEntity().apply {
            resId = R.string.material_zz
            product = L2
            dataMode = CanvasConstant.DATA_MODE_BLACK_WHITE
            px = LaserPeckerHelper.PX_1_3K.toHexInt()
            type = LaserPeckerHelper.LASER_TYPE_BLUE.toHexInt()
            power = 100
            depth = 5
        })
        materialList.add(MaterialEntity().apply {
            resId = R.string.material_zz
            product = L2
            dataMode = CanvasConstant.DATA_MODE_BLACK_WHITE
            px = LaserPeckerHelper.PX_2K.toHexInt()
            type = LaserPeckerHelper.LASER_TYPE_BLUE.toHexInt()
            power = 100
            depth = 3
        })

        materialList.add(MaterialEntity().apply {
            resId = R.string.material_zz
            product = L2
            dataMode = CanvasConstant.DATA_MODE_GREY
            px = LaserPeckerHelper.PX_1K.toHexInt()
            type = LaserPeckerHelper.LASER_TYPE_BLUE.toHexInt()
            power = 100
            depth = 40
        })
        materialList.add(MaterialEntity().apply {
            resId = R.string.material_zz
            product = L2
            dataMode = CanvasConstant.DATA_MODE_GREY
            px = LaserPeckerHelper.PX_1_3K.toHexInt()
            type = LaserPeckerHelper.LASER_TYPE_BLUE.toHexInt()
            power = 100
            depth = 30
        })
        materialList.add(MaterialEntity().apply {
            resId = R.string.material_zz
            product = L2
            dataMode = CanvasConstant.DATA_MODE_GREY
            px = LaserPeckerHelper.PX_2K.toHexInt()
            type = LaserPeckerHelper.LASER_TYPE_BLUE.toHexInt()
            power = 100
            depth = 20
        })

        //木质
        materialList.add(MaterialEntity().apply {
            resId = R.string.material_mz
            product = L2
            dataMode = CanvasConstant.DATA_MODE_BLACK_WHITE
            px = LaserPeckerHelper.PX_1K.toHexInt()
            type = LaserPeckerHelper.LASER_TYPE_BLUE.toHexInt()
            power = 100
            depth = 10
        })
        materialList.add(MaterialEntity().apply {
            resId = R.string.material_mz
            product = L2
            dataMode = CanvasConstant.DATA_MODE_BLACK_WHITE
            px = LaserPeckerHelper.PX_1_3K.toHexInt()
            type = LaserPeckerHelper.LASER_TYPE_BLUE.toHexInt()
            power = 100
            depth = 5
        })
        materialList.add(MaterialEntity().apply {
            resId = R.string.material_mz
            product = L2
            dataMode = CanvasConstant.DATA_MODE_BLACK_WHITE
            px = LaserPeckerHelper.PX_2K.toHexInt()
            type = LaserPeckerHelper.LASER_TYPE_BLUE.toHexInt()
            power = 100
            depth = 3
        })

        materialList.add(MaterialEntity().apply {
            resId = R.string.material_mz
            product = L2
            dataMode = CanvasConstant.DATA_MODE_GREY
            px = LaserPeckerHelper.PX_1K.toHexInt()
            type = LaserPeckerHelper.LASER_TYPE_BLUE.toHexInt()
            power = 100
            depth = 30
        })
        materialList.add(MaterialEntity().apply {
            resId = R.string.material_mz
            product = L2
            dataMode = CanvasConstant.DATA_MODE_GREY
            px = LaserPeckerHelper.PX_1_3K.toHexInt()
            type = LaserPeckerHelper.LASER_TYPE_BLUE.toHexInt()
            power = 100
            depth = 20
        })
        materialList.add(MaterialEntity().apply {
            resId = R.string.material_mz
            product = L2
            dataMode = CanvasConstant.DATA_MODE_GREY
            px = LaserPeckerHelper.PX_2K.toHexInt()
            type = LaserPeckerHelper.LASER_TYPE_BLUE.toHexInt()
            power = 100
            depth = 10
        })

        //软木
        materialList.add(MaterialEntity().apply {
            resId = R.string.material_rm
            product = L2
            dataMode = CanvasConstant.DATA_MODE_BLACK_WHITE
            px = LaserPeckerHelper.PX_1K.toHexInt()
            type = LaserPeckerHelper.LASER_TYPE_BLUE.toHexInt()
            power = 100
            depth = 1
        })
        materialList.add(MaterialEntity().apply {
            resId = R.string.material_rm
            product = L2
            dataMode = CanvasConstant.DATA_MODE_BLACK_WHITE
            px = LaserPeckerHelper.PX_1_3K.toHexInt()
            type = LaserPeckerHelper.LASER_TYPE_BLUE.toHexInt()
            power = 80
            depth = 1
        })
        materialList.add(MaterialEntity().apply {
            resId = R.string.material_rm
            product = L2
            dataMode = CanvasConstant.DATA_MODE_BLACK_WHITE
            px = LaserPeckerHelper.PX_2K.toHexInt()
            type = LaserPeckerHelper.LASER_TYPE_BLUE.toHexInt()
            power = 50
            depth = 1
        })

        materialList.add(MaterialEntity().apply {
            resId = R.string.material_rm
            product = L2
            dataMode = CanvasConstant.DATA_MODE_GREY
            px = LaserPeckerHelper.PX_1K.toHexInt()
            type = LaserPeckerHelper.LASER_TYPE_BLUE.toHexInt()
            power = 30
            depth = 1
        })
        materialList.add(MaterialEntity().apply {
            resId = R.string.material_rm
            product = L2
            dataMode = CanvasConstant.DATA_MODE_GREY
            px = LaserPeckerHelper.PX_1_3K.toHexInt()
            type = LaserPeckerHelper.LASER_TYPE_BLUE.toHexInt()
            power = 30
            depth = 1
        })
        materialList.add(MaterialEntity().apply {
            resId = R.string.material_rm
            product = L2
            dataMode = CanvasConstant.DATA_MODE_GREY
            px = LaserPeckerHelper.PX_2K.toHexInt()
            type = LaserPeckerHelper.LASER_TYPE_BLUE.toHexInt()
            power = 30
            depth = 1
        })

        //塑料
        materialList.add(MaterialEntity().apply {
            resId = R.string.material_sl
            product = L2
            dataMode = CanvasConstant.DATA_MODE_BLACK_WHITE
            px = LaserPeckerHelper.PX_1K.toHexInt()
            type = LaserPeckerHelper.LASER_TYPE_BLUE.toHexInt()
            power = 100
            depth = 2
        })
        materialList.add(MaterialEntity().apply {
            resId = R.string.material_sl
            product = L2
            dataMode = CanvasConstant.DATA_MODE_BLACK_WHITE
            px = LaserPeckerHelper.PX_1_3K.toHexInt()
            type = LaserPeckerHelper.LASER_TYPE_BLUE.toHexInt()
            power = 100
            depth = 2
        })
        materialList.add(MaterialEntity().apply {
            resId = R.string.material_sl
            product = L2
            dataMode = CanvasConstant.DATA_MODE_BLACK_WHITE
            px = LaserPeckerHelper.PX_2K.toHexInt()
            type = LaserPeckerHelper.LASER_TYPE_BLUE.toHexInt()
            power = 50
            depth = 2
        })

        materialList.add(MaterialEntity().apply {
            resId = R.string.material_sl
            product = L2
            dataMode = CanvasConstant.DATA_MODE_GREY
            px = LaserPeckerHelper.PX_1K.toHexInt()
            type = LaserPeckerHelper.LASER_TYPE_BLUE.toHexInt()
            power = 100
            depth = 5
        })
        materialList.add(MaterialEntity().apply {
            resId = R.string.material_sl
            product = L2
            dataMode = CanvasConstant.DATA_MODE_GREY
            px = LaserPeckerHelper.PX_1_3K.toHexInt()
            type = LaserPeckerHelper.LASER_TYPE_BLUE.toHexInt()
            power = 100
            depth = 5
        })
        materialList.add(MaterialEntity().apply {
            resId = R.string.material_sl
            product = L2
            dataMode = CanvasConstant.DATA_MODE_GREY
            px = LaserPeckerHelper.PX_2K.toHexInt()
            type = LaserPeckerHelper.LASER_TYPE_BLUE.toHexInt()
            power = 100
            depth = 5
        })

        //布料
        materialList.add(MaterialEntity().apply {
            resId = R.string.material_bl
            product = L2
            dataMode = CanvasConstant.DATA_MODE_BLACK_WHITE
            px = LaserPeckerHelper.PX_1K.toHexInt()
            type = LaserPeckerHelper.LASER_TYPE_BLUE.toHexInt()
            power = 70
            depth = 2
        })
        materialList.add(MaterialEntity().apply {
            resId = R.string.material_bl
            product = L2
            dataMode = CanvasConstant.DATA_MODE_BLACK_WHITE
            px = LaserPeckerHelper.PX_1_3K.toHexInt()
            type = LaserPeckerHelper.LASER_TYPE_BLUE.toHexInt()
            power = 45
            depth = 2
        })
        materialList.add(MaterialEntity().apply {
            resId = R.string.material_bl
            product = L2
            dataMode = CanvasConstant.DATA_MODE_BLACK_WHITE
            px = LaserPeckerHelper.PX_2K.toHexInt()
            type = LaserPeckerHelper.LASER_TYPE_BLUE.toHexInt()
            power = 25
            depth = 2
        })

        materialList.add(MaterialEntity().apply {
            resId = R.string.material_bl
            product = L2
            dataMode = CanvasConstant.DATA_MODE_GREY
            px = LaserPeckerHelper.PX_1K.toHexInt()
            type = LaserPeckerHelper.LASER_TYPE_BLUE.toHexInt()
            power = 100
            depth = 15
        })
        materialList.add(MaterialEntity().apply {
            resId = R.string.material_bl
            product = L2
            dataMode = CanvasConstant.DATA_MODE_GREY
            px = LaserPeckerHelper.PX_1_3K.toHexInt()
            type = LaserPeckerHelper.LASER_TYPE_BLUE.toHexInt()
            power = 100
            depth = 10
        })
        materialList.add(MaterialEntity().apply {
            resId = R.string.material_bl
            product = L2
            dataMode = CanvasConstant.DATA_MODE_GREY
            px = LaserPeckerHelper.PX_2K.toHexInt()
            type = LaserPeckerHelper.LASER_TYPE_BLUE.toHexInt()
            power = 80
            depth = 1
        })

        //毛毡布
        materialList.add(MaterialEntity().apply {
            resId = R.string.material_mzb
            product = L2
            dataMode = CanvasConstant.DATA_MODE_BLACK_WHITE
            px = LaserPeckerHelper.PX_1K.toHexInt()
            type = LaserPeckerHelper.LASER_TYPE_BLUE.toHexInt()
            power = 100
            depth = 1
        })
        materialList.add(MaterialEntity().apply {
            resId = R.string.material_mzb
            product = L2
            dataMode = CanvasConstant.DATA_MODE_BLACK_WHITE
            px = LaserPeckerHelper.PX_1_3K.toHexInt()
            type = LaserPeckerHelper.LASER_TYPE_BLUE.toHexInt()
            power = 80
            depth = 1
        })
        materialList.add(MaterialEntity().apply {
            resId = R.string.material_mzb
            product = L2
            dataMode = CanvasConstant.DATA_MODE_BLACK_WHITE
            px = LaserPeckerHelper.PX_2K.toHexInt()
            type = LaserPeckerHelper.LASER_TYPE_BLUE.toHexInt()
            power = 40
            depth = 1
        })

        materialList.add(MaterialEntity().apply {
            resId = R.string.material_mzb
            product = L2
            dataMode = CanvasConstant.DATA_MODE_GREY
            px = LaserPeckerHelper.PX_1K.toHexInt()
            type = LaserPeckerHelper.LASER_TYPE_BLUE.toHexInt()
            power = 100
            depth = 1
        })
        materialList.add(MaterialEntity().apply {
            resId = R.string.material_mzb
            product = L2
            dataMode = CanvasConstant.DATA_MODE_GREY
            px = LaserPeckerHelper.PX_1_3K.toHexInt()
            type = LaserPeckerHelper.LASER_TYPE_BLUE.toHexInt()
            power = 100
            depth = 1
        })
        materialList.add(MaterialEntity().apply {
            resId = R.string.material_mzb
            product = L2
            dataMode = CanvasConstant.DATA_MODE_GREY
            px = LaserPeckerHelper.PX_2K.toHexInt()
            type = LaserPeckerHelper.LASER_TYPE_BLUE.toHexInt()
            power = 100
            depth = 1
        })

        //不透明亚克力
        materialList.add(MaterialEntity().apply {
            resId = R.string.material_btmykl
            product = L2
            dataMode = CanvasConstant.DATA_MODE_BLACK_WHITE
            px = LaserPeckerHelper.PX_1K.toHexInt()
            type = LaserPeckerHelper.LASER_TYPE_BLUE.toHexInt()
            power = 100
            depth = 2
        })
        materialList.add(MaterialEntity().apply {
            resId = R.string.material_btmykl
            product = L2
            dataMode = CanvasConstant.DATA_MODE_BLACK_WHITE
            px = LaserPeckerHelper.PX_1_3K.toHexInt()
            type = LaserPeckerHelper.LASER_TYPE_BLUE.toHexInt()
            power = 80
            depth = 2
        })
        materialList.add(MaterialEntity().apply {
            resId = R.string.material_btmykl
            product = L2
            dataMode = CanvasConstant.DATA_MODE_BLACK_WHITE
            px = LaserPeckerHelper.PX_2K.toHexInt()
            type = LaserPeckerHelper.LASER_TYPE_BLUE.toHexInt()
            power = 40
            depth = 2
        })

        materialList.add(MaterialEntity().apply {
            resId = R.string.material_btmykl
            product = L2
            dataMode = CanvasConstant.DATA_MODE_GREY
            px = LaserPeckerHelper.PX_1K.toHexInt()
            type = LaserPeckerHelper.LASER_TYPE_BLUE.toHexInt()
            power = 90
            depth = 20
        })
        materialList.add(MaterialEntity().apply {
            resId = R.string.material_btmykl
            product = L2
            dataMode = CanvasConstant.DATA_MODE_GREY
            px = LaserPeckerHelper.PX_1_3K.toHexInt()
            type = LaserPeckerHelper.LASER_TYPE_BLUE.toHexInt()
            power = 70
            depth = 15
        })
        materialList.add(MaterialEntity().apply {
            resId = R.string.material_btmykl
            product = L2
            dataMode = CanvasConstant.DATA_MODE_GREY
            px = LaserPeckerHelper.PX_2K.toHexInt()
            type = LaserPeckerHelper.LASER_TYPE_BLUE.toHexInt()
            power = 70
            depth = 10
        })

        //光敏印章
        materialList.add(MaterialEntity().apply {
            resId = R.string.material_gmyz
            product = L2
            dataMode = CanvasConstant.DATA_MODE_BLACK_WHITE
            px = LaserPeckerHelper.PX_1K.toHexInt()
            type = LaserPeckerHelper.LASER_TYPE_BLUE.toHexInt()
            power = 50
            depth = 1
        })
        materialList.add(MaterialEntity().apply {
            resId = R.string.material_gmyz
            product = L2
            dataMode = CanvasConstant.DATA_MODE_BLACK_WHITE
            px = LaserPeckerHelper.PX_1_3K.toHexInt()
            type = LaserPeckerHelper.LASER_TYPE_BLUE.toHexInt()
            power = 40
            depth = 1
        })
        materialList.add(MaterialEntity().apply {
            resId = R.string.material_gmyz
            product = L2
            dataMode = CanvasConstant.DATA_MODE_BLACK_WHITE
            px = LaserPeckerHelper.PX_2K.toHexInt()
            type = LaserPeckerHelper.LASER_TYPE_BLUE.toHexInt()
            power = 20
            depth = 1
        })

        materialList.add(MaterialEntity().apply {
            resId = R.string.material_gmyz
            product = L2
            dataMode = CanvasConstant.DATA_MODE_GREY
            px = LaserPeckerHelper.PX_1K.toHexInt()
            type = LaserPeckerHelper.LASER_TYPE_BLUE.toHexInt()
            power = 100
            depth = 1
        })
        materialList.add(MaterialEntity().apply {
            resId = R.string.material_gmyz
            product = L2
            dataMode = CanvasConstant.DATA_MODE_GREY
            px = LaserPeckerHelper.PX_1_3K.toHexInt()
            type = LaserPeckerHelper.LASER_TYPE_BLUE.toHexInt()
            power = 100
            depth = 1
        })
        materialList.add(MaterialEntity().apply {
            resId = R.string.material_gmyz
            product = L2
            dataMode = CanvasConstant.DATA_MODE_GREY
            px = LaserPeckerHelper.PX_2K.toHexInt()
            type = LaserPeckerHelper.LASER_TYPE_BLUE.toHexInt()
            power = 100
            depth = 1
        })

        //果皮
        materialList.add(MaterialEntity().apply {
            resId = R.string.material_gp
            product = L2
            dataMode = CanvasConstant.DATA_MODE_BLACK_WHITE
            px = LaserPeckerHelper.PX_1K.toHexInt()
            type = LaserPeckerHelper.LASER_TYPE_BLUE.toHexInt()
            power = 100
            depth = 5
        })
        materialList.add(MaterialEntity().apply {
            resId = R.string.material_gp
            product = L2
            dataMode = CanvasConstant.DATA_MODE_BLACK_WHITE
            px = LaserPeckerHelper.PX_1_3K.toHexInt()
            type = LaserPeckerHelper.LASER_TYPE_BLUE.toHexInt()
            power = 100
            depth = 5
        })
        materialList.add(MaterialEntity().apply {
            resId = R.string.material_gp
            product = L2
            dataMode = CanvasConstant.DATA_MODE_BLACK_WHITE
            px = LaserPeckerHelper.PX_2K.toHexInt()
            type = LaserPeckerHelper.LASER_TYPE_BLUE.toHexInt()
            power = 100
            depth = 3
        })

        materialList.add(MaterialEntity().apply {
            resId = R.string.material_gp
            product = L2
            dataMode = CanvasConstant.DATA_MODE_GREY
            px = LaserPeckerHelper.PX_1K.toHexInt()
            type = LaserPeckerHelper.LASER_TYPE_BLUE.toHexInt()
            power = 100
            depth = 80
        })
        materialList.add(MaterialEntity().apply {
            resId = R.string.material_gp
            product = L2
            dataMode = CanvasConstant.DATA_MODE_GREY
            px = LaserPeckerHelper.PX_1_3K.toHexInt()
            type = LaserPeckerHelper.LASER_TYPE_BLUE.toHexInt()
            power = 100
            depth = 80
        })
        materialList.add(MaterialEntity().apply {
            resId = R.string.material_gp
            product = L2
            dataMode = CanvasConstant.DATA_MODE_GREY
            px = LaserPeckerHelper.PX_2K.toHexInt()
            type = LaserPeckerHelper.LASER_TYPE_BLUE.toHexInt()
            power = 100
            depth = 80
        })

        //可乐罐
        materialList.add(MaterialEntity().apply {
            resId = R.string.material_klg
            product = L2
            dataMode = CanvasConstant.DATA_MODE_BLACK_WHITE
            px = LaserPeckerHelper.PX_1K.toHexInt()
            type = LaserPeckerHelper.LASER_TYPE_BLUE.toHexInt()
            power = 100
            depth = 10
        })
        materialList.add(MaterialEntity().apply {
            resId = R.string.material_klg
            product = L2
            dataMode = CanvasConstant.DATA_MODE_BLACK_WHITE
            px = LaserPeckerHelper.PX_1_3K.toHexInt()
            type = LaserPeckerHelper.LASER_TYPE_BLUE.toHexInt()
            power = 100
            depth = 5
        })
        materialList.add(MaterialEntity().apply {
            resId = R.string.material_klg
            product = L2
            dataMode = CanvasConstant.DATA_MODE_BLACK_WHITE
            px = LaserPeckerHelper.PX_2K.toHexInt()
            type = LaserPeckerHelper.LASER_TYPE_BLUE.toHexInt()
            power = 100
            depth = 3
        })

        materialList.add(MaterialEntity().apply {
            resId = R.string.material_klg
            product = L2
            dataMode = CanvasConstant.DATA_MODE_GREY
            px = LaserPeckerHelper.PX_1K.toHexInt()
            type = LaserPeckerHelper.LASER_TYPE_BLUE.toHexInt()
            power = 100
            depth = 100
        })
        materialList.add(MaterialEntity().apply {
            resId = R.string.material_klg
            product = L2
            dataMode = CanvasConstant.DATA_MODE_GREY
            px = LaserPeckerHelper.PX_1_3K.toHexInt()
            type = LaserPeckerHelper.LASER_TYPE_BLUE.toHexInt()
            power = 100
            depth = 80
        })
        materialList.add(MaterialEntity().apply {
            resId = R.string.material_klg
            product = L2
            dataMode = CanvasConstant.DATA_MODE_GREY
            px = LaserPeckerHelper.PX_2K.toHexInt()
            type = LaserPeckerHelper.LASER_TYPE_BLUE.toHexInt()
            power = 100
            depth = 50
        })

        //石头
        materialList.add(MaterialEntity().apply {
            resId = R.string.material_st
            product = L2
            dataMode = CanvasConstant.DATA_MODE_BLACK_WHITE
            px = LaserPeckerHelper.PX_1K.toHexInt()
            type = LaserPeckerHelper.LASER_TYPE_BLUE.toHexInt()
            power = 100
            depth = 100
        })
        materialList.add(MaterialEntity().apply {
            resId = R.string.material_st
            product = L2
            dataMode = CanvasConstant.DATA_MODE_BLACK_WHITE
            px = LaserPeckerHelper.PX_1_3K.toHexInt()
            type = LaserPeckerHelper.LASER_TYPE_BLUE.toHexInt()
            power = 100
            depth = 100
        })
        materialList.add(MaterialEntity().apply {
            resId = R.string.material_st
            product = L2
            dataMode = CanvasConstant.DATA_MODE_BLACK_WHITE
            px = LaserPeckerHelper.PX_2K.toHexInt()
            type = LaserPeckerHelper.LASER_TYPE_BLUE.toHexInt()
            power = 100
            depth = 50
        })

        materialList.add(MaterialEntity().apply {
            resId = R.string.material_st
            product = L2
            dataMode = CanvasConstant.DATA_MODE_GREY
            px = LaserPeckerHelper.PX_1K.toHexInt()
            type = LaserPeckerHelper.LASER_TYPE_BLUE.toHexInt()
            power = 100
            depth = 100
        })
        materialList.add(MaterialEntity().apply {
            resId = R.string.material_st
            product = L2
            dataMode = CanvasConstant.DATA_MODE_GREY
            px = LaserPeckerHelper.PX_1_3K.toHexInt()
            type = LaserPeckerHelper.LASER_TYPE_BLUE.toHexInt()
            power = 100
            depth = 100
        })
        materialList.add(MaterialEntity().apply {
            resId = R.string.material_st
            product = L2
            dataMode = CanvasConstant.DATA_MODE_GREY
            px = LaserPeckerHelper.PX_2K.toHexInt()
            type = LaserPeckerHelper.LASER_TYPE_BLUE.toHexInt()
            power = 100
            depth = 100
        })

        //氧化金属
        materialList.add(MaterialEntity().apply {
            resId = R.string.material_yhjs
            product = L2
            dataMode = CanvasConstant.DATA_MODE_BLACK_WHITE
            px = LaserPeckerHelper.PX_1K.toHexInt()
            type = LaserPeckerHelper.LASER_TYPE_BLUE.toHexInt()
            power = 100
            depth = 10
        })
        materialList.add(MaterialEntity().apply {
            resId = R.string.material_yhjs
            product = L2
            dataMode = CanvasConstant.DATA_MODE_BLACK_WHITE
            px = LaserPeckerHelper.PX_1_3K.toHexInt()
            type = LaserPeckerHelper.LASER_TYPE_BLUE.toHexInt()
            power = 100
            depth = 10
        })
        materialList.add(MaterialEntity().apply {
            resId = R.string.material_yhjs
            product = L2
            dataMode = CanvasConstant.DATA_MODE_BLACK_WHITE
            px = LaserPeckerHelper.PX_2K.toHexInt()
            type = LaserPeckerHelper.LASER_TYPE_BLUE.toHexInt()
            power = 100
            depth = 5
        })

        materialList.add(MaterialEntity().apply {
            resId = R.string.material_yhjs
            product = L2
            dataMode = CanvasConstant.DATA_MODE_GREY
            px = LaserPeckerHelper.PX_1K.toHexInt()
            type = LaserPeckerHelper.LASER_TYPE_BLUE.toHexInt()
            power = 100
            depth = 100
        })
        materialList.add(MaterialEntity().apply {
            resId = R.string.material_yhjs
            product = L2
            dataMode = CanvasConstant.DATA_MODE_GREY
            px = LaserPeckerHelper.PX_1_3K.toHexInt()
            type = LaserPeckerHelper.LASER_TYPE_BLUE.toHexInt()
            power = 100
            depth = 60
        })
        materialList.add(MaterialEntity().apply {
            resId = R.string.material_yhjs
            product = L2
            dataMode = CanvasConstant.DATA_MODE_GREY
            px = LaserPeckerHelper.PX_2K.toHexInt()
            type = LaserPeckerHelper.LASER_TYPE_BLUE.toHexInt()
            power = 100
            depth = 10
        })

        //陶制品
        materialList.add(MaterialEntity().apply {
            resId = R.string.material_tzp
            product = L2
            dataMode = CanvasConstant.DATA_MODE_BLACK_WHITE
            px = LaserPeckerHelper.PX_1K.toHexInt()
            type = LaserPeckerHelper.LASER_TYPE_BLUE.toHexInt()
            power = 100
            depth = 10
        })
        materialList.add(MaterialEntity().apply {
            resId = R.string.material_tzp
            product = L2
            dataMode = CanvasConstant.DATA_MODE_BLACK_WHITE
            px = LaserPeckerHelper.PX_1_3K.toHexInt()
            type = LaserPeckerHelper.LASER_TYPE_BLUE.toHexInt()
            power = 100
            depth = 10
        })
        materialList.add(MaterialEntity().apply {
            resId = R.string.material_tzp
            product = L2
            dataMode = CanvasConstant.DATA_MODE_BLACK_WHITE
            px = LaserPeckerHelper.PX_2K.toHexInt()
            type = LaserPeckerHelper.LASER_TYPE_BLUE.toHexInt()
            power = 100
            depth = 5
        })

        materialList.add(MaterialEntity().apply {
            resId = R.string.material_tzp
            product = L2
            dataMode = CanvasConstant.DATA_MODE_GREY
            px = LaserPeckerHelper.PX_1K.toHexInt()
            type = LaserPeckerHelper.LASER_TYPE_BLUE.toHexInt()
            power = 100
            depth = 50
        })
        materialList.add(MaterialEntity().apply {
            resId = R.string.material_tzp
            product = L2
            dataMode = CanvasConstant.DATA_MODE_GREY
            px = LaserPeckerHelper.PX_1_3K.toHexInt()
            type = LaserPeckerHelper.LASER_TYPE_BLUE.toHexInt()
            power = 100
            depth = 50
        })
        materialList.add(MaterialEntity().apply {
            resId = R.string.material_tzp
            product = L2
            dataMode = CanvasConstant.DATA_MODE_GREY
            px = LaserPeckerHelper.PX_2K.toHexInt()
            type = LaserPeckerHelper.LASER_TYPE_BLUE.toHexInt()
            power = 100
            depth = 50
        })

        //漆涂层
        materialList.add(MaterialEntity().apply {
            resId = R.string.material_qtc
            product = L2
            dataMode = CanvasConstant.DATA_MODE_BLACK_WHITE
            px = LaserPeckerHelper.PX_1K.toHexInt()
            type = LaserPeckerHelper.LASER_TYPE_BLUE.toHexInt()
            power = 70
            depth = 1
        })
        materialList.add(MaterialEntity().apply {
            resId = R.string.material_qtc
            product = L2
            dataMode = CanvasConstant.DATA_MODE_BLACK_WHITE
            px = LaserPeckerHelper.PX_1_3K.toHexInt()
            type = LaserPeckerHelper.LASER_TYPE_BLUE.toHexInt()
            power = 70
            depth = 1
        })
        materialList.add(MaterialEntity().apply {
            resId = R.string.material_qtc
            product = L2
            dataMode = CanvasConstant.DATA_MODE_BLACK_WHITE
            px = LaserPeckerHelper.PX_2K.toHexInt()
            type = LaserPeckerHelper.LASER_TYPE_BLUE.toHexInt()
            power = 50
            depth = 1
        })

        materialList.add(MaterialEntity().apply {
            resId = R.string.material_qtc
            product = L2
            dataMode = CanvasConstant.DATA_MODE_GREY
            px = LaserPeckerHelper.PX_1K.toHexInt()
            type = LaserPeckerHelper.LASER_TYPE_BLUE.toHexInt()
            power = 100
            depth = 10
        })
        materialList.add(MaterialEntity().apply {
            resId = R.string.material_qtc
            product = L2
            dataMode = CanvasConstant.DATA_MODE_GREY
            px = LaserPeckerHelper.PX_1_3K.toHexInt()
            type = LaserPeckerHelper.LASER_TYPE_BLUE.toHexInt()
            power = 100
            depth = 10
        })
        materialList.add(MaterialEntity().apply {
            resId = R.string.material_qtc
            product = L2
            dataMode = CanvasConstant.DATA_MODE_GREY
            px = LaserPeckerHelper.PX_2K.toHexInt()
            type = LaserPeckerHelper.LASER_TYPE_BLUE.toHexInt()
            power = 100
            depth = 10
        })

        //不锈钢
        materialList.add(MaterialEntity().apply {
            resId = R.string.material_bxg
            product = L2
            dataMode = CanvasConstant.DATA_MODE_BLACK_WHITE
            px = LaserPeckerHelper.PX_1K.toHexInt()
            type = LaserPeckerHelper.LASER_TYPE_BLUE.toHexInt()
            power = 100
            depth = 100
        })
        materialList.add(MaterialEntity().apply {
            resId = R.string.material_bxg
            product = L2
            dataMode = CanvasConstant.DATA_MODE_BLACK_WHITE
            px = LaserPeckerHelper.PX_1_3K.toHexInt()
            type = LaserPeckerHelper.LASER_TYPE_BLUE.toHexInt()
            power = 100
            depth = 100
        })
        materialList.add(MaterialEntity().apply {
            resId = R.string.material_bxg
            product = L2
            dataMode = CanvasConstant.DATA_MODE_BLACK_WHITE
            px = LaserPeckerHelper.PX_2K.toHexInt()
            type = LaserPeckerHelper.LASER_TYPE_BLUE.toHexInt()
            power = 100
            depth = 100
        })

        materialList.add(MaterialEntity().apply {
            resId = R.string.material_bxg
            product = L2
            dataMode = CanvasConstant.DATA_MODE_GREY
            px = LaserPeckerHelper.PX_1K.toHexInt()
            type = LaserPeckerHelper.LASER_TYPE_BLUE.toHexInt()
            power = 100
            depth = 100
        })
        materialList.add(MaterialEntity().apply {
            resId = R.string.material_bxg
            product = L2
            dataMode = CanvasConstant.DATA_MODE_GREY
            px = LaserPeckerHelper.PX_1_3K.toHexInt()
            type = LaserPeckerHelper.LASER_TYPE_BLUE.toHexInt()
            power = 100
            depth = 100
        })
        materialList.add(MaterialEntity().apply {
            resId = R.string.material_bxg
            product = L2
            dataMode = CanvasConstant.DATA_MODE_GREY
            px = LaserPeckerHelper.PX_2K.toHexInt()
            type = LaserPeckerHelper.LASER_TYPE_BLUE.toHexInt()
            power = 100
            depth = 100
        })
    }

    /**L3设备推荐参数*/
    fun initL3MaterialList(materialList: MutableList<MaterialEntity> = EngraveHelper.materialList) {
        materialList.clear()
        val L3 = buildString {
            append(LaserPeckerHelper.LIII)
            append(MATERIAL_SPLIT)
            append(LaserPeckerHelper.LIII_YT)
        }
        //光敏印章
        materialList.add(MaterialEntity().apply {
            resId = R.string.material_gmyz
            product = L3
            dataMode = CanvasConstant.DATA_MODE_BLACK_WHITE
            px = LaserPeckerHelper.PX_1K.toHexInt()
            type = LaserPeckerHelper.LASER_TYPE_WHITE.toHexInt()
            power = 60
            depth = 20
        })
        materialList.add(MaterialEntity().apply {
            resId = R.string.material_gmyz
            product = L3
            dataMode = CanvasConstant.DATA_MODE_BLACK_WHITE
            px = LaserPeckerHelper.PX_2K.toHexInt()
            type = LaserPeckerHelper.LASER_TYPE_WHITE.toHexInt()
            power = 60
            depth = 10
        })
        materialList.add(MaterialEntity().apply {
            resId = R.string.material_gmyz
            product = L3
            dataMode = CanvasConstant.DATA_MODE_BLACK_WHITE
            px = LaserPeckerHelper.PX_4K.toHexInt()
            type = LaserPeckerHelper.LASER_TYPE_WHITE.toHexInt()
            power = 60
            depth = 3
        })

        materialList.add(MaterialEntity().apply {
            resId = R.string.material_gmyz
            product = L3
            dataMode = CanvasConstant.DATA_MODE_GREY
            px = LaserPeckerHelper.PX_1K.toHexInt()
            type = LaserPeckerHelper.LASER_TYPE_WHITE.toHexInt()
            power = 60
            depth = 30
        })
        materialList.add(MaterialEntity().apply {
            resId = R.string.material_gmyz
            product = L3
            dataMode = CanvasConstant.DATA_MODE_GREY
            px = LaserPeckerHelper.PX_2K.toHexInt()
            type = LaserPeckerHelper.LASER_TYPE_WHITE.toHexInt()
            power = 60
            depth = 10
        })
        materialList.add(MaterialEntity().apply {
            resId = R.string.material_gmyz
            product = L3
            dataMode = CanvasConstant.DATA_MODE_GREY
            px = LaserPeckerHelper.PX_4K.toHexInt()
            type = LaserPeckerHelper.LASER_TYPE_WHITE.toHexInt()
            power = 60
            depth = 5
        })

        //硅胶
        materialList.add(MaterialEntity().apply {
            resId = R.string.material_gj
            product = L3
            dataMode = CanvasConstant.DATA_MODE_BLACK_WHITE
            px = LaserPeckerHelper.PX_1K.toHexInt()
            type = LaserPeckerHelper.LASER_TYPE_WHITE.toHexInt()
            power = 100
            depth = 5
        })
        materialList.add(MaterialEntity().apply {
            resId = R.string.material_gj
            product = L3
            dataMode = CanvasConstant.DATA_MODE_BLACK_WHITE
            px = LaserPeckerHelper.PX_2K.toHexInt()
            type = LaserPeckerHelper.LASER_TYPE_WHITE.toHexInt()
            power = 100
            depth = 1
        })
        materialList.add(MaterialEntity().apply {
            resId = R.string.material_gj
            product = L3
            dataMode = CanvasConstant.DATA_MODE_BLACK_WHITE
            px = LaserPeckerHelper.PX_4K.toHexInt()
            type = LaserPeckerHelper.LASER_TYPE_WHITE.toHexInt()
            power = 40
            depth = 1
        })

        materialList.add(MaterialEntity().apply {
            resId = R.string.material_gj
            product = L3
            dataMode = CanvasConstant.DATA_MODE_GREY
            px = LaserPeckerHelper.PX_1K.toHexInt()
            type = LaserPeckerHelper.LASER_TYPE_WHITE.toHexInt()
            power = 100
            depth = 60
        })
        materialList.add(MaterialEntity().apply {
            resId = R.string.material_gj
            product = L3
            dataMode = CanvasConstant.DATA_MODE_GREY
            px = LaserPeckerHelper.PX_2K.toHexInt()
            type = LaserPeckerHelper.LASER_TYPE_WHITE.toHexInt()
            power = 100
            depth = 30
        })
        materialList.add(MaterialEntity().apply {
            resId = R.string.material_gj
            product = L3
            dataMode = CanvasConstant.DATA_MODE_GREY
            px = LaserPeckerHelper.PX_4K.toHexInt()
            type = LaserPeckerHelper.LASER_TYPE_WHITE.toHexInt()
            power = 100
            depth = 10
        })

        //塑料
        materialList.add(MaterialEntity().apply {
            resId = R.string.material_sl
            product = L3
            dataMode = CanvasConstant.DATA_MODE_BLACK_WHITE
            px = LaserPeckerHelper.PX_1K.toHexInt()
            type = LaserPeckerHelper.LASER_TYPE_WHITE.toHexInt()
            power = 100
            depth = 5
        })
        materialList.add(MaterialEntity().apply {
            resId = R.string.material_sl
            product = L3
            dataMode = CanvasConstant.DATA_MODE_BLACK_WHITE
            px = LaserPeckerHelper.PX_2K.toHexInt()
            type = LaserPeckerHelper.LASER_TYPE_WHITE.toHexInt()
            power = 100
            depth = 1
        })
        materialList.add(MaterialEntity().apply {
            resId = R.string.material_sl
            product = L3
            dataMode = CanvasConstant.DATA_MODE_BLACK_WHITE
            px = LaserPeckerHelper.PX_4K.toHexInt()
            type = LaserPeckerHelper.LASER_TYPE_WHITE.toHexInt()
            power = 40
            depth = 1
        })

        materialList.add(MaterialEntity().apply {
            resId = R.string.material_sl
            product = L3
            dataMode = CanvasConstant.DATA_MODE_GREY
            px = LaserPeckerHelper.PX_1K.toHexInt()
            type = LaserPeckerHelper.LASER_TYPE_WHITE.toHexInt()
            power = 100
            depth = 30
        })
        materialList.add(MaterialEntity().apply {
            resId = R.string.material_sl
            product = L3
            dataMode = CanvasConstant.DATA_MODE_GREY
            px = LaserPeckerHelper.PX_2K.toHexInt()
            type = LaserPeckerHelper.LASER_TYPE_WHITE.toHexInt()
            power = 80
            depth = 15
        })
        materialList.add(MaterialEntity().apply {
            resId = R.string.material_sl
            product = L3
            dataMode = CanvasConstant.DATA_MODE_GREY
            px = LaserPeckerHelper.PX_4K.toHexInt()
            type = LaserPeckerHelper.LASER_TYPE_WHITE.toHexInt()
            power = 80
            depth = 5
        })

        //漆面纸板
        materialList.add(MaterialEntity().apply {
            resId = R.string.material_qmzb
            product = L3
            dataMode = CanvasConstant.DATA_MODE_BLACK_WHITE
            px = LaserPeckerHelper.PX_1K.toHexInt()
            type = LaserPeckerHelper.LASER_TYPE_WHITE.toHexInt()
            power = 100
            depth = 5
        })
        materialList.add(MaterialEntity().apply {
            resId = R.string.material_qmzb
            product = L3
            dataMode = CanvasConstant.DATA_MODE_BLACK_WHITE
            px = LaserPeckerHelper.PX_2K.toHexInt()
            type = LaserPeckerHelper.LASER_TYPE_WHITE.toHexInt()
            power = 100
            depth = 3
        })
        materialList.add(MaterialEntity().apply {
            resId = R.string.material_qmzb
            product = L3
            dataMode = CanvasConstant.DATA_MODE_BLACK_WHITE
            px = LaserPeckerHelper.PX_4K.toHexInt()
            type = LaserPeckerHelper.LASER_TYPE_WHITE.toHexInt()
            power = 100
            depth = 1
        })

        materialList.add(MaterialEntity().apply {
            resId = R.string.material_qmzb
            product = L3
            dataMode = CanvasConstant.DATA_MODE_GREY
            px = LaserPeckerHelper.PX_1K.toHexInt()
            type = LaserPeckerHelper.LASER_TYPE_WHITE.toHexInt()
            power = 100
            depth = 30
        })
        materialList.add(MaterialEntity().apply {
            resId = R.string.material_qmzb
            product = L3
            dataMode = CanvasConstant.DATA_MODE_GREY
            px = LaserPeckerHelper.PX_2K.toHexInt()
            type = LaserPeckerHelper.LASER_TYPE_WHITE.toHexInt()
            power = 80
            depth = 10
        })
        materialList.add(MaterialEntity().apply {
            resId = R.string.material_qmzb
            product = L3
            dataMode = CanvasConstant.DATA_MODE_GREY
            px = LaserPeckerHelper.PX_4K.toHexInt()
            type = LaserPeckerHelper.LASER_TYPE_WHITE.toHexInt()
            power = 80
            depth = 5
        })

        //氧化金属
        materialList.add(MaterialEntity().apply {
            resId = R.string.material_yhjs
            product = L3
            dataMode = CanvasConstant.DATA_MODE_BLACK_WHITE
            px = LaserPeckerHelper.PX_1K.toHexInt()
            type = LaserPeckerHelper.LASER_TYPE_WHITE.toHexInt()
            power = 100
            depth = 20
        })
        materialList.add(MaterialEntity().apply {
            resId = R.string.material_yhjs
            product = L3
            dataMode = CanvasConstant.DATA_MODE_BLACK_WHITE
            px = LaserPeckerHelper.PX_2K.toHexInt()
            type = LaserPeckerHelper.LASER_TYPE_WHITE.toHexInt()
            power = 100
            depth = 5
        })
        materialList.add(MaterialEntity().apply {
            resId = R.string.material_yhjs
            product = L3
            dataMode = CanvasConstant.DATA_MODE_BLACK_WHITE
            px = LaserPeckerHelper.PX_4K.toHexInt()
            type = LaserPeckerHelper.LASER_TYPE_WHITE.toHexInt()
            power = 100
            depth = 1
        })

        materialList.add(MaterialEntity().apply {
            resId = R.string.material_yhjs
            product = L3
            dataMode = CanvasConstant.DATA_MODE_GREY
            px = LaserPeckerHelper.PX_1K.toHexInt()
            type = LaserPeckerHelper.LASER_TYPE_WHITE.toHexInt()
            power = 100
            depth = 60
        })
        materialList.add(MaterialEntity().apply {
            resId = R.string.material_yhjs
            product = L3
            dataMode = CanvasConstant.DATA_MODE_GREY
            px = LaserPeckerHelper.PX_2K.toHexInt()
            type = LaserPeckerHelper.LASER_TYPE_WHITE.toHexInt()
            power = 100
            depth = 30
        })
        materialList.add(MaterialEntity().apply {
            resId = R.string.material_yhjs
            product = L3
            dataMode = CanvasConstant.DATA_MODE_GREY
            px = LaserPeckerHelper.PX_4K.toHexInt()
            type = LaserPeckerHelper.LASER_TYPE_WHITE.toHexInt()
            power = 100
            depth = 6
        })

        //拉丝不锈钢
        materialList.add(MaterialEntity().apply {
            resId = R.string.material_lsbxg
            product = L3
            dataMode = CanvasConstant.DATA_MODE_BLACK_WHITE
            px = LaserPeckerHelper.PX_1K.toHexInt()
            type = LaserPeckerHelper.LASER_TYPE_WHITE.toHexInt()
            power = 100
            depth = 50
        })
        materialList.add(MaterialEntity().apply {
            resId = R.string.material_lsbxg
            product = L3
            dataMode = CanvasConstant.DATA_MODE_BLACK_WHITE
            px = LaserPeckerHelper.PX_2K.toHexInt()
            type = LaserPeckerHelper.LASER_TYPE_WHITE.toHexInt()
            power = 100
            depth = 20
        })
        materialList.add(MaterialEntity().apply {
            resId = R.string.material_lsbxg
            product = L3
            dataMode = CanvasConstant.DATA_MODE_BLACK_WHITE
            px = LaserPeckerHelper.PX_4K.toHexInt()
            type = LaserPeckerHelper.LASER_TYPE_WHITE.toHexInt()
            power = 100
            depth = 5
        })

        materialList.add(MaterialEntity().apply {
            resId = R.string.material_lsbxg
            product = L3
            dataMode = CanvasConstant.DATA_MODE_GREY
            px = LaserPeckerHelper.PX_1K.toHexInt()
            type = LaserPeckerHelper.LASER_TYPE_WHITE.toHexInt()
            power = 100
            depth = 100
        })
        materialList.add(MaterialEntity().apply {
            resId = R.string.material_lsbxg
            product = L3
            dataMode = CanvasConstant.DATA_MODE_GREY
            px = LaserPeckerHelper.PX_2K.toHexInt()
            type = LaserPeckerHelper.LASER_TYPE_WHITE.toHexInt()
            power = 100
            depth = 100
        })
        materialList.add(MaterialEntity().apply {
            resId = R.string.material_lsbxg
            product = L3
            dataMode = CanvasConstant.DATA_MODE_GREY
            px = LaserPeckerHelper.PX_4K.toHexInt()
            type = LaserPeckerHelper.LASER_TYPE_WHITE.toHexInt()
            power = 100
            depth = 60
        })

        //拉丝不锈钢
        materialList.add(MaterialEntity().apply {
            resId = R.string.material_lsbxg
            product = L3
            dataMode = CanvasConstant.DATA_MODE_BLACK_WHITE
            px = LaserPeckerHelper.PX_1K.toHexInt()
            type = LaserPeckerHelper.LASER_TYPE_WHITE.toHexInt()
            power = 100
            depth = 50
        })
        materialList.add(MaterialEntity().apply {
            resId = R.string.material_lsbxg
            product = L3
            dataMode = CanvasConstant.DATA_MODE_BLACK_WHITE
            px = LaserPeckerHelper.PX_2K.toHexInt()
            type = LaserPeckerHelper.LASER_TYPE_WHITE.toHexInt()
            power = 100
            depth = 20
        })
        materialList.add(MaterialEntity().apply {
            resId = R.string.material_lsbxg
            product = L3
            dataMode = CanvasConstant.DATA_MODE_BLACK_WHITE
            px = LaserPeckerHelper.PX_4K.toHexInt()
            type = LaserPeckerHelper.LASER_TYPE_WHITE.toHexInt()
            power = 100
            depth = 5
        })

        materialList.add(MaterialEntity().apply {
            resId = R.string.material_lsbxg
            product = L3
            dataMode = CanvasConstant.DATA_MODE_GREY
            px = LaserPeckerHelper.PX_1K.toHexInt()
            type = LaserPeckerHelper.LASER_TYPE_WHITE.toHexInt()
            power = 100
            depth = 100
        })
        materialList.add(MaterialEntity().apply {
            resId = R.string.material_lsbxg
            product = L3
            dataMode = CanvasConstant.DATA_MODE_GREY
            px = LaserPeckerHelper.PX_2K.toHexInt()
            type = LaserPeckerHelper.LASER_TYPE_WHITE.toHexInt()
            power = 100
            depth = 100
        })
        materialList.add(MaterialEntity().apply {
            resId = R.string.material_lsbxg
            product = L3
            dataMode = CanvasConstant.DATA_MODE_GREY
            px = LaserPeckerHelper.PX_4K.toHexInt()
            type = LaserPeckerHelper.LASER_TYPE_WHITE.toHexInt()
            power = 100
            depth = 60
        })

        //铜
        materialList.add(MaterialEntity().apply {
            resId = R.string.material_tong
            product = L3
            dataMode = CanvasConstant.DATA_MODE_BLACK_WHITE
            px = LaserPeckerHelper.PX_1K.toHexInt()
            type = LaserPeckerHelper.LASER_TYPE_WHITE.toHexInt()
            power = 100
            depth = 30
        })
        materialList.add(MaterialEntity().apply {
            resId = R.string.material_tong
            product = L3
            dataMode = CanvasConstant.DATA_MODE_BLACK_WHITE
            px = LaserPeckerHelper.PX_2K.toHexInt()
            type = LaserPeckerHelper.LASER_TYPE_WHITE.toHexInt()
            power = 100
            depth = 10
        })
        materialList.add(MaterialEntity().apply {
            resId = R.string.material_tong
            product = L3
            dataMode = CanvasConstant.DATA_MODE_BLACK_WHITE
            px = LaserPeckerHelper.PX_4K.toHexInt()
            type = LaserPeckerHelper.LASER_TYPE_WHITE.toHexInt()
            power = 100
            depth = 1
        })

        materialList.add(MaterialEntity().apply {
            resId = R.string.material_tong
            product = L3
            dataMode = CanvasConstant.DATA_MODE_GREY
            px = LaserPeckerHelper.PX_1K.toHexInt()
            type = LaserPeckerHelper.LASER_TYPE_WHITE.toHexInt()
            power = 100
            depth = 60
        })
        materialList.add(MaterialEntity().apply {
            resId = R.string.material_tong
            product = L3
            dataMode = CanvasConstant.DATA_MODE_GREY
            px = LaserPeckerHelper.PX_2K.toHexInt()
            type = LaserPeckerHelper.LASER_TYPE_WHITE.toHexInt()
            power = 100
            depth = 30
        })
        materialList.add(MaterialEntity().apply {
            resId = R.string.material_tong
            product = L3
            dataMode = CanvasConstant.DATA_MODE_GREY
            px = LaserPeckerHelper.PX_4K.toHexInt()
            type = LaserPeckerHelper.LASER_TYPE_WHITE.toHexInt()
            power = 100
            depth = 10
        })

        //铝合金
        materialList.add(MaterialEntity().apply {
            resId = R.string.material_lhj
            product = L3
            dataMode = CanvasConstant.DATA_MODE_BLACK_WHITE
            px = LaserPeckerHelper.PX_1K.toHexInt()
            type = LaserPeckerHelper.LASER_TYPE_WHITE.toHexInt()
            power = 100
            depth = 60
        })
        materialList.add(MaterialEntity().apply {
            resId = R.string.material_lhj
            product = L3
            dataMode = CanvasConstant.DATA_MODE_BLACK_WHITE
            px = LaserPeckerHelper.PX_2K.toHexInt()
            type = LaserPeckerHelper.LASER_TYPE_WHITE.toHexInt()
            power = 100
            depth = 20
        })
        materialList.add(MaterialEntity().apply {
            resId = R.string.material_lhj
            product = L3
            dataMode = CanvasConstant.DATA_MODE_BLACK_WHITE
            px = LaserPeckerHelper.PX_4K.toHexInt()
            type = LaserPeckerHelper.LASER_TYPE_WHITE.toHexInt()
            power = 100
            depth = 3
        })

        materialList.add(MaterialEntity().apply {
            resId = R.string.material_lhj
            product = L3
            dataMode = CanvasConstant.DATA_MODE_GREY
            px = LaserPeckerHelper.PX_1K.toHexInt()
            type = LaserPeckerHelper.LASER_TYPE_WHITE.toHexInt()
            power = 100
            depth = 100
        })
        materialList.add(MaterialEntity().apply {
            resId = R.string.material_lhj
            product = L3
            dataMode = CanvasConstant.DATA_MODE_GREY
            px = LaserPeckerHelper.PX_2K.toHexInt()
            type = LaserPeckerHelper.LASER_TYPE_WHITE.toHexInt()
            power = 100
            depth = 60
        })
        materialList.add(MaterialEntity().apply {
            resId = R.string.material_lhj
            product = L3
            dataMode = CanvasConstant.DATA_MODE_GREY
            px = LaserPeckerHelper.PX_4K.toHexInt()
            type = LaserPeckerHelper.LASER_TYPE_WHITE.toHexInt()
            power = 100
            depth = 20
        })

        //亚克力
        materialList.add(MaterialEntity().apply {
            resId = R.string.material_ykl
            product = L3
            dataMode = CanvasConstant.DATA_MODE_BLACK_WHITE
            px = LaserPeckerHelper.PX_1K.toHexInt()
            type = LaserPeckerHelper.LASER_TYPE_WHITE.toHexInt()
            power = 100
            depth = 40
        })
        materialList.add(MaterialEntity().apply {
            resId = R.string.material_ykl
            product = L3
            dataMode = CanvasConstant.DATA_MODE_BLACK_WHITE
            px = LaserPeckerHelper.PX_2K.toHexInt()
            type = LaserPeckerHelper.LASER_TYPE_WHITE.toHexInt()
            power = 100
            depth = 20
        })
        materialList.add(MaterialEntity().apply {
            resId = R.string.material_ykl
            product = L3
            dataMode = CanvasConstant.DATA_MODE_BLACK_WHITE
            px = LaserPeckerHelper.PX_4K.toHexInt()
            type = LaserPeckerHelper.LASER_TYPE_WHITE.toHexInt()
            power = 100
            depth = 5
        })

        materialList.add(MaterialEntity().apply {
            resId = R.string.material_ykl
            product = L3
            dataMode = CanvasConstant.DATA_MODE_GREY
            px = LaserPeckerHelper.PX_1K.toHexInt()
            type = LaserPeckerHelper.LASER_TYPE_WHITE.toHexInt()
            power = 100
            depth = 80
        })
        materialList.add(MaterialEntity().apply {
            resId = R.string.material_ykl
            product = L3
            dataMode = CanvasConstant.DATA_MODE_GREY
            px = LaserPeckerHelper.PX_2K.toHexInt()
            type = LaserPeckerHelper.LASER_TYPE_WHITE.toHexInt()
            power = 100
            depth = 40
        })
        materialList.add(MaterialEntity().apply {
            resId = R.string.material_ykl
            product = L3
            dataMode = CanvasConstant.DATA_MODE_GREY
            px = LaserPeckerHelper.PX_4K.toHexInt()
            type = LaserPeckerHelper.LASER_TYPE_WHITE.toHexInt()
            power = 100
            depth = 15
        })

        //漆涂层
        materialList.add(MaterialEntity().apply {
            resId = R.string.material_qtc
            product = L3
            dataMode = CanvasConstant.DATA_MODE_BLACK_WHITE
            px = LaserPeckerHelper.PX_1K.toHexInt()
            type = LaserPeckerHelper.LASER_TYPE_WHITE.toHexInt()
            power = 70
            depth = 25
        })
        materialList.add(MaterialEntity().apply {
            resId = R.string.material_qtc
            product = L3
            dataMode = CanvasConstant.DATA_MODE_BLACK_WHITE
            px = LaserPeckerHelper.PX_2K.toHexInt()
            type = LaserPeckerHelper.LASER_TYPE_WHITE.toHexInt()
            power = 70
            depth = 5
        })
        materialList.add(MaterialEntity().apply {
            resId = R.string.material_qtc
            product = L3
            dataMode = CanvasConstant.DATA_MODE_BLACK_WHITE
            px = LaserPeckerHelper.PX_4K.toHexInt()
            type = LaserPeckerHelper.LASER_TYPE_WHITE.toHexInt()
            power = 60
            depth = 1
        })

        materialList.add(MaterialEntity().apply {
            resId = R.string.material_qtc
            product = L3
            dataMode = CanvasConstant.DATA_MODE_GREY
            px = LaserPeckerHelper.PX_1K.toHexInt()
            type = LaserPeckerHelper.LASER_TYPE_WHITE.toHexInt()
            power = 70
            depth = 50
        })
        materialList.add(MaterialEntity().apply {
            resId = R.string.material_qtc
            product = L3
            dataMode = CanvasConstant.DATA_MODE_GREY
            px = LaserPeckerHelper.PX_2K.toHexInt()
            type = LaserPeckerHelper.LASER_TYPE_WHITE.toHexInt()
            power = 70
            depth = 20
        })
        materialList.add(MaterialEntity().apply {
            resId = R.string.material_qtc
            product = L3
            dataMode = CanvasConstant.DATA_MODE_GREY
            px = LaserPeckerHelper.PX_4K.toHexInt()
            type = LaserPeckerHelper.LASER_TYPE_WHITE.toHexInt()
            power = 70
            depth = 10
        })

        //皮革
        materialList.add(MaterialEntity().apply {
            resId = R.string.material_pg
            product = L3
            dataMode = CanvasConstant.DATA_MODE_BLACK_WHITE
            px = LaserPeckerHelper.PX_1K.toHexInt()
            type = LaserPeckerHelper.LASER_TYPE_WHITE.toHexInt()
            power = 100
            depth = 30
        })
        materialList.add(MaterialEntity().apply {
            resId = R.string.material_pg
            product = L3
            dataMode = CanvasConstant.DATA_MODE_BLACK_WHITE
            px = LaserPeckerHelper.PX_2K.toHexInt()
            type = LaserPeckerHelper.LASER_TYPE_WHITE.toHexInt()
            power = 100
            depth = 15
        })
        materialList.add(MaterialEntity().apply {
            resId = R.string.material_pg
            product = L3
            dataMode = CanvasConstant.DATA_MODE_BLACK_WHITE
            px = LaserPeckerHelper.PX_4K.toHexInt()
            type = LaserPeckerHelper.LASER_TYPE_WHITE.toHexInt()
            power = 100
            depth = 5
        })

        materialList.add(MaterialEntity().apply {
            resId = R.string.material_pg
            product = L3
            dataMode = CanvasConstant.DATA_MODE_GREY
            px = LaserPeckerHelper.PX_1K.toHexInt()
            type = LaserPeckerHelper.LASER_TYPE_WHITE.toHexInt()
            power = 100
            depth = 80
        })
        materialList.add(MaterialEntity().apply {
            resId = R.string.material_pg
            product = L3
            dataMode = CanvasConstant.DATA_MODE_GREY
            px = LaserPeckerHelper.PX_2K.toHexInt()
            type = LaserPeckerHelper.LASER_TYPE_WHITE.toHexInt()
            power = 100
            depth = 25
        })
        materialList.add(MaterialEntity().apply {
            resId = R.string.material_pg
            product = L3
            dataMode = CanvasConstant.DATA_MODE_GREY
            px = LaserPeckerHelper.PX_4K.toHexInt()
            type = LaserPeckerHelper.LASER_TYPE_WHITE.toHexInt()
            power = 100
            depth = 10
        })
    }

    /**默认参数*/
    fun getProductMaterialList(): List<MaterialEntity> {
        val productName: String? = vmApp<LaserPeckerModel>().productInfoData.value?.name
        val dataMode: Int = vmApp<EngraveModel>().engraveReadyInfoData.value?.dataMode
            ?: CanvasConstant.DATA_MODE_BLACK_WHITE //默认黑白
        val px: Byte = vmApp<EngraveModel>().engraveReadyInfoData.value?.engraveData?.px
            ?: LaserPeckerHelper.DEFAULT_PX
        val type: Byte = vmApp<EngraveModel>().engraveOptionInfoData.value?.type
            ?: LaserPeckerHelper.LASER_TYPE_BLUE
        return getProductMaterialList(productName, dataMode, px, type)
    }

    /**获取推荐的材质列表
     * [productName] 需要查询那个产品的推荐参数
     * [dataMode] 数据的处理模式, 对应不同的推荐参数 [CanvasConstant.DATA_MODE_BLACK_WHITE] [CanvasConstant.DATA_MODE_GREY]
     * [px] 数据的分辨率 [LaserPeckerHelper.DEFAULT_PX]
     * [type] 雕刻激光类型选择，0为1064nm激光 (白光-雕)，1为450nm激光 (蓝光-烧)。(L3max新增)
     * */
    fun getProductMaterialList(
        productName: String?,
        dataMode: Int, /*默认黑白*/
        px: Byte,
        type: Byte
    ): List<MaterialEntity> {
        val result = mutableListOf<MaterialEntity>()
        productName?.let {
            materialList.filterTo(result) { entity ->
                var match = entity.product == productName &&
                        entity.px.toByte() == px &&
                        entity.type.toByte() == type
                match =
                    if (dataMode == CanvasConstant.DATA_MODE_GREY || dataMode == CanvasConstant.DATA_MODE_DITHERING) {
                        //灰度 抖动
                        match && entity.dataMode == CanvasConstant.DATA_MODE_GREY
                    } else {
                        //其他模式下, 都用黑白参数
                        match && entity.dataMode == CanvasConstant.DATA_MODE_BLACK_WHITE
                    }
                match
            }
        }
        //自定义, 自动记住了上一次的值
        val custom = MaterialEntity()
        custom.resId = R.string.material_custom
        custom.power = lastPower
        custom.depth = lastDepth
        result.add(0, custom)
        return result
    }

    //</editor-fold desc="material">
}