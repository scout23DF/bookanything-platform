package de.org.dexterity.bookanything.dom01geolocation.infrastructure.adapters.input.messaging.kafkaconsumers

import de.org.dexterity.bookanything.dom01geolocation.domain.events.CountryDataToMakeGeoLocationsEvent
import de.org.dexterity.bookanything.dom01geolocation.domain.models.*
import de.org.dexterity.bookanything.dom01geolocation.domain.ports.*
import org.locationtech.jts.io.WKTReader
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
@Transactional
class GeoLocationsMakerFromGeoJsonImportedFileKafkaConsumer(
    private val continentRepository: IContinentRepositoryPort,
    private val countryRepository: ICountryRepositoryPort,
    private val regionRepository: IRegionRepositoryPort,
    private val provinceRepository: IProvinceRepositoryPort,
    private val cityRepository: ICityRepositoryPort,
    private val districtRepository: IDistrictRepositoryPort,
    @Value("\${topics.geolocation.geojson-imported-file.ready-to-make-geo-locations}") private val geoJsonImportedFileReadyToMakeGeoLocationsTopic: String,
    private val geoJsonImportedFileRepositoryPort: GeoJsonImportedFileRepositoryPort
) {

    private val logger = LoggerFactory.getLogger(javaClass)
    private val wktReader = WKTReader()

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

        val geoLocationType : GeoLocationType = discoverGeoLocationTypeToCreate(geoJsonImportedFileModel)

        geoJsonImportedFileModel?.featuresList?.forEach { oneFeature ->

            val newGeoLocationModel: IGeoLocationModel? = when (geoLocationType) {
                GeoLocationType.CONTINENT -> handleContinentCreation(oneFeature, countryDataToMakeGeoLocationsEvent.parentAliasToAttach, countryDataToMakeGeoLocationsEvent.forceReimportIfExists)
                GeoLocationType.REGION    -> handleRegionCreation(oneFeature, countryDataToMakeGeoLocationsEvent.parentAliasToAttach, countryDataToMakeGeoLocationsEvent.forceReimportIfExists)
                GeoLocationType.COUNTRY   -> handleCountryCreation(oneFeature, countryDataToMakeGeoLocationsEvent.parentAliasToAttach, countryDataToMakeGeoLocationsEvent.forceReimportIfExists)
                GeoLocationType.PROVINCE  -> handleProvinceCreation(oneFeature, countryDataToMakeGeoLocationsEvent.parentAliasToAttach, countryDataToMakeGeoLocationsEvent.forceReimportIfExists)
                GeoLocationType.CITY      -> handleCityCreation(oneFeature, countryDataToMakeGeoLocationsEvent.parentAliasToAttach, countryDataToMakeGeoLocationsEvent.forceReimportIfExists)
                GeoLocationType.DISTRICT  -> handleDistrictCreation(oneFeature, countryDataToMakeGeoLocationsEvent.parentAliasToAttach, countryDataToMakeGeoLocationsEvent.forceReimportIfExists)
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

    private fun discoverGeoLocationTypeToCreate(geoJsonImportedFileModel: GeoJsonImportedFileModel?): GeoLocationType {

        val levelOfImportedFile = geoJsonImportedFileModel?.sourceStoredAsset?.metadataMap["level"] as String ?: "0"

        val resultGeoLocationType = when {
            levelOfImportedFile.toInt() == 0 -> GeoLocationType.COUNTRY
            levelOfImportedFile.toInt() == 1 -> GeoLocationType.PROVINCE
            levelOfImportedFile.toInt() == 2 -> GeoLocationType.CITY
            levelOfImportedFile.toInt() == 3 -> GeoLocationType.DISTRICT
            levelOfImportedFile.toInt() == 4 -> GeoLocationType.DISTRICT
            else -> {GeoLocationType.CONTINENT}
        }

        return resultGeoLocationType

    }

    private fun handleContinentCreation(
        geoJsonFeatureModel: GeoJsonFeatureModel,
        parentAliasToAttach: String,
        forceReimportIfExists: Boolean
    ): ContinentModel? {
        return null
    }

    private fun handleRegionCreation(
        geoJsonFeatureModel: GeoJsonFeatureModel,
        parentAliasToAttach: String,
        forceReimportIfExists: Boolean
    ): RegionModel? {
        return null
    }

    private fun handleCountryCreation(
        geoJsonFeatureModel: GeoJsonFeatureModel,
        parentAliasToAttach: String,
        forceReimportIfExists: Boolean
    ): CountryModel? {

        val foundCountryModel = countryRepository.findByFriendlyIdContainingIgnoreCase(
            geoJsonFeatureModel.featurePropertiesMap["COUNTRY"] as String
        ).firstOrNull()

        val parentRegionModel = regionRepository.findByFriendlyIdContainingIgnoreCase(parentAliasToAttach).firstOrNull()

        if (foundCountryModel == null) {

            parentRegionModel?.let {

                val newCountryModel = CountryModel(
                    id = GeoLocationId(-1),
                    name = geoJsonFeatureModel.featurePropertiesMap["COUNTRY"] as String,
                    friendlyId = geoJsonFeatureModel.featurePropertiesMap["COUNTRY"] as String,
                    alias = geoJsonFeatureModel.featurePropertiesMap["GID_0"] as String?,
                    additionalDetailsMap = geoJsonFeatureModel.featurePropertiesMap,
                    boundaryRepresentation = geoJsonFeatureModel.featureGeometry?.let { wktReader.read(it.toText()) },
                    parentId = it.id.id,
                    region = it,
                    provincesList = null
                )

                return countryRepository.saveNew(newCountryModel)

            }

        } else if (forceReimportIfExists) {

            parentRegionModel?.let {

                val updateCountryModel = foundCountryModel.copy(
                    name = geoJsonFeatureModel.featurePropertiesMap["COUNTRY"] as String,
                    friendlyId = geoJsonFeatureModel.featurePropertiesMap["COUNTRY"] as String,
                    alias = geoJsonFeatureModel.featurePropertiesMap["GID_0"] as String?,
                    additionalDetailsMap = geoJsonFeatureModel.featurePropertiesMap,
                    boundaryRepresentation = geoJsonFeatureModel.featureGeometry?.let { wktReader.read(it.toText()) },
                    parentId = it.id.id,
                    region = it
                )

                return countryRepository.update(updateCountryModel)
           }

        }

        return foundCountryModel

    }

    private fun handleProvinceCreation(
        geoJsonFeatureModel: GeoJsonFeatureModel,
        parentAliasToAttach: String,
        forceReimportIfExists: Boolean
    ): ProvinceModel? {

        val foundProvinceModel = provinceRepository.findByPropertiesDetailsMapContains(
            "HASC_1",
            geoJsonFeatureModel.featurePropertiesMap["HASC_1"] as String
        ).firstOrNull()

        val parentCountryModel = countryRepository.findByFriendlyIdContainingIgnoreCase(
            geoJsonFeatureModel.featurePropertiesMap["COUNTRY"] as String
        ).firstOrNull()

        if (foundProvinceModel == null) {

            parentCountryModel?.let {

                val newProvinceModel = ProvinceModel(
                    id = GeoLocationId(-1),
                    name = geoJsonFeatureModel.featurePropertiesMap["NAME_1"] as String,
                    friendlyId = geoJsonFeatureModel.featurePropertiesMap["NAME_1"] as String,
                    alias = geoJsonFeatureModel.featurePropertiesMap["HASC_1"] as String?,
                    additionalDetailsMap = geoJsonFeatureModel.featurePropertiesMap,
                    boundaryRepresentation = geoJsonFeatureModel.featureGeometry?.let { wktReader.read(it.toText()) },
                    parentId = it.id.id,
                    country = it,
                    citiesList = null
                )

                return provinceRepository.saveNew(newProvinceModel)

            }

        } else if (forceReimportIfExists) {

            parentCountryModel?.let {

                val updateProvinceModel = foundProvinceModel.copy(
                    name = geoJsonFeatureModel.featurePropertiesMap["NAME_1"] as String,
                    friendlyId = geoJsonFeatureModel.featurePropertiesMap["NAME_1"] as String,
                    alias = geoJsonFeatureModel.featurePropertiesMap["HASC_1"] as String?,
                    additionalDetailsMap = geoJsonFeatureModel.featurePropertiesMap,
                    boundaryRepresentation = geoJsonFeatureModel.featureGeometry?.let { wktReader.read(it.toText()) },
                    parentId = it.id.id,
                    country = it
                )

                return provinceRepository.update(updateProvinceModel)
            }

        }

        return foundProvinceModel

    }

    private fun handleCityCreation(
        geoJsonFeatureModel: GeoJsonFeatureModel,
        parentAliasToAttach: String,
        forceReimportIfExists: Boolean
    ): CityModel? {
        return null
    }

    private fun handleDistrictCreation(
        geoJsonFeatureModel: GeoJsonFeatureModel,
        parentAliasToAttach: String,
        forceReimportIfExists: Boolean
    ): DistrictModel? {
        return null
    }

}