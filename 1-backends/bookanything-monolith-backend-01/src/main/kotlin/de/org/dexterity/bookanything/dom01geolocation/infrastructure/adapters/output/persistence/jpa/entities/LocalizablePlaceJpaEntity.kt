package de.org.dexterity.bookanything.dom01geolocation.infrastructure.adapters.output.persistence.jpa.entities

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import org.locationtech.jts.geom.Point
import java.util.UUID

@Entity
@Table(name = "tb_localizable_place")
data class LocalizablePlaceJpaEntity(
    @Id
    var id: UUID? = null,

    @Column(name="ds_friendly_id", nullable = false, length = 40)
    var friendlyId: String,

    @Column(name="ds_name", nullable = false, unique = true)
    var name: String? = null,

    @Column(name="ds_alias", length = 20)
    var alias: String? = null,

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "json_additional_details", columnDefinition = "jsonb")
    var additionalDetailsMap: Map<String, Any>? = null,

    @Column(name="ge_location", columnDefinition="geometry(Point,4326)", nullable = false)
    var locationPoint: Point? = null
) {
    // Construtor sem argumentos para o Hibernate
    constructor() : this(null, "", null, null, null, null)
}