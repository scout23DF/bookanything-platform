package de.org.dexterity.bookanything.dom01geolocation.application.usecases

import de.org.dexterity.bookanything.dom01geolocation.domain.models.*
import de.org.dexterity.bookanything.dom01geolocation.domain.ports.*
import java.util.*

class ContinentUseCase(private val repository: IContinentRepositoryPort) {
    fun create(model: ContinentModel): ContinentModel = repository.saveNew(model)
    fun findById(id: GeoLocationId): Optional<ContinentModel> = repository.findById(id)
    fun findAll(): List<ContinentModel> = repository.findAll()
    fun update(model: ContinentModel): ContinentModel? = repository.update(model)
    fun deleteById(id: GeoLocationId) = repository.deleteById(id)
}

class RegionUseCase(private val repository: IRegionRepositoryPort) {
    fun create(model: RegionModel): RegionModel = repository.saveNew(model)
    fun findById(id: GeoLocationId): Optional<RegionModel> = repository.findById(id)
    fun findAll(): List<RegionModel> = repository.findAll()
    fun update(model: RegionModel): RegionModel? = repository.update(model)
    fun deleteById(id: GeoLocationId) = repository.deleteById(id)
}

class CountryUseCase(private val repository: ICountryRepositoryPort) {
    fun create(model: CountryModel): CountryModel = repository.saveNew(model)
    fun findById(id: GeoLocationId): Optional<CountryModel> = repository.findById(id)
    fun findAll(): List<CountryModel> = repository.findAll()
    fun update(model: CountryModel): CountryModel? = repository.update(model)
    fun deleteById(id: GeoLocationId) = repository.deleteById(id)
}

class ProvinceUseCase(private val repository: IProvinceRepositoryPort) {
    fun create(model: ProvinceModel): ProvinceModel = repository.saveNew(model)
    fun findById(id: GeoLocationId): Optional<ProvinceModel> = repository.findById(id)
    fun findAll(): List<ProvinceModel> = repository.findAll()
    fun update(model: ProvinceModel): ProvinceModel? = repository.update(model)
    fun deleteById(id: GeoLocationId) = repository.deleteById(id)
}

class CityUseCase(private val repository: ICityRepositoryPort) {
    fun create(model: CityModel): CityModel = repository.saveNew(model)
    fun findById(id: GeoLocationId): Optional<CityModel> = repository.findById(id)
    fun findAll(): List<CityModel> = repository.findAll()
    fun update(model: CityModel): CityModel? = repository.update(model)
    fun deleteById(id: GeoLocationId) = repository.deleteById(id)
}

class DistrictUseCase(private val repository: IDistrictRepositoryPort) {
    fun create(model: DistrictModel): DistrictModel = repository.saveNew(model)
    fun findById(id: GeoLocationId): Optional<DistrictModel> = repository.findById(id)
    fun findAll(): List<DistrictModel> = repository.findAll()
    fun update(model: DistrictModel): DistrictModel? = repository.update(model)
    fun deleteById(id: GeoLocationId) = repository.deleteById(id)
}

class AddressUseCase(private val repository: IAddressRepositoryPort) {
    fun create(model: AddressModel): AddressModel = repository.saveNew(model)
    fun findById(id: GeoLocationId): Optional<AddressModel> = repository.findById(id)
    fun findAll(): List<AddressModel> = repository.findAll()
    fun update(model: AddressModel): AddressModel? = repository.update(model)
    fun deleteById(id: GeoLocationId) = repository.deleteById(id)
}
