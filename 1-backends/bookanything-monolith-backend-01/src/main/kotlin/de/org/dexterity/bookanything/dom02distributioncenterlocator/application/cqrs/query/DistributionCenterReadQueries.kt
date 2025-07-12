package de.org.dexterity.bookanything.dom02distributioncenterlocator.application.cqrs.query

import de.org.dexterity.bookanything.dom02distributioncenterlocator.application.CentroDistribuicaoOrchestrationService
import de.org.dexterity.bookanything.dom02distributioncenterlocator.domain.models.CentroDistribuicaoModel
import de.org.dexterity.bookanything.shared.mediators.IGenericDataRequest
import de.org.dexterity.bookanything.shared.mediators.IGenericRequestHandler
import org.locationtech.jts.geom.Point
import org.springframework.stereotype.Component
import java.util.*


data class GetAllCentroDistribuicaoCQRSResponse(val centrosDistribuicaoModelList: List<CentroDistribuicaoModel>)

data class GetAllCentroDistribuicaoCQRSRequest(
    override val commandId: UUID
) : IGenericDataRequest<GetAllCentroDistribuicaoCQRSResponse?>

@Component
class GetAllCentroDistribuicaoHandler(
    val centroDistribuicaoOrchestrationService: CentroDistribuicaoOrchestrationService
) : IGenericRequestHandler<GetAllCentroDistribuicaoCQRSRequest, GetAllCentroDistribuicaoCQRSResponse> {

    override fun getRequestType(): Class<GetAllCentroDistribuicaoCQRSRequest> {
        return GetAllCentroDistribuicaoCQRSRequest::class.java
    }

    override fun handleRequest(requestHolder: GetAllCentroDistribuicaoCQRSRequest): GetAllCentroDistribuicaoCQRSResponse? {

        val allFoundModelsList = centroDistribuicaoOrchestrationService.buscarTodos()

        return GetAllCentroDistribuicaoCQRSResponse(allFoundModelsList)
    }

}


data class GetByIdCentroDistribuicaoCQRSResponse(val centroDistribuicaoModel: CentroDistribuicaoModel?)

data class GetByIdCentroDistribuicaoCQRSRequest(
    override val commandId: UUID,
    val id: UUID
) : IGenericDataRequest<GetByIdCentroDistribuicaoCQRSResponse?>

@Component
class GetByIdCentroDistribuicaoHandler(
    val centroDistribuicaoOrchestrationService: CentroDistribuicaoOrchestrationService
) : IGenericRequestHandler<GetByIdCentroDistribuicaoCQRSRequest, GetByIdCentroDistribuicaoCQRSResponse> {

    override fun getRequestType(): Class<GetByIdCentroDistribuicaoCQRSRequest> {
        return GetByIdCentroDistribuicaoCQRSRequest::class.java
    }

    override fun handleRequest(requestHolder: GetByIdCentroDistribuicaoCQRSRequest): GetByIdCentroDistribuicaoCQRSResponse? {

        val foundModel = centroDistribuicaoOrchestrationService.buscarPorId(requestHolder.id)

        return GetByIdCentroDistribuicaoCQRSResponse(foundModel)
    }

}


data class GetByNearestCentroDistribuicaoCQRSResponse(val centrosDistribuicaoModelList: List<CentroDistribuicaoModel>)

data class GetByNearestCentroDistribuicaoCQRSRequest(
    override val commandId: UUID,
    val locationPointRef: Point,
    val raioEmKm: Double
) : IGenericDataRequest<GetByNearestCentroDistribuicaoCQRSResponse?>

@Component
class GetByNearestCentroDistribuicaoHandler(
    val centroDistribuicaoOrchestrationService: CentroDistribuicaoOrchestrationService
) : IGenericRequestHandler<GetByNearestCentroDistribuicaoCQRSRequest, GetByNearestCentroDistribuicaoCQRSResponse> {

    override fun getRequestType(): Class<GetByNearestCentroDistribuicaoCQRSRequest> {
        return GetByNearestCentroDistribuicaoCQRSRequest::class.java
    }

    override fun handleRequest(requestHolder: GetByNearestCentroDistribuicaoCQRSRequest): GetByNearestCentroDistribuicaoCQRSResponse? {

        val allFoundModelsList = centroDistribuicaoOrchestrationService.buscarCentrosProximos(
            requestHolder.locationPointRef,
            requestHolder.raioEmKm
        )

        return GetByNearestCentroDistribuicaoCQRSResponse(allFoundModelsList)
    }

}
