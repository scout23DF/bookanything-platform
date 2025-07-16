package de.org.dexterity.bookanything.shared.integrationtests

import dasniko.testcontainers.keycloak.KeycloakContainer
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.containers.KafkaContainer
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.elasticsearch.ElasticsearchContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.utility.DockerImageName

@Testcontainers
abstract class AbstractIntegrationTest {

    companion object {
        @Container
        val postgres: PostgreSQLContainer<*> = PostgreSQLContainer(
            DockerImageName.parse("postgis/postgis:16-3.4").asCompatibleSubstituteFor("postgres")
        )
            .withDatabaseName("DBBookAnythingPlatform")
            .withUsername("dbabookanythingapps01")
            .withPassword("1a88a1")

        @Container
        val elasticsearch: ElasticsearchContainer = ElasticsearchContainer(DockerImageName.parse("elasticsearch:8.14.0"))
            .withEnv("discovery.type", "single-node")
            .withEnv("ES_JAVA_OPTS", "-Xms512m -Xmx512m")
            .withEnv("xpack.security.enabled", "false")

        @Container
        val kafka: KafkaContainer = KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.6.0"))
            .withEnv("KAFKA_MESSAGE_MAX_BYTES", "52428800")
            .withEnv("KAFKA_REPLICA_FETCH_MAX_BYTES", "52428800")

        @Container
        val keycloak: KeycloakContainer = KeycloakContainer("keycloak/keycloak:26.3.1")
            .withRealmImportFile("keycloak/dexterity-apps-01-realm.json")


        @JvmStatic
        @DynamicPropertySource
        fun properties(registry: DynamicPropertyRegistry) {
            registry.add("spring.datasource.url", postgres::getJdbcUrl)
            registry.add("spring.datasource.username", postgres::getUsername)
            registry.add("spring.datasource.password", postgres::getPassword)
            registry.add("spring.elasticsearch.uris", elasticsearch::getHttpHostAddress)
            registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers)
            registry.add("spring.security.oauth2.resourceserver.jwt.issuer-uri") { keycloak.authServerUrl + "/realms/dexterity-apps-01" }
            registry.add("spring.security.oauth2.client.provider.oidc.issuer-uri") { keycloak.authServerUrl + "/realms/dexterity-apps-01" }
        }
    }
}