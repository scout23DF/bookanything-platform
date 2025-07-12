package de.org.dexterity.bookanything.dom01geolocation.application.usecases

import de.org.dexterity.bookanything.dom01geolocation.domain.models.LocalizablePlaceModel
import de.org.dexterity.bookanything.dom01geolocation.domain.ports.EventPublisherPort
import de.org.dexterity.bookanything.dom01geolocation.domain.events.LocalizablePlaceCreatedEvent
import de.org.dexterity.bookanything.dom01geolocation.domain.events.LocalizablePlaceDeletedEvent
import de.org.dexterity.bookanything.dom01geolocation.domain.events.LocalizablePlacesAllDeletedEvent
import de.org.dexterity.bookanything.dom01geolocation.domain.features.LocalizablePlaceReadFeaturePort
import de.org.dexterity.bookanything.dom01geolocation.domain.features.LocalizablePlaceWriteFeaturePort
import de.org.dexterity.bookanything.dom01geolocation.domain.ports.LocalizablePlacePersistRepositoryPort
import de.org.dexterity.bookanything.dom01geolocation.domain.ports.LocalizablePlaceQueryRepositoryPort
import org.locationtech.jts.geom.Point
import java.util.*

class LocalizablePlaceCRUDUseCase(
    private val localizablePlacePersistRepositoryPort: LocalizablePlacePersistRepositoryPort,
    private val localizablePlaceQueryRepositoryPort: LocalizablePlaceQueryRepositoryPort,
    private val eventPublisherPort: EventPublisherPort
) : LocalizablePlaceWriteFeaturePort, LocalizablePlaceReadFeaturePort {

    override fun cadastrar(nome: String, localizacao: Point): LocalizablePlaceModel {
        if (localizablePlacePersistRepositoryPort.existsByName(nome)) {
            throw IllegalArgumentException("Centro de Distribuição com o nome '$nome' já existe.")
        }
        val localizablePlaceModel = LocalizablePlaceModel(
            id = UUID.randomUUID(),
            name = nome,
            locationPoint = localizacao
        )
        val savedCentroDistribuicao = localizablePlacePersistRepositoryPort.salvar(localizablePlaceModel)
        eventPublisherPort.publish(
            LocalizablePlaceCreatedEvent(
                id = savedCentroDistribuicao.id,
                nome = savedCentroDistribuicao.name,
                latitude = savedCentroDistribuicao.locationPoint.y,
                longitude = savedCentroDistribuicao.locationPoint.x
            )
        )
        return savedCentroDistribuicao
    }

    override fun buscarPorId(id: UUID): LocalizablePlaceModel? {
        return localizablePlaceQueryRepositoryPort.buscarPorId(id)
    }

    override fun buscarTodos(): List<LocalizablePlaceModel> {
        return localizablePlaceQueryRepositoryPort.buscarTodos()
    }

    override fun buscarCentrosProximos(localizacao: Point, raioEmKm: Double): List<LocalizablePlaceModel> {
        return localizablePlaceQueryRepositoryPort.buscarCentrosProximos(localizacao, raioEmKm)
    }

    override fun deletarPorId(id: UUID) {
        localizablePlacePersistRepositoryPort.deletarPorId(id)
        eventPublisherPort.publish(LocalizablePlaceDeletedEvent(id))
    }

    override fun synchronizeAll(): Map<String, Int> {
        return this.localizablePlaceQueryRepositoryPort.synchronizeFromWriteRepository(
            this.localizablePlacePersistRepositoryPort.findAllForSync()
        )
    }

    override fun deletarTodos() {
        localizablePlacePersistRepositoryPort.deletarTodos()
        eventPublisherPort.publish(LocalizablePlacesAllDeletedEvent())
    }

}
