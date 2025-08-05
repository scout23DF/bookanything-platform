package de.org.dexterity.bookanything.dom01geolocation.infrastructure.adapters.output.persistence.jpa.repositories

import de.org.dexterity.bookanything.dom01geolocation.infrastructure.adapters.output.persistence.jpa.entities.*
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface ContinentJpaRepository : JpaRepository<ContinentEntity, Long> {
    fun findByNameStartingWithIgnoreCase(name: String, pageable: Pageable): Page<ContinentEntity>
    fun findByAliasStartingWithIgnoreCase(searchedAlias: String, pageable: Pageable): Page<ContinentEntity>

    fun findByFriendlyIdContainingIgnoreCase(friendlyId: String, pageable: Pageable): Page<ContinentEntity>

    @Query(value= "SELECT g.* FROM tb_geo_location g WHERE g.json_additional_details ->> ?1 = ?2 AND g.tp_geo_location = 'CONTINENT'", nativeQuery = true)
    fun findByPropertiesDetailsMapContains(key: String, value: String, pageable: Pageable): Page<ContinentEntity>

    @Query("SELECT c FROM ContinentEntity c LEFT JOIN FETCH c.regionsList WHERE c.id = :id")
    fun findDeepById(@Param("id") id: Long): Optional<ContinentEntity>

    @Query("SELECT c FROM ContinentEntity c LEFT JOIN FETCH c.regionsList WHERE c.name = :name")
    fun findDeepByName(@Param("name") name: String): Optional<ContinentEntity>
}

@Repository
interface RegionJpaRepository : JpaRepository<RegionEntity, Long> {
    fun findAllByContinentId(continentId: Long, pageable: Pageable): Page<RegionEntity>
    fun findByContinentIdAndNameStartingWithIgnoreCase(continentId: Long, name: String, pageable: Pageable): Page<RegionEntity>
    fun findByContinentIdAndAliasStartingWithIgnoreCase(continentId: Long, searchedAlias: String, pageable: Pageable): Page<RegionEntity>

    fun findByFriendlyIdContainingIgnoreCase(friendlyId: String, pageable: Pageable): Page<RegionEntity>

    @Query(value= "SELECT g.* FROM tb_geo_location g WHERE g.json_additional_details ->> ?1 = ?2 AND g.tp_geo_location = 'REGION'", nativeQuery = true)
    fun findByPropertiesDetailsMapContains(key: String, value: String, pageable: Pageable): Page<RegionEntity>

    @Query("SELECT r FROM RegionEntity r LEFT JOIN FETCH r.countriesList WHERE r.id = :id")
    fun findDeepById(@Param("id") id: Long): Optional<RegionEntity>

    @Query("SELECT r FROM RegionEntity r LEFT JOIN FETCH r.countriesList WHERE r.name = :name")
    fun findDeepByName(@Param("name") name: String): Optional<RegionEntity>
}

@Repository
interface CountryJpaRepository : JpaRepository<CountryEntity, Long> {
    fun findAllByRegionId(regionId: Long, pageable: Pageable): Page<CountryEntity>
    fun findByRegionIdAndNameStartingWithIgnoreCase(regionId: Long, name: String, pageable: Pageable): Page<CountryEntity>
    fun findByRegionIdAndAliasStartingWithIgnoreCase(regionId: Long, searchedAlias: String, pageable: Pageable): Page<CountryEntity>

    fun findByFriendlyIdContainingIgnoreCase(friendlyId: String, pageable: Pageable): Page<CountryEntity>

    @Query(value= "SELECT g.* FROM tb_geo_location g WHERE g.json_additional_details ->> ?1 = ?2 AND g.tp_geo_location = 'COUNTRY'", nativeQuery = true)
    fun findByPropertiesDetailsMapContains(key: String, value: String, pageable: Pageable): Page<CountryEntity>

    @Query("SELECT c FROM CountryEntity c LEFT JOIN FETCH c.provincesList WHERE c.id = :id")
    fun findDeepById(@Param("id") id: Long): Optional<CountryEntity>

    @Query("SELECT c FROM CountryEntity c LEFT JOIN FETCH c.provincesList WHERE c.name = :name")
    fun findDeepByName(@Param("name") name: String): Optional<CountryEntity>
}

@Repository
interface ProvinceJpaRepository : JpaRepository<ProvinceEntity, Long> {
    fun findAllByCountryId(countryId: Long, pageable: Pageable): Page<ProvinceEntity>
    fun findByCountryIdAndNameStartingWithIgnoreCase(countryId: Long, name: String, pageable: Pageable): Page<ProvinceEntity>
    fun findByCountryIdAndAliasStartingWithIgnoreCase(countryId: Long, searchedAlias: String, pageable: Pageable): Page<ProvinceEntity>

    fun findByFriendlyIdContainingIgnoreCase(friendlyId: String, pageable: Pageable): Page<ProvinceEntity>

    @Query(value= "SELECT g.* FROM tb_geo_location g WHERE g.json_additional_details ->> ?1 = ?2 AND g.tp_geo_location = 'PROVINCE'", nativeQuery = true)
    fun findByPropertiesDetailsMapContains(key: String, value: String, pageable: Pageable): Page<ProvinceEntity>

    @Query("SELECT p FROM ProvinceEntity p LEFT JOIN FETCH p.citiesList WHERE p.id = :id")
    fun findDeepById(@Param("id") id: Long): Optional<ProvinceEntity>

    @Query("SELECT p FROM ProvinceEntity p LEFT JOIN FETCH p.citiesList WHERE p.name = :name")
    fun findDeepByName(@Param("name") name: String): Optional<ProvinceEntity>
}

@Repository
interface CityJpaRepository : JpaRepository<CityEntity, Long> {
    fun findAllByProvinceId(provinceId: Long, pageable: Pageable): Page<CityEntity>
    fun findByProvinceIdAndNameStartingWithIgnoreCase(provinceId: Long, name: String, pageable: Pageable): Page<CityEntity>
    fun findByProvinceIdAndAliasStartingWithIgnoreCase(provinceId: Long, searchedAlias: String, pageable: Pageable): Page<CityEntity>

    fun findByFriendlyIdContainingIgnoreCase(friendlyId: String, pageable: Pageable): Page<CityEntity>

    @Query(value= "SELECT g.* FROM tb_geo_location g WHERE g.json_additional_details ->> ?1 = ?2 AND g.tp_geo_location = 'CITY'", nativeQuery = true)
    fun findByPropertiesDetailsMapContains(key: String, value: String, pageable: Pageable): Page<CityEntity>

    @Query("SELECT c FROM CityEntity c LEFT JOIN FETCH c.districtsList WHERE c.id = :id")
    fun findDeepById(@Param("id") id: Long): Optional<CityEntity>

    @Query("SELECT c FROM CityEntity c LEFT JOIN FETCH c.districtsList WHERE c.name = :name")
    fun findDeepByName(@Param("name") name: String): Optional<CityEntity>
}

@Repository
interface DistrictJpaRepository : JpaRepository<DistrictEntity, Long> {
    fun findAllByCityId(cityId: Long, pageable: Pageable): Page<DistrictEntity>
    fun findByCityIdAndNameStartingWithIgnoreCase(cityId: Long, name: String, pageable: Pageable): Page<DistrictEntity>
    fun findByCityIdAndAliasStartingWithIgnoreCase(cityId: Long, searchedAlias: String, pageable: Pageable): Page<DistrictEntity>

    fun findByFriendlyIdContainingIgnoreCase(friendlyId: String, pageable: Pageable): Page<DistrictEntity>

    @Query(value= "SELECT g.* FROM tb_geo_location g WHERE g.json_additional_details ->> ?1 = ?2 AND g.tp_geo_location = 'DISTRICT'", nativeQuery = true)
    fun findByPropertiesDetailsMapContains(key: String, value: String, pageable: Pageable): Page<DistrictEntity>

    @Query("SELECT d FROM DistrictEntity d LEFT JOIN FETCH d.addressesList WHERE d.id = :id")
    fun findDeepById(@Param("id") id: Long): Optional<DistrictEntity>

    @Query("SELECT d FROM DistrictEntity d LEFT JOIN FETCH d.addressesList WHERE d.name = :name")
    fun findDeepByName(@Param("name") name: String): Optional<DistrictEntity>
}
