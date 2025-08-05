package de.org.dexterity.bookanything.dom01geolocation.application.usecases

import de.org.dexterity.bookanything.dom01geolocation.domain.models.*
import de.org.dexterity.bookanything.dom01geolocation.domain.ports.*
import org.springframework.context.annotation.Lazy
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import java.util.*

interface IGeoLocationUseCase<T : IGeoLocationModel> {
    fun create(model: T): T
    fun findById(id: GeoLocationId): Optional<T>
    fun findAll(pageable: Pageable): Page<T>
    fun update(model: T): T?
    fun deleteById(id: GeoLocationId)
    fun deleteAll()
    fun deleteByParentId(parentId: GeoLocationId)
    fun findByParentIdAndNameStartingWith(parentId: GeoLocationId?, namePrefix: String, pageable: Pageable): Page<T>
    fun findByParentIdAndAliasStartingWith(parentId: GeoLocationId?, aliasPrefix: String, pageable: Pageable): Page<T>
    fun findDeepById(id: GeoLocationId): Optional<T>
    fun findDeepByName(name: String): Optional<T>
    fun findByFriendlyIdContaining(friendlyId: String, pageable: Pageable): Page<T>
    fun findByPropertiesDetailsMapContains(key: String, value: String, pageable: Pageable): Page<T>
}

@Service
class ContinentUseCase(private val continentRepositoryPort: IContinentRepositoryPort) : IGeoLocationUseCase<ContinentModel> {

    override fun create(model: ContinentModel): ContinentModel {
        return continentRepositoryPort.saveNew(model)
    }

    override fun findById(id: GeoLocationId): Optional<ContinentModel> {
        return continentRepositoryPort.findById(id)
    }

    override fun findAll(pageable: Pageable): Page<ContinentModel> {
        return continentRepositoryPort.findAll(pageable)
    }

    override fun update(model: ContinentModel): ContinentModel? {
        return continentRepositoryPort.update(model)
    }

    override fun deleteById(id: GeoLocationId) {
        return continentRepositoryPort.deleteById(id)
    }

    override fun deleteAll() {
        return continentRepositoryPort.deleteAll()
    }

    override fun deleteByParentId(parentId: GeoLocationId) {
        // Continents do not have a parent, so this method is not applicable
    }

    override fun findByParentIdAndNameStartingWith(parentId: GeoLocationId?, namePrefix: String, pageable: Pageable): Page<ContinentModel> {
        // Continents do not have a parent, so parentId should be null
        if (parentId != null) {
            return Page.empty()
        }
        return continentRepositoryPort.findByNameStartingWith(namePrefix, pageable)
    }

    override fun findByParentIdAndAliasStartingWith(parentId: GeoLocationId?, aliasPrefix: String, pageable: Pageable): Page<ContinentModel> {
        // Continents do not have a parent, so parentId should be null
        if (parentId != null) {
            return Page.empty()
        }
        return continentRepositoryPort.findByAliasStartingWith(aliasPrefix, pageable)
    }

    override fun findDeepById(id: GeoLocationId): Optional<ContinentModel> {
        return continentRepositoryPort.findDeepById(id)
    }

    override fun findDeepByName(name: String): Optional<ContinentModel> {
        return continentRepositoryPort.findDeepByName(name)
    }

    override fun findByFriendlyIdContaining(friendlyId: String, pageable: Pageable): Page<ContinentModel> {
        return continentRepositoryPort.findByFriendlyIdContainingIgnoreCase(friendlyId, pageable)
    }

    override fun findByPropertiesDetailsMapContains(key: String, value: String, pageable: Pageable): Page<ContinentModel> {
        return continentRepositoryPort.findByPropertiesDetailsMapContains(key, value, pageable)
    }

}

@Service
class RegionUseCase(
    private val regionRepositoryPort: IRegionRepositoryPort,
    @Lazy private val countryUseCase: CountryUseCase
) : IGeoLocationUseCase<RegionModel> {
    override fun create(model: RegionModel): RegionModel = regionRepositoryPort.saveNew(model)
    override fun findById(id: GeoLocationId): Optional<RegionModel> = regionRepositoryPort.findById(id)
    override fun findAll(pageable: Pageable): Page<RegionModel> = regionRepositoryPort.findAll(pageable)
    override fun update(model: RegionModel): RegionModel? = regionRepositoryPort.update(model)
    override fun deleteById(id: GeoLocationId) = regionRepositoryPort.deleteById(id)
    override fun deleteAll() = regionRepositoryPort.deleteAll()

    override fun deleteByParentId(parentId: GeoLocationId) {
        val children = regionRepositoryPort.findAllByContinentId(parentId, Pageable.ofSize(10000))
        children.forEach { child ->
            countryUseCase.deleteByParentId(child.id)
            deleteById(child.id)
        }
    }

    override fun findByParentIdAndNameStartingWith(parentId: GeoLocationId?, namePrefix: String, pageable: Pageable): Page<RegionModel> {
        if (parentId == null) {
            return Page.empty() // Regions must have a parent
        }
        return regionRepositoryPort.findByContinentIdAndNameStartingWith(parentId, namePrefix, pageable)
    }

    override fun findByParentIdAndAliasStartingWith(parentId: GeoLocationId?, aliasPrefix: String, pageable: Pageable): Page<RegionModel> {
        if (parentId == null) {
            return Page.empty() // Regions must have a parent
        }
        return regionRepositoryPort.findByContinentIdAndAliasStartingWith(parentId, aliasPrefix, pageable)
    }

    override fun findDeepById(id: GeoLocationId): Optional<RegionModel> {
        return regionRepositoryPort.findDeepById(id)
    }

    override fun findDeepByName(name: String): Optional<RegionModel> {
        return regionRepositoryPort.findDeepByName(name)
    }

    override fun findByFriendlyIdContaining(friendlyId: String, pageable: Pageable): Page<RegionModel> {
        return regionRepositoryPort.findByFriendlyIdContainingIgnoreCase(friendlyId, pageable)
    }

    override fun findByPropertiesDetailsMapContains(key: String, value: String, pageable: Pageable): Page<RegionModel> {
        return regionRepositoryPort.findByPropertiesDetailsMapContains(key, value, pageable)
    }

}

@Service
class CountryUseCase(
    private val countryRepositoryPort: ICountryRepositoryPort,
    @Lazy private val provinceUseCase: ProvinceUseCase
) : IGeoLocationUseCase<CountryModel> {
    override fun create(model: CountryModel): CountryModel = countryRepositoryPort.saveNew(model)
    override fun findById(id: GeoLocationId): Optional<CountryModel> = countryRepositoryPort.findById(id)
    override fun findAll(pageable: Pageable): Page<CountryModel> = countryRepositoryPort.findAll(pageable)
    override fun update(model: CountryModel): CountryModel? = countryRepositoryPort.update(model)
    override fun deleteById(id: GeoLocationId) = countryRepositoryPort.deleteById(id)
    override fun deleteAll() = countryRepositoryPort.deleteAll()

    override fun deleteByParentId(parentId: GeoLocationId) {
        val children = countryRepositoryPort.findAllByRegionId(parentId, Pageable.ofSize(10000))
        children.forEach { child ->
            provinceUseCase.deleteByParentId(child.id)
            deleteById(child.id)
        }
    }

    override fun findByParentIdAndNameStartingWith(parentId: GeoLocationId?, namePrefix: String, pageable: Pageable): Page<CountryModel> {
        if (parentId == null) {
            return Page.empty() // Countries must have a parent
        }
        return countryRepositoryPort.findByRegionIdAndNameStartingWith(parentId, namePrefix, pageable)
    }

    override fun findByParentIdAndAliasStartingWith(parentId: GeoLocationId?, aliasPrefix: String, pageable: Pageable): Page<CountryModel> {
        if (parentId == null) {
            return Page.empty() // Regions must have a parent
        }
        return countryRepositoryPort.findByRegionIdAndAliasStartingWith(parentId, aliasPrefix, pageable)
    }

    override fun findDeepById(id: GeoLocationId): Optional<CountryModel> {
        return countryRepositoryPort.findDeepById(id)
    }

    override fun findDeepByName(name: String): Optional<CountryModel> {
        return countryRepositoryPort.findDeepByName(name)
    }

    override fun findByFriendlyIdContaining(friendlyId: String, pageable: Pageable): Page<CountryModel> {
        return countryRepositoryPort.findByFriendlyIdContainingIgnoreCase(friendlyId, pageable)
    }

    override fun findByPropertiesDetailsMapContains(key: String, value: String, pageable: Pageable): Page<CountryModel> {
        return countryRepositoryPort.findByPropertiesDetailsMapContains(key, value, pageable)
    }

}

@Service
class ProvinceUseCase(
    private val provinceRepositoryPort: IProvinceRepositoryPort,
    @Lazy private val cityUseCase: CityUseCase
) : IGeoLocationUseCase<ProvinceModel> {
    override fun create(model: ProvinceModel): ProvinceModel = provinceRepositoryPort.saveNew(model)
    override fun findById(id: GeoLocationId): Optional<ProvinceModel> = provinceRepositoryPort.findById(id)
    override fun findAll(pageable: Pageable): Page<ProvinceModel> = provinceRepositoryPort.findAll(pageable)
    override fun update(model: ProvinceModel): ProvinceModel? = provinceRepositoryPort.update(model)
    override fun deleteById(id: GeoLocationId) = provinceRepositoryPort.deleteById(id)
    override fun deleteAll() = provinceRepositoryPort.deleteAll()

    override fun deleteByParentId(parentId: GeoLocationId) {
        val children = provinceRepositoryPort.findAllByCountryId(parentId, Pageable.ofSize(10000))
        children.forEach { child ->
            cityUseCase.deleteByParentId(child.id)
            deleteById(child.id)
        }
    }

    override fun findByParentIdAndNameStartingWith(parentId: GeoLocationId?, namePrefix: String, pageable: Pageable): Page<ProvinceModel> {
        if (parentId == null) {
            return Page.empty() // Provinces must have a parent
        }
        return provinceRepositoryPort.findByCountryIdAndNameStartingWith(parentId, namePrefix, pageable)
    }

    override fun findByParentIdAndAliasStartingWith(parentId: GeoLocationId?, aliasPrefix: String, pageable: Pageable): Page<ProvinceModel> {
        if (parentId == null) {
            return Page.empty() // Regions must have a parent
        }
        return provinceRepositoryPort.findByCountryIdAndAliasStartingWith(parentId, aliasPrefix, pageable)
    }

    override fun findDeepById(id: GeoLocationId): Optional<ProvinceModel> {
        return provinceRepositoryPort.findDeepById(id)
    }

    override fun findDeepByName(name: String): Optional<ProvinceModel> {
        return provinceRepositoryPort.findDeepByName(name)
    }

    override fun findByFriendlyIdContaining(friendlyId: String, pageable: Pageable): Page<ProvinceModel> {
        return provinceRepositoryPort.findByFriendlyIdContainingIgnoreCase(friendlyId, pageable)
    }

    override fun findByPropertiesDetailsMapContains(key: String, value: String, pageable: Pageable): Page<ProvinceModel> {
        return provinceRepositoryPort.findByPropertiesDetailsMapContains(key, value, pageable)
    }

}

@Service
class CityUseCase(
    private val cityRepositoryPort: ICityRepositoryPort,
    @Lazy private val districtUseCase: DistrictUseCase
) : IGeoLocationUseCase<CityModel> {
    override fun create(model: CityModel): CityModel = cityRepositoryPort.saveNew(model)
    override fun findById(id: GeoLocationId): Optional<CityModel> = cityRepositoryPort.findById(id)
    override fun findAll(pageable: Pageable): Page<CityModel> = cityRepositoryPort.findAll(pageable)
    override fun update(model: CityModel): CityModel? = cityRepositoryPort.update(model)
    override fun deleteById(id: GeoLocationId) = cityRepositoryPort.deleteById(id)
    override fun deleteAll() = cityRepositoryPort.deleteAll()

    override fun deleteByParentId(parentId: GeoLocationId) {
        val children = cityRepositoryPort.findAllByProvinceId(parentId, Pageable.ofSize(10000))
        children.forEach { child ->
            districtUseCase.deleteByParentId(child.id)
            deleteById(child.id)
        }
    }

    override fun findByParentIdAndNameStartingWith(parentId: GeoLocationId?, namePrefix: String, pageable: Pageable): Page<CityModel> {
        if (parentId == null) {
            return Page.empty() // Cities must have a parent
        }
        return cityRepositoryPort.findByProvinceIdAndNameStartingWith(parentId, namePrefix, pageable)
    }

    override fun findByParentIdAndAliasStartingWith(parentId: GeoLocationId?, aliasPrefix: String, pageable: Pageable): Page<CityModel> {
        if (parentId == null) {
            return Page.empty() // Regions must have a parent
        }
        return cityRepositoryPort.findByProvinceIdAndAliasStartingWith(parentId, aliasPrefix, pageable)
    }

    override fun findDeepById(id: GeoLocationId): Optional<CityModel> {
        return cityRepositoryPort.findDeepById(id)
    }

    override fun findDeepByName(name: String): Optional<CityModel> {
        return cityRepositoryPort.findDeepByName(name)
    }

    override fun findByFriendlyIdContaining(friendlyId: String, pageable: Pageable): Page<CityModel> {
        return cityRepositoryPort.findByFriendlyIdContainingIgnoreCase(friendlyId, pageable)
    }

    override fun findByPropertiesDetailsMapContains(key: String, value: String, pageable: Pageable): Page<CityModel> {
        return cityRepositoryPort.findByPropertiesDetailsMapContains(key, value, pageable)
    }

}

@Service
class DistrictUseCase(private val districtRepositoryPort: IDistrictRepositoryPort) : IGeoLocationUseCase<DistrictModel> {
    override fun create(model: DistrictModel): DistrictModel = districtRepositoryPort.saveNew(model)
    override fun findById(id: GeoLocationId): Optional<DistrictModel> = districtRepositoryPort.findById(id)
    override fun findAll(pageable: Pageable): Page<DistrictModel> = districtRepositoryPort.findAll(pageable)
    override fun update(model: DistrictModel): DistrictModel? = districtRepositoryPort.update(model)
    override fun deleteById(id: GeoLocationId) = districtRepositoryPort.deleteById(id)
    override fun deleteAll() = districtRepositoryPort.deleteAll()

    override fun deleteByParentId(parentId: GeoLocationId) {
        val children = districtRepositoryPort.findAllByCityId(parentId, Pageable.ofSize(10000))
        children.forEach { child ->
            deleteById(child.id)
        }
    }

    override fun findByParentIdAndNameStartingWith(parentId: GeoLocationId?, namePrefix: String, pageable: Pageable): Page<DistrictModel> {
        if (parentId == null) {
            return Page.empty() // Districts must have a parent
        }
        return districtRepositoryPort.findByCityIdAndNameStartingWith(parentId, namePrefix, pageable)
    }

    override fun findByParentIdAndAliasStartingWith(parentId: GeoLocationId?, aliasPrefix: String, pageable: Pageable): Page<DistrictModel> {
        if (parentId == null) {
            return Page.empty() // Regions must have a parent
        }
        return districtRepositoryPort.findByCityIdAndAliasStartingWith(parentId, aliasPrefix, pageable)
    }

    override fun findDeepById(id: GeoLocationId): Optional<DistrictModel> {
        return districtRepositoryPort.findDeepById(id)
    }

    override fun findDeepByName(name: String): Optional<DistrictModel> {
        return districtRepositoryPort.findDeepByName(name)
    }

    override fun findByFriendlyIdContaining(friendlyId: String, pageable: Pageable): Page<DistrictModel> {
        return districtRepositoryPort.findByFriendlyIdContainingIgnoreCase(friendlyId, pageable)
    }

    override fun findByPropertiesDetailsMapContains(key: String, value: String, pageable: Pageable): Page<DistrictModel> {
        return districtRepositoryPort.findByPropertiesDetailsMapContains(key, value, pageable)
    }

}
