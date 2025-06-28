package br.com.geminiproject.dcl.application

import br.com.geminiproject.dcl.domain.CadastrarCentroDistribuicaoUseCase
import br.com.geminiproject.dcl.domain.CentroDistribuicao
import br.com.geminiproject.dcl.domain.CentroDistribuicaoRepositoryPort
import br.com.geminiproject.dcl.domain.EventPublisherPort
import br.com.geminiproject.dcl.domain.events.CentroDistribuicaoCadastradoEvent
import org.locationtech.jts.geom.Point
import java.util.UUID

class CentroDistribuicaoService(
    private val repository: CentroDistribuicaoRepositoryPort,
    private val eventPublisher: EventPublisherPort
) : CadastrarCentroDistribuicaoUseCase {

    override fun cadastrar(nome: String, localizacao: Point): CentroDistribuicao {
        val centroDistribuicao = CentroDistribuicao(
            id = UUID.randomUUID(),
            nome = nome,
            localizacao = localizacao
        )
        val savedCentroDistribuicao = repository.salvar(centroDistribuicao)
        eventPublisher.publish(
            CentroDistribuicaoCadastradoEvent(
                id = savedCentroDistribuicao.id,
                nome = savedCentroDistribuicao.nome,
                latitude = savedCentroDistribuicao.localizacao.y,
                longitude = savedCentroDistribuicao.localizacao.x
            )
        )
        return savedCentroDistribuicao
    }
}
