package de.org.dexterity.bookanything.dom01geolocation.application.usecases

import com.fasterxml.jackson.databind.ObjectMapper
import de.org.dexterity.bookanything.dom01geolocation.domain.dtos.GeoJsonFeatureDto
import de.org.dexterity.bookanything.dom01geolocation.domain.events.CountryDataToMakeGeoLocationsEvent
import de.org.dexterity.bookanything.dom01geolocation.domain.models.GeoJsonFeatureModel
import de.org.dexterity.bookanything.dom01geolocation.domain.models.GeoJsonImportStatus
import de.org.dexterity.bookanything.dom01geolocation.domain.models.GeoJsonImportedFileModel
import de.org.dexterity.bookanything.dom01geolocation.domain.ports.EventPublisherPort
import de.org.dexterity.bookanything.dom01geolocation.domain.ports.GeoJsonImportedFileRepositoryPort
import de.org.dexterity.bookanything.dom02assetmanager.domain.models.AssetModel
import de.org.dexterity.bookanything.dom02assetmanager.domain.ports.StorageProviderPort
import org.geojson.FeatureCollection
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.time.Instant
import java.util.*

@Service
class GeoJsonImporterUseCase(
    private val geoJsonImportedFileRepositoryPort: GeoJsonImportedFileRepositoryPort,
    private val storageProvider: StorageProviderPort,
    private val objectMapper: ObjectMapper,
    private val eventPublisher: EventPublisherPort
) {

    private val logger = LoggerFactory.getLogger(this::class.java)

    @Transactional
    suspend fun execute(assetFromEvent: AssetModel, parentAliasToAttach: String, forceReimportIfExists: Boolean) {

        logger.info("Executing GeoJSON import for asset: {}", assetFromEvent.id)

        val geoJsonImportedFileModel = GeoJsonImportedFileModel(
            id = UUID.randomUUID(),
            fileName = assetFromEvent.fileName,
            originalContentType = assetFromEvent.mimeType,
            importTimestamp = Instant.now(),
            status = GeoJsonImportStatus.PROCESSING,
            sourceStoredAsset = assetFromEvent
        )
        geoJsonImportedFileRepositoryPort.save(geoJsonImportedFileModel)

        try {
            val fileContentOutputStream: OutputStream = ByteArrayOutputStream()
            storageProvider.download(
                assetFromEvent.bucket.name,
                assetFromEvent.storageKey
            ).body?.writeTo(fileContentOutputStream)

            val fileContentInputStream: InputStream = ByteArrayInputStream((fileContentOutputStream as ByteArrayOutputStream).toByteArray())

            val featureCollection = objectMapper.readValue(fileContentInputStream, FeatureCollection::class.java)

            geoJsonImportedFileModel.featuresList = featureCollection.map { oneGeoJsonFeature ->

                val featureJson = objectMapper.writeValueAsString(oneGeoJsonFeature)
                val featureDto = objectMapper.readValue(featureJson, GeoJsonFeatureDto::class.java)

                val newGeoJsonFeatureModel = GeoJsonFeatureModel(
                    id = UUID.randomUUID(),
                    geoJsonImportedFile = geoJsonImportedFileModel,
                    featureGeometry = featureDto.geometry,
                    featurePropertiesMap = featureDto.properties
                )

                newGeoJsonFeatureModel

            }.toMutableList()

            geoJsonImportedFileModel.status = GeoJsonImportStatus.COMPLETED
            val savedFileModel = geoJsonImportedFileRepositoryPort.save(geoJsonImportedFileModel)

            // geoJsonFeatureRepositoryPort.synchronizeFeatureGeometryDataByImportedFileId(savedFileModel.id)

            logger.info("Successfully imported GeoJSON file: {}", assetFromEvent.fileName)

            val countryDataToMakeGeoLocationsEvent : CountryDataToMakeGeoLocationsEvent = CountryDataToMakeGeoLocationsEvent(
                geoJsonImportedFileId = savedFileModel.id,
                parentAliasToAttach = parentAliasToAttach,
                forceReimportIfExists = forceReimportIfExists
            )
            eventPublisher.publish(countryDataToMakeGeoLocationsEvent)

            logger.info("Event published successfully :: countryDataToMakeGeoLocationsEvent = {}", countryDataToMakeGeoLocationsEvent)

        } catch (e: Exception) {
            logger.error("Failed to import GeoJSON file: ${assetFromEvent.fileName}", e)
            geoJsonImportedFileModel.status = GeoJsonImportStatus.FAILED
            geoJsonImportedFileModel.statusDetails = e.message?.take(2000) // Truncate to avoid oversized error messages
            geoJsonImportedFileRepositoryPort.save(geoJsonImportedFileModel)
        }
    }

}
