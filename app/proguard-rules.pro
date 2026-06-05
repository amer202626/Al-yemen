# Kotlinx Serialization
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod
-keepclassmembers class * {
    @kotlinx.serialization.SerialName <fields>;
}
-keep,unused class kotlinx.serialization.json.** { *; }

# Room Database
-keep class * extends androidx.room.RoomDatabase
-dontwarn androidx.room.paging.**

# Jetpack Compose and Animators
-keepclassmembers class androidx.compose.animation.core.Animatable { *; }
-dontwarn androidx.compose.**

# Coil Image Loader
-keep class coil.** { *; }
-dontwarn coil.**
