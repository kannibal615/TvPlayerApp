# Project-specific R8 rules.
# Keep this file narrow: the default optimized Android rules and library metadata
# cover the current dependencies.

# Fire OS 7/Android 9 variants can omit java.lang.ClassValue. Keep the Android
# runtime detector and weak-map fallback intact so R8 cannot fold the fallback
# path into the JVM-only ClassValue cache.
-keep class kotlinx.coroutines.internal.FastServiceLoaderKt { *; }
-keep class kotlinx.coroutines.internal.WeakMapCtorCache { *; }
-keep class kotlinx.coroutines.android.** { *; }
