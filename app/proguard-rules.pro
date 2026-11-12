-keep class com.edgeback.customizer.** { *; }
-dontwarn io.github.libxposed.**
-assumenosideeffects class android.util.Log {
    public static int v(...);
    public static int d(...);
}
