package de.org.dexterity.bookanything.dom01geolocation.infrastructure.adapters.output.messaging.kafkapublishers

import de.org.dexterity.bookanything.dom01geolocation.domain.dtos.GeoJsonUploadedFileDTO
import de.org.dexterity.bookanything.dom01geolocation.domain.ports.GeoJsonFilePublisherPort
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Component

@Component
class KafkaGeoJsonFilePublisherAdapter(
    private val kafkaTemplate: KafkaTemplate<String, GeoJsonUploadedFileDTO>
) : GeoJsonFilePublisherPort {

    override fun publish(geoJsonUploadedFileDTO: GeoJsonUploadedFileDTO) {

        this.kafkaTemplate.send("geojson-upload-topic", geoJsonUploadedFileDTO)

        kafkaTemplate.flush()

    }
}