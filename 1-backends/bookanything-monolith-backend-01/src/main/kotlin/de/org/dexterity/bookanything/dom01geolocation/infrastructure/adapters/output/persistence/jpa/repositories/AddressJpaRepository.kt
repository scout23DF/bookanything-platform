package de.org.dexterity.bookanything.dom01geolocation.infrastructure.adapters.output.persistence.jpa.repositories

import de.org.dexterity.bookanything.dom01geolocation.infrastructure.adapters.output.persistence.jpa.entities.AddressEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface AddressJpaRepository : JpaRepository<AddressEntity, Long> {
    fun findByDistrictIdAndStreetNameStartingWithIgnoreCase(districtId: Long, streetName: String): List<AddressEntity>
}