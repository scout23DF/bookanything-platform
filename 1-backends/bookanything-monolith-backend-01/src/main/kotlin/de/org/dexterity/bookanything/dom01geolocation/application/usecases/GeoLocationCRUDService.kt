package de.org.dexterity.bookanything.dom01geolocation.application.usecases

import de.org.dexterity.bookanything.dom01geolocation.domain.models.*
import de.org.dexterity.bookanything.dom01geolocation.infrastructure.adapters.input.web.dtos.CreateGeoLocationRequest
import de.org.dexterity.bookanything.dom01geolocation.infrastructure.adapters.input.web.dtos.UpdateGeoLocationRequest
import de.org.dexterity.bookanything.dom01geolocation.infrastructure.adapters.input.web.dtos.toModel
import org.locationtech.jts.io.WKTReader
import org.springframework.stereotype.Service
import java.util.Optional

@Service
class GeoLocationCRUDService(
    private val continentUseCase: ContinentUseCase,
    private val regionUseCase: RegionUseCase,
    private val countryUseCase: CountryUseCase,
    private val provinceUseCase: ProvinceUseCase,
    private val cityUseCase: CityUseCase,
    private val districtUseCase: DistrictUseCase
) {

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
        val model = request.toModel(type, parent)
        return useCase.create(model)
    }

    fun findById(type: GeoLocationType, id: Long): Optional<out IGeoLocationModel> {
        val useCase = getUseCase<IGeoLocationModel>(type)
        return useCase.findById(GeoLocationId(id))
    }

    fun findAll(type: GeoLocationType): List<IGeoLocationModel> {
        val useCase = getUseCase<IGeoLocationModel>(type)
        return useCase.findAll()
    }

    fun update(type: GeoLocationType, id: Long, request: UpdateGeoLocationRequest): IGeoLocationModel? {
        val useCase = getUseCase<IGeoLocationModel>(type)
        val existingModel = useCase.findById(GeoLocationId(id)).orElse(null) ?: return null

        val updatedModel = when (type) {
            GeoLocationType.CONTINENT -> (existingModel as ContinentModel).copy(name = request.name, boundaryRepresentation = request.boundaryRepresentation?.let { WKTReader().read(it) })
            GeoLocationType.REGION -> (existingModel as RegionModel).copy(name = request.name, boundaryRepresentation = request.boundaryRepresentation?.let { WKTReader().read(it) })
            GeoLocationType.COUNTRY -> (existingModel as CountryModel).copy(name = request.name, boundaryRepresentation = request.boundaryRepresentation?.let { WKTReader().read(it) })
            GeoLocationType.PROVINCE -> (existingModel as ProvinceModel).copy(name = request.name, boundaryRepresentation = request.boundaryRepresentation?.let { WKTReader().read(it) })
            GeoLocationType.CITY -> (existingModel as CityModel).copy(name = request.name, boundaryRepresentation = request.boundaryRepresentation?.let { WKTReader().read(it) })
            GeoLocationType.DISTRICT -> (existingModel as DistrictModel).copy(name = request.name, boundaryRepresentation = request.boundaryRepresentation?.let { WKTReader().read(it) })
        }
        return useCase.update(updatedModel)
    }

    fun deleteById(type: GeoLocationType, id: Long) {
        val useCase = getUseCase<IGeoLocationModel>(type)
        useCase.deleteById(GeoLocationId(id))
    }
}