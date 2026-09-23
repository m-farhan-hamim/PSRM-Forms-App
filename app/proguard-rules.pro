# Moshi reflective adapters need class/member names kept for the DTOs.
-keep class com.psrm.forms.psbdx.data.remote.dto.** { *; }
-keep class com.psrm.forms.psbdx.data.local.** { *; }
-keepclassmembers class * {
    @com.squareup.moshi.Json <fields>;
}
-keep @com.squareup.moshi.JsonClass class *

# ── androidx.security:security-crypto (Jetpack Security / EncryptedSharedPreferences) ──
#
# security-crypto pulls in Google Tink, whose annotations
# (com.google.errorprone.annotations.*, javax.annotation.*,
# org.checkerframework.*) are compile-time-only — they're never on the
# runtime classpath, which is exactly why R8 can't find them and fails the
# release build with "Missing classes" otherwise. They're pure annotations
# with no runtime behavior to preserve, so -dontwarn (not -keep) is the
# correct fix: it tells R8 these references are fine to leave unresolved
# rather than pretending we need to keep classes we don't even ship.
-dontwarn com.google.errorprone.annotations.**
-dontwarn javax.annotation.**
-dontwarn javax.annotation.concurrent.**
-dontwarn org.checkerframework.**
-dontwarn com.google.j2objc.annotations.**
-dontwarn java.lang.SafeVarargs

