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
            "org.geolatte.geom.Polygon",
            "org.hibernate.dialect.PostgreSQLDialect",
            "org.hibernate.dialect.DatabaseVersion",
            "org.hibernate.spatial.dialect.postgis.PostgisDialect",
            "org.hibernate.spatial.dialect.postgis.PostgisPG95Dialect",
            "org.hibernate.spatial.contributor.SpatialTypeContributor",
            "org.hibernate.spatial.contributor.ContributorResolver",
            "org.postgresql.Driver",
            "org.postgresql.util.PGobject",
            "org.postgresql.geometric.PGpoint",
            "org.postgresql.geometric.PGpolygon",
            "org.postgresql.geometric.PGbox"
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

        val arrayTypes = listOf(
            "java.util.UUID[]",
            "java.lang.Long[]",
            "java.lang.String[]",
            "java.lang.Integer[]",
            "java.lang.Object[]",
            "[Ljava.util.UUID;",
            "[Ljava.lang.Long;",
            "[Ljava.lang.String;",
            "[Ljava.lang.Integer;",
            "[Ljava.lang.Object;"
        )
        for (arrType in arrayTypes) {
            try {
                if (arrType.endsWith("[]")) {
                    hints.reflection().registerType(org.springframework.aot.hint.TypeReference.of(arrType))
                } else {
                    val arrClazz = Class.forName(arrType, false, classLoader ?: javaClass.classLoader)
                    hints.reflection().registerType(arrClazz)
                }
            } catch (_: Throwable) {
                // Ignore if type reference parsing or class loading differs
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
