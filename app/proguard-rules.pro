-keepattributes *Annotation*
# Room
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
# kotlinx.serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** { *** Companion; }
-keepclasseswithmembers class kotlinx.serialization.json.** { kotlinx.serialization.KSerializer serializer(...); }
-keep,includedescriptorclasses class com.aibot.crm.**$$serializer { *; }
-keepclassmembers class com.aibot.crm.** { *** Companion; }
-keepclasseswithmembers class com.aibot.crm.** { kotlinx.serialization.KSerializer serializer(...); }
# App Functions
-keep class androidx.appfunctions.** { *; }
-keep class com.aibot.crm.functions.** { *; }
