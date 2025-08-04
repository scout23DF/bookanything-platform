package de.org.dexterity.bookanything.dom01geolocation.application.usecases

import com.fasterxml.jackson.databind.ObjectMapper
import de.org.dexterity.bookanything.dom01geolocation.domain.dtos.GeoJsonFeatureDto
import de.org.dexterity.bookanything.dom01geolocation.domain.dtos.HierarchyDetailsRequest
import de.org.dexterity.bookanything.dom01geolocation.domain.events.CountryDataToMakeGeoLocationsEvent
import de.org.dexterity.bookanything.dom01geolocation.domain.models.*
import de.org.dexterity.bookanything.dom01geolocation.domain.ports.*
import de.org.dexterity.bookanything.dom02assetmanager.domain.models.AssetModel
import de.org.dexterity.bookanything.dom02assetmanager.domain.ports.StorageProviderPort
import org.geojson.FeatureCollection
import org.locationtech.jts.io.WKTReader
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
@Transactional
class GeoJsonImporterUseCase(
    private val geoJsonImportedFileRepositoryPort: GeoJsonImportedFileRepositoryPort,
    private val storageProvider: StorageProviderPort,
    private val objectMapper: ObjectMapper,
    private val eventPublisher: EventPublisherPort,
    private val countryRepository: ICountryRepositoryPort,
    private val regionRepository: IRegionRepositoryPort,
    private val provinceRepository: IProvinceRepositoryPort,
    private val cityRepository: ICityRepositoryPort
) {

    private val logger = LoggerFactory.getLogger(this::class.java)
    private val wktReader = WKTReader()

    @Transactional
    suspend fun execute(
        assetFromEvent: AssetModel,
        targetCountryCode: String,
        hierarchyDetailsRequest: HierarchyDetailsRequest
    ) {

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
                countryIso3Code = targetCountryCode,
                hierarchyDetailsRequest = hierarchyDetailsRequest
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

    fun handleContinentCreation(
        geoJsonFeatureModel: GeoJsonFeatureModel,
        hierarchyDetailsRequest: HierarchyDetailsRequest
    ): ContinentModel? {
        return null
    }

    fun handleRegionCreation(
        geoJsonFeatureModel: GeoJsonFeatureModel,
        hierarchyDetailsRequest: HierarchyDetailsRequest,
    ): RegionModel? {
        return null
    }

    fun handleCountryCreation(
        geoJsonFeatureModel: GeoJsonFeatureModel,
        hierarchyDetailsRequest: HierarchyDetailsRequest
    ): CountryModel? {

        val foundCountryModel = countryRepository.findByFriendlyIdContainingIgnoreCase(
            geoJsonFeatureModel.featurePropertiesMap[hierarchyDetailsRequest.propertyForSearchIfExists] as String
        ).firstOrNull()

        val parentRegionModel = regionRepository.findByFriendlyIdContainingIgnoreCase(
            hierarchyDetailsRequest.parentAliasToAttach
        ).firstOrNull()

        if (foundCountryModel == null) {

            parentRegionModel?.let {

                val newCountryModel = CountryModel(
                    id = GeoLocationId(-1),
                    name = geoJsonFeatureModel.featurePropertiesMap[hierarchyDetailsRequest.propertyForFieldNameData] as String,
                    friendlyId = geoJsonFeatureModel.featurePropertiesMap[hierarchyDetailsRequest.propertyForFieldFriendlyIdData] as String,
                    alias = geoJsonFeatureModel.featurePropertiesMap[hierarchyDetailsRequest.propertyForFieldAliasData] as String?,
                    additionalDetailsMap = geoJsonFeatureModel.featurePropertiesMap,
                    boundaryRepresentation = geoJsonFeatureModel.featureGeometry?.let { wktReader.read(it.toText()) },
                    parentId = it.id.id,
                    region = it,
                    provincesList = null
                )

                return countryRepository.saveNew(newCountryModel)

            }

        } else if (hierarchyDetailsRequest.forceReimportIfExists) {

            parentRegionModel?.let {

                val updateCountryModel = foundCountryModel.copy(
                    name = geoJsonFeatureModel.featurePropertiesMap[hierarchyDetailsRequest.propertyForFieldNameData] as String,
                    friendlyId = geoJsonFeatureModel.featurePropertiesMap[hierarchyDetailsRequest.propertyForFieldFriendlyIdData] as String,
                    alias = geoJsonFeatureModel.featurePropertiesMap[hierarchyDetailsRequest.propertyForFieldAliasData] as String?,
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

    fun handleProvinceCreation(
        geoJsonFeatureModel: GeoJsonFeatureModel,
        hierarchyDetailsRequest: HierarchyDetailsRequest
    ): ProvinceModel? {

        val foundProvinceModel = provinceRepository.findByPropertiesDetailsMapContains(
            hierarchyDetailsRequest.propertyForSearchIfExists as String,
            geoJsonFeatureModel.featurePropertiesMap[hierarchyDetailsRequest.propertyForSearchIfExists] as String
        ).firstOrNull()

        val parentCountryModel = countryRepository.findByFriendlyIdContainingIgnoreCase(
            geoJsonFeatureModel.featurePropertiesMap[hierarchyDetailsRequest.propertyForParentSearch] as String
        ).firstOrNull()

        if (foundProvinceModel == null) {

            parentCountryModel?.let {

                val newProvinceModel = ProvinceModel(
                    id = GeoLocationId(-1),
                    name = geoJsonFeatureModel.featurePropertiesMap[hierarchyDetailsRequest.propertyForFieldNameData] as String,
                    friendlyId = geoJsonFeatureModel.featurePropertiesMap[hierarchyDetailsRequest.propertyForFieldFriendlyIdData] as String,
                    alias = geoJsonFeatureModel.featurePropertiesMap[hierarchyDetailsRequest.propertyForFieldAliasData] as String?,
                    additionalDetailsMap = geoJsonFeatureModel.featurePropertiesMap,
                    boundaryRepresentation = geoJsonFeatureModel.featureGeometry?.let { wktReader.read(it.toText()) },
                    parentId = it.id.id,
                    country = it,
                    citiesList = null
                )

                return provinceRepository.saveNew(newProvinceModel)

            }

        } else if (hierarchyDetailsRequest.forceReimportIfExists) {

            parentCountryModel?.let {

                val updateProvinceModel = foundProvinceModel.copy(
                    name = geoJsonFeatureModel.featurePropertiesMap[hierarchyDetailsRequest.propertyForFieldNameData] as String,
                    friendlyId = geoJsonFeatureModel.featurePropertiesMap[hierarchyDetailsRequest.propertyForFieldFriendlyIdData] as String,
                    alias = geoJsonFeatureModel.featurePropertiesMap[hierarchyDetailsRequest.propertyForFieldAliasData] as String?,
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

    fun handleCityCreation(
        geoJsonFeatureModel: GeoJsonFeatureModel,
        hierarchyDetailsRequest: HierarchyDetailsRequest
    ): CityModel? {

        try {

            val foundCityModel = cityRepository.findByPropertiesDetailsMapContains(
                hierarchyDetailsRequest.propertyForSearchIfExists as String,
                geoJsonFeatureModel.featurePropertiesMap[hierarchyDetailsRequest.propertyForSearchIfExists] as String
            ).firstOrNull()

            val parentProvinceModel = provinceRepository.findByFriendlyIdContainingIgnoreCase(
                geoJsonFeatureModel.featurePropertiesMap[hierarchyDetailsRequest.propertyForParentSearch] as String
            ).firstOrNull()

            if (foundCityModel == null) {

                parentProvinceModel?.let { it ->

                    val newCityModel = CityModel(
                        id = GeoLocationId(-1),
                        name = geoJsonFeatureModel.featurePropertiesMap[hierarchyDetailsRequest.propertyForFieldNameData] as String,
                        friendlyId = geoJsonFeatureModel.featurePropertiesMap[hierarchyDetailsRequest.propertyForFieldFriendlyIdData] as String,
                        alias = geoJsonFeatureModel.featurePropertiesMap[hierarchyDetailsRequest.propertyForFieldAliasData] as String?,
                        additionalDetailsMap = geoJsonFeatureModel.featurePropertiesMap,
                        boundaryRepresentation = geoJsonFeatureModel.featureGeometry?.let { wktReader.read(it.toText()) },
                        parentId = it.id.id,
                        province = it,
                        districtsList = null
                    )

                    return cityRepository.saveNew(newCityModel)

                }

            } else if (hierarchyDetailsRequest.forceReimportIfExists) {

                parentProvinceModel?.let {

                    val updateCityModel = foundCityModel.copy(
                        name = geoJsonFeatureModel.featurePropertiesMap[hierarchyDetailsRequest.propertyForFieldNameData] as String,
                        friendlyId = geoJsonFeatureModel.featurePropertiesMap[hierarchyDetailsRequest.propertyForFieldFriendlyIdData] as String,
                        alias = geoJsonFeatureModel.featurePropertiesMap[hierarchyDetailsRequest.propertyForFieldAliasData] as String?,
                        additionalDetailsMap = geoJsonFeatureModel.featurePropertiesMap,
                        boundaryRepresentation = geoJsonFeatureModel.featureGeometry?.let { wktReader.read(it.toText()) },
                        parentId = it.id.id,
                        province = it
                    )

                    return cityRepository.update(updateCityModel)
                }

            }

            return foundCityModel

        } catch (ex: Exception) {
            logger.error("===> Error while creating City from GeoJsonFeature: [${geoJsonFeatureModel.id} | ${geoJsonFeatureModel.geoJsonImportedFile.id} | ${geoJsonFeatureModel.featurePropertiesMap}] - \n - Exception: ${ex.message}")
            return null
        }

    }

    fun handleDistrictCreation(
        geoJsonFeatureModel: GeoJsonFeatureModel,
        hierarchyDetailsRequest: HierarchyDetailsRequest
    ): DistrictModel? {
        return null
    }

    fun createCityFromGeoJsonFeature(
        geoJsonFeatureModel: GeoJsonFeatureModel,
        hierarchyDetailsRequest: HierarchyDetailsRequest
    ): CityModel? {

        val foundCityModel = cityRepository.findByPropertiesDetailsMapContains(
            hierarchyDetailsRequest.propertyForSearchIfExists as String,
            geoJsonFeatureModel.featurePropertiesMap[hierarchyDetailsRequest.propertyForSearchIfExists] as String
        ).firstOrNull()

        val parentProvinceModel = provinceRepository.findByFriendlyIdContainingIgnoreCase(
            geoJsonFeatureModel.featurePropertiesMap[hierarchyDetailsRequest.propertyForParentSearch] as String
        ).firstOrNull()

        if (foundCityModel == null) {

            parentProvinceModel?.let { it ->

                val newCityModel = CityModel(
                    id = GeoLocationId(-1),
                    name = geoJsonFeatureModel.featurePropertiesMap[hierarchyDetailsRequest.propertyForFieldNameData] as String,
                    friendlyId = geoJsonFeatureModel.featurePropertiesMap[hierarchyDetailsRequest.propertyForFieldFriendlyIdData] as String,
                    alias = geoJsonFeatureModel.featurePropertiesMap[hierarchyDetailsRequest.propertyForFieldAliasData] as String?,
                    additionalDetailsMap = geoJsonFeatureModel.featurePropertiesMap,
                    boundaryRepresentation = geoJsonFeatureModel.featureGeometry?.let { wktReader.read(it.toText()) },
                    parentId = it.id.id,
                    province = it,
                    districtsList = null
                )

                return cityRepository.saveNew(newCityModel)

            }

        } else if (hierarchyDetailsRequest.forceReimportIfExists) {

            parentProvinceModel?.let {

                val updateCityModel = foundCityModel.copy(
                    name = geoJsonFeatureModel.featurePropertiesMap[hierarchyDetailsRequest.propertyForFieldNameData] as String,
                    friendlyId = geoJsonFeatureModel.featurePropertiesMap[hierarchyDetailsRequest.propertyForFieldFriendlyIdData] as String,
                    alias = geoJsonFeatureModel.featurePropertiesMap[hierarchyDetailsRequest.propertyForFieldAliasData] as String?,
                    additionalDetailsMap = geoJsonFeatureModel.featurePropertiesMap,
                    boundaryRepresentation = geoJsonFeatureModel.featureGeometry?.let { wktReader.read(it.toText()) },
                    parentId = it.id.id,
                    province = it
                )

                return cityRepository.update(updateCityModel)
            }

        }

        return foundCityModel

    }

}
