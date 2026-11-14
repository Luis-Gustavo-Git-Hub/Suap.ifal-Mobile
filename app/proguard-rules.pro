# ProGuard / R8 Rules for SUAP / SIGAA Academic Assistant

# Preserve generic signatures, annotations, inner classes, and line numbers for debugging
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod,SourceFile,LineNumberTable

# -------------------------------------------------------------
# 1. Javascript Interfaces (WebViews)
# -------------------------------------------------------------
-keepattributes JavascriptInterface
-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}

# -------------------------------------------------------------
# 2. Room Database
# -------------------------------------------------------------
-keepclassmembers class * extends androidx.room.RoomDatabase {
    <init>();
}
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Dao interface * { *; }
-keep @androidx.room.Entity class * { *; }
-keep class * extends androidx.room.migration.Migration { *; }
-keep class * extends androidx.room.migration.AutoMigrationSpec { *; }
-dontwarn androidx.room.paging.**

# -------------------------------------------------------------
# 3. Jsoup HTML Parser
# -------------------------------------------------------------
-keep public class org.jsoup.** { public *; }
-keepclassmembers class org.jsoup.** {
    *** *(...);
}
-dontwarn org.jsoup.**

# -------------------------------------------------------------
# 4. Firebase & Google Identity / Play Services
# -------------------------------------------------------------
-keep class com.google.firebase.** { *; }
-keep class com.google.android.gms.** { *; }
-dontwarn com.google.firebase.**
-dontwarn com.google.android.gms.**
-keep class androidx.credentials.** { *; }

# -------------------------------------------------------------
# 5. Project Domain Models & Entities
# -------------------------------------------------------------
-keep class com.example.data.** { *; }
-keepclassmembers class com.example.data.** { *; }
-keep class com.example.ui.** {
    public <init>(...);
}
-keep class com.example.util.CredentialStore { *; }
-keep class com.example.util.AppLog { *; }

# -------------------------------------------------------------
# 6. OkHttp & Coroutines & WorkManager
# -------------------------------------------------------------
-dontwarn okhttp3.**
-dontwarn okio.**
-keepnames class okhttp3.internal.publicsuffix.PublicSuffixDatabase
-dontwarn kotlinx.coroutines.**
-keepclassmembers class * extends androidx.work.Worker {
    public <init>(android.content.Context, androidx.work.WorkerParameters);
}
-keepclassmembers class * extends androidx.work.CoroutineWorker {
    public <init>(android.content.Context, androidx.work.WorkerParameters);
}
-keep class * extends androidx.work.Worker
-keep class * extends androidx.work.CoroutineWorker
