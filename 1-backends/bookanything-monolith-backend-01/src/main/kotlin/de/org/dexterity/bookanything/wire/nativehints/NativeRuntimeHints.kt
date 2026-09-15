package de.org.dexterity.bookanything.wire.nativehints

import org.springframework.aot.hint.MemberCategory
import org.springframework.aot.hint.RuntimeHints
import org.springframework.aot.hint.RuntimeHintsRegistrar

class NativeRuntimeHints : RuntimeHintsRegistrar {
    override fun registerHints(hints: RuntimeHints, classLoader: ClassLoader?) {
        val reflectionClasses = listOf(
            "org.hibernate.spatial.HSMessageLogger_\$logger",
            "org.hibernate.spatial.HSMessageLogger",
            "org.hibernate.spatial.integration.SpatialService",
            "org.hibernate.spatial.integration.SpatialInitializer",
            "org.locationtech.jts.geom.Geometry",
            "org.locationtech.jts.geom.Point",
            "org.locationtech.jts.geom.Polygon",
            "org.locationtech.jts.geom.MultiPolygon",
            "org.locationtech.jts.geom.LineString",
            "org.locationtech.jts.geom.MultiLineString",
            "org.locationtech.jts.geom.Coordinate",
            "org.locationtech.jts.geom.Envelope",
            "org.locationtech.jts.geom.PrecisionModel",
            "org.locationtech.jts.geom.GeometryFactory",
            "org.geolatte.geom.Geometry",
            "org.geolatte.geom.Point",
            "org.geolatte.geom.Polygon"
        )

        for (className in reflectionClasses) {
            try {
                val clazz = Class.forName(className, false, classLoader ?: javaClass.classLoader)
                hints.reflection().registerType(
                    clazz,
                    MemberCategory.INVOKE_PUBLIC_CONSTRUCTORS,
                    MemberCategory.INVOKE_DECLARED_CONSTRUCTORS,
                    MemberCategory.INVOKE_PUBLIC_METHODS,
                    MemberCategory.INVOKE_DECLARED_METHODS,
                    MemberCategory.ACCESS_DECLARED_FIELDS,
                    MemberCategory.ACCESS_PUBLIC_FIELDS
                )
            } catch (_: ClassNotFoundException) {
                // Class not present on classpath, skip
            }
        }

        hints.resources().registerPattern("org/hibernate/spatial/*")
        hints.resources().registerPattern("org/locationtech/jts/*")
        try {
            hints.resources().registerResourceBundle("org.hibernate.spatial.HSMessageLogger")
            hints.resources().registerResourceBundle("org.hibernate.spatial.HSMessageLogger.i18n")
        } catch (_: Exception) {
            // Ignore if bundle registration format differs
        }
    }
}
