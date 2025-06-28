package br.com.geminiproject.dcl

import br.com.geminiproject.dcl.application.BuscarCentroDistribuicaoQueryService
import br.com.geminiproject.dcl.application.BuscarCentrosProximosService
import br.com.geminiproject.dcl.application.CentroDistribuicaoService
import br.com.geminiproject.dcl.domain.BuscarCentroDistribuicaoQueryPort
import br.com.geminiproject.dcl.domain.BuscarCentrosProximosUseCase
import br.com.geminiproject.dcl.domain.CentroDistribuicaoQueryRepositoryPort
import br.com.geminiproject.dcl.domain.CentroDistribuicaoRepositoryPort
import br.com.geminiproject.dcl.domain.EventPublisherPort
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class BeanConfig {

    @Bean
    fun centroDistribuicaoService(repositoryPort: CentroDistribuicaoRepositoryPort, eventPublisher: EventPublisherPort): CentroDistribuicaoService {
        return CentroDistribuicaoService(repositoryPort, eventPublisher)
    }

    @Bean
    fun buscarCentroDistribuicaoQueryService(queryRepositoryPort: CentroDistribuicaoQueryRepositoryPort): BuscarCentroDistribuicaoQueryService {
        return BuscarCentroDistribuicaoQueryService(queryRepositoryPort)
    }

    @Bean
    fun buscarCentrosProximosUseCase(centroDistribuicaoQueryRepositoryPort: CentroDistribuicaoQueryRepositoryPort): BuscarCentrosProximosService {
        return BuscarCentrosProximosService(centroDistribuicaoQueryRepositoryPort)
    }
}
