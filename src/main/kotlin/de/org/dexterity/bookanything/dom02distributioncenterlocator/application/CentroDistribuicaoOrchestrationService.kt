package de.org.dexterity.bookanything.dom02distributioncenterlocator.application

import de.org.dexterity.bookanything.dom02distributioncenterlocator.domain.models.CentroDistribuicaoModel
import de.org.dexterity.bookanything.dom02distributioncenterlocator.domain.ports.EventPublisherPort
import de.org.dexterity.bookanything.dom02distributioncenterlocator.domain.events.CentroDistribuicaoCadastradoEvent
import de.org.dexterity.bookanything.dom02distributioncenterlocator.domain.events.CentroDistribuicaoDeletadoEvent
import de.org.dexterity.bookanything.dom02distributioncenterlocator.domain.events.CentrosDistribuicaoDeletadosEvent
import de.org.dexterity.bookanything.dom02distributioncenterlocator.domain.features.CentroDistribuicaoReadFeaturePort
import de.org.dexterity.bookanything.dom02distributioncenterlocator.domain.features.CentroDistribuicaoWriteFeaturePort
import de.org.dexterity.bookanything.dom02distributioncenterlocator.domain.ports.CentroDistribuicaoPersistRepositoryPort
import de.org.dexterity.bookanything.dom02distributioncenterlocator.domain.ports.CentroDistribuicaoQueryRepositoryPort
import org.locationtech.jts.geom.Point
import java.util.*

class CentroDistribuicaoOrchestrationService(
    private val centroDistribuicaoPersistRepositoryPort: CentroDistribuicaoPersistRepositoryPort,
    private val centroDistribuicaoQueryRepositoryPort: CentroDistribuicaoQueryRepositoryPort,
    private val eventPublisherPort: EventPublisherPort
) : CentroDistribuicaoWriteFeaturePort, CentroDistribuicaoReadFeaturePort {

    override fun cadastrar(nome: String, localizacao: Point): CentroDistribuicaoModel {
        if (centroDistribuicaoPersistRepositoryPort.existsByName(nome)) {
            throw IllegalArgumentException("Centro de Distribuição com o nome '$nome' já existe.")
        }
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
