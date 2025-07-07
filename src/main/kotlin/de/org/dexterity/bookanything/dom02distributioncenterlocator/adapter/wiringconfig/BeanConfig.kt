package de.org.dexterity.bookanything.dom02distributioncenterlocator.adapter.wiringconfig

import de.org.dexterity.bookanything.dom02distributioncenterlocator.application.CentroDistribuicaoOrchestrationService
import de.org.dexterity.bookanything.dom02distributioncenterlocator.application.GeoJsonProcessingService
import de.org.dexterity.bookanything.dom02distributioncenterlocator.domain.EventPublisherPort
import de.org.dexterity.bookanything.dom02distributioncenterlocator.domain.geojson.GeoJsonUploadedFileDTO
import de.org.dexterity.bookanything.dom02distributioncenterlocator.domain.ports.CentroDistribuicaoPersistRepositoryPort
import de.org.dexterity.bookanything.dom02distributioncenterlocator.domain.ports.CentroDistribuicaoQueryRepositoryPort
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.kafka.core.KafkaTemplate

@Configuration
class BeanConfig {

    @Bean
    fun centroDistribuicaoOrchestrationService(
        centroDistribuicaoPersistRepositoryPort: CentroDistribuicaoPersistRepositoryPort,
        centroDistribuicaoQueryRepositoryPort: CentroDistribuicaoQueryRepositoryPort,
        eventPublisher: EventPublisherPort
    ): CentroDistribuicaoOrchestrationService {

        return CentroDistribuicaoOrchestrationService(
            centroDistribuicaoPersistRepositoryPort,
            centroDistribuicaoQueryRepositoryPort,
            eventPublisher
        )
    }

    @Bean
    fun geoJsonProcessingService(
        kafkaTemplate: KafkaTemplate<String, GeoJsonUploadedFileDTO>,
        centroDistribuicaoOrchestrationService: CentroDistribuicaoOrchestrationService
    ): GeoJsonProcessingService {

        return GeoJsonProcessingService(kafkaTemplate, centroDistribuicaoOrchestrationService)
    }
}