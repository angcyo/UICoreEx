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

获取状态栏的高度`Int`

```js
androidJs.getStatusBarHeight() //48
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