package de.org.dexterity.bookanything.dom01geolocation.infrastructure.adapters.input.web.mappers

import de.org.dexterity.bookanything.dom01geolocation.application.cqrs.command.CreateCentroDistribuicaoCQRSRequest
import de.org.dexterity.bookanything.dom01geolocation.application.cqrs.query.GetByNearestCentroDistribuicaoCQRSRequest
import de.org.dexterity.bookanything.dom01geolocation.domain.models.LocalizablePlaceModel
import de.org.dexterity.bookanything.dom01geolocation.infrastructure.adapters.input.web.dtos.LocalizablePlaceRestResponse
import de.org.dexterity.bookanything.dom01geolocation.infrastructure.adapters.input.web.dtos.CreateLocalizablePlaceRestRequest
import de.org.dexterity.bookanything.shared.annotations.Mapper
import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.geom.GeometryFactory
import org.locationtech.jts.geom.PrecisionModel
import java.util.*


@Mapper
class LocalizablePlaceRestMapper {

    private val geometryFactory = GeometryFactory(PrecisionModel(), 4326)

    fun fromRestRequestToCQRSRequest(sourceRequestDTO: CreateLocalizablePlaceRestRequest): CreateCentroDistribuicaoCQRSRequest {

        val pointLocationTmp = geometryFactory.createPoint(Coordinate(sourceRequestDTO.longitude, sourceRequestDTO.latitude))

        return CreateCentroDistribuicaoCQRSRequest(
            commandId = UUID.randomUUID(),
            locationName = sourceRequestDTO.name,
            locationPoint = pointLocationTmp,
        )
    }

    fun fromDomainToRestResponse(sourceDomainModel: LocalizablePlaceModel): LocalizablePlaceRestResponse {
        return LocalizablePlaceRestResponse(
            id = sourceDomainModel.id,
            name = sourceDomainModel.name,
            latitude = sourceDomainModel.locationPoint.y,
            longitude = sourceDomainModel.locationPoint.x
        )

    }

    fun fromRequestParamsToCQRSRequest(latitude: Double, longitude: Double, raioEmKm: Double): GetByNearestCentroDistribuicaoCQRSRequest {

        val pointLocationTmp = geometryFactory.createPoint(Coordinate(longitude, latitude))

        return GetByNearestCentroDistribuicaoCQRSRequest(
            commandId = UUID.randomUUID(),
            locationPointRef = pointLocationTmp,
            raioEmKm = raioEmKm
        )

    }

}
