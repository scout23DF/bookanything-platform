package br.com.geminiproject.dcl.domain.ports

import br.com.geminiproject.dcl.domain.CentroDistribuicaoModel
import java.util.*

interface CentroDistribuicaoPersistRepositoryPort {
    fun salvar(centroDistribuicaoModel: CentroDistribuicaoModel): CentroDistribuicaoModel
    fun deletarPorId(id: UUID)
    fun findAllForSync(): List<CentroDistribuicaoModel>
}