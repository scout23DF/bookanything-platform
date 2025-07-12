package de.org.dexterity.bookanything.dom01geolocation.infrastructure.adapters.input.messaging.kafkaconsumers

import de.org.dexterity.bookanything.dom01geolocation.domain.events.LocalizablePlacesAllDeletedEvent
import de.org.dexterity.bookanything.dom01geolocation.domain.ports.LocalizablePlaceQueryRepositoryPort
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.stereotype.Component

@Component
class LocalizablePlacesAllDeletedKafkaConsumer(
    private val localizablePlaceQueryRepositoryPort: LocalizablePlaceQueryRepositoryPort
) {

    @KafkaListener(topics = ["localizable-places-all-deleted-topic"], groupId = "elasticsearch-deleter-all")
    fun listen(event: LocalizablePlacesAllDeletedEvent) {
        localizablePlaceQueryRepositoryPort.deletarTodos()
        println("All the Localizable Places were removed successfully from ElasticSearch.")
    }
}
