package de.org.dexterity.bookanything.dom02distributioncenterlocator.infrastructure.adapters.output.persistence.jpa.entities

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.locationtech.jts.geom.Point
import java.util.UUID

@Entity
@Table(name = "tb_distribution_center")
data class CentroDistribuicaoJpaEntity(
    @Id
    var id: UUID? = null,

    @Column(name="ds_name", nullable = false, unique = true)
    var nome: String? = null,

    @Column(name="ge_location", columnDefinition="geometry(Point,4326)", nullable = false)
    var localizacao: Point? = null
) {
    // Construtor sem argumentos para o Hibernate
    constructor() : this(null, null, null)
}