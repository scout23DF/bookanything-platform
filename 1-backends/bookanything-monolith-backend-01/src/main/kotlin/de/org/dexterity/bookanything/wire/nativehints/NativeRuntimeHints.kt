package de.org.dexterity.bookanything.wire.nativehints

import io.temporal.activity.ActivityInterface
import io.temporal.workflow.WorkflowInterface
import org.springframework.aot.hint.BindingReflectionHintsRegistrar
import org.springframework.aot.hint.MemberCategory
import org.springframework.aot.hint.RuntimeHints
import org.springframework.aot.hint.RuntimeHintsRegistrar
import org.springframework.beans.factory.annotation.AnnotatedBeanDefinition
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider

class NativeRuntimeHints : RuntimeHintsRegistrar {
    override fun registerHints(hints: RuntimeHints, classLoader: ClassLoader?) {
        registerApplicationTypes(hints, classLoader ?: javaClass.classLoader)

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
            "org.postgresql.geometric.PGbox",
            "org.postgresql.geometric.PGcircle",
            "org.postgresql.geometric.PGline",
            "org.postgresql.geometric.PGlseg",
            "org.postgresql.geometric.PGpath",
            "org.postgresql.util.PGmoney",
            "org.postgresql.util.PGInterval",
            "net.postgis.jdbc.PGboxbase",
            "net.postgis.jdbc.PGbox3d",
            "net.postgis.jdbc.PGbox2d",
            "net.postgis.jdbc.PGgeometry",
            "net.postgis.jdbc.PGgeography",
            "net.postgis.jdbc.PGgeo",
            "net.postgis.jdbc.PGgeometryLW",
            "net.postgis.jdbc.PGgeographyLW",
            "net.postgis.jdbc.DriverWrapper",
            "net.postgis.jdbc.geometry.Geometry",
            "net.postgis.jdbc.geometry.Point",
            "net.postgis.jdbc.geometry.Polygon",
            "net.postgis.jdbc.geometry.LineString",
            "net.postgis.jdbc.geometry.MultiPoint",
            "net.postgis.jdbc.geometry.MultiPolygon",
            "net.postgis.jdbc.geometry.MultiLineString",
            "net.postgis.jdbc.geometry.GeometryCollection",
            "net.postgis.jdbc.geometry.LinearRing",
            "org.apache.kafka.common.serialization.StringSerializer",
            "org.apache.kafka.common.serialization.StringDeserializer",
            "org.apache.kafka.common.serialization.ByteArraySerializer",
            "org.apache.kafka.common.serialization.ByteArrayDeserializer",
            "org.springframework.kafka.support.serializer.JsonSerializer",
            "org.springframework.kafka.support.serializer.JsonDeserializer",
            "org.springframework.kafka.support.serializer.ErrorHandlingDeserializer",
            "org.springframework.security.oauth2.jwt.JwtDecoder",
            "org.springframework.security.oauth2.jwt.NimbusJwtDecoder",
            "org.springframework.security.oauth2.jwt.JwtValidators",
            "com.nimbusds.jose.jwk.JWKSet",
            "com.nimbusds.jose.jwk.RSAKey",
            "com.nimbusds.jose.jwk.ECKey",
            "com.nimbusds.jose.jwk.OctetSequenceKey",
            "java.lang.reflect.RecordComponent",
            "io.swagger.v3.oas.models.OpenAPI",
            "io.swagger.v3.oas.models.Paths",
            "io.swagger.v3.oas.models.PathItem",
            "io.swagger.v3.oas.models.Operation"
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
        hints.resources().registerPattern("org/postgresql/*")
        hints.resources().registerPattern("org/postgresql/driverconfig.properties")
        hints.resources().registerPattern("net/postgis/*")
        try {
            hints.resources().registerResourceBundle("org.hibernate.spatial.HSMessageLogger")
            hints.resources().registerResourceBundle("org.hibernate.spatial.HSMessageLogger.i18n")
        } catch (_: Exception) {
            // Ignore if bundle registration format differs
        }
    }

    /**
     * Spring AOT only registers reflection for types it can see in bean signatures (e.g.
     * controller DTOs). Types that are (de)serialized elsewhere -- Kafka payloads, NiFi and
     * Temporal messages, Wikidata/JSReport responses read with ObjectMapper -- and the
     * Temporal workflow/activity stubs (JDK proxies built at run time) would fail only in the
     * native image. Scan the application's own classes at build time instead of keeping a
     * hand-written list that silently goes stale:
     *  - Kotlin data classes, enums and records -> Jackson binding hints;
     *  - @WorkflowInterface / @ActivityInterface -> JDK proxy + method reflection;
     *  - their implementations -> constructor/method reflection (Temporal inspects them).
     */
    private fun registerApplicationTypes(hints: RuntimeHints, classLoader: ClassLoader) {
        val scanner = object : ClassPathScanningCandidateComponentProvider(false) {
            // Include interfaces and abstract types too; filtering happens below.
            override fun isCandidateComponent(beanDefinition: AnnotatedBeanDefinition) = true
        }.apply {
            setResourceLoader(org.springframework.core.io.DefaultResourceLoader(classLoader))
            addIncludeFilter { _, _ -> true }
        }
        val binding = BindingReflectionHintsRegistrar()
        val temporalInterfaces = mutableListOf<Class<*>>()
        val types = scanner.findCandidateComponents(APP_PACKAGE).mapNotNull { bd ->
            try {
                Class.forName(bd.beanClassName, false, classLoader)
            } catch (_: Throwable) {
                null
            }
        }

        for (clazz in types) {
            if (clazz.isAnnotationPresent(WorkflowInterface::class.java) || clazz.isAnnotationPresent(ActivityInterface::class.java)) {
                temporalInterfaces += clazz
                hints.proxies().registerJdkProxy(clazz)
                hints.reflection().registerType(clazz, MemberCategory.INVOKE_PUBLIC_METHODS, MemberCategory.INVOKE_DECLARED_METHODS)
            } else if (clazz.isEnum || clazz.isRecord || isKotlinDataClass(clazz)) {
                binding.registerReflectionHints(hints.reflection(), clazz)
            }
        }
        for (clazz in types) {
            if (!clazz.isInterface && temporalInterfaces.any { it.isAssignableFrom(clazz) }) {
                hints.reflection().registerType(
                    clazz,
                    MemberCategory.INVOKE_DECLARED_CONSTRUCTORS,
                    MemberCategory.INVOKE_PUBLIC_METHODS,
                    MemberCategory.INVOKE_DECLARED_METHODS
                )
            }
        }
    }

    private fun isKotlinDataClass(clazz: Class<*>): Boolean =
        try {
            clazz.getAnnotation(Metadata::class.java) != null && clazz.kotlin.isData
        } catch (_: Throwable) {
            false
        }

    private companion object {
        const val APP_PACKAGE = "de.org.dexterity.bookanything"
    }
}
