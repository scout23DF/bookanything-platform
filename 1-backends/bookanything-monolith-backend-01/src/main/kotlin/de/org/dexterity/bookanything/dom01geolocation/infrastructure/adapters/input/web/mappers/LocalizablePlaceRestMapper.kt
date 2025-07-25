package de.org.dexterity.bookanything.dom01geolocation.infrastructure.adapters.input.web.mappers

import de.org.dexterity.bookanything.dom01geolocation.application.cqrs.command.CreateLocalizablePlaceCQRSRequest
import de.org.dexterity.bookanything.dom01geolocation.application.cqrs.query.GetByAliasLocalizablePlacesCQRSRequest
import de.org.dexterity.bookanything.dom01geolocation.application.cqrs.query.GetByNearestLocalizablePlacesCQRSRequest
import de.org.dexterity.bookanything.dom01geolocation.domain.models.LocalizablePlaceModel
import de.org.dexterity.bookanything.dom01geolocation.infrastructure.adapters.input.web.dtos.CreateLocalizablePlaceRestRequest
import de.org.dexterity.bookanything.dom01geolocation.infrastructure.adapters.input.web.dtos.LocalizablePlaceRestResponse
import de.org.dexterity.bookanything.shared.annotations.Mapper
import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.geom.GeometryFactory
import org.locationtech.jts.geom.PrecisionModel
import java.util.*


@Mapper
class LocalizablePlaceRestMapper {

    private val geometryFactory = GeometryFactory(PrecisionModel(), 4326)

    fun fromRestRequestToCQRSRequest(sourceRequestDTO: CreateLocalizablePlaceRestRequest): CreateLocalizablePlaceCQRSRequest {

        val pointLocationTmp = geometryFactory.createPoint(Coordinate(sourceRequestDTO.longitude, sourceRequestDTO.latitude))

        return CreateLocalizablePlaceCQRSRequest(
            commandId = UUID.randomUUID(),
            locationName = sourceRequestDTO.name,
            alias = sourceRequestDTO.alias,
            friendlyId = sourceRequestDTO.friendlyId,
            propertiesDetailsMap = sourceRequestDTO.propertiesDetailsMap,
            locationPoint = pointLocationTmp,
        )
    }

    fun fromDomainToRestResponse(sourceDomainModel: LocalizablePlaceModel): LocalizablePlaceRestResponse {
        return LocalizablePlaceRestResponse(
            id = sourceDomainModel.id,
            friendlyId = sourceDomainModel.friendlyId,
            name = sourceDomainModel.name,
            alias = sourceDomainModel.alias,
            propertiesDetailsMap = sourceDomainModel.propertiesDetailsMap,
            latitude = sourceDomainModel.locationPoint.y,
            longitude = sourceDomainModel.locationPoint.x
        )

    }

    fun fromRequestParamsToCQRSRequest(latitude: Double, longitude: Double, raioEmKm: Double): GetByNearestLocalizablePlacesCQRSRequest {

        val pointLocationTmp = geometryFactory.createPoint(Coordinate(longitude, latitude))

        return GetByNearestLocalizablePlacesCQRSRequest(
            commandId = UUID.randomUUID(),
            locationPointRef = pointLocationTmp,
            raioEmKm = raioEmKm
        )

    }

    fun fromRequestParamsToCQRSRequest(aliasPrefix: String): GetByAliasLocalizablePlacesCQRSRequest {

        return GetByAliasLocalizablePlacesCQRSRequest(commandId = UUID.randomUUID(), aliasPrefix)

    }

}
