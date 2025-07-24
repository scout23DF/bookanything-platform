package de.org.dexterity.bookanything.dom02assetmanager.infrastructure.adapters.output.messaging.kafka

import de.org.dexterity.bookanything.dom02assetmanager.domain.events.AssetUploadedEvent
import de.org.dexterity.bookanything.dom02assetmanager.domain.ports.AssetEventPublisherPort
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Component

@Component
class AssetKafkaEventPublisherAdapter(
    private val kafkaTemplate: KafkaTemplate<String, Any>
) : AssetEventPublisherPort {

    override suspend fun publishAssetUploadedEvent(event: AssetUploadedEvent) {
        kafkaTemplate.send("asset-uploads", event)
    }
}
