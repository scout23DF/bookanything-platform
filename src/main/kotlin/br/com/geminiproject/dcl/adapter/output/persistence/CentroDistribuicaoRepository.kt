package br.com.geminiproject.dcl.adapter.output.persistence

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.locationtech.jts.geom.Point
import java.util.UUID

interface CentroDistribuicaoRepository : JpaRepository<CentroDistribuicaoEntity, UUID> {

    @Query(value = "SELECT * FROM centro_distribuicao WHERE ST_DWithin(localizacao, :point, :distanceInMeters, true)", nativeQuery = true)
    fun findByLocalizacaoWithinDistance(point: Point, distanceInMeters: Double): List<CentroDistribuicaoEntity>
}
