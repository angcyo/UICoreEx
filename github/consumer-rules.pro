#CalendarView
-keepclasseswithmembers class * {
    public <init>(android.content.Context);
}
-keep class **.**MonthView {
    public <init>(android.content.Context);
}
-keep class **.**WeekBar {
    public <init>(android.content.Context);
}
-keep class **.**WeekView {
    public <init>(android.content.Context);
}
-keep class **.**YearView {
    public <init>(android.content.Context);
}

-keepclasseswithmembers class * {
    public <init>(android.content.Context);
}

# liveeventbus
-dontwarn com.jeremyliao.liveeventbus.**
-keep class com.jeremyliao.liveeventbus.** { *; }
-keep class androidx.lifecycle.** { *; }
-keep class androidx.arch.core.** { *; }