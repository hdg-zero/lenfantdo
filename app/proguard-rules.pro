# ProGuard / R8 optimization and obfuscation rules for L'enfant do

# Preserve Kotlin Reflection and Attributes for stacktraces
-keepattributes SourceFile,LineNumberTable
-keepattributes *Annotation*,InnerClasses,EnclosingMethod

# Room Database
-keep class androidx.room.RoomDatabase
-dontwarn androidx.room.paging.**
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao interface * { *; }

# DataStore Preferences
-keep class androidx.datastore.** { *; }

# Kotlin Coroutines
-dontwarn kotlinx.coroutines.**
-keepclassmembers class kotlinx.coroutines.** {
    volatile <fields>;
}

# Domain Models
-keep class com.hdgdev.lenfantdo.domain.model.** { *; }
-keep class com.hdgdev.lenfantdo.domain.analytics.** { *; }

# Jetpack Compose and Material 3
-keep class androidx.compose.material3.** { *; }
-dontwarn androidx.compose.**

# AboutLibraries
-keep class .R
-keep class **.R$* {
    <fields>;
}