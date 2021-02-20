-keep class com.umeng.** {*;}
# 您如果使用了稳定性模块可以加入该混淆
-keep class com.uc.** {*;}
-keepclassmembers class * {
   public <init> (org.json.JSONObject);
}
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}
