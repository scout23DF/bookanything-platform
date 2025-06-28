package br.com.geminiproject.dcl.adapter.output.persistence

import br.com.geminiproject.dcl.domain.CentroDistribuicao
import br.com.geminiproject.dcl.domain.CentroDistribuicaoQueryRepositoryPort
import org.springframework.stereotype.Component
import java.util.UUID
import org.slf4j.LoggerFactory

@Component
class CentroDistribuicaoQueryPersistenceAdapter(
    private val repository: CentroDistribuicaoRepository
) : CentroDistribuicaoQueryRepositoryPort {

    companion object {
        private val logger = LoggerFactory.getLogger(CentroDistribuicaoQueryPersistenceAdapter::class.java)
    }

    override fun buscarPorId(id: UUID): CentroDistribuicao? {
        logger.info("Attempting to find CentroDistribuicao with ID: {}", id)
        val optionalEntity = repository.findById(id)
        if (optionalEntity.isPresent) {
            val entity = optionalEntity.get()
            logger.info("Found CentroDistribuicao: {}", entity.id)
            return CentroDistribuicao(
                id = entity.id!!,
                nome = entity.nome!!,
                localizacao = entity.localizacao!!
            )
        } else {
            logger.warn("CentroDistribuicao with ID {} not found.", id)
            return null
        }
    }

    override fun buscarTodos(): List<CentroDistribuicao> {
        logger.info("Attempting to find all CentroDistribuicao entities.")
        return repository.findAll().map { entity ->
            CentroDistribuicao(
                id = entity.id!!,
                nome = entity.nome!!,
                localizacao = entity.localizacao!!
            )
        }
    }

    override fun buscarCentrosProximos(localizacao: org.locationtech.jts.geom.Point, raioEmKm: Double): List<CentroDistribuicao> {
        logger.info("Attempting to find CentroDistribuicao within {} km of latitude {} and longitude {}", raioEmKm, localizacao.y, localizacao.x)
        return repository.findByLocalizacaoWithinDistance(localizacao, raioEmKm).map { entity ->
            CentroDistribuicao(
                id = entity.id!!,
                nome = entity.nome!!,
                localizacao = entity.localizacao!!
            )
        }
    }
}
