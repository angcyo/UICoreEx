package com.angcyo.engrave.transition

/**
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2022/09/29
 */

/**传输数据为空*/
class EmptyException(cause: Throwable? = null) : Exception(cause)

/**传输数据异常*/
class DataException(cause: Throwable? = null) : Exception(cause)

/**传输失败*/
class FailException(cause: Throwable? = null) : Exception(cause)