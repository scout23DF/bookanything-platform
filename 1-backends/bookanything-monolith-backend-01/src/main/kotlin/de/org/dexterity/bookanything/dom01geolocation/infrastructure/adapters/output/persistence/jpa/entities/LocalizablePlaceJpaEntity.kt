package de.org.dexterity.bookanything.dom01geolocation.infrastructure.adapters.output.persistence.jpa.entities

import de.org.dexterity.bookanything.dom01geolocation.domain.models.AddressModel
import de.org.dexterity.bookanything.dom01geolocation.domain.models.GeoCoordinate
import de.org.dexterity.bookanything.dom01geolocation.domain.models.GeoLocationId
import de.org.dexterity.bookanything.dom01geolocation.infrastructure.adapters.output.persistence.jpa.mappers.GeoLocationJpaMapper
import de.org.dexterity.bookanything.shared.annotations.Mapper
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.geom.GeometryFactory
import org.locationtech.jts.geom.Point
import java.util.UUID

@Entity
@Table(name = "tb_localizable_place")
data class LocalizablePlaceJpaEntity(
    @Id
    var id: UUID? = null,

    @Column(name="ds_name", nullable = false, unique = true)
    var name: String? = null,

    @Column(name="ds_alias", length = 20)
    var alias: String? = null,

    @Column(name="ge_location", columnDefinition="geometry(Point,4326)", nullable = false)
    var locationPoint: Point? = null
) {
    // Construtor sem argumentos para o Hibernate
    constructor() : this(null, null, null, null)
}

