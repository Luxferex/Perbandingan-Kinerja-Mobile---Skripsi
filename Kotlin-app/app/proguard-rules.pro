# Retrofit & Gson
-keepattributes Signature
-keepattributes *Annotation*
-keep class com.benchmark.androidnative.model.** { *; }
-dontwarn okhttp3.**
-dontwarn retrofit2.**
-keepclasseswithmembers class * {
    @retrofit2.http.* <methods>;
}

# Room
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-dontwarn androidx.room.paging.**

# Coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}

# Keep line numbers for crash reports
-keepattributes SourceFile,LineNumberTable
