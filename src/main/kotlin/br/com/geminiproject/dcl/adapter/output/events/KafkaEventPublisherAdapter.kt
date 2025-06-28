package br.com.geminiproject.dcl.adapter.output.events

import br.com.geminiproject.dcl.domain.EventPublisherPort
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Component

@Component
class KafkaEventPublisherAdapter(
    private val kafkaTemplate: KafkaTemplate<String, Any>
) : EventPublisherPort {

    override fun publish(event: Any) {
        val topic = when (event) {
            is br.com.geminiproject.dcl.domain.events.CentroDistribuicaoCadastradoEvent -> "centro-distribuicao-cadastrado"
            else -> throw IllegalArgumentException("Unknown event type: ${event.javaClass.name}")
        }
        kafkaTemplate.send(topic, event)
        kafkaTemplate.flush()
    }
}
