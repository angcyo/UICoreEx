package com.angcyo.objectbox

import io.objectbox.annotation.Entity
import io.objectbox.annotation.Id

/**
 * 占位[Entity], 用于生成[MyObjectBox]类.
 *
 * [MyObjectBox]生成的包名, 会是库查找到的第一个[Entity]所在的包名.
 *
 * 所以提供一个在[程序包名]下面占位的[Entity]
 *
 * Email:angcyo@126.com
 * @author angcyo
 * @date 2020-4-14
 * Copyright (c) 2019 ShenZhen O&M Cloud Co., Ltd. All rights reserved.
 */
@Entity
data class PlaceholderEntity(
    @Id var entityId: Long = 0L

    /**[io.objectbox.annotation.Transient]*/
    /**[io.objectbox.annotation.NameInDb]*/
    /**[io.objectbox.annotation.Uid]*/
    /**[io.objectbox.annotation.Index]*/
)