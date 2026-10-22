# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

# ── Gson / Retrofit (reflection-based JSON mapping) ──
# Keep all DTO/data model classes so Gson can deserialize them after R8.
-keep class com.example.echowithin.data.model.** { *; }
-keep class com.example.echowithin.data.network.EchoWithinApiService { *; }
-keepattributes Signature
-keepattributes *Annotation*

# ── WebView JavaScript bridge ──
# Keep methods annotated @JavascriptInterface so the note math renderer
# (assets/katex/math_renderer.html) can still call AndroidBridge.onContentHeight.
-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}