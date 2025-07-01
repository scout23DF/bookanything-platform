package br.com.geminiproject.dcl.application

import br.com.geminiproject.dcl.domain.CentroDistribuicaoModel
import br.com.geminiproject.dcl.domain.EventPublisherPort
import br.com.geminiproject.dcl.domain.events.CentroDistribuicaoCadastradoEvent
import br.com.geminiproject.dcl.domain.events.CentroDistribuicaoDeletadoEvent
import br.com.geminiproject.dcl.domain.events.CentrosDistribuicaoDeletadosEvent
import br.com.geminiproject.dcl.domain.features.CentroDistribuicaoReadFeaturePort
import br.com.geminiproject.dcl.domain.features.CentroDistribuicaoWriteFeaturePort
import br.com.geminiproject.dcl.domain.ports.CentroDistribuicaoPersistRepositoryPort
import br.com.geminiproject.dcl.domain.ports.CentroDistribuicaoQueryRepositoryPort
import org.locationtech.jts.geom.Point
import java.util.*

class CentroDistribuicaoOrchestrationService(
    private val centroDistribuicaoPersistRepositoryPort: CentroDistribuicaoPersistRepositoryPort,
    private val centroDistribuicaoQueryRepositoryPort: CentroDistribuicaoQueryRepositoryPort,
    private val eventPublisherPort: EventPublisherPort
) : CentroDistribuicaoWriteFeaturePort, CentroDistribuicaoReadFeaturePort {

    override fun cadastrar(nome: String, localizacao: Point): CentroDistribuicaoModel {
        val centroDistribuicaoModel = CentroDistribuicaoModel(
            id = UUID.randomUUID(),
            nome = nome,
            localizacao = localizacao
        )
        val savedCentroDistribuicao = centroDistribuicaoPersistRepositoryPort.salvar(centroDistribuicaoModel)
        eventPublisherPort.publish(
            CentroDistribuicaoCadastradoEvent(
                id = savedCentroDistribuicao.id,
                nome = savedCentroDistribuicao.nome,
                latitude = savedCentroDistribuicao.localizacao.y,
                longitude = savedCentroDistribuicao.localizacao.x
            )
        )
        return savedCentroDistribuicao
    }

    override fun buscarPorId(id: UUID): CentroDistribuicaoModel? {
        return centroDistribuicaoQueryRepositoryPort.buscarPorId(id)
    }

    override fun buscarTodos(): List<CentroDistribuicaoModel> {
        return centroDistribuicaoQueryRepositoryPort.buscarTodos()
    }

    override fun buscarCentrosProximos(localizacao: Point, raioEmKm: Double): List<CentroDistribuicaoModel> {
        return centroDistribuicaoQueryRepositoryPort.buscarCentrosProximos(localizacao, raioEmKm)
    }

    override fun deletarPorId(id: UUID) {
        centroDistribuicaoPersistRepositoryPort.deletarPorId(id)
        eventPublisherPort.publish(CentroDistribuicaoDeletadoEvent(id))
    }

    override fun synchronizeAll(): Map<String, Int> {
        return this.centroDistribuicaoQueryRepositoryPort.synchronizeFromWriteRepository(
            this.centroDistribuicaoPersistRepositoryPort.findAllForSync()
        )
    }

    override fun deletarTodos() {
        centroDistribuicaoPersistRepositoryPort.deletarTodos()
        eventPublisherPort.publish(CentrosDistribuicaoDeletadosEvent())
    }

}
