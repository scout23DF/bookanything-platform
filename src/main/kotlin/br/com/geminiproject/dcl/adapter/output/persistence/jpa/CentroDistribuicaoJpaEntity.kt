package br.com.geminiproject.dcl.adapter.output.persistence.jpa

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.locationtech.jts.geom.Point
import java.util.UUID

@Entity
@Table(name = "centro_distribuicao")
data class CentroDistribuicaoJpaEntity(
    @Id
    var id: UUID? = null,

    @Column(nullable = false, unique = true)
    var nome: String? = null,

    @Column(columnDefinition = "geometry(Point,4326)", nullable = false)
    var localizacao: Point? = null
) {
    // Construtor sem argumentos para o Hibernate
    constructor() : this(null, null, null)
}
