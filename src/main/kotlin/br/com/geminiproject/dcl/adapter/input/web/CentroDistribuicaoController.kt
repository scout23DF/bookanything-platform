package br.com.geminiproject.dcl.adapter.input.web

import br.com.geminiproject.dcl.domain.BuscarCentroDistribuicaoQueryPort
import br.com.geminiproject.dcl.domain.BuscarCentrosProximosUseCase
import br.com.geminiproject.dcl.domain.CadastrarCentroDistribuicaoUseCase
import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.geom.GeometryFactory
import org.locationtech.jts.geom.PrecisionModel
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.servlet.support.ServletUriComponentsBuilder
import java.util.UUID


@RestController
@RequestMapping("/cds")
class CentroDistribuicaoController(
    private val cadastrarCentroDistribuicaoUseCase: CadastrarCentroDistribuicaoUseCase,
    private val buscarCentroDistribuicaoQueryPort: BuscarCentroDistribuicaoQueryPort,
    private val buscarCentrosProximosUseCase: BuscarCentrosProximosUseCase
) {

    private val geometryFactory = GeometryFactory(PrecisionModel(), 4326)

    @PostMapping
    fun cadastrar(@RequestBody request: CadastrarCentroDistribuicaoRequest): ResponseEntity<CentroDistribuicaoResponse> {
        val localizacao = geometryFactory.createPoint(Coordinate(request.longitude, request.latitude))
        val centroDistribuicao = cadastrarCentroDistribuicaoUseCase.cadastrar(request.nome, localizacao)
        val response = CentroDistribuicaoResponse.fromDomain(centroDistribuicao)
        val uri = ServletUriComponentsBuilder.fromCurrentRequest()
            .path("/{id}")
            .buildAndExpand(centroDistribuicao.id)
            .toUri()
        return ResponseEntity.created(uri).body(response)
    }

    @GetMapping("/{id}")
    fun buscarPorId(@PathVariable id: UUID): ResponseEntity<CentroDistribuicaoResponse> {
        val centroDistribuicao = buscarCentroDistribuicaoQueryPort.buscarPorId(id)
        return if (centroDistribuicao != null) {
            ResponseEntity.ok(CentroDistribuicaoResponse.fromDomain(centroDistribuicao))
        } else {
            ResponseEntity.notFound().build()
        }
    }

    @GetMapping
    fun buscarTodos(): ResponseEntity<List<CentroDistribuicaoResponse>> {
        val centrosDistribuicao = buscarCentroDistribuicaoQueryPort.buscarTodos()
        val responseList = centrosDistribuicao.map { CentroDistribuicaoResponse.fromDomain(it) }
        return ResponseEntity.ok(responseList)
    }

    @GetMapping("/search-nearby")
    fun buscarCentrosProximos(
        @RequestParam latitude: Double,
        @RequestParam longitude: Double,
        @RequestParam raioEmKm: Double
    ): ResponseEntity<List<CentroDistribuicaoResponse>> {
        val localizacao = geometryFactory.createPoint(Coordinate(longitude, latitude))
        val centrosProximos = buscarCentrosProximosUseCase.buscarCentrosProximos(localizacao, raioEmKm)
        val responseList = centrosProximos.map { CentroDistribuicaoResponse.fromDomain(it) }
        return ResponseEntity.ok(responseList)
    }
}