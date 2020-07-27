# bmob
2020-02-07

bmob 数据存储. **不需要绑定应用包名**

[https://www.bmob.cn/](https://www.bmob.cn/)

[http://doc.bmob.cn/data/android/](http://doc.bmob.cn/data/android/)

[https://github.com/bmob](https://github.com/bmob)

[https://github.com/bmob/bmob-android-sdk](https://github.com/bmob/bmob-android-sdk)

```kotlin
//Bmob的maven仓库地址--必填
maven { url "https://raw.github.com/bmob/bmob-android-sdk/master" }

//官方
maven {url 'https://dl.bintray.com/chaozhouzhang/maven' }
```

# 版本列表

https://dl.bintray.com/chaozhouzhang/maven/cn/bmob/android/

# 快速入门

http://doc.bmob.cn/data/android/index.html

http://docs.bmob.cn/android/faststart/index.html?menukey=fast_start&key=start_android

# 使用方式

```groovy
//可以需要
android {
    useLibrary 'org.apache.http.legacy'
}

//依赖
dependencies {
    implementation 'cn.bmob.android:bmob-sdk:3.7.8'
    implementation "io.reactivex.rxjava2:rxjava:2.2.8"
    implementation 'io.reactivex.rxjava2:rxandroid:2.1.1'
    implementation 'com.squareup.okhttp3:okhttp:3.14.1'
    implementation 'com.squareup.okio:okio:2.2.2'
    implementation 'com.google.code.gson:gson:2.8.5'
}
```

```kotlin
com.angcyo.bmob.DslBmob.initBmob(context, appId)
```

# API安全码

如果bmob开启了`API安全码`, 那么只能`js`使用.

`js`可以通过`Rest Api`方式访问. 这样`Android`端不用变化.

或者`Android`端也使用`Rest Api`方式访问.