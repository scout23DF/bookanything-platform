package de.org.dexterity.bookanything.dom01geolocation.infrastructure.adapters.input.messaging.kafkaconsumers

import de.org.dexterity.bookanything.dom01geolocation.application.usecases.GeoJsonImporterUseCase
import de.org.dexterity.bookanything.dom01geolocation.domain.dtos.HierarchyDetailsRequest
import de.org.dexterity.bookanything.dom01geolocation.domain.events.CountryDataToMakeGeoLocationsEvent
import de.org.dexterity.bookanything.dom01geolocation.domain.models.GeoLocationType
import de.org.dexterity.bookanything.dom01geolocation.domain.models.IGeoLocationModel
import de.org.dexterity.bookanything.dom01geolocation.domain.ports.EventPublisherPort
import de.org.dexterity.bookanything.dom01geolocation.domain.ports.GeoJsonFeatureRepositoryPort
import de.org.dexterity.bookanything.dom01geolocation.domain.ports.GeoJsonImportedFileRepositoryPort
import org.slf4j.LoggerFactory
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
@Transactional
class GeoLocationsMakerFromGeoJsonImportedFileKafkaConsumer(
    private val geoJsonImporterUseCase: GeoJsonImporterUseCase,
    private val geoJsonImportedFileRepositoryPort: GeoJsonImportedFileRepositoryPort,
    private val geoJsonFeatureRepositoryPort: GeoJsonFeatureRepositoryPort,
    private val eventPublisher: EventPublisherPort
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    @KafkaListener(
        topics = ["geolocation.geojson-imported-file.ready-to-make-geo-locations"],
        groupId = "geolocation-maker-from-geojson-imported-processor"
    )
    fun listen(countryDataToMakeGeoLocationsEvent: CountryDataToMakeGeoLocationsEvent) {

        var messageToLog : String = "===> Event consumed: $countryDataToMakeGeoLocationsEvent"

        val geoJsonImportedFileId = countryDataToMakeGeoLocationsEvent.geoJsonImportedFileId

        val geoJsonImportedFileModel = geoJsonImportedFileRepositoryPort.findById(geoJsonImportedFileId)

        messageToLog += " :: GeoJsonImportedFile found: '${geoJsonImportedFileModel?.fileName}' - Features Count: '${geoJsonImportedFileModel?.featuresList?.size}'"

        logger.info(messageToLog)

        val geoLocationType : GeoLocationType = discoverGeoLocationTypeToCreate(
            countryDataToMakeGeoLocationsEvent.hierarchyDetailsRequest
        )

        geoJsonImportedFileModel?.featuresList?.forEach { oneFeature ->

            val newGeoLocationModel: IGeoLocationModel? = when (geoLocationType) {
                GeoLocationType.CONTINENT -> geoJsonImporterUseCase.handleContinentCreation(oneFeature, countryDataToMakeGeoLocationsEvent.hierarchyDetailsRequest)
                GeoLocationType.REGION    -> geoJsonImporterUseCase.handleRegionCreation(oneFeature, countryDataToMakeGeoLocationsEvent.hierarchyDetailsRequest)
                GeoLocationType.COUNTRY   -> geoJsonImporterUseCase.handleCountryCreation(oneFeature, countryDataToMakeGeoLocationsEvent.hierarchyDetailsRequest)
                GeoLocationType.PROVINCE  -> geoJsonImporterUseCase.handleProvinceCreation(oneFeature, countryDataToMakeGeoLocationsEvent.hierarchyDetailsRequest)
                GeoLocationType.CITY      -> geoJsonImporterUseCase.handleCityCreation(oneFeature, countryDataToMakeGeoLocationsEvent.hierarchyDetailsRequest)
                GeoLocationType.DISTRICT  -> geoJsonImporterUseCase.handleDistrictCreation(oneFeature, countryDataToMakeGeoLocationsEvent.hierarchyDetailsRequest)
            }

            if (newGeoLocationModel != null) {
                messageToLog = "==> GeoLocation created/updated from imported Feature: Type: ${newGeoLocationModel.type} | Name: ${newGeoLocationModel.name} | Alias: ${newGeoLocationModel.alias} | FriendlyId: ${newGeoLocationModel.friendlyId} | ParentId: ${newGeoLocationModel.parentId}"
            } else {
                messageToLog = "==> No new GeoLocation created/updated from imported Feature."
            }
            logger.info(messageToLog)

        } // foreach Feature

        messageToLog = "==> End of GeoLocations creation"
        logger.info(messageToLog)

    }

    private fun discoverGeoLocationTypeToCreate(
        hierarchyDetailsRequest: HierarchyDetailsRequest
    ): GeoLocationType {

        val hierarchyType = hierarchyDetailsRequest.hierarchyType

        val resultGeoLocationType = when (hierarchyType) {
            "PROVINCE" -> GeoLocationType.PROVINCE
            "CITY" -> GeoLocationType.CITY
            "DISTRICT" -> GeoLocationType.DISTRICT
            else -> {GeoLocationType.COUNTRY}
        }

        return resultGeoLocationType

    }

 }