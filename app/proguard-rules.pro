# ProGuard rules for OTA Pulse

# ---- Gson ----
# Keep Gson-serialized model classes
-keepattributes Signature
-keepattributes *Annotation*
-keep class com.abhinav.otapulse.catalog.model.** { *; }
-keep class com.abhinav.otapulse.core.model.** { *; }
-keep class com.abhinav.otapulse.core.network.** { *; }

# Gson specific
-dontwarn sun.misc.**
-keep class com.google.gson.** { *; }
-keep class * extends com.google.gson.TypeAdapter
-keep class * implements com.google.gson.TypeAdapterFactory
-keep class * implements com.google.gson.JsonSerializer
-keep class * implements com.google.gson.JsonDeserializer

# ---- OkHttp ----
-dontwarn okhttp3.**
-dontwarn okio.**
-keep class okhttp3.** { *; }

# ---- Hilt ----
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }
-keep class * extends dagger.hilt.android.internal.managers.** { *; }

# ---- Kotlin / Coroutines ----
-dontwarn kotlinx.coroutines.**
-keep class kotlinx.coroutines.** { *; }

# ---- Stack traces ----
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# ---- Apache Commons Compress (XZ) ----
-dontwarn org.apache.commons.compress.**
-keep class org.apache.commons.compress.** { *; }
-dontwarn org.tukaani.xz.**
-keep class org.tukaani.xz.** { *; }

# ---- Protobuf Lite ----
-dontwarn com.google.protobuf.**
-keep class com.google.protobuf.** { *; }
-keep class chromeos_update_engine.** { *; }
-keep class * extends com.google.protobuf.GeneratedMessageLite { *; }
-keep class * extends com.google.protobuf.MessageLite { *; }
-keep interface * extends com.google.protobuf.MessageLiteOrBuilder { *; }
-keep class * extends com.google.protobuf.GeneratedMessageLite$Builder { *; }