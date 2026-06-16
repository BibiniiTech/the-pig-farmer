# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Uncomment this to preserve the line number information for
# debugging stack traces.
-keepattributes SourceFile,LineNumberTable,Signature,*Annotation*

# If you keep the line number information, uncomment this to
# hide the original source file name.
-renamesourcefileattribute SourceFile

# R8 missing classes fix
-dontwarn org.slf4j.impl.StaticLoggerBinder

# Hardening: Repackage all obfuscated classes into a single package to hide original structure
-repackageclasses 'com.example.smartswine.h'

# Optimization: Aggressively modify access to methods and fields to allow more inlining
-allowaccessmodification

# Remove Log statements from release builds for security and performance
-assumenosideeffects class android.util.Log {
    public static *** d(...);
    public static *** v(...);
    public static *** i(...);
    public static *** w(...);
}

# Keep WorkManager Database implementation (fixes crash on launch)
-keep class androidx.work.impl.WorkDatabase_Impl {
    public <init>();
}
-keep class * extends androidx.room.RoomDatabase {
    public <init>();
}

# Keep WorkManager Worker classes
-keep class * extends androidx.work.ListenableWorker {
    public <init>(...);
}

# Firebase/Firestore specific rules
-keep class com.google.firebase.** { *; }
-dontwarn com.google.firebase.**

# GMS and Billing Client rules
-keep class com.google.android.gms.** { *; }
-dontwarn com.google.android.gms.**
-keep class com.android.billingclient.** { *; }

# Keep all classes annotated with @Keep
-keep @androidx.annotation.Keep class * { *; }
-keepclassmembers class * {
    @androidx.annotation.Keep *;
}

# iText 7 ProGuard rules
-keep class com.itextpdf.** { *; }
-dontwarn com.itextpdf.**
-keepattributes Signature,AnnotationDefault,EnclosingMethod,InnerClasses,SourceFile,LineNumberTable

# GSON and Retrofit
-keep class com.google.gson.** { *; }
-keepattributes Signature
-keepattributes *Annotation*
-keep class sun.misc.Unsafe { *; }
-keep class com.google.gson.reflect.TypeToken
-keep class * extends com.google.gson.TypeAdapter

# Keep model classes from being obfuscated or stripped
# Explicitly keep names and constructors for Firestore and GSON mapping
-keep class com.example.smartswine.model.** {
    public <init>(...);
    public <fields>;
    public <methods>;
}
-keepclassmembers class com.example.smartswine.model.** {
    public <init>(...);
    public <fields>;
    public <methods>;
}

# Keep UserProfile specifically (used in Auth and Firestore)
-keep class com.example.smartswine.ui.auth.UserProfile {
    public <init>(...);
    public <fields>;
    public <methods>;
}
-keepclassmembers class com.example.smartswine.ui.auth.UserProfile {
    public <init>(...);
    public <fields>;
    public <methods>;
}
