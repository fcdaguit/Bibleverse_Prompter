# ---- Vosk / JNA ------------------------------------------------------------
# Vosk calls into native code through JNA, which resolves classes by name.
-keep class org.vosk.** { *; }
-keep class com.sun.jna.** { *; }
-keepclassmembers class * extends com.sun.jna.** { public *; }
-dontwarn java.awt.**
-dontwarn javax.swing.**

# ---- Room ------------------------------------------------------------------
-keep class androidx.room.** { *; }
-keep @androidx.room.Entity class * { *; }

# ---- OkHttp (optional Bible downloader) -----------------------------------
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**

# ---- Kotlin ----------------------------------------------------------------
-keepclassmembers class kotlin.Metadata { public <methods>; }
