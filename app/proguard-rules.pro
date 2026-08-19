# kotlinx.serialization keeps generated serializers reachable
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**
-keepclassmembers class dev.plumage.data.remote.dto.** {
    *** Companion;
}
-keepclasseswithmembers class dev.plumage.data.remote.dto.** {
    kotlinx.serialization.KSerializer serializer(...);
}
