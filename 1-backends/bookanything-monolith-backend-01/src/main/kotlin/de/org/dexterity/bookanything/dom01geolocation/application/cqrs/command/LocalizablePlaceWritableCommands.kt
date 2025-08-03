package de.org.dexterity.bookanything.dom01geolocation.application.cqrs.command

import org.locationtech.jts.geom.Point
import de.org.dexterity.bookanything.dom01geolocation.application.usecases.LocalizablePlaceCRUDUseCase
import de.org.dexterity.bookanything.dom01geolocation.application.usecases.GeoJsonFileManagerUseCase
import de.org.dexterity.bookanything.dom01geolocation.domain.models.LocalizablePlaceModel
import de.org.dexterity.bookanything.shared.mediators.IGenericDataRequest
import de.org.dexterity.bookanything.shared.mediators.IGenericRequestHandler
import org.springframework.stereotype.Component
import org.springframework.web.multipart.MultipartFile
import java.util.*

data class CreateLocalizablePlaceCQRSResponse(val localizablePlaceModel: LocalizablePlaceModel)

data class CreateLocalizablePlaceCQRSRequest(
    override val commandId: UUID,
    val locationName: String,
    val alias: String?,
    val friendlyId: String,
    val additionalDetailsMap: Map<String, Any>?,
    val locationPoint: Point
) : IGenericDataRequest<CreateLocalizablePlaceCQRSResponse?>

@Component
class CreateCentroDistribuicaoHandler(
    val centroDistribuicaoCRUDUseCase: LocalizablePlaceCRUDUseCase
) : IGenericRequestHandler<CreateLocalizablePlaceCQRSRequest, CreateLocalizablePlaceCQRSResponse> {

    override fun getRequestType(): Class<CreateLocalizablePlaceCQRSRequest> {
        return CreateLocalizablePlaceCQRSRequest::class.java
    }

    override fun handleRequest(requestHolder: CreateLocalizablePlaceCQRSRequest): CreateLocalizablePlaceCQRSResponse? {
        val centroDistribuicaoModelResult = centroDistribuicaoCRUDUseCase.create(
            requestHolder.locationName,
            requestHolder.alias,
            requestHolder.friendlyId,
            requestHolder.additionalDetailsMap,
            requestHolder.locationPoint
        )

        return CreateLocalizablePlaceCQRSResponse(centroDistribuicaoModelResult)
    }

}


data class RemoveByIdCentroDistribuicaoCQRSRequest(
    override val commandId: UUID,
    val id: UUID
) : IGenericDataRequest<Void?>

@Component
class RemoveByIdCentroDistribuicaoHandler(
    val centroDistribuicaoCRUDUseCase: LocalizablePlaceCRUDUseCase
) : IGenericRequestHandler<RemoveByIdCentroDistribuicaoCQRSRequest, Void> {

    override fun getRequestType(): Class<RemoveByIdCentroDistribuicaoCQRSRequest> {
        return RemoveByIdCentroDistribuicaoCQRSRequest::class.java
    }

    override fun handleRequest(requestHolder: RemoveByIdCentroDistribuicaoCQRSRequest): Void? {

        centroDistribuicaoCRUDUseCase.removeById(requestHolder.id)

        return null
    }

}


data class RemoveAllCentroDistribuicaoCQRSRequest(
    override val commandId: UUID
) : IGenericDataRequest<Void?>

@Component
class RemoveAllCentroDistribuicaoHandler(
    val centroDistribuicaoCRUDUseCase: LocalizablePlaceCRUDUseCase
) : IGenericRequestHandler<RemoveAllCentroDistribuicaoCQRSRequest, Void> {

    override fun getRequestType(): Class<RemoveAllCentroDistribuicaoCQRSRequest> {
        return RemoveAllCentroDistribuicaoCQRSRequest::class.java
    }

    override fun handleRequest(requestHolder: RemoveAllCentroDistribuicaoCQRSRequest): Void? {

        centroDistribuicaoCRUDUseCase.removeAll()

        return null
    }

}


data class SynchronizeCentroDistribuicaoCQRSResponse(val resultSyncMap : Map<String, Int>)

data class SynchronizeCentroDistribuicaoCQRSRequest(
    override val commandId: UUID
) : IGenericDataRequest<SynchronizeCentroDistribuicaoCQRSResponse?>

@Component
class SynchronizeCentroDistribuicaoHandler(
    val centroDistribuicaoCRUDUseCase: LocalizablePlaceCRUDUseCase
) : IGenericRequestHandler<SynchronizeCentroDistribuicaoCQRSRequest, SynchronizeCentroDistribuicaoCQRSResponse> {

    override fun getRequestType(): Class<SynchronizeCentroDistribuicaoCQRSRequest> {
        return SynchronizeCentroDistribuicaoCQRSRequest::class.java
    }

    override fun handleRequest(requestHolder: SynchronizeCentroDistribuicaoCQRSRequest): SynchronizeCentroDistribuicaoCQRSResponse? {

        val syncResultMap = centroDistribuicaoCRUDUseCase.synchronizeAll()

        return SynchronizeCentroDistribuicaoCQRSResponse(syncResultMap)
    }

}


data class UploadGeoJsonCentroDistribuicaoCQRSResponse(val resultUploadMap : Map<String, String>)

data class UploadGeoJsonCentroDistribuicaoCQRSRequest(
    override val commandId: UUID,
    val contentDataType: String,
    val uploadedGeoJSONFile: MultipartFile
) : IGenericDataRequest<UploadGeoJsonCentroDistribuicaoCQRSResponse?>

@Component
class UploadGeoJsonCentroDistribuicaoHandler(
    val geoJsonFileManagerUseCase: GeoJsonFileManagerUseCase
) : IGenericRequestHandler<UploadGeoJsonCentroDistribuicaoCQRSRequest, UploadGeoJsonCentroDistribuicaoCQRSResponse> {

    override fun getRequestType(): Class<UploadGeoJsonCentroDistribuicaoCQRSRequest> {
        return UploadGeoJsonCentroDistribuicaoCQRSRequest::class.java
    }

    override fun handleRequest(requestHolder: UploadGeoJsonCentroDistribuicaoCQRSRequest): UploadGeoJsonCentroDistribuicaoCQRSResponse? {

        val syncResultMap = geoJsonFileManagerUseCase.processGeoJsonFile(
            requestHolder.contentDataType,
            requestHolder.uploadedGeoJSONFile
        )

        return UploadGeoJsonCentroDistribuicaoCQRSResponse(syncResultMap)
    }

}
