package de.org.dexterity.bookanything.dom01geolocation.application.cqrs.query

import org.locationtech.jts.geom.Point
import de.org.dexterity.bookanything.dom01geolocation.application.usecases.LocalizablePlaceCRUDUseCase
import de.org.dexterity.bookanything.dom01geolocation.domain.models.LocalizablePlaceModel
import de.org.dexterity.bookanything.shared.mediators.IGenericDataRequest
import de.org.dexterity.bookanything.shared.mediators.IGenericRequestHandler
import org.springframework.stereotype.Component
import java.util.*


data class GetAllLocalizablePlacesCQRSResponse(val localizablePlacesModelsList: List<LocalizablePlaceModel>)

data class GetAllLocalizablePlacesCQRSRequest(
    override val commandId: UUID
) : IGenericDataRequest<GetAllLocalizablePlacesCQRSResponse?>

@Component
class GetAllLocalizablePlacesHandler(
    val localizablePlaceCRUDUseCase: LocalizablePlaceCRUDUseCase
) : IGenericRequestHandler<GetAllLocalizablePlacesCQRSRequest, GetAllLocalizablePlacesCQRSResponse> {

    override fun getRequestType(): Class<GetAllLocalizablePlacesCQRSRequest> {
        return GetAllLocalizablePlacesCQRSRequest::class.java
    }

    override fun handleRequest(requestHolder: GetAllLocalizablePlacesCQRSRequest): GetAllLocalizablePlacesCQRSResponse? {

        val allFoundModelsList = localizablePlaceCRUDUseCase.searchAll()

        return GetAllLocalizablePlacesCQRSResponse(allFoundModelsList)
    }

}


data class GetByIdLocalizablePlaceCQRSResponse(val localizablePlaceModel: LocalizablePlaceModel?)

data class GetByIdLocalizablePlaceCQRSRequest(
    override val commandId: UUID,
    val id: UUID
) : IGenericDataRequest<GetByIdLocalizablePlaceCQRSResponse?>

@Component
class GetByIdLocalizablePlaceHandler(
    val localizablePlaceCRUDUseCase: LocalizablePlaceCRUDUseCase
) : IGenericRequestHandler<GetByIdLocalizablePlaceCQRSRequest, GetByIdLocalizablePlaceCQRSResponse> {

    override fun getRequestType(): Class<GetByIdLocalizablePlaceCQRSRequest> {
        return GetByIdLocalizablePlaceCQRSRequest::class.java
    }

    override fun handleRequest(requestHolder: GetByIdLocalizablePlaceCQRSRequest): GetByIdLocalizablePlaceCQRSResponse? {

        val foundModel = localizablePlaceCRUDUseCase.searchById(requestHolder.id)

        return GetByIdLocalizablePlaceCQRSResponse(foundModel)
    }

}


data class GetByNearestLocalizablePlacesCQRSResponse(val localizablePlacesModelsList: List<LocalizablePlaceModel>)

data class GetByNearestLocalizablePlacesCQRSRequest(
    override val commandId: UUID,
    val locationPointRef: Point,
    val raioEmKm: Double
) : IGenericDataRequest<GetByNearestLocalizablePlacesCQRSResponse?>

@Component
class GetByNearestLocalizablePlacesHandler(
    val localizablePlaceCRUDUseCase: LocalizablePlaceCRUDUseCase
) : IGenericRequestHandler<GetByNearestLocalizablePlacesCQRSRequest, GetByNearestLocalizablePlacesCQRSResponse> {

    override fun getRequestType(): Class<GetByNearestLocalizablePlacesCQRSRequest> {
        return GetByNearestLocalizablePlacesCQRSRequest::class.java
    }

    override fun handleRequest(requestHolder: GetByNearestLocalizablePlacesCQRSRequest): GetByNearestLocalizablePlacesCQRSResponse? {

        val allFoundModelsList = localizablePlaceCRUDUseCase.searchNearestLocalizablePlaces(
            requestHolder.locationPointRef,
            requestHolder.raioEmKm
        )

        return GetByNearestLocalizablePlacesCQRSResponse(allFoundModelsList)
    }

}


data class GetByAliasLocalizablePlacesCQRSResponse(val localizablePlacesModelsList: List<LocalizablePlaceModel>)

data class GetByAliasLocalizablePlacesCQRSRequest(
    override val commandId: UUID,
    val searchedAliasPrefix: String
) : IGenericDataRequest<GetByAliasLocalizablePlacesCQRSResponse?>

@Component
class GetByAliasCentroDistribuicaoHandler(
    val localizablePlaceCRUDUseCase: LocalizablePlaceCRUDUseCase
) : IGenericRequestHandler<GetByAliasLocalizablePlacesCQRSRequest, GetByAliasLocalizablePlacesCQRSResponse> {

    override fun getRequestType(): Class<GetByAliasLocalizablePlacesCQRSRequest> {
        return GetByAliasLocalizablePlacesCQRSRequest::class.java
    }

    override fun handleRequest(requestHolder: GetByAliasLocalizablePlacesCQRSRequest): GetByAliasLocalizablePlacesCQRSResponse? {

        val allFoundModelsList = localizablePlaceCRUDUseCase.searchByAliasStartingWith(requestHolder.searchedAliasPrefix)

        return GetByAliasLocalizablePlacesCQRSResponse(allFoundModelsList)
    }

}

data class GetByFriendlyIdLocalizablePlacesCQRSResponse(val localizablePlacesModelsList: List<LocalizablePlaceModel>)

data class GetByFriendlyIdLocalizablePlacesCQRSRequest(
    override val commandId: UUID,
    val friendlyId: String
) : IGenericDataRequest<GetByFriendlyIdLocalizablePlacesCQRSResponse?>

@Component
class GetByFriendlyIdLocalizablePlacesHandler(
    val localizablePlaceCRUDUseCase: LocalizablePlaceCRUDUseCase
) : IGenericRequestHandler<GetByFriendlyIdLocalizablePlacesCQRSRequest, GetByFriendlyIdLocalizablePlacesCQRSResponse> {

    override fun getRequestType(): Class<GetByFriendlyIdLocalizablePlacesCQRSRequest> {
        return GetByFriendlyIdLocalizablePlacesCQRSRequest::class.java
    }

    override fun handleRequest(requestHolder: GetByFriendlyIdLocalizablePlacesCQRSRequest): GetByFriendlyIdLocalizablePlacesCQRSResponse? {
        val allFoundModelsList = localizablePlaceCRUDUseCase.findByFriendlyIdContaining(requestHolder.friendlyId)
        return GetByFriendlyIdLocalizablePlacesCQRSResponse(allFoundModelsList)
    }

}

data class GetByPropertyLocalizablePlacesCQRSResponse(val localizablePlacesModelsList: List<LocalizablePlaceModel>)

data class GetByPropertyLocalizablePlacesCQRSRequest(
    override val commandId: UUID,
    val key: String,
    val value: String
) : IGenericDataRequest<GetByPropertyLocalizablePlacesCQRSResponse?>

@Component
class GetByPropertyLocalizablePlacesHandler(
    val localizablePlaceCRUDUseCase: LocalizablePlaceCRUDUseCase
) : IGenericRequestHandler<GetByPropertyLocalizablePlacesCQRSRequest, GetByPropertyLocalizablePlacesCQRSResponse> {

    override fun getRequestType(): Class<GetByPropertyLocalizablePlacesCQRSRequest> {
        return GetByPropertyLocalizablePlacesCQRSRequest::class.java
    }

    override fun handleRequest(requestHolder: GetByPropertyLocalizablePlacesCQRSRequest): GetByPropertyLocalizablePlacesCQRSResponse? {
        val allFoundModelsList = localizablePlaceCRUDUseCase.findByPropertiesDetailsMapContains(requestHolder.key, requestHolder.value)
        return GetByPropertyLocalizablePlacesCQRSResponse(allFoundModelsList)
    }

}
