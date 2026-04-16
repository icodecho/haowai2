# Add project specific ProGuard rules here.
-keepattributes *Annotation*
-keepattributes Exceptions
-keepattributes InnerClasses
-keepattributes Signature
-keepattributes SourceFile,LineNumberTable

-keep class com.huawei.hms.** { *; }
-keep class evilcode.notification.hwpush.** { *; }
-dontwarn com.huawei.hms.**
