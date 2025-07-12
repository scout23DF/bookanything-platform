package de.org.dexterity.bookanything.dom01geolocation.application.cqrs.query

import de.org.dexterity.bookanything.dom01geolocation.application.usecases.LocalizablePlaceCRUDUseCase
import de.org.dexterity.bookanything.dom01geolocation.domain.models.LocalizablePlaceModel
import de.org.dexterity.bookanything.shared.mediators.IGenericDataRequest
import de.org.dexterity.bookanything.shared.mediators.IGenericRequestHandler
import org.locationtech.jts.geom.Point
import org.springframework.stereotype.Component
import java.util.*


data class GetAllCentroDistribuicaoCQRSResponse(val centrosDistribuicaoModelList: List<LocalizablePlaceModel>)

data class GetAllCentroDistribuicaoCQRSRequest(
    override val commandId: UUID
) : IGenericDataRequest<GetAllCentroDistribuicaoCQRSResponse?>

@Component
class GetAllCentroDistribuicaoHandler(
    val centroDistribuicaoCRUDUseCase: LocalizablePlaceCRUDUseCase
) : IGenericRequestHandler<GetAllCentroDistribuicaoCQRSRequest, GetAllCentroDistribuicaoCQRSResponse> {

    override fun getRequestType(): Class<GetAllCentroDistribuicaoCQRSRequest> {
        return GetAllCentroDistribuicaoCQRSRequest::class.java
    }

    override fun handleRequest(requestHolder: GetAllCentroDistribuicaoCQRSRequest): GetAllCentroDistribuicaoCQRSResponse? {

        val allFoundModelsList = centroDistribuicaoCRUDUseCase.buscarTodos()

        return GetAllCentroDistribuicaoCQRSResponse(allFoundModelsList)
    }

}


data class GetByIdCentroDistribuicaoCQRSResponse(val localizablePlaceModel: LocalizablePlaceModel?)

data class GetByIdCentroDistribuicaoCQRSRequest(
    override val commandId: UUID,
    val id: UUID
) : IGenericDataRequest<GetByIdCentroDistribuicaoCQRSResponse?>

@Component
class GetByIdCentroDistribuicaoHandler(
    val centroDistribuicaoCRUDUseCase: LocalizablePlaceCRUDUseCase
) : IGenericRequestHandler<GetByIdCentroDistribuicaoCQRSRequest, GetByIdCentroDistribuicaoCQRSResponse> {

    override fun getRequestType(): Class<GetByIdCentroDistribuicaoCQRSRequest> {
        return GetByIdCentroDistribuicaoCQRSRequest::class.java
    }

    override fun handleRequest(requestHolder: GetByIdCentroDistribuicaoCQRSRequest): GetByIdCentroDistribuicaoCQRSResponse? {

        val foundModel = centroDistribuicaoCRUDUseCase.buscarPorId(requestHolder.id)

        return GetByIdCentroDistribuicaoCQRSResponse(foundModel)
    }

}


data class GetByNearestCentroDistribuicaoCQRSResponse(val centrosDistribuicaoModelList: List<LocalizablePlaceModel>)

data class GetByNearestCentroDistribuicaoCQRSRequest(
    override val commandId: UUID,
    val locationPointRef: Point,
    val raioEmKm: Double
) : IGenericDataRequest<GetByNearestCentroDistribuicaoCQRSResponse?>

@Component
class GetByNearestCentroDistribuicaoHandler(
    val centroDistribuicaoCRUDUseCase: LocalizablePlaceCRUDUseCase
) : IGenericRequestHandler<GetByNearestCentroDistribuicaoCQRSRequest, GetByNearestCentroDistribuicaoCQRSResponse> {

    override fun getRequestType(): Class<GetByNearestCentroDistribuicaoCQRSRequest> {
        return GetByNearestCentroDistribuicaoCQRSRequest::class.java
    }

    override fun handleRequest(requestHolder: GetByNearestCentroDistribuicaoCQRSRequest): GetByNearestCentroDistribuicaoCQRSResponse? {

        val allFoundModelsList = centroDistribuicaoCRUDUseCase.buscarCentrosProximos(
            requestHolder.locationPointRef,
            requestHolder.raioEmKm
        )

        return GetByNearestCentroDistribuicaoCQRSResponse(allFoundModelsList)
    }

}
