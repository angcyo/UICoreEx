# 2023-8-11

注入的方法

## toast

显示一个原生的`Toast`

```js
androidJs.toast('显示的内容')
```

## log

将内容写入日志, 并控制台输出

```js
androidJs.log('打印的内容')
```

## getAppVersionName

获取app的版本号`String`

```js
androidJs.getAppVersionName() //5.3.3-alpha9
```

## getAppVersionCode

获取app的版本号`Long`

```js
androidJs.getAppVersionCode() //5339
```

## getStatusBarHeight

获取状态栏的高度`Int`px

```js
androidJs.getStatusBarHeight() //48px
```

## getNavBarHeight

获取导航栏的高度`Int`px

```js
androidJs.getNavBarHeight() //48px
```

## getScreenWidth

获取屏幕的宽度`Int`px

```js
androidJs.getScreenWidth() //1080px
```

## getScreenHeight

获取屏幕的高度`Int`px

```js
androidJs.getScreenHeight() //1920px
```

## back

触发返回按键

```js
androidJs.back()
```

## finish

关闭当前页面

```js
androidJs.finish()
```

## open

打开一个新的页面

```js
androidJs.open('http://www.angcyo.com')

androidJs.open({
    target: '_self', //打开方式, _self:在当前窗口打开 _blank:在新窗口中打开被链接文档。
    url: 'http://www.angcyo.com'
}) //JsonString
```

## getValue

获取一个`key`对应的`value`

```js
androidJs.getValue('key') //String
```

## setValue

设置一个`key`对应的`value`

```js
androidJs.setValue({
  key: xxx,
  value: xxx
}) //Json String
```