package br.com.geminiproject.dcl.adapter.input.kafka

import br.com.geminiproject.dcl.domain.events.CentrosDistribuicaoDeletadosEvent
import br.com.geminiproject.dcl.domain.ports.CentroDistribuicaoQueryRepositoryPort
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.stereotype.Component

@Component
class CentrosDistribuicaoDeletadosKafkaConsumer(
    private val centroDistribuicaoQueryRepositoryPort: CentroDistribuicaoQueryRepositoryPort
) {

    @KafkaListener(topics = ["centros-distribuicao-deletados"], groupId = "elasticsearch-deleter-all")
    fun listen(event: CentrosDistribuicaoDeletadosEvent) {
        centroDistribuicaoQueryRepositoryPort.deletarTodos()
        println("Todos os Centros de Distribuicao foram removidos do Elasticsearch.")
    }
}
