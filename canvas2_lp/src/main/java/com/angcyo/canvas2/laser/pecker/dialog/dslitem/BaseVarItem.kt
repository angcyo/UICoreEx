package com.angcyo.canvas2.laser.pecker.dialog.dslitem

import com.angcyo.dsladapter.DslAdapterItem
import com.angcyo.laserpacker.bean.LPVariableBean

/**
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2023/09/04
 */
abstract class BaseVarItem : DslAdapterItem()

/**数据结构*/
val DslAdapterItem._itemVariableBean: LPVariableBean?
    get() = itemData as? LPVariableBean