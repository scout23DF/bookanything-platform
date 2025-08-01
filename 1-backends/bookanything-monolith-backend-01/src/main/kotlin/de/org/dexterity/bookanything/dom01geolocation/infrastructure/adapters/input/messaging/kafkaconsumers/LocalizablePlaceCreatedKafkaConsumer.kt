package de.org.dexterity.bookanything.dom01geolocation.infrastructure.adapters.input.messaging.kafkaconsumers

import de.org.dexterity.bookanything.dom01geolocation.domain.events.LocalizablePlaceCreatedEvent
import de.org.dexterity.bookanything.dom01geolocation.infrastructure.adapters.output.persistence.elasticsearch.entities.LocalizablePlaceElasticEntity
import de.org.dexterity.bookanything.dom01geolocation.infrastructure.adapters.output.persistence.elasticsearch.repositories.LocalizablePlaceElasticRepository
import org.slf4j.LoggerFactory
import org.springframework.data.elasticsearch.core.geo.GeoPoint
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.stereotype.Component

@Component
class LocalizablePlaceCreatedKafkaConsumer(
    private val localizablePlaceElasticRepository: LocalizablePlaceElasticRepository
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    @KafkaListener(topics = ["localizable-place-created-topic"], groupId = "elasticsearch-indexer")
    fun listen(event: LocalizablePlaceCreatedEvent) {
        logger.info("Received one Event about the creation of a new Localizable Place: id={}, nome={}", event.id, event.name)
        val elasticEntity = LocalizablePlaceElasticEntity(
            id = event.id,
            friendlyId = "friendly-id-from-event", // TODO: Adicionar friendlyId ao evento
            name = event.name,
            alias = event.alias,
            additionalDetailsMap = null, // TODO: Adicionar propertiesDetailsMap ao evento
            locationPoint = GeoPoint(event.latitude, event.longitude)
        )
        localizablePlaceElasticRepository.save(elasticEntity)
        logger.info("Localizable Place was indexed in ElasticSearch: id={}, nome={}", elasticEntity.id, elasticEntity.name)
    }
}