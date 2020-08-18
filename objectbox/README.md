# ObjectBox
2020-4-14

```kotlin
DslBox.default_package_name = BuildConfig.APPLICATION_ID
DslBox.init(content, debug)
```

https://objectbox.io/

https://objectbox.io/dev-get-started/

https://docs.objectbox.io/getting-started

https://github.com/objectbox/

https://github.com/objectbox/objectbox-java

# 使用入门

## 

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