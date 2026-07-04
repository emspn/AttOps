# Credential Manager
-keep class androidx.credentials.** { *; }
-keep class com.google.android.libraries.identity.googleid.** { *; }

# Supabase & Serialization
-keepattributes *Annotation*, Signature, InnerClasses
-keep class io.github.jan.supabase.** { *; }
-keep class kotlinx.serialization.** { *; }
-keepclassmembers class * {
    @kotlinx.serialization.Serializable *;
}

# Hilt
-keep class dagger.hilt.android.internal.** { *; }
