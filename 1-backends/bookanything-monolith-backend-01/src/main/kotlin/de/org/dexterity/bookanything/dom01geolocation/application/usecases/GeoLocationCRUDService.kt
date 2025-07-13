package de.org.dexterity.bookanything.dom01geolocation.application.usecases

import de.org.dexterity.bookanything.dom01geolocation.domain.models.*
import de.org.dexterity.bookanything.dom01geolocation.infrastructure.adapters.input.web.dtos.CreateGeoLocationRequest
import de.org.dexterity.bookanything.dom01geolocation.infrastructure.adapters.input.web.dtos.UpdateGeoLocationRequest
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

    @Suppress("UNCHECKED_CAST")
    private fun <T : IGeoLocationModel> getUseCase(type: GeoLocationType): IGeoLocationUseCase<T> {
        return when (type) {
            GeoLocationType.CONTINENT -> continentUseCase as IGeoLocationUseCase<T>
            GeoLocationType.REGION -> regionUseCase as IGeoLocationUseCase<T>
            GeoLocationType.COUNTRY -> countryUseCase as IGeoLocationUseCase<T>
            GeoLocationType.PROVINCE -> provinceUseCase as IGeoLocationUseCase<T>
            GeoLocationType.CITY -> cityUseCase as IGeoLocationUseCase<T>
            GeoLocationType.DISTRICT -> districtUseCase as IGeoLocationUseCase<T>
        }
    }

    fun create(type: GeoLocationType, request: CreateGeoLocationRequest): IGeoLocationModel {
        val useCase = getUseCase<IGeoLocationModel>(type)
        val model = when (type) {
            GeoLocationType.CONTINENT -> request.toContinentModel()
            GeoLocationType.REGION -> {
                val parent = continentUseCase.findById(GeoLocationId(request.parentId!!)).orElseThrow { IllegalArgumentException("Continent not found for ID: ${request.parentId}") }
                request.toRegionModel(parent)
            }
            GeoLocationType.COUNTRY -> {
                val parent = regionUseCase.findById(GeoLocationId(request.parentId!!)).orElseThrow { IllegalArgumentException("Region not found for ID: ${request.parentId}") }
                request.toCountryModel(parent)
            }
            GeoLocationType.PROVINCE -> {
                val parent = countryUseCase.findById(GeoLocationId(request.parentId!!)).orElseThrow { IllegalArgumentException("Country not found for ID: ${request.parentId}") }
                request.toProvinceModel(parent)
            }
            GeoLocationType.CITY -> {
                val parent = provinceUseCase.findById(GeoLocationId(request.parentId!!)).orElseThrow { IllegalArgumentException("Province not found for ID: ${request.parentId}") }
                request.toCityModel(parent)
            }
            GeoLocationType.DISTRICT -> {
                val parent = cityUseCase.findById(GeoLocationId(request.parentId!!)).orElseThrow { IllegalArgumentException("City not found for ID: ${request.parentId}") }
                request.toDistrictModel(parent)
            }
        }
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