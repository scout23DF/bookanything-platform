package br.com.geminiproject.dcl.adapter.input.kafka

import br.com.geminiproject.dcl.adapter.output.persistence.elasticsearch.CentroDistribuicaoElasticEntity
import br.com.geminiproject.dcl.adapter.output.persistence.elasticsearch.CentroDistribuicaoElasticRepository
import br.com.geminiproject.dcl.domain.events.CentroDistribuicaoCadastradoEvent
import org.locationtech.jts.geom.GeometryFactory
import org.locationtech.jts.geom.PrecisionModel
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.stereotype.Component
import org.springframework.data.elasticsearch.core.geo.GeoPoint

@Component
class CentroDistribuicaoKafkaConsumer(
    private val centroDistribuicaoElasticRepository: CentroDistribuicaoElasticRepository
) {

    private val geometryFactory = GeometryFactory(PrecisionModel(), 4326)

    @KafkaListener(topics = ["centro-distribuicao-cadastrado"], groupId = "elasticsearch-indexer")
    fun listen(event: CentroDistribuicaoCadastradoEvent) {
        val elasticEntity = CentroDistribuicaoElasticEntity(
            id = event.id,
            nome = event.nome,
            localizacao = GeoPoint(event.latitude, event.longitude)
        )
        centroDistribuicaoElasticRepository.save(elasticEntity)
        println("Centro de Distribuicao indexado no Elasticsearch: ${elasticEntity.id}")
    }
}
