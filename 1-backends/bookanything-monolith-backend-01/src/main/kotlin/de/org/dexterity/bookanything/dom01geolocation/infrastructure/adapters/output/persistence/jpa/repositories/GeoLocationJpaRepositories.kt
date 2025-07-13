package de.org.dexterity.bookanything.dom01geolocation.infrastructure.adapters.output.persistence.jpa.repositories

import de.org.dexterity.bookanything.dom01geolocation.infrastructure.adapters.output.persistence.jpa.entities.AddressEntity
import de.org.dexterity.bookanything.dom01geolocation.infrastructure.adapters.output.persistence.jpa.entities.CityEntity
import de.org.dexterity.bookanything.dom01geolocation.infrastructure.adapters.output.persistence.jpa.entities.ContinentEntity
import de.org.dexterity.bookanything.dom01geolocation.infrastructure.adapters.output.persistence.jpa.entities.CountryEntity
import de.org.dexterity.bookanything.dom01geolocation.infrastructure.adapters.output.persistence.jpa.entities.DistrictEntity
import de.org.dexterity.bookanything.dom01geolocation.infrastructure.adapters.output.persistence.jpa.entities.ProvinceEntity
import de.org.dexterity.bookanything.dom01geolocation.infrastructure.adapters.output.persistence.jpa.entities.RegionEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface ContinentJpaRepository : JpaRepository<ContinentEntity, Long> {
    fun findByNameStartingWithIgnoreCase(name: String): List<ContinentEntity>
}

@Repository
interface RegionJpaRepository : JpaRepository<RegionEntity, Long> {
    fun findByContinentIdAndNameStartingWithIgnoreCase(continentId: Long, name: String): List<RegionEntity>
}

@Repository
interface CountryJpaRepository : JpaRepository<CountryEntity, Long> {
    fun findByRegionIdAndNameStartingWithIgnoreCase(regionId: Long, name: String): List<CountryEntity>
}

@Repository
interface ProvinceJpaRepository : JpaRepository<ProvinceEntity, Long> {
    fun findByCountryIdAndNameStartingWithIgnoreCase(countryId: Long, name: String): List<ProvinceEntity>
}

@Repository
interface CityJpaRepository : JpaRepository<CityEntity, Long> {
    fun findByProvinceIdAndNameStartingWithIgnoreCase(provinceId: Long, name: String): List<CityEntity>
}

@Repository
interface DistrictJpaRepository : JpaRepository<DistrictEntity, Long> {
    fun findByCityIdAndNameStartingWithIgnoreCase(cityId: Long, name: String): List<DistrictEntity>
}

@Repository
interface AddressJpaRepository : JpaRepository<AddressEntity, Long> {
    fun findByDistrictIdAndStreetNameStartingWithIgnoreCase(districtId: Long, streetName: String): List<AddressEntity>
}