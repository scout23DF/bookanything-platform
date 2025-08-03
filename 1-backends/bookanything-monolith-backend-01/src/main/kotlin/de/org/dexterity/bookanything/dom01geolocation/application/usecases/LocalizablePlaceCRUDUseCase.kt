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
import org.springframework.data.elasticsearch.utils.geohash.Geohash
import java.util.*

class LocalizablePlaceCRUDUseCase(
    private val localizablePlacePersistRepositoryPort: LocalizablePlacePersistRepositoryPort,
    private val localizablePlaceQueryRepositoryPort: LocalizablePlaceQueryRepositoryPort,
    private val eventPublisherPort: EventPublisherPort
) : LocalizablePlaceWriteFeaturePort, LocalizablePlaceReadFeaturePort {

    override fun create(
        locationName: String,
        locationAlias: String?,
        friendlyId: String,
        propertiesDetailsMap: Map<String, Any>?,
        locationPoint: Point
    ): LocalizablePlaceModel {

        if (localizablePlacePersistRepositoryPort.existsByName(locationName)) {
            throw IllegalArgumentException("Localizable Please named as: '$locationName' already exists.")
        }

        val calculatedLocationGeohashed = Geohash.stringEncode(locationPoint.x, locationPoint.y, 9)

        val localizablePlaceModel = LocalizablePlaceModel(
            id = UUID.randomUUID(),
            friendlyId = friendlyId,
            name = locationName,
            alias = locationAlias,
            additionalDetailsMap = propertiesDetailsMap,
            locationPoint = locationPoint,
            locationAsGeoHash = calculatedLocationGeohashed
        )



        val savedLocalizablePlace = localizablePlacePersistRepositoryPort.salvar(localizablePlaceModel)
        eventPublisherPort.publish(
            LocalizablePlaceCreatedEvent(
                id = savedLocalizablePlace.id,
                friendlyId = savedLocalizablePlace.friendlyId,
                name = savedLocalizablePlace.name,
                alias = savedLocalizablePlace.alias,
                propertiesDetailsMap = savedLocalizablePlace.additionalDetailsMap,
                latitude = savedLocalizablePlace.locationPoint?.y ?: 0.0,
                longitude = savedLocalizablePlace.locationPoint?.x ?: 0.0,
                locationAsGeoHash = savedLocalizablePlace.locationAsGeoHash
            )
        )
        return savedLocalizablePlace
    }

    override fun searchById(id: UUID): LocalizablePlaceModel? {
        return localizablePlaceQueryRepositoryPort.searchById(id)
    }

    override fun searchAll(): List<LocalizablePlaceModel> {
        return localizablePlaceQueryRepositoryPort.searchAll()
    }

    override fun searchNearestLocalizablePlaces(localizacao: Point, raioEmKm: Double): List<LocalizablePlaceModel> {
        return localizablePlaceQueryRepositoryPort.searchByNearest(localizacao, raioEmKm)
    }

    override fun searchByAliasStartingWith(searchedAlias: String): List<LocalizablePlaceModel> {
        return localizablePlaceQueryRepositoryPort.searchByAliasStartingWith(searchedAlias)
    }

    override fun removeById(id: UUID) {
        localizablePlacePersistRepositoryPort.deletarPorId(id)
        eventPublisherPort.publish(LocalizablePlaceDeletedEvent(id))
    }

    override fun synchronizeAll(): Map<String, Int> {
        return this.localizablePlaceQueryRepositoryPort.synchronizeFromWriteRepository(
            this.localizablePlacePersistRepositoryPort.findAllForSync()
        )
    }

    override fun removeAll() {
        localizablePlacePersistRepositoryPort.deletarTodos()
        eventPublisherPort.publish(LocalizablePlacesAllDeletedEvent())
    }

    fun findByFriendlyIdContaining(friendlyId: String): List<LocalizablePlaceModel> {
        return localizablePlaceQueryRepositoryPort.searchByFriendlyIdContaining(friendlyId)
    }

    fun findByPropertiesDetailsMapContains(key: String, value: String): List<LocalizablePlaceModel> {
        return localizablePlaceQueryRepositoryPort.searchByPropertiesDetailsMapContains(key, value)
    }

}
