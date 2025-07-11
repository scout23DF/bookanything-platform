package de.org.dexterity.bookanything.dom02distributioncenterlocator.application.cqrs.command

import de.org.dexterity.bookanything.dom02distributioncenterlocator.application.CentroDistribuicaoOrchestrationService
import de.org.dexterity.bookanything.dom02distributioncenterlocator.application.GeoJsonProcessingService
import de.org.dexterity.bookanything.dom02distributioncenterlocator.domain.models.CentroDistribuicaoModel
import de.org.dexterity.bookanything.shared.mediators.IGenericDataRequest
import de.org.dexterity.bookanything.shared.mediators.IGenericRequestHandler
import org.locationtech.jts.geom.Point
import org.springframework.stereotype.Component
import org.springframework.web.multipart.MultipartFile
import java.util.*

data class CreateCentroDistribuicaoCQRSResponse(val centroDistribuicaoModel: CentroDistribuicaoModel)

data class CreateCentroDistribuicaoCQRSRequest(
    override val commandId: UUID,
    val locationName: String,
    val locationPoint: Point
) : IGenericDataRequest<CreateCentroDistribuicaoCQRSResponse?>

@Component
class CreateCentroDistribuicaoHandler(
    val centroDistribuicaoOrchestrationService: CentroDistribuicaoOrchestrationService
) : IGenericRequestHandler<CreateCentroDistribuicaoCQRSRequest, CreateCentroDistribuicaoCQRSResponse> {

    override fun getRequestType(): Class<CreateCentroDistribuicaoCQRSRequest> {
        return CreateCentroDistribuicaoCQRSRequest::class.java
    }

    override fun handleRequest(requestHolder: CreateCentroDistribuicaoCQRSRequest): CreateCentroDistribuicaoCQRSResponse? {
        val centroDistribuicaoModelResult = centroDistribuicaoOrchestrationService.cadastrar(
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
    val centroDistribuicaoOrchestrationService: CentroDistribuicaoOrchestrationService
) : IGenericRequestHandler<RemoveByIdCentroDistribuicaoCQRSRequest, Void> {

    override fun getRequestType(): Class<RemoveByIdCentroDistribuicaoCQRSRequest> {
        return RemoveByIdCentroDistribuicaoCQRSRequest::class.java
    }

    override fun handleRequest(requestHolder: RemoveByIdCentroDistribuicaoCQRSRequest): Void? {

        centroDistribuicaoOrchestrationService.deletarPorId(requestHolder.id)

        return null
    }

}


data class RemoveAllCentroDistribuicaoCQRSRequest(
    override val commandId: UUID
) : IGenericDataRequest<Void?>

@Component
class RemoveAllCentroDistribuicaoHandler(
    val centroDistribuicaoOrchestrationService: CentroDistribuicaoOrchestrationService
) : IGenericRequestHandler<RemoveAllCentroDistribuicaoCQRSRequest, Void> {

    override fun getRequestType(): Class<RemoveAllCentroDistribuicaoCQRSRequest> {
        return RemoveAllCentroDistribuicaoCQRSRequest::class.java
    }

    override fun handleRequest(requestHolder: RemoveAllCentroDistribuicaoCQRSRequest): Void? {

        centroDistribuicaoOrchestrationService.deletarTodos()

        return null
    }

}


data class SynchronizeCentroDistribuicaoCQRSResponse(val resultSyncMap : Map<String, Int>)

data class SynchronizeCentroDistribuicaoCQRSRequest(
    override val commandId: UUID
) : IGenericDataRequest<SynchronizeCentroDistribuicaoCQRSResponse?>

@Component
class SynchronizeCentroDistribuicaoHandler(
    val centroDistribuicaoOrchestrationService: CentroDistribuicaoOrchestrationService
) : IGenericRequestHandler<SynchronizeCentroDistribuicaoCQRSRequest, SynchronizeCentroDistribuicaoCQRSResponse> {

    override fun getRequestType(): Class<SynchronizeCentroDistribuicaoCQRSRequest> {
        return SynchronizeCentroDistribuicaoCQRSRequest::class.java
    }

    override fun handleRequest(requestHolder: SynchronizeCentroDistribuicaoCQRSRequest): SynchronizeCentroDistribuicaoCQRSResponse? {

        val syncResultMap = centroDistribuicaoOrchestrationService.synchronizeAll()

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
    val geoJsonProcessingService: GeoJsonProcessingService
) : IGenericRequestHandler<UploadGeoJsonCentroDistribuicaoCQRSRequest, UploadGeoJsonCentroDistribuicaoCQRSResponse> {

    override fun getRequestType(): Class<UploadGeoJsonCentroDistribuicaoCQRSRequest> {
        return UploadGeoJsonCentroDistribuicaoCQRSRequest::class.java
    }

    override fun handleRequest(requestHolder: UploadGeoJsonCentroDistribuicaoCQRSRequest): UploadGeoJsonCentroDistribuicaoCQRSResponse? {

        val syncResultMap = geoJsonProcessingService.processGeoJsonFile(
            requestHolder.contentDataType,
            requestHolder.uploadedGeoJSONFile
        )

        return UploadGeoJsonCentroDistribuicaoCQRSResponse(syncResultMap)
    }

}
