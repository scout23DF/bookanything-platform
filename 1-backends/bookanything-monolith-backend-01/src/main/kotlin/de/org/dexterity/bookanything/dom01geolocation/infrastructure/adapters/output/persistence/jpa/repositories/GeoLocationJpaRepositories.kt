package de.org.dexterity.bookanything.dom01geolocation.infrastructure.adapters.output.persistence.jpa.repositories

import de.org.dexterity.bookanything.dom01geolocation.infrastructure.adapters.output.persistence.jpa.entities.*
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface ContinentJpaRepository : JpaRepository<ContinentEntity, Long> {
    fun findByNameStartingWithIgnoreCase(name: String): List<ContinentEntity>
    fun findByAliasStartingWithIgnoreCase(searchedAlias: String): List<ContinentEntity>
}

@Repository
interface RegionJpaRepository : JpaRepository<RegionEntity, Long> {
    fun findByContinentIdAndNameStartingWithIgnoreCase(continentId: Long, name: String): List<RegionEntity>
    fun findByContinentIdAndAliasStartingWithIgnoreCase(continentId: Long, searchedAlias: String): List<RegionEntity>
}

@Repository
interface CountryJpaRepository : JpaRepository<CountryEntity, Long> {
    fun findByRegionIdAndNameStartingWithIgnoreCase(regionId: Long, name: String): List<CountryEntity>
    fun findByRegionIdAndAliasStartingWithIgnoreCase(regionId: Long, searchedAlias: String): List<CountryEntity>
}

@Repository
interface ProvinceJpaRepository : JpaRepository<ProvinceEntity, Long> {
    fun findByCountryIdAndNameStartingWithIgnoreCase(countryId: Long, name: String): List<ProvinceEntity>
    fun findByCountryIdAndAliasStartingWithIgnoreCase(countryId: Long, searchedAlias: String): List<ProvinceEntity>
}

@Repository
interface CityJpaRepository : JpaRepository<CityEntity, Long> {
    fun findByProvinceIdAndNameStartingWithIgnoreCase(provinceId: Long, name: String): List<CityEntity>
    fun findByProvinceIdAndAliasStartingWithIgnoreCase(provinceId: Long, searchedAlias: String): List<CityEntity>
}

@Repository
interface DistrictJpaRepository : JpaRepository<DistrictEntity, Long> {
    fun findByCityIdAndNameStartingWithIgnoreCase(cityId: Long, name: String): List<DistrictEntity>
    fun findByCityIdAndAliasStartingWithIgnoreCase(cityId: Long, searchedAlias: String): List<DistrictEntity>
}
