# 2022-10-23

JavascriptEngine

https://developer.android.com/jetpack/androidx/releases/javascriptengine

https://developer.android.com/develop/ui/views/layout/webapps/jsengine

# WebView

https://developer.android.com/develop/ui/views/layout/webapps/webview

https://developer.android.com/develop/ui/views/layout/webapps/webview-testing

# Android JavaScript 引擎学习之初探 V8

https://zhaomenghuan.js.org/blog/android-javascript-engine-v8.html

JavaScript Engine| Android| iOS | 维护
--|--|--|--
JavaScriptCore| Interpreter and JIT |Interpreter only                |Apple
V8            | JIT                 |JIT only for jailbroken devices |Google
SpiderMonkey  | Interpreter and JIT |Interpreter only                |Mozilla
Rhino         | Interpreter         |Unsupported                     |Mozilla

## J2V8 框架初探

J2V8 是一套针对 V8 的 Java 绑定。J2V8 的开发为 Android 平台带来了高效的 Javascript 的执行环境，其以性能与内存消耗为设计目标。它采用了“基本类型优先”原则，意味着一个执行结果是基本类型，那么所返回的值也就是该基本类型。它还采用了“懒加载”技术，只有当 JS 执行结果被访问时，才会通过 JNI 复制到 Java 中。此外 J2V8 提供了 release()方法，开发者在某一对象不再需要时主动调用该方法去释放本地对象句柄，释放规则如下：

- 如果是由代码创建的对象，那么必须释放它；如果一个对象是通过返回语句传回来的话，系统会替你释放它；
- 如果是由系统创建的对象，则无需担心释放，而如果对象是 JS 方法的返回值，就必须手动的释放它。
- 官网地址：https://eclipsesource.com/blogs/tutorials/getting-started-with-j2v8/
- github 地址：https://github.com/eclipsesource/J2V8
- maven 仓库地址：http://central.maven.org/maven2/com/eclipsesource/j2v8

