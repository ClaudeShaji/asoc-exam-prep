# Only with -PreleaseTests: keep the androidx/kotlin libraries the test runner shares
# with the app. App code is still shrunk and obfuscated as in a real release.
-keep class androidx.** { *; }
-keep class kotlin.** { *; }
-keep class kotlinx.coroutines.** { *; }
-dontwarn com.google.errorprone.annotations.**
# App internals the device tests call directly (container, settings, game, data, content).
-keepclassmembers,allowobfuscation class com.asocexamprep.RadioPrepApp { *; }
-keepclassmembers,allowobfuscation class com.asocexamprep.AppContainer { *; }
-keepclassmembers,allowobfuscation class com.asocexamprep.data.settings.** { *; }
-keepclassmembers,allowobfuscation class com.asocexamprep.data.db.** { *; }
-keepclassmembers,allowobfuscation class com.asocexamprep.game.** { *; }
-keepclassmembers,allowobfuscation class com.asocexamprep.content.** { *; }
-keepclassmembers,allowobfuscation class com.asocexamprep.morse.** { *; }
-keep,allowobfuscation interface com.asocexamprep.morse.MorsePlayer { *; }
