# pine

2023-09-24

Pine是一个在虚拟机层面、以Java方法为粒度的运行时动态hook框架，它可以拦截本进程内几乎所有的java方法调用。

目前它支持Android 4.4（只支持ART）~ 13 且使用 thumb-2/arm64 指令集的设备。

关于它的实现原理，可以参考本文。

注：在Android 6.0 & 32位架构上，参数解析可能错误；另外在Android 9.0及以上，Pine会关闭系统的隐藏API限制策略。

https://github.com/canyie/pine/blob/master/README_cn.md