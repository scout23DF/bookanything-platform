package de.org.dexterity.bookanything.dom01geolocation.infrastructure.adapters.input.messaging.kafkaconsumers

import de.org.dexterity.bookanything.dom01geolocation.application.usecases.GeoJsonFileManagerUseCase
import de.org.dexterity.bookanything.dom01geolocation.domain.dtos.GeoJsonUploadedFileDTO
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.stereotype.Component

@Component
class UploadedGeoJsonFileKafkaConsumer(
    private val geoJsonFileManagerUseCase: GeoJsonFileManagerUseCase
) {

    @KafkaListener(topics = ["geojson-upload-topic"], groupId = "geojson-processor")
    fun listen(geoJsonUploadedFileDTO: GeoJsonUploadedFileDTO) {

        geoJsonFileManagerUseCase.handleUploadedGeoJsonFileConsumedFromQueue(geoJsonUploadedFileDTO)

    }

}
