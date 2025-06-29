package br.com.geminiproject.dcl.domain.features

import br.com.geminiproject.dcl.domain.CentroDistribuicaoModel
import org.locationtech.jts.geom.Point
import java.util.*

interface CentroDistribuicaoWriteFeaturePort {
    fun cadastrar(nome: String, localizacao: Point): CentroDistribuicaoModel
    fun deletarPorId(id: UUID)
    fun synchronizeAll(): Map<String, Int>
}
