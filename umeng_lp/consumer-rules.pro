-keep class com.umeng.** {*;}

-keep class org.repackage.** {*;}

-keepclassmembers class * {
   public <init> (org.json.JSONObject);
}

-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

-keep public class com.hingin.l1.hiprint.R$*{
public static final int *;
}

-keep public class com.hingin.l1.hiprint.preview.R$*{
public static final int *;
}