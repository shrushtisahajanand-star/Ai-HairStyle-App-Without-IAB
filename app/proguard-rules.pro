# =============================================================================
# AI Hairstyle — R8 / ProGuard release rules
#
# Rule of thumb: keep ONLY what is reached by reflection or by name from XML.
# Every extra `-keep ... { *; }` both leaks readable names to decompilers
# (JADX) and blocks shrinking, which grows the APK.
#
# Libraries (Firebase, GMS Ads, Coil, Lottie, ML Kit, Play Core,
# OkHttp, Gson, Hilt, AndroidX) all ship their own consumer rules inside their
# AARs — do NOT re-declare blanket keeps for them here.
# =============================================================================

# ----- Attributes -----
# Signature + InnerClasses/EnclosingMethod: generic types for Gson.
# SourceFile/LineNumberTable + rename: readable Crashlytics stack traces
# without exposing the real .kt file names.
-keepattributes Signature
-keepattributes InnerClasses
-keepattributes EnclosingMethod
-keepattributes Exceptions
-keepattributes *Annotation*
-keepattributes RuntimeVisibleAnnotations
-keepattributes RuntimeInvisibleAnnotations
-keepattributes RuntimeVisibleParameterAnnotations
-keepattributes RuntimeInvisibleParameterAnnotations
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# ----- Stronger obfuscation -----
# Flatten every class into the root package and allow R8 to widen access —
# decompiled output becomes a flat list of a/b/c classes instead of the real
# package tree. Upload build/outputs/mapping/release/mapping.txt to Crashlytics.
-repackageclasses ''
-allowaccessmodification

# NOTE: kotlin.Metadata is intentionally NOT kept. It stores original Kotlin
# names and JADX uses it to restore them. Nothing here uses kotlin-reflect.

# =============================================================================
# Platform / framework reflection
# =============================================================================

# Enum constants are persisted by name() in SharedPreferences.
-keepclassmembers enum * {
    <fields>;
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

-keepclassmembers class * implements android.os.Parcelable {
    public static final ** CREATOR;
}

-keepclassmembers class * implements java.io.Serializable {
    static final long serialVersionUID;
    private static final java.io.ObjectStreamField[] serialPersistentFields;
    private void writeObject(java.io.ObjectOutputStream);
    private void readObject(java.io.ObjectInputStream);
    java.lang.Object writeReplace();
    java.lang.Object readResolve();
}

-keepclasseswithmembernames class * {
    native <methods>;
}

# Navigation Safe Args passes these by name.
-keepnames class * extends android.os.Parcelable
-keepnames class * implements java.io.Serializable

# Fragments are named as strings inside res/navigation/nav_graph.xml, so their
# class names must survive. Members are still fully obfuscated.
-keepnames class * extends androidx.fragment.app.Fragment
-keepclassmembers class * extends androidx.fragment.app.Fragment {
    public <init>();
}

# =============================================================================
# App JSON models (Gson) — field names are the JSON keys, so keep member
# names only. Class names stay obfuscated.
# =============================================================================

-keepclassmembers class com.hairstyle.changer.aitryon.data.model.** {
    <init>(...);
    <fields>;
}
-keepclassmembers class com.hairstyle.changer.aitryon.domain.model.** {
    <init>(...);
    <fields>;
}
-keepclassmembers class com.hairstyle.changer.aitryon.data.remoteconfig.** {
    <init>(...);
    <fields>;
}

# Gson: fields annotated with @SerializedName / @JsonAdapter anywhere else.
-keepclassmembers,allowobfuscation class * {
    @com.google.gson.annotations.SerializedName <fields>;
}
-keepclassmembers class * {
    @com.google.gson.annotations.JsonAdapter <fields>;
}
-keep class * extends com.google.gson.TypeAdapter
-keep class * implements com.google.gson.TypeAdapterFactory
-keep class * implements com.google.gson.JsonSerializer
-keep class * implements com.google.gson.JsonDeserializer
# R8: preserve generic signatures of TypeToken subclasses.
-keep class * extends com.google.gson.reflect.TypeToken { *; }
-keepclassmembers class * extends com.google.gson.reflect.TypeToken {
    <init>();
}

# =============================================================================
# Retrofit / OkHttp
# =============================================================================

-keepclasseswithmembers class * {
    @retrofit2.http.* <methods>;
}
-if interface * { @retrofit2.http.* <methods>; }
-keep,allowobfuscation interface <1>
-keep,allowobfuscation,allowshrinking class kotlin.coroutines.Continuation

-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn retrofit2.**
-dontwarn javax.annotation.**
-dontwarn org.codehaus.mojo.animal_sniffer.**

# =============================================================================
# Ads — AdMob loads mediation adapters by class name from the server, so those
# adapter entry points must keep their names.
# =============================================================================

-keep class com.google.ads.mediation.** { *; }
-keep class com.google.android.gms.ads.mediation.** { *; }
-keep class com.facebook.ads.** { *; }
-dontwarn com.google.android.gms.ads.**
-dontwarn com.facebook.ads.**

# =============================================================================
# Misc warnings (no keeps — these libraries bring their own rules)
# =============================================================================

-dontwarn kotlin.**
-dontwarn kotlinx.coroutines.**
-dontwarn dagger.hilt.**
-dontwarn com.google.errorprone.annotations.**
-dontwarn com.google.firebase.**
-dontwarn com.google.android.gms.**
-dontwarn com.google.mlkit.**
# ML Kit face detection — keep entry points for release (model / reflection).
-keep class com.google.mlkit.vision.face.** { *; }
-keep class com.google.android.gms.vision.** { *; }
-keep class com.google.android.gms.internal.mlkit_vision_face.** { *; }
-dontwarn com.google.android.play.core.**
-dontwarn com.yalantis.ucrop.**
-dontwarn coil.**
-dontwarn com.airbnb.lottie.**
-dontwarn com.facebook.shimmer.**
-dontwarn com.intuit.sdp.**
-dontwarn com.intuit.ssp.**
-dontwarn org.bouncycastle.**
-dontwarn org.conscrypt.**
-dontwarn org.openjsse.**
-dontwarn javax.lang.model.**
-dontwarn com.google.auto.value.**

# =============================================================================
# Strip all Log calls from release builds (also removes log strings from DEX).
# =============================================================================

-assumenosideeffects class android.util.Log {
    public static *** v(...);
    public static *** d(...);
    public static *** i(...);
    public static *** w(...);
    public static boolean isLoggable(...);
}
