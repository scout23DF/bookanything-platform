package de.org.dexterity.bookanything.dom01geolocation.infrastructure.adapters.input.messaging.kafkaconsumers

import de.org.dexterity.bookanything.dom01geolocation.application.usecases.LocalizablePlaceCRUDUseCase
import de.org.dexterity.bookanything.dom01geolocation.application.usecases.GeoJsonFileManagerUseCase
import de.org.dexterity.bookanything.dom01geolocation.domain.dtos.GeoJsonUploadedFileDTO
import org.locationtech.jts.geom.GeometryFactory
import org.locationtech.jts.geom.PrecisionModel
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.stereotype.Component

@Component
class UploadedGeoJsonFileKafkaConsumer(
    private val geoJsonFileManagerUseCase: GeoJsonFileManagerUseCase,
    private val centroDistribuicaoCRUDUseCase: LocalizablePlaceCRUDUseCase
) {

    private val geometryFactory = GeometryFactory(PrecisionModel(), 4326)

    @KafkaListener(topics = ["geojson-upload-topic"], groupId = "geojson-processor")
    fun listen(geoJsonUploadedFileDTO: GeoJsonUploadedFileDTO) {

        geoJsonFileManagerUseCase.handleUploadedGeoJsonFileConsumedFromQueue(geoJsonUploadedFileDTO)

    }

}
