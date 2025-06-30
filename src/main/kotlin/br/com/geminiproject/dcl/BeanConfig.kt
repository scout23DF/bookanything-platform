package br.com.geminiproject.dcl

import br.com.geminiproject.dcl.application.CentroDistribuicaoOrchestrationService
import br.com.geminiproject.dcl.application.GeoJsonProcessingService
import br.com.geminiproject.dcl.domain.EventPublisherPort
import br.com.geminiproject.dcl.domain.ports.CentroDistribuicaoPersistRepositoryPort
import br.com.geminiproject.dcl.domain.ports.CentroDistribuicaoQueryRepositoryPort
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class BeanConfig {

    @Bean
    fun centroDistribuicaoOrchestrationService(
        centroDistribuicaoPersistRepositoryPort: CentroDistribuicaoPersistRepositoryPort,
        centroDistribuicaoQueryRepositoryPort: CentroDistribuicaoQueryRepositoryPort,
        eventPublisher: EventPublisherPort): CentroDistribuicaoOrchestrationService {

        return CentroDistribuicaoOrchestrationService(
            centroDistribuicaoPersistRepositoryPort,
            centroDistribuicaoQueryRepositoryPort,
            eventPublisher
        )
    }

    @Bean
    fun geoJsonProcessingService(centroDistribuicaoOrchestrationService: CentroDistribuicaoOrchestrationService): GeoJsonProcessingService {
        return GeoJsonProcessingService(centroDistribuicaoOrchestrationService)
    }
}
