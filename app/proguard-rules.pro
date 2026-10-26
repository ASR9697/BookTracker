# R8 rules for the phone app. Libraries (Room, WorkManager, CameraX, ML Kit, Coil,
# OkHttp, Play services) ship their own consumer rules; these cover what they can't.

# --- Gson -------------------------------------------------------------------
# Book fields (creators, publication, loans, ratings…) are stored in Room as Gson
# JSON. Field names ARE the stored format, so the models must keep them exactly.
-keepattributes Signature, InnerClasses, EnclosingMethod, *Annotation*
-keep class com.example.booktracker.shared.models.** { *; }
-keep class * extends com.google.gson.reflect.TypeToken
-keep,allowobfuscation,allowshrinking class com.google.gson.reflect.TypeToken
-dontwarn sun.misc.Unsafe

# --- Persisted enum names ------------------------------------------------------
# Enum names are written to the database, settings and backups (status, format,
# palette, timer mode) and read back with valueOf().
-keepclassmembers enum com.example.booktracker.** {
    <fields>;
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# --- Glance -------------------------------------------------------------------
# Widget click actions are instantiated reflectively by class name.
-keep class * implements androidx.glance.appwidget.action.ActionCallback { <init>(); }

# Keep line numbers so Play Console crash reports are readable (with the uploaded mapping file).
-keepattributes SourceFile, LineNumberTable
-renamesourcefileattribute SourceFile
