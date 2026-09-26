package de.org.dexterity.bookanything.shared.integrationtests

import de.org.dexterity.bookanything.dom01geolocation.application.workflow.GeoLocationArtifactsLauncher
import io.mockk.mockk
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary

/**
 * Keeps the integration tests away from Temporal. The Kafka enrichment consumer starts a
 * `geo-artifacts-<id>` workflow for every GeoLocation the tests create, and the default Temporal address
 * (`application.yml`) is the cluster's ClusterIP, reachable from the developer machine: without this, the
 * tests started 34 workflows in the real `corporate-core` namespace (backlog item 29).
 *
 * The launcher is replaced by a no-op double (so the consumer never blocks trying to connect) and
 * [AbstractIntegrationTest] also points `temporal.service-address` to an address nothing listens on.
 */
@TestConfiguration
class NoTemporalTestConfig {

    @Bean
    @Primary
    fun noOpGeoLocationArtifactsLauncher(): GeoLocationArtifactsLauncher =
        mockk<GeoLocationArtifactsLauncher>(relaxed = true)
}
