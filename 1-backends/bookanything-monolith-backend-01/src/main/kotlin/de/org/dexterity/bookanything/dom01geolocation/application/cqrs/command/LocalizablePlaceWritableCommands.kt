package de.org.dexterity.bookanything.dom01geolocation.application.cqrs.command

import de.org.dexterity.bookanything.dom01geolocation.application.usecases.LocalizablePlaceCRUDUseCase
import de.org.dexterity.bookanything.dom01geolocation.application.usecases.GeoJsonFileManagerUseCase
import de.org.dexterity.bookanything.dom01geolocation.domain.models.LocalizablePlaceModel
import de.org.dexterity.bookanything.shared.mediators.IGenericDataRequest
import de.org.dexterity.bookanything.shared.mediators.IGenericRequestHandler
import org.locationtech.jts.geom.Point
import org.springframework.stereotype.Component
import org.springframework.web.multipart.MultipartFile
import java.util.*

data class CreateCentroDistribuicaoCQRSResponse(val localizablePlaceModel: LocalizablePlaceModel)

data class CreateCentroDistribuicaoCQRSRequest(
    override val commandId: UUID,
    val locationName: String,
    val locationPoint: Point
) : IGenericDataRequest<CreateCentroDistribuicaoCQRSResponse?>

@Component
class CreateCentroDistribuicaoHandler(
    val centroDistribuicaoCRUDUseCase: LocalizablePlaceCRUDUseCase
) : IGenericRequestHandler<CreateCentroDistribuicaoCQRSRequest, CreateCentroDistribuicaoCQRSResponse> {

    override fun getRequestType(): Class<CreateCentroDistribuicaoCQRSRequest> {
        return CreateCentroDistribuicaoCQRSRequest::class.java
    }

    override fun handleRequest(requestHolder: CreateCentroDistribuicaoCQRSRequest): CreateCentroDistribuicaoCQRSResponse? {
        val centroDistribuicaoModelResult = centroDistribuicaoCRUDUseCase.cadastrar(
            requestHolder.locationName,
            requestHolder.locationPoint
        )

        return CreateCentroDistribuicaoCQRSResponse(centroDistribuicaoModelResult)
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

        centroDistribuicaoCRUDUseCase.deletarPorId(requestHolder.id)

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

        centroDistribuicaoCRUDUseCase.deletarTodos()

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
