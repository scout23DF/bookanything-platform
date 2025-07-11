package de.org.dexterity.bookanything.dom02distributioncenterlocator.infrastructure.adapters.input.web.controllers

import de.org.dexterity.bookanything.dom02distributioncenterlocator.application.cqrs.command.*
import de.org.dexterity.bookanything.dom02distributioncenterlocator.application.cqrs.query.*
import de.org.dexterity.bookanything.dom02distributioncenterlocator.infrastructure.adapters.input.web.dtos.CentroDistribuicaoRestResponse
import de.org.dexterity.bookanything.dom02distributioncenterlocator.infrastructure.adapters.input.web.dtos.CreateCentroDistribuicaoRestRequest
import de.org.dexterity.bookanything.dom02distributioncenterlocator.infrastructure.adapters.input.web.mappers.CentroDistribuicaoRestMapper
import de.org.dexterity.bookanything.shared.mediators.HandlersMediatorManager
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile
import org.springframework.web.servlet.support.ServletUriComponentsBuilder
import java.util.*

@RestController
@RequestMapping("/cds")
class CentroDistribuicaoController(
    private val handlerMediatorManager: HandlersMediatorManager,
    private val centroDistribuicaoRestMapper: CentroDistribuicaoRestMapper
) {

    @PostMapping
    fun create(@RequestBody restRequest: CreateCentroDistribuicaoRestRequest): ResponseEntity<CentroDistribuicaoRestResponse> {

        val cqrsCommandRequest : CreateCentroDistribuicaoCQRSRequest = centroDistribuicaoRestMapper.fromRestRequestToCQRSRequest(restRequest)

        val cqrsCommandResponse : CreateCentroDistribuicaoCQRSResponse? = handlerMediatorManager.dispatch(cqrsCommandRequest)

        val uri = ServletUriComponentsBuilder.fromCurrentRequest()
            .path("/{id}")
            .buildAndExpand(cqrsCommandResponse?.centroDistribuicaoModel?.id)
            .toUri()

        return ResponseEntity.created(uri).body(
            centroDistribuicaoRestMapper.fromDomainToRestResponse(cqrsCommandResponse!!.centroDistribuicaoModel)
        )

    }

    @GetMapping("/all")
    fun searchAll(): ResponseEntity<List<CentroDistribuicaoRestResponse>> {

        val cqrsCommandRequest = GetAllCentroDistribuicaoCQRSRequest(
            commandId = UUID.randomUUID()
        )

        val cqrsCommandResponse : GetAllCentroDistribuicaoCQRSResponse? = handlerMediatorManager.dispatch(cqrsCommandRequest)

        val responseList = cqrsCommandResponse?.centrosDistribuicaoModelList?.map {
            centroDistribuicaoRestMapper.fromDomainToRestResponse(it)
        }
        return ResponseEntity.ok(responseList)
    }

    @GetMapping("/{id}")
    fun searchById(@PathVariable id: UUID): ResponseEntity<CentroDistribuicaoRestResponse> {

        val cqrsCommandRequest = GetByIdCentroDistribuicaoCQRSRequest(
            commandId = UUID.randomUUID(),
            id = id
        )

        val cqrsCommandResponse : GetByIdCentroDistribuicaoCQRSResponse? = handlerMediatorManager.dispatch(cqrsCommandRequest)

        return if (cqrsCommandResponse?.centroDistribuicaoModel != null) {
            ResponseEntity.ok(centroDistribuicaoRestMapper.fromDomainToRestResponse(cqrsCommandResponse.centroDistribuicaoModel))
        } else {
            ResponseEntity.notFound().build()
        }

    }

    @GetMapping("/search-nearby")
    fun searchByNearest(
        @RequestParam latitude: Double,
        @RequestParam longitude: Double,
        @RequestParam raioEmKm: Double
    ): ResponseEntity<List<CentroDistribuicaoRestResponse>> {

        val cqrsCommandRequest : GetByNearestCentroDistribuicaoCQRSRequest = centroDistribuicaoRestMapper.fromRequestParamsToCQRSRequest(
            latitude,
            longitude,
            raioEmKm
        )

        val cqrsCommandResponse : GetByNearestCentroDistribuicaoCQRSResponse? = handlerMediatorManager.dispatch(cqrsCommandRequest)

        val responseList = cqrsCommandResponse?.centrosDistribuicaoModelList?.map {
            centroDistribuicaoRestMapper.fromDomainToRestResponse(it)
        }
        return ResponseEntity.ok(responseList)

    }

    @DeleteMapping("/{id}")
    fun removeById(@PathVariable id: UUID): ResponseEntity<Void> {

        val cqrsCommandRequest = RemoveByIdCentroDistribuicaoCQRSRequest(
            commandId = UUID.randomUUID(),
            id = id
        )

        handlerMediatorManager.dispatch(cqrsCommandRequest)

        return ResponseEntity.noContent().build()
    }

    @DeleteMapping("/all")
    fun removeAll(): ResponseEntity<Void> {

        val cqrsCommandRequest = RemoveAllCentroDistribuicaoCQRSRequest(
            commandId = UUID.randomUUID()
        )

        handlerMediatorManager.dispatch(cqrsCommandRequest)

        return ResponseEntity.noContent().build()

    }

    @PostMapping("/synchronize")
    fun synchronize(): ResponseEntity<Map<String, Int>> {

        val cqrsCommandRequest = SynchronizeCentroDistribuicaoCQRSRequest(
            commandId = UUID.randomUUID()
        )

        val cqrsCommandResponse : SynchronizeCentroDistribuicaoCQRSResponse? = handlerMediatorManager.dispatch(cqrsCommandRequest)

        return ResponseEntity.ok().body(cqrsCommandResponse?.resultSyncMap ?: emptyMap())
    }

    @PostMapping(value = ["/upload-geojson"], consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    fun uploadGeoJson(@RequestParam("contentDataType") contentDataType : String,
                            @RequestPart("file") uploadedGeoJSONFile: MultipartFile
    ): ResponseEntity<Map<String, String>?> {

        val cqrsCommandRequest = UploadGeoJsonCentroDistribuicaoCQRSRequest(
            commandId = UUID.randomUUID(),
            contentDataType = contentDataType,
            uploadedGeoJSONFile = uploadedGeoJSONFile
        )

        val cqrsCommandResponse : UploadGeoJsonCentroDistribuicaoCQRSResponse? = handlerMediatorManager.dispatch(cqrsCommandRequest)

        return ResponseEntity.ok().body(cqrsCommandResponse?.resultUploadMap ?: emptyMap())

    }

}