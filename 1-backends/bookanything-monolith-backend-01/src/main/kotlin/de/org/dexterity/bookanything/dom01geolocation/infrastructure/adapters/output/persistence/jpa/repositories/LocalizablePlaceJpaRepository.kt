package de.org.dexterity.bookanything.dom01geolocation.infrastructure.adapters.output.persistence.jpa.repositories

import de.org.dexterity.bookanything.dom01geolocation.infrastructure.adapters.output.persistence.jpa.entities.LocalizablePlaceJpaEntity
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface LocalizablePlaceJpaRepository : JpaRepository<LocalizablePlaceJpaEntity, UUID> {

    fun existsByName(name: String): Boolean

    /*
    @Query(value = "SELECT * FROM centro_distribuicao WHERE ST_DWithin(localizacao, :point, :distanceInMeters, true)", nativeQuery = true)
    fun findByLocalizacaoWithinDistance(point: Point, distanceInMeters: Double): List<CentroDistribuicaoJpaEntity>
    */

}