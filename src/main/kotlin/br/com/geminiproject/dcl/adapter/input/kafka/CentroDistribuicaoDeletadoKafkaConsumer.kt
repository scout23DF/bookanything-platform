package br.com.geminiproject.dcl.adapter.input.kafka

import br.com.geminiproject.dcl.adapter.output.persistence.elasticsearch.CentroDistribuicaoElasticRepository
import br.com.geminiproject.dcl.domain.events.CentroDistribuicaoDeletadoEvent
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
