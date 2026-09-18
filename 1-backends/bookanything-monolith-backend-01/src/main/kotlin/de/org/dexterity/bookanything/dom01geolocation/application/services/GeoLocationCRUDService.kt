package de.org.dexterity.bookanything.dom01geolocation.application.services

import de.org.dexterity.bookanything.dom01geolocation.application.usecases.*
import de.org.dexterity.bookanything.dom01geolocation.domain.events.GeoLocationEnrichmentEvent
import de.org.dexterity.bookanything.dom01geolocation.domain.models.*
import de.org.dexterity.bookanything.dom01geolocation.domain.ports.EventPublisherPort
import de.org.dexterity.bookanything.dom01geolocation.infrastructure.adapters.input.web.dtos.CreateGeoLocationRequest
import de.org.dexterity.bookanything.dom01geolocation.infrastructure.adapters.input.web.dtos.UpdateGeoLocationRequest
import de.org.dexterity.bookanything.dom01geolocation.infrastructure.adapters.input.web.mappers.GeoLocationRestMapper
import org.locationtech.jts.io.WKTReader
import org.springframework.beans.factory.annotation.Value
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import java.util.Optional

@Service
class GeoLocationCRUDService(
    private val continentUseCase: ContinentUseCase,
    private val regionUseCase: RegionUseCase,
    private val countryUseCase: CountryUseCase,
    private val provinceUseCase: ProvinceUseCase,
    private val cityUseCase: CityUseCase,
    private val districtUseCase: DistrictUseCase,
    private val geoLocationRestMapper: GeoLocationRestMapper,
    private val eventPublisherPort: EventPublisherPort
) {

    @Value("\${application.domain-settings.geolocation.update-boundary-via-ia.feature-enabled}")
    private val shouldUpdateBoundaryViaIA: Boolean = true

    private val useCaseMap: Map<GeoLocationType, IGeoLocationUseCase<out IGeoLocationModel>> = mapOf(
        GeoLocationType.CONTINENT to continentUseCase,
        GeoLocationType.REGION to regionUseCase,
        GeoLocationType.COUNTRY to countryUseCase,
        GeoLocationType.PROVINCE to provinceUseCase,
        GeoLocationType.CITY to cityUseCase,
        GeoLocationType.DISTRICT to districtUseCase
    )

    @Suppress("UNCHECKED_CAST")
    private fun <T : IGeoLocationModel> getUseCase(type: GeoLocationType): IGeoLocationUseCase<T> {
        return useCaseMap[type] as? IGeoLocationUseCase<T>
            ?: throw IllegalArgumentException("Unsupported GeoLocationType: $type")
    }

    fun create(type: GeoLocationType, request: CreateGeoLocationRequest): IGeoLocationModel {
        val useCase = getUseCase<IGeoLocationModel>(type)
        val parent: IGeoLocationModel? = request.parentId?.let {
            when (type) {
                GeoLocationType.REGION -> continentUseCase.findById(GeoLocationId(it)).orElseThrow { IllegalArgumentException("Continent not found for ID: $it") }
                GeoLocationType.COUNTRY -> regionUseCase.findById(GeoLocationId(it)).orElseThrow { IllegalArgumentException("Region not found for ID: $it") }
                GeoLocationType.PROVINCE -> countryUseCase.findById(GeoLocationId(it)).orElseThrow { IllegalArgumentException("Country not found for ID: $it") }
                GeoLocationType.CITY -> provinceUseCase.findById(GeoLocationId(it)).orElseThrow { IllegalArgumentException("Province not found for ID: $it") }
                GeoLocationType.DISTRICT -> cityUseCase.findById(GeoLocationId(it)).orElseThrow { IllegalArgumentException("City not found for ID: $it") }
                else -> null
            }
        }
        var savedModel : IGeoLocationModel = geoLocationRestMapper.fromCreateGeoLocationRequestToModel(type, request, parent)
        savedModel = useCase.create(savedModel)

        // Always publish event for downstream processing (artifacts and reports)
        eventPublisherPort.publish(GeoLocationEnrichmentEvent(savedModel.id.id, savedModel.type))

        return savedModel
    }

    fun findById(type: GeoLocationType, id: Long): Optional<out IGeoLocationModel> {
        val useCase = getUseCase<IGeoLocationModel>(type)
        return useCase.findById(GeoLocationId(id))
    }

    fun findAll(type: GeoLocationType, pageable: Pageable): Page<IGeoLocationModel> {
        val useCase = getUseCase<IGeoLocationModel>(type)
        return useCase.findAll(pageable)
    }

    fun update(type: GeoLocationType, id: Long, request: UpdateGeoLocationRequest): IGeoLocationModel? {
        val useCase = getUseCase<IGeoLocationModel>(type)
        val existingModel = useCase.findById(GeoLocationId(id)).orElse(null) ?: return null

        val boundary = request.boundaryRepresentation?.let {
            try {
                val trimmed = it.trim()
                if (trimmed.startsWith("{")) {
                    org.locationtech.jts.io.geojson.GeoJsonReader().read(trimmed)
                } else {
                    WKTReader().read(trimmed)
                }
            } catch (e: Exception) {
                WKTReader().read(it)
            }
        } ?: existingModel.boundaryRepresentation

        var updatedModel : IGeoLocationModel = when (type) {
            GeoLocationType.CONTINENT -> (existingModel as ContinentModel).copy(name = request.name, friendlyId = request.friendlyId, boundaryRepresentation = boundary)
            GeoLocationType.REGION -> (existingModel as RegionModel).copy(name = request.name, friendlyId = request.friendlyId, boundaryRepresentation = boundary)
            GeoLocationType.COUNTRY -> (existingModel as CountryModel).copy(name = request.name, friendlyId = request.friendlyId, boundaryRepresentation = boundary)
            GeoLocationType.PROVINCE -> (existingModel as ProvinceModel).copy(name = request.name, friendlyId = request.friendlyId, boundaryRepresentation = boundary)
            GeoLocationType.CITY -> (existingModel as CityModel).copy(name = request.name, friendlyId = request.friendlyId, boundaryRepresentation = boundary)
            GeoLocationType.DISTRICT -> (existingModel as DistrictModel).copy(name = request.name, friendlyId = request.friendlyId, boundaryRepresentation = boundary)
        }

        updatedModel = useCase.update(updatedModel)!!

        // Always publish event for downstream processing (artifacts and reports)
        eventPublisherPort.publish(GeoLocationEnrichmentEvent(updatedModel.id.id, updatedModel.type))

        return updatedModel
    }

    fun updateAdditionalDetails(type: GeoLocationType, id: Long, details: Map<String, Any?>): IGeoLocationModel? {
        val useCase = getUseCase<IGeoLocationModel>(type)
        val existingModel = useCase.findById(GeoLocationId(id)).orElse(null) ?: return null

        val updatedModel: IGeoLocationModel = when (type) {
            GeoLocationType.CONTINENT -> (existingModel as ContinentModel).copy(additionalDetailsMap = details)
            GeoLocationType.REGION -> (existingModel as RegionModel).copy(additionalDetailsMap = details)
            GeoLocationType.COUNTRY -> (existingModel as CountryModel).copy(additionalDetailsMap = details)
            GeoLocationType.PROVINCE -> (existingModel as ProvinceModel).copy(additionalDetailsMap = details)
            GeoLocationType.CITY -> (existingModel as CityModel).copy(additionalDetailsMap = details)
            GeoLocationType.DISTRICT -> (existingModel as DistrictModel).copy(additionalDetailsMap = details)
        }

        return useCase.update(updatedModel)
    }

    fun deleteById(type: GeoLocationType, id: Long) {
        val useCase = getUseCase<IGeoLocationModel>(type)
        useCase.deleteById(GeoLocationId(id))
    }

    fun deleteAll(geoLocationType: GeoLocationType) {
        val useCase = getUseCase<IGeoLocationModel>(geoLocationType)
        useCase.deleteAll()
    }

    fun deleteByParentId(type: GeoLocationType, parentId: Long) {
        val useCase = getUseCase<IGeoLocationModel>(type)
        useCase.deleteByParentId(GeoLocationId(parentId))
    }

    fun searchByParentIdAndNameStartingWith(type: GeoLocationType, parentId: Long?, namePrefix: String, pageable: Pageable): Page<IGeoLocationModel> {
        val useCase = getUseCase<IGeoLocationModel>(type)
        val parentGeoLocationId = parentId?.let { GeoLocationId(it) }
        return useCase.findByParentIdAndNameStartingWith(parentGeoLocationId, namePrefix, pageable)
    }

    fun searchByParentIdAndAliasStartingWith(type: GeoLocationType, parentId: Long?, aliasPrefix: String, pageable: Pageable): Page<IGeoLocationModel> {
        val useCase = getUseCase<IGeoLocationModel>(type)
        val parentGeoLocationId = parentId?.let { GeoLocationId(it) }
        return useCase.findByParentIdAndAliasStartingWith(parentGeoLocationId, aliasPrefix, pageable)
    }

    fun findDeepGeoLocation(type: GeoLocationType, id: Long?, name: String?): IGeoLocationModel? {
        val useCase = getUseCase<IGeoLocationModel>(type)
        return when {
            id != null -> useCase.findDeepById(GeoLocationId(id)).orElse(null)
            name != null -> useCase.findDeepByName(name).orElse(null)
            else -> throw IllegalArgumentException("Either ID or name must be provided for deep search.")
        }
    }

    fun findByFriendlyId(type: GeoLocationType, friendlyId: String, pageable: Pageable): Page<IGeoLocationModel> {
        val useCase = getUseCase<IGeoLocationModel>(type)
        return useCase.findByFriendlyIdContaining(friendlyId, pageable)
    }

    fun findByPropertiesDetailsMap(type: GeoLocationType, key: String, value: String, pageable: Pageable): Page<IGeoLocationModel> {
        val useCase = getUseCase<IGeoLocationModel>(type)
        return useCase.findByPropertiesDetailsMapContains(key, value, pageable)
    }

    /**
     * Attempts to find a GeoLocation by ID across all hierarchy levels (Continent, Region, Country, Province, City, District).
     */
    fun findAnyById(id: Long): IGeoLocationModel? {
        for (type in GeoLocationType.entries) {
            val found = findById(type, id)
            if (found.isPresent) {
                return found.get()
            }
        }
        return null
    }

    /**
     * Resolves the parent GeoLocation model for a given GeoLocation if parentId exists.
     */
    fun findParentModel(model: IGeoLocationModel): IGeoLocationModel? {
        val pId = model.parentId ?: return null
        val parentType = when (model.type) {
            GeoLocationType.REGION -> GeoLocationType.CONTINENT
            GeoLocationType.COUNTRY -> GeoLocationType.REGION
            GeoLocationType.PROVINCE -> GeoLocationType.COUNTRY
            GeoLocationType.CITY -> GeoLocationType.PROVINCE
            GeoLocationType.DISTRICT -> GeoLocationType.CITY
            GeoLocationType.CONTINENT -> null
        } ?: return null
        return findById(parentType, pId).orElse(null)
    }

}