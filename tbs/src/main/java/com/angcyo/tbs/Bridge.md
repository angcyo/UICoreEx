# 2022-1-17

Android / Js 通信桥梁

https://github.com/hjhrq1991/JsBridge

## Android 注入

```kotlin
fun register(fragment: Fragment, webView: TbsWebView) {
    webView.registerHandler(PAGER_HANDLER_NAME) { data, function ->
        val bean = data?.fromJson<ImagePickerBean>()
        if (bean == null || bean.images.isNullOrEmpty()) {
            function?.onCallBack("无效的数据")
        } else {
            val size = bean.images.size()
            val index = min(max(bean.index, 0), size - 1)
            fragment.dslPager {
                startPosition = index
                loaderMediaList.addAll(bean.images.map { LoaderMedia(url = it) })
            }
            function?.onCallBack("开始预览:[$index/${size}]")
        }
    }
}
```

## Js 调用

```js
const defaultData = {
    error: "1"
}

const connectAndroidJsBridge = function (callback) {
    try {
        if (window.androidJs) {
            callback(window.androidJs)
        } else {
            document.addEventListener("androidJsReady", () => {
                callback(window.androidJs)
            }, false)
        }
    } catch (ex) {
        console.log(ex)
    }
}

const androidJsBridge = {
    callMethod: function (methodName, data) {
        if (!data) {
            data = defaultData
        }
        connectAndroidJsBridge(function (bridge) {
            if (typeof bridge === "undefined") {
                return
            }
            bridge.callHandler(methodName, JSON.stringify(data))
        })
    }
}
```

### 使用示例

```js

const imgDoms = document.querySelectorAll("#container img")
const imgs = []

imgDoms.forEach((item, index) => {
    let _index = index
    item.addEventListener("click", () => {
        showImage(_index)
    })
    imgs.push(item)
})

function showImage(index) {
    androidJsBridge.callMethod("showImagePager", {
        index: index,
        images: imgs.map(item => item.getAttribute("src"))
    })
}

```