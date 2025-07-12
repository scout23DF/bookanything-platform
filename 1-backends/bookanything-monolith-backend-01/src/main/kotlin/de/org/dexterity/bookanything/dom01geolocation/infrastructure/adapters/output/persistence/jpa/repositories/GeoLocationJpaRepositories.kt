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
interface ContinentJpaRepository : JpaRepository<ContinentEntity, Long>

@Repository
interface RegionJpaRepository : JpaRepository<RegionEntity, Long>

@Repository
interface CountryJpaRepository : JpaRepository<CountryEntity, Long>

@Repository
interface ProvinceJpaRepository : JpaRepository<ProvinceEntity, Long>

@Repository
interface CityJpaRepository : JpaRepository<CityEntity, Long>

@Repository
interface DistrictJpaRepository : JpaRepository<DistrictEntity, Long>

@Repository
interface AddressJpaRepository : JpaRepository<AddressEntity, Long>
