# 2021-7-5

[](https://github.com/yanzhenjie/AndServer)

[](https://yanzhenjie.com/AndServer/)

## @RequestParam

[](https://yanzhenjie.com/AndServer/annotation/RequestParam.html)

RequestParam支持注解的参数类型有：MultipartFile、String、int、long、float、double、boolean，其中MultipartFile表示表单中的文件，其它类型可以是 Url 中的参数、Body 中的参数（客户端Content-Type是application/x-www-form-urlencoded，数据又在 Body 中时）、表单中的参数。

如果要单独获取 Url 中的参数请使用QueryParam注解。

```
@RestController
public class UserController {

    @PostMapping("/user/login")
    void login(@RequestParam("account") String account,
        @RequestParam("password") String password) {
        ...
    }
}
```

## @QueryParam

[](https://yanzhenjie.com/AndServer/annotation/QueryParam.html)

QueryParam和RequestParam相同只能用在方法参数上，用来获取客户端的请求参数，支持类型除了MultipartFile外其他完全一致。不同的是QueryParam仅用来获取 Url 中的参数。

```
@RestController
public class UserController {

    @GetMapping("/user/info")
    String info(@QueryParam(name = "id") long id) {
        ...
    }
}
```