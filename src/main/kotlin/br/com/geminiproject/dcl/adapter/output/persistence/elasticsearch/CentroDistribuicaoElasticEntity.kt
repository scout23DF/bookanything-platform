package br.com.geminiproject.dcl.adapter.output.persistence.elasticsearch

import org.springframework.data.annotation.Id
import org.springframework.data.elasticsearch.annotations.Document
import org.springframework.data.elasticsearch.annotations.GeoPointField
import org.springframework.data.elasticsearch.core.geo.GeoPoint
import java.util.UUID

@Document(indexName = "centros_distribuicao")
data class CentroDistribuicaoElasticEntity(
    @Id
    val id: UUID,
    val nome: String,
    @GeoPointField
    val localizacao: GeoPoint
)
