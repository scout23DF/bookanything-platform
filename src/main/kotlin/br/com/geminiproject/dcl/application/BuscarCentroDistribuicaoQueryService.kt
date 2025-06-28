package br.com.geminiproject.dcl.application

import br.com.geminiproject.dcl.domain.BuscarCentroDistribuicaoQueryPort
import br.com.geminiproject.dcl.domain.CentroDistribuicao
import br.com.geminiproject.dcl.domain.CentroDistribuicaoQueryRepositoryPort
import java.util.UUID

class BuscarCentroDistribuicaoQueryService(
    private val repository: CentroDistribuicaoQueryRepositoryPort
) : BuscarCentroDistribuicaoQueryPort {

    override fun buscarPorId(id: UUID): CentroDistribuicao? {
        return repository.buscarPorId(id)
    }

    override fun buscarTodos(): List<CentroDistribuicao> {
        return repository.buscarTodos()
    }
}
