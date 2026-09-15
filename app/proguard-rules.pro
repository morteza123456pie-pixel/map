# MapLibre and its GeoJSON/Gson stack ship their own consumer ProGuard rules inside the
# AAR, so only project-specific rules belong here.

# Keep Hilt/Dagger generated components intact.
-keep,allowobfuscation,allowshrinking class dagger.hilt.** { *; }

# Model classes are serialized/deserialized by name in later phases (search, places).
-keepclassmembers class com.aimaps.app.domain.model.** { *; }
