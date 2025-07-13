package de.org.dexterity.bookanything.dom01geolocation.application.usecases

import de.org.dexterity.bookanything.dom01geolocation.domain.models.*
import de.org.dexterity.bookanything.dom01geolocation.infrastructure.adapters.input.web.dtos.CreateGeoLocationRequest
import de.org.dexterity.bookanything.dom01geolocation.infrastructure.adapters.input.web.dtos.toContinentModel
import de.org.dexterity.bookanything.dom01geolocation.infrastructure.adapters.input.web.dtos.toCountryModel
import de.org.dexterity.bookanything.dom01geolocation.infrastructure.adapters.input.web.dtos.toRegionModel
import org.springframework.stereotype.Service

@Service
class GeoLocationCRUDUseCase(
    private val continentUseCase: ContinentUseCase,
    private val regionUseCase: RegionUseCase,
    private val countryUseCase: CountryUseCase,
    private val provinceUseCase: ProvinceUseCase,
    private val cityUseCase: CityUseCase,
    private val districtUseCase: DistrictUseCase
) {

    fun create(type: GeoLocationType, request: CreateGeoLocationRequest): IGeoLocationModel {
        return when (type) {
            GeoLocationType.CONTINENT -> continentUseCase.create(request.toContinentModel())
            GeoLocationType.REGION -> {
                val parent = continentUseCase.findById(GeoLocationId(request.parentId!!)).orElseThrow()
                regionUseCase.create(request.toRegionModel(parent))
            }
            GeoLocationType.COUNTRY -> {
                val parent = regionUseCase.findById(GeoLocationId(request.parentId!!)).orElseThrow()
                countryUseCase.create(request.toCountryModel(parent))
            }
            // ... Implement other cases
            else -> throw IllegalArgumentException("Unsupported GeoLocationType: $type")
        }
    }

    // ... Implement findById, findAll, update, delete
}