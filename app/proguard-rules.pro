# kotlinx.serialization: keep generated serializers for navigation routes and DTOs
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**
-keepclassmembers class kotlinx.serialization.json.** { *** Companion; }
-keepclasseswithmembers class com.danielcrvo.sintonia.** {
    kotlinx.serialization.KSerializer serializer(...);
}
