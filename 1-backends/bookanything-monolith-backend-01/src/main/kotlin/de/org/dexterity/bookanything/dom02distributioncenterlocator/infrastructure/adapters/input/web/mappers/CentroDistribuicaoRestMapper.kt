package de.org.dexterity.bookanything.dom02distributioncenterlocator.infrastructure.adapters.input.web.mappers

import de.org.dexterity.bookanything.dom02distributioncenterlocator.application.cqrs.command.CreateCentroDistribuicaoCQRSRequest
import de.org.dexterity.bookanything.dom02distributioncenterlocator.application.cqrs.query.GetByNearestCentroDistribuicaoCQRSRequest
import de.org.dexterity.bookanything.dom02distributioncenterlocator.domain.models.CentroDistribuicaoModel
import de.org.dexterity.bookanything.dom02distributioncenterlocator.infrastructure.adapters.input.web.dtos.CentroDistribuicaoRestResponse
import de.org.dexterity.bookanything.dom02distributioncenterlocator.infrastructure.adapters.input.web.dtos.CreateCentroDistribuicaoRestRequest
import de.org.dexterity.bookanything.shared.annotations.Mapper
import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.geom.GeometryFactory
import org.locationtech.jts.geom.PrecisionModel
import java.util.*


@Mapper
class CentroDistribuicaoRestMapper {

    private val geometryFactory = GeometryFactory(PrecisionModel(), 4326)

    fun fromRestRequestToCQRSRequest(sourceRequestDTO: CreateCentroDistribuicaoRestRequest): CreateCentroDistribuicaoCQRSRequest {

        val pointLocationTmp = geometryFactory.createPoint(Coordinate(sourceRequestDTO.longitude, sourceRequestDTO.latitude))

        return CreateCentroDistribuicaoCQRSRequest(
            commandId = UUID.randomUUID(),
            locationName = sourceRequestDTO.nome,
            locationPoint = pointLocationTmp,
        )
    }

    fun fromDomainToRestResponse(sourceDomainModel: CentroDistribuicaoModel): CentroDistribuicaoRestResponse {
        return CentroDistribuicaoRestResponse(
            id = sourceDomainModel.id,
            nome = sourceDomainModel.nome,
            latitude = sourceDomainModel.localizacao.y,
            longitude = sourceDomainModel.localizacao.x
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
