package de.org.dexterity.bookanything.dom02distributioncenterlocator.adapter.output.persistence.jpa

import org.springframework.data.jpa.repository.JpaRepository
import java.util.*

interface CentroDistribuicaoJpaRepository : JpaRepository<CentroDistribuicaoJpaEntity, UUID> {

    fun existsByNome(nome: String): Boolean

    /*
    @Query(value = "SELECT * FROM centro_distribuicao WHERE ST_DWithin(localizacao, :point, :distanceInMeters, true)", nativeQuery = true)
    fun findByLocalizacaoWithinDistance(point: Point, distanceInMeters: Double): List<CentroDistribuicaoJpaEntity>
    */

}
