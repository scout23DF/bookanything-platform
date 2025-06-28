package br.com.geminiproject.dcl.adapter.output.persistence

import br.com.geminiproject.dcl.domain.CentroDistribuicao
import br.com.geminiproject.dcl.domain.CentroDistribuicaoRepositoryPort
import org.springframework.stereotype.Component

@Component
class CentroDistribuicaoPersistenceAdapter(
    private val repository: CentroDistribuicaoRepository
) : CentroDistribuicaoRepositoryPort {

    override fun salvar(centroDistribuicao: CentroDistribuicao): CentroDistribuicao {
        val entity = CentroDistribuicaoEntity(
            id = centroDistribuicao.id,
            nome = centroDistribuicao.nome,
            localizacao = centroDistribuicao.localizacao
        )
        val savedEntity = repository.save(entity)
        return CentroDistribuicao(
            id = savedEntity.id!!,
            nome = savedEntity.nome!!,
            localizacao = savedEntity.localizacao!!
        )
    }
}
