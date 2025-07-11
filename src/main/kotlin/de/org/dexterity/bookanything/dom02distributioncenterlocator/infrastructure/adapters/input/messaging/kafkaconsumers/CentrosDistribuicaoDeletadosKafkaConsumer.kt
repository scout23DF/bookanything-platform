package de.org.dexterity.bookanything.dom02distributioncenterlocator.adapter.input.kafka

import de.org.dexterity.bookanything.dom02distributioncenterlocator.domain.events.CentrosDistribuicaoDeletadosEvent
import de.org.dexterity.bookanything.dom02distributioncenterlocator.domain.ports.CentroDistribuicaoQueryRepositoryPort
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
