package com.angcyo.engrave.transition

import com.angcyo.engrave.R
import com.angcyo.library.ex._string

/**
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2022/09/29
 */

/**传输数据为空*/
class EmptyException(cause: Throwable? = null) :
    Exception(_string(R.string.no_data_transfer), cause)

/**传输数据异常*/
class DataException(cause: Throwable? = null) : Exception(_string(R.string.data_exception), cause)

/**传输失败*/
class FailException(cause: Throwable? = null) :
    Exception(_string(R.string.transfer_data_exception), cause)