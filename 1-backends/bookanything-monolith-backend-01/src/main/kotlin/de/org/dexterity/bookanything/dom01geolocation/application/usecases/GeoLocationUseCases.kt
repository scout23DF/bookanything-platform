package de.org.dexterity.bookanything.dom01geolocation.application.usecases

import de.org.dexterity.bookanything.dom01geolocation.domain.models.*
import de.org.dexterity.bookanything.dom01geolocation.domain.ports.*
import org.springframework.stereotype.Service
import java.util.*

interface IGeoLocationUseCase<T : IGeoLocationModel> {
    fun create(model: T): T
    fun findById(id: GeoLocationId): Optional<T>
    fun findAll(): List<T>
    fun update(model: T): T?
    fun deleteById(id: GeoLocationId)
    fun findByParentIdAndNameStartingWith(parentId: GeoLocationId?, namePrefix: String): List<T>
}

@Service
class ContinentUseCase(private val repository: IContinentRepositoryPort) : IGeoLocationUseCase<ContinentModel> {
    override fun create(model: ContinentModel): ContinentModel = repository.saveNew(model)
    override fun findById(id: GeoLocationId): Optional<ContinentModel> = repository.findById(id)
    override fun findAll(): List<ContinentModel> = repository.findAll()
    override fun update(model: ContinentModel): ContinentModel? = repository.update(model)
    override fun deleteById(id: GeoLocationId) = repository.deleteById(id)
    override fun findByParentIdAndNameStartingWith(parentId: GeoLocationId?, namePrefix: String): List<ContinentModel> {
        // Continents do not have a parent, so parentId should be null
        if (parentId != null) {
            return emptyList()
        }
        return repository.findByNameStartingWith(namePrefix)
    }
}

@Service
class RegionUseCase(private val repository: IRegionRepositoryPort) : IGeoLocationUseCase<RegionModel> {
    override fun create(model: RegionModel): RegionModel = repository.saveNew(model)
    override fun findById(id: GeoLocationId): Optional<RegionModel> = repository.findById(id)
    override fun findAll(): List<RegionModel> = repository.findAll()
    override fun update(model: RegionModel): RegionModel? = repository.update(model)
    override fun deleteById(id: GeoLocationId) = repository.deleteById(id)
    override fun findByParentIdAndNameStartingWith(parentId: GeoLocationId?, namePrefix: String): List<RegionModel> {
        if (parentId == null) {
            return emptyList() // Regions must have a parent
        }
        return repository.findByContinentIdAndNameStartingWith(parentId, namePrefix)
    }
}

@Service
class CountryUseCase(private val repository: ICountryRepositoryPort) : IGeoLocationUseCase<CountryModel> {
    override fun create(model: CountryModel): CountryModel = repository.saveNew(model)
    override fun findById(id: GeoLocationId): Optional<CountryModel> = repository.findById(id)
    override fun findAll(): List<CountryModel> = repository.findAll()
    override fun update(model: CountryModel): CountryModel? = repository.update(model)
    override fun deleteById(id: GeoLocationId) = repository.deleteById(id)
    override fun findByParentIdAndNameStartingWith(parentId: GeoLocationId?, namePrefix: String): List<CountryModel> {
        if (parentId == null) {
            return emptyList() // Countries must have a parent
        }
        return repository.findByRegionIdAndNameStartingWith(parentId, namePrefix)
    }
}

@Service
class ProvinceUseCase(private val repository: IProvinceRepositoryPort) : IGeoLocationUseCase<ProvinceModel> {
    override fun create(model: ProvinceModel): ProvinceModel = repository.saveNew(model)
    override fun findById(id: GeoLocationId): Optional<ProvinceModel> = repository.findById(id)
    override fun findAll(): List<ProvinceModel> = repository.findAll()
    override fun update(model: ProvinceModel): ProvinceModel? = repository.update(model)
    override fun deleteById(id: GeoLocationId) = repository.deleteById(id)
    override fun findByParentIdAndNameStartingWith(parentId: GeoLocationId?, namePrefix: String): List<ProvinceModel> {
        if (parentId == null) {
            return emptyList() // Provinces must have a parent
        }
        return repository.findByCountryIdAndNameStartingWith(parentId, namePrefix)
    }
}

@Service
class CityUseCase(private val repository: ICityRepositoryPort) : IGeoLocationUseCase<CityModel> {
    override fun create(model: CityModel): CityModel = repository.saveNew(model)
    override fun findById(id: GeoLocationId): Optional<CityModel> = repository.findById(id)
    override fun findAll(): List<CityModel> = repository.findAll()
    override fun update(model: CityModel): CityModel? = repository.update(model)
    override fun deleteById(id: GeoLocationId) = repository.deleteById(id)
    override fun findByParentIdAndNameStartingWith(parentId: GeoLocationId?, namePrefix: String): List<CityModel> {
        if (parentId == null) {
            return emptyList() // Cities must have a parent
        }
        return repository.findByProvinceIdAndNameStartingWith(parentId, namePrefix)
    }
}

@Service
class DistrictUseCase(private val repository: IDistrictRepositoryPort) : IGeoLocationUseCase<DistrictModel> {
    override fun create(model: DistrictModel): DistrictModel = repository.saveNew(model)
    override fun findById(id: GeoLocationId): Optional<DistrictModel> = repository.findById(id)
    override fun findAll(): List<DistrictModel> = repository.findAll()
    override fun update(model: DistrictModel): DistrictModel? = repository.update(model)
    override fun deleteById(id: GeoLocationId) = repository.deleteById(id)
    override fun findByParentIdAndNameStartingWith(parentId: GeoLocationId?, namePrefix: String): List<DistrictModel> {
        if (parentId == null) {
            return emptyList() // Districts must have a parent
        }
        return repository.findByCityIdAndNameStartingWith(parentId, namePrefix)
    }
}
