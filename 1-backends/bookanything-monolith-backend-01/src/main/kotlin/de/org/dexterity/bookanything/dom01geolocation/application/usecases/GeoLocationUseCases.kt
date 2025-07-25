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
    fun deleteAll()
    fun findByParentIdAndNameStartingWith(parentId: GeoLocationId?, namePrefix: String): List<T>
    fun findByParentIdAndAliasStartingWith(parentId: GeoLocationId?, aliasPrefix: String): List<T>
    fun findDeepById(id: GeoLocationId): Optional<T>
    fun findDeepByName(name: String): Optional<T>
    fun findByFriendlyIdContaining(friendlyId: String): List<T>
    fun findByPropertiesDetailsMapContains(key: String, value: String): List<T>
}

@Service
class ContinentUseCase(private val repository: IContinentRepositoryPort) : IGeoLocationUseCase<ContinentModel> {

    override fun create(model: ContinentModel): ContinentModel {
        return repository.saveNew(model)
    }

    override fun findById(id: GeoLocationId): Optional<ContinentModel> {
        return repository.findById(id)
    }

    override fun findAll(): List<ContinentModel> {
        return repository.findAll()
    }

    override fun update(model: ContinentModel): ContinentModel? {
        return repository.update(model)
    }

    override fun deleteById(id: GeoLocationId) {
        return repository.deleteById(id)
    }

    override fun deleteAll() {
        return repository.deleteAll()
    }

    override fun findByParentIdAndNameStartingWith(parentId: GeoLocationId?, namePrefix: String): List<ContinentModel> {
        // Continents do not have a parent, so parentId should be null
        if (parentId != null) {
            return emptyList()
        }
        return repository.findByNameStartingWith(namePrefix)
    }

    override fun findByParentIdAndAliasStartingWith(parentId: GeoLocationId?, aliasPrefix: String): List<ContinentModel> {
        // Continents do not have a parent, so parentId should be null
        if (parentId != null) {
            return emptyList()
        }
        return repository.findByAliasStartingWith(aliasPrefix)
    }

    override fun findDeepById(id: GeoLocationId): Optional<ContinentModel> {
        return repository.findDeepById(id)
    }

    override fun findDeepByName(name: String): Optional<ContinentModel> {
        return repository.findDeepByName(name)
    }

    override fun findByFriendlyIdContaining(friendlyId: String): List<ContinentModel> {
        return repository.findByFriendlyIdContainingIgnoreCase(friendlyId)
    }

    override fun findByPropertiesDetailsMapContains(key: String, value: String): List<ContinentModel> {
        return repository.findByPropertiesDetailsMapContains(key, value)
    }

}

@Service
class RegionUseCase(private val repository: IRegionRepositoryPort) : IGeoLocationUseCase<RegionModel> {
    override fun create(model: RegionModel): RegionModel = repository.saveNew(model)
    override fun findById(id: GeoLocationId): Optional<RegionModel> = repository.findById(id)
    override fun findAll(): List<RegionModel> = repository.findAll()
    override fun update(model: RegionModel): RegionModel? = repository.update(model)
    override fun deleteById(id: GeoLocationId) = repository.deleteById(id)
    override fun deleteAll() = repository.deleteAll()
    override fun findByParentIdAndNameStartingWith(parentId: GeoLocationId?, namePrefix: String): List<RegionModel> {
        if (parentId == null) {
            return emptyList() // Regions must have a parent
        }
        return repository.findByContinentIdAndNameStartingWith(parentId, namePrefix)
    }

    override fun findByParentIdAndAliasStartingWith(parentId: GeoLocationId?, aliasPrefix: String): List<RegionModel> {
        if (parentId == null) {
            return emptyList() // Regions must have a parent
        }
        return repository.findByContinentIdAndAliasStartingWith(parentId, aliasPrefix)
    }

    override fun findDeepById(id: GeoLocationId): Optional<RegionModel> {
        return repository.findDeepById(id)
    }

    override fun findDeepByName(name: String): Optional<RegionModel> {
        return repository.findDeepByName(name)
    }

    override fun findByFriendlyIdContaining(friendlyId: String): List<RegionModel> {
        return repository.findByFriendlyIdContainingIgnoreCase(friendlyId)
    }

    override fun findByPropertiesDetailsMapContains(key: String, value: String): List<RegionModel> {
        return repository.findByPropertiesDetailsMapContains(key, value)
    }

}

@Service
class CountryUseCase(private val repository: ICountryRepositoryPort) : IGeoLocationUseCase<CountryModel> {
    override fun create(model: CountryModel): CountryModel = repository.saveNew(model)
    override fun findById(id: GeoLocationId): Optional<CountryModel> = repository.findById(id)
    override fun findAll(): List<CountryModel> = repository.findAll()
    override fun update(model: CountryModel): CountryModel? = repository.update(model)
    override fun deleteById(id: GeoLocationId) = repository.deleteById(id)
    override fun deleteAll() = repository.deleteAll()
    override fun findByParentIdAndNameStartingWith(parentId: GeoLocationId?, namePrefix: String): List<CountryModel> {
        if (parentId == null) {
            return emptyList() // Countries must have a parent
        }
        return repository.findByRegionIdAndNameStartingWith(parentId, namePrefix)
    }

    override fun findByParentIdAndAliasStartingWith(parentId: GeoLocationId?, aliasPrefix: String): List<CountryModel> {
        if (parentId == null) {
            return emptyList() // Regions must have a parent
        }
        return repository.findByRegionIdAndAliasStartingWith(parentId, aliasPrefix)
    }

    override fun findDeepById(id: GeoLocationId): Optional<CountryModel> {
        return repository.findDeepById(id)
    }

    override fun findDeepByName(name: String): Optional<CountryModel> {
        return repository.findDeepByName(name)
    }

    override fun findByFriendlyIdContaining(friendlyId: String): List<CountryModel> {
        return repository.findByFriendlyIdContainingIgnoreCase(friendlyId)
    }

    override fun findByPropertiesDetailsMapContains(key: String, value: String): List<CountryModel> {
        return repository.findByPropertiesDetailsMapContains(key, value)
    }

}

@Service
class ProvinceUseCase(private val repository: IProvinceRepositoryPort) : IGeoLocationUseCase<ProvinceModel> {
    override fun create(model: ProvinceModel): ProvinceModel = repository.saveNew(model)
    override fun findById(id: GeoLocationId): Optional<ProvinceModel> = repository.findById(id)
    override fun findAll(): List<ProvinceModel> = repository.findAll()
    override fun update(model: ProvinceModel): ProvinceModel? = repository.update(model)
    override fun deleteById(id: GeoLocationId) = repository.deleteById(id)
    override fun deleteAll() = repository.deleteAll()
    override fun findByParentIdAndNameStartingWith(parentId: GeoLocationId?, namePrefix: String): List<ProvinceModel> {
        if (parentId == null) {
            return emptyList() // Provinces must have a parent
        }
        return repository.findByCountryIdAndNameStartingWith(parentId, namePrefix)
    }

    override fun findByParentIdAndAliasStartingWith(parentId: GeoLocationId?, aliasPrefix: String): List<ProvinceModel> {
        if (parentId == null) {
            return emptyList() // Regions must have a parent
        }
        return repository.findByCountryIdAndAliasStartingWith(parentId, aliasPrefix)
    }

    override fun findDeepById(id: GeoLocationId): Optional<ProvinceModel> {
        return repository.findDeepById(id)
    }

    override fun findDeepByName(name: String): Optional<ProvinceModel> {
        return repository.findDeepByName(name)
    }

    override fun findByFriendlyIdContaining(friendlyId: String): List<ProvinceModel> {
        return repository.findByFriendlyIdContainingIgnoreCase(friendlyId)
    }

    override fun findByPropertiesDetailsMapContains(key: String, value: String): List<ProvinceModel> {
        return repository.findByPropertiesDetailsMapContains(key, value)
    }

}

@Service
class CityUseCase(private val repository: ICityRepositoryPort) : IGeoLocationUseCase<CityModel> {
    override fun create(model: CityModel): CityModel = repository.saveNew(model)
    override fun findById(id: GeoLocationId): Optional<CityModel> = repository.findById(id)
    override fun findAll(): List<CityModel> = repository.findAll()
    override fun update(model: CityModel): CityModel? = repository.update(model)
    override fun deleteById(id: GeoLocationId) = repository.deleteById(id)
    override fun deleteAll() = repository.deleteAll()
    override fun findByParentIdAndNameStartingWith(parentId: GeoLocationId?, namePrefix: String): List<CityModel> {
        if (parentId == null) {
            return emptyList() // Cities must have a parent
        }
        return repository.findByProvinceIdAndNameStartingWith(parentId, namePrefix)
    }

    override fun findByParentIdAndAliasStartingWith(parentId: GeoLocationId?, aliasPrefix: String): List<CityModel> {
        if (parentId == null) {
            return emptyList() // Regions must have a parent
        }
        return repository.findByProvinceIdAndAliasStartingWith(parentId, aliasPrefix)
    }

    override fun findDeepById(id: GeoLocationId): Optional<CityModel> {
        return repository.findDeepById(id)
    }

    override fun findDeepByName(name: String): Optional<CityModel> {
        return repository.findDeepByName(name)
    }

    override fun findByFriendlyIdContaining(friendlyId: String): List<CityModel> {
        return repository.findByFriendlyIdContainingIgnoreCase(friendlyId)
    }

    override fun findByPropertiesDetailsMapContains(key: String, value: String): List<CityModel> {
        return repository.findByPropertiesDetailsMapContains(key, value)
    }

}

@Service
class DistrictUseCase(private val repository: IDistrictRepositoryPort) : IGeoLocationUseCase<DistrictModel> {
    override fun create(model: DistrictModel): DistrictModel = repository.saveNew(model)
    override fun findById(id: GeoLocationId): Optional<DistrictModel> = repository.findById(id)
    override fun findAll(): List<DistrictModel> = repository.findAll()
    override fun update(model: DistrictModel): DistrictModel? = repository.update(model)
    override fun deleteById(id: GeoLocationId) = repository.deleteById(id)
    override fun deleteAll() = repository.deleteAll()
    override fun findByParentIdAndNameStartingWith(parentId: GeoLocationId?, namePrefix: String): List<DistrictModel> {
        if (parentId == null) {
            return emptyList() // Districts must have a parent
        }
        return repository.findByCityIdAndNameStartingWith(parentId, namePrefix)
    }

    override fun findByParentIdAndAliasStartingWith(parentId: GeoLocationId?, aliasPrefix: String): List<DistrictModel> {
        if (parentId == null) {
            return emptyList() // Regions must have a parent
        }
        return repository.findByCityIdAndAliasStartingWith(parentId, aliasPrefix)
    }

    override fun findDeepById(id: GeoLocationId): Optional<DistrictModel> {
        return repository.findDeepById(id)
    }

    override fun findDeepByName(name: String): Optional<DistrictModel> {
        return repository.findDeepByName(name)
    }

    override fun findByFriendlyIdContaining(friendlyId: String): List<DistrictModel> {
        return repository.findByFriendlyIdContainingIgnoreCase(friendlyId)
    }

    override fun findByPropertiesDetailsMapContains(key: String, value: String): List<DistrictModel> {
        return repository.findByPropertiesDetailsMapContains(key, value)
    }

}
