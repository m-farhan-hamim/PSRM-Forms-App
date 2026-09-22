# Moshi reflective adapters need class/member names kept for the DTOs.
-keep class com.psrm.forms.psbdx.data.remote.dto.** { *; }
-keep class com.psrm.forms.psbdx.data.local.** { *; }
-keepclassmembers class * {
    @com.squareup.moshi.Json <fields>;
}
-keep @com.squareup.moshi.JsonClass class *
