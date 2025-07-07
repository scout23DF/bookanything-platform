package de.org.dexterity.bookanything.dom02distributioncenterlocator.adapter.input.kafka

import de.org.dexterity.bookanything.dom02distributioncenterlocator.adapter.output.persistence.elasticsearch.CentroDistribuicaoElasticRepository
import de.org.dexterity.bookanything.dom02distributioncenterlocator.domain.events.CentroDistribuicaoDeletadoEvent
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.stereotype.Component

@Component
class CentroDistribuicaoDeletadoKafkaConsumer(
    private val centroDistribuicaoElasticRepository: CentroDistribuicaoElasticRepository
) {

    @KafkaListener(topics = ["centro-distribuicao-deletado"], groupId = "elasticsearch-deleter")
    fun listen(event: CentroDistribuicaoDeletadoEvent) {
        centroDistribuicaoElasticRepository.deleteById(event.id)
        println("Centro de Distribuicao removido do Elasticsearch: ${event.id}")
    }
}
