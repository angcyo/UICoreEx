# ObjectBox

2020-4-14

修改`Entity`之后, 注意`Build Project`.

```kotlin
DslBox.default_package_name = BuildConfig.APPLICATION_ID
DslBox.init(content, debug)
```

[https://objectbox.io/](https://objectbox.io/)

[https://objectbox.io/dev-get-started/](https://objectbox.io/dev-get-started/)

[https://docs.objectbox.io/getting-started](https://docs.objectbox.io/getting-started)

[https://github.com/objectbox/](https://github.com/objectbox/)

[https://github.com/objectbox/objectbox-java](https://github.com/objectbox/objectbox-java)

# 使用入门

## gradle

```
buildscript {
    ext.objectboxVersion = '2.5.1'
    dependencies {
        // Android Gradle Plugin 3.2.1
        classpath 'com.android.tools.build:gradle:3.5.4'

        classpath "io.objectbox:objectbox-gradle-plugin:$objectboxVersion"
    }
}
```

```
apply plugin: 'io.objectbox' // after applying Android plugin
```

```
@Entity public class Playlist { ... }

boxStore = MyObjectBox.builder().androidContext(this).build();

Box<Playlist> box = boxStore.boxFor(Playlist.class);
```

> Build > Make Project

# CRUD

## 关键类

```
Box<User> userBox = boxStore.boxFor(User.class);
Box<Order> orderBox = boxStore.boxFor(Order.class);
```

## 增加

```
User user = new User("Tina");
userBox.put(user);
​
List<User> users = getNewUsers();
userBox.put(users);
```

## 查询

```
User user = userBox.get(userId);
​
List<User> users = userBox.getAll();
```

```
List<User> results = userBox.query()
    .equal(User_.name, "Tom")
    .order(User_.name)
    .build()
    .find();
```

## 删除

```
boolean isRemoved = userBox.remove(userId);
​
userBox.remove(users);
// alternatively:
userBox.removeByIds(userIds);
​
userBox.removeAll();
```

## 其他

```
long userCount = userBox.count();
```

```
User user = new User();
// user.id == 0
box.put(user);
// user.id != 0
long id = user.id;
```

# 支持的数据类型

https://docs.objectbox.io/advanced/custom-types

```
boolean, Boolean
int, Integer
short, Short
long, Long
float, Float
double, Double
byte, Byte
char, Character
byte[]
String
Date // Time with millisecond precision.

// As of 3.0.0-alpha2 the following work out of the box:
String[]
@Type(DateNano) long, Long // Time with nanosecond precision.
```

https://docs.objectbox.io/advanced/custom-types#list-array-types

列表/数组类型 您可以使用具有列表类型的转换器。例如，可以将字符串列表转换为 JSON 数组，从而生成数据库的单个字符串。目前无法将数组与转换器一起使用（您可以跟踪此功能请求）。