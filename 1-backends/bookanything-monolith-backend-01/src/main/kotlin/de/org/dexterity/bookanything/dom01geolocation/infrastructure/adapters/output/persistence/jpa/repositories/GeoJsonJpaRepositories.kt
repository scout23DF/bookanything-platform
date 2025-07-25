package de.org.dexterity.bookanything.dom01geolocation.infrastructure.adapters.output.persistence.jpa.repositories

import de.org.dexterity.bookanything.dom01geolocation.infrastructure.adapters.output.persistence.jpa.entities.GeoJsonFeatureEntity
import de.org.dexterity.bookanything.dom01geolocation.infrastructure.adapters.output.persistence.jpa.entities.GeoJsonImportedEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface GeoJsonImportJpaRepository : JpaRepository<GeoJsonImportedEntity, UUID>

@Repository
interface GeoJsonFeatureJpaRepository : JpaRepository<GeoJsonFeatureEntity, UUID>
