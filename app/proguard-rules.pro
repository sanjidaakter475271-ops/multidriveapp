# ProGuard / R8 rules for MultiDrive (BUG-033 FIX)

# Retrofit & OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn retrofit2.**
-keep class retrofit2.** { *; }
-keepattributes Signature, InnerClasses, EnclosingMethod
-keepattributes RuntimeVisibleAnnotations, RuntimeVisibleParameterAnnotations

# Gson
-keepattributes *Annotation*
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
}
-keep class com.multidrive.app.data.remote.** { *; }

# Room
-keep class * extends androidx.room.RoomDatabase
-dontwarn androidx.room.paging.**
-keep class com.multidrive.app.data.local.entity.** { *; }
-keep class com.multidrive.app.data.local.dao.** { *; }

# Coroutines
-dontwarn kotlinx.coroutines.**
