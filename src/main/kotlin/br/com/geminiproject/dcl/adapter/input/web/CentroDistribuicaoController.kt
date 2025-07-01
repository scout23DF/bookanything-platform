package br.com.geminiproject.dcl.adapter.input.web

import br.com.geminiproject.dcl.application.CentroDistribuicaoOrchestrationService
import br.com.geminiproject.dcl.application.GeoJsonProcessingService
import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.geom.GeometryFactory
import org.locationtech.jts.geom.PrecisionModel
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.servlet.support.ServletUriComponentsBuilder
import java.util.*


import org.springframework.web.multipart.MultipartFile


@RestController
@RequestMapping("/cds")
class CentroDistribuicaoController(
    private val centroDistribuicaoOrchestrationService: CentroDistribuicaoOrchestrationService,
    private val geoJsonProcessingService: GeoJsonProcessingService
) {

    private val geometryFactory = GeometryFactory(PrecisionModel(), 4326)

    @PostMapping
    fun cadastrar(@RequestBody request: CadastrarCentroDistribuicaoRequest): ResponseEntity<CentroDistribuicaoResponse> {
        val localizacao = geometryFactory.createPoint(Coordinate(request.longitude, request.latitude))
        val centroDistribuicao = centroDistribuicaoOrchestrationService.cadastrar(request.nome, localizacao)
        val response = CentroDistribuicaoResponse.fromDomain(centroDistribuicao)
        val uri = ServletUriComponentsBuilder.fromCurrentRequest()
            .path("/{id}")
            .buildAndExpand(centroDistribuicao.id)
            .toUri()
        return ResponseEntity.created(uri).body(response)
    }

    @GetMapping("/{id}")
    fun buscarPorId(@PathVariable id: UUID): ResponseEntity<CentroDistribuicaoResponse> {
        val centroDistribuicao = centroDistribuicaoOrchestrationService.buscarPorId(id)
        return if (centroDistribuicao != null) {
            ResponseEntity.ok(CentroDistribuicaoResponse.fromDomain(centroDistribuicao))
        } else {
            ResponseEntity.notFound().build()
        }
    }


    @GetMapping("/search-nearby")
    fun buscarCentrosProximos(
        @RequestParam latitude: Double,
        @RequestParam longitude: Double,
        @RequestParam raioEmKm: Double
    ): ResponseEntity<List<CentroDistribuicaoResponse>> {
        val localizacao = geometryFactory.createPoint(Coordinate(longitude, latitude))
        val centrosProximos = centroDistribuicaoOrchestrationService.buscarCentrosProximos(localizacao, raioEmKm)
        val responseList = centrosProximos.map { CentroDistribuicaoResponse.fromDomain(it) }
        return ResponseEntity.ok(responseList)
    }

    @GetMapping("/all")
    fun buscarTodos(): ResponseEntity<List<CentroDistribuicaoResponse>> {
        val todosCentros = centroDistribuicaoOrchestrationService.buscarTodos()
        val responseList = todosCentros.map { CentroDistribuicaoResponse.fromDomain(it) }
        return ResponseEntity.ok(responseList)
    }

    @DeleteMapping("/{id}")
    fun deletarPorId(@PathVariable id: UUID): ResponseEntity<Void> {
        centroDistribuicaoOrchestrationService.deletarPorId(id)
        return ResponseEntity.noContent().build()
    }

    @PostMapping("/synchronize")
    fun synchronize(): ResponseEntity<Map<String, Int>> {
        val resultSyncMap : Map<String, Int> = centroDistribuicaoOrchestrationService.synchronizeAll()
        return ResponseEntity.ok().body(resultSyncMap)
    }

    @PostMapping(value = ["/upload-geojson"], consumes = [org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE])
    fun uploadGeoJson(@RequestParam("contentDataType") contentDataType : String,
                      @RequestPart("file") uploadedGeoJSONFile: MultipartFile): ResponseEntity<String> {

        geoJsonProcessingService.processGeoJsonFile(contentDataType, uploadedGeoJSONFile)
        return ResponseEntity.ok("GeoJSON file added to the queue successfully. When its processing has finished, you'll get a proper notification.")
    }

    @DeleteMapping("/all")
    fun deletarTodos(): ResponseEntity<Void> {
        centroDistribuicaoOrchestrationService.deletarTodos()
        return ResponseEntity.noContent().build()
    }
}