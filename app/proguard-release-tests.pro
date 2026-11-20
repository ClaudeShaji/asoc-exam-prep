# Only with -PreleaseTests: keep the androidx/kotlin libraries the test runner shares
# with the app. App code is still shrunk and obfuscated as in a real release.
-keep class androidx.** { *; }
-keep class kotlin.** { *; }
-keep class kotlinx.coroutines.** { *; }
-dontwarn com.google.errorprone.annotations.**
