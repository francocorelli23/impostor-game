# Impostor - R8 / ProGuard rules
#
# The app has no reflection-based serialization, no JNI and no dynamic class loading,
# so the default optimized rules plus the Compose rules shipped with the libraries
# are enough. The entries below only harden things that R8 cannot infer.

# Keep line numbers useful in Play Console crash reports while still obfuscating names.
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Compose runtime ships its own consumer rules; nothing extra is required here.

# Strip all logging from the release build.
-assumenosideeffects class android.util.Log {
    public static int v(...);
    public static int d(...);
    public static int i(...);
    public static int w(...);
    public static int e(...);
}

# org.json is part of the Android platform - never bundled, never renamed.
-dontwarn org.json.**

# Kotlin metadata that is only needed by reflection-heavy libraries we do not use.
-dontwarn kotlin.Metadata
