package de.org.dexterity.bookanything.dom01geolocation.infrastructure.adapters.input.messaging.kafkaconsumers

import de.org.dexterity.bookanything.dom01geolocation.domain.events.LocalizablePlaceDeletedEvent
import de.org.dexterity.bookanything.dom01geolocation.infrastructure.adapters.output.persistence.elasticsearch.repositories.LocalizablePlaceElasticRepository
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.stereotype.Component

@Component
class LocalizablePlaceDeletedKafkaConsumer(
    private val localizablePlaceElasticRepository: LocalizablePlaceElasticRepository
) {

    @KafkaListener(topics = ["localizable-place-deleted-topic"], groupId = "elasticsearch-deleter")
    fun listen(event: LocalizablePlaceDeletedEvent) {
        localizablePlaceElasticRepository.deleteById(event.id)
        println("Localizable Place removed successfully from ElasticSearch: ${event.id}")
    }
}
