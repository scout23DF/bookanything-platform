package de.org.dexterity.bookanything.dom02distributioncenterlocator.adapter.input.kafka

import de.org.dexterity.bookanything.dom02distributioncenterlocator.infrastructure.adapters.output.persistence.elasticsearch.entities.CentroDistribuicaoElasticEntity
import de.org.dexterity.bookanything.dom02distributioncenterlocator.adapter.output.persistence.elasticsearch.CentroDistribuicaoElasticRepository
import de.org.dexterity.bookanything.dom02distributioncenterlocator.domain.events.CentroDistribuicaoCadastradoEvent
import org.slf4j.LoggerFactory
import org.springframework.data.elasticsearch.core.geo.GeoPoint
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.stereotype.Component

@Component
class CentroDistribuicaoKafkaConsumer(
    private val centroDistribuicaoElasticRepository: CentroDistribuicaoElasticRepository,
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    @KafkaListener(topics = ["centro-distribuicao-cadastrado"], groupId = "elasticsearch-indexer")
    fun listen(event: CentroDistribuicaoCadastradoEvent) {
        logger.info("Recebido evento de cadastro para o centro de distribuição: id={}, nome={}", event.id, event.nome)
        val elasticEntity = CentroDistribuicaoElasticEntity(
            id = event.id,
            nome = event.nome,
            localizacao = GeoPoint(event.latitude, event.longitude)
        )
        centroDistribuicaoElasticRepository.save(elasticEntity)
        logger.info("Centro de Distribuição indexado no Elasticsearch: id={}, nome={}", elasticEntity.id, elasticEntity.nome)
    }
}