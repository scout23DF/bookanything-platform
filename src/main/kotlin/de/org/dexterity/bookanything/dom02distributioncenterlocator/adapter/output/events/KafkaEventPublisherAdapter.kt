package de.org.dexterity.bookanything.dom02distributioncenterlocator.adapter.output.events

import de.org.dexterity.bookanything.dom02distributioncenterlocator.domain.EventPublisherPort
import de.org.dexterity.bookanything.dom02distributioncenterlocator.domain.events.CentroDistribuicaoCadastradoEvent
import de.org.dexterity.bookanything.dom02distributioncenterlocator.domain.events.CentroDistribuicaoDeletadoEvent
import de.org.dexterity.bookanything.dom02distributioncenterlocator.domain.events.CentrosDistribuicaoDeletadosEvent
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Component

@Component
class KafkaEventPublisherAdapter(
    private val kafkaTemplate: KafkaTemplate<String, Any>
) : EventPublisherPort {

    override fun publish(event: Any) {
        val topic = when (event) {
            is CentroDistribuicaoCadastradoEvent -> "centro-distribuicao-cadastrado"
            is CentroDistribuicaoDeletadoEvent -> "centro-distribuicao-deletado"
            is CentrosDistribuicaoDeletadosEvent -> "centros-distribuicao-deletados"
            else -> throw IllegalArgumentException("Unknown event type: ${event.javaClass.name}")
        }
        kafkaTemplate.send(topic, event)
        kafkaTemplate.flush()
    }
}
