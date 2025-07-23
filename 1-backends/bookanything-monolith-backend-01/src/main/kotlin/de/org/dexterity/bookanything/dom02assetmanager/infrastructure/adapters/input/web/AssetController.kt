package de.org.dexterity.bookanything.dom02assetmanager.infrastructure.adapters.input.web

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import de.org.dexterity.bookanything.dom02assetmanager.application.usecases.AssetUseCase
import de.org.dexterity.bookanything.dom02assetmanager.domain.dtos.AssetDto
import de.org.dexterity.bookanything.dom02assetmanager.domain.dtos.UploadAssetResponseDto
import de.org.dexterity.bookanything.dom02assetmanager.domain.models.AssetCategory
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile

@RestController
@RequestMapping("/api/v1/assets")
class AssetController(private val assetUseCase: AssetUseCase) {

    @PostMapping("/upload", consumes = ["multipart/form-data"])
    suspend fun uploadAsset(
        @RequestPart("file") file: MultipartFile,
        @RequestPart("category") category: String,
        @RequestPart("bucket", required = false) bucket: String?,
        @RequestPart("metadata", required = false) metadataJson: String?
    ): ResponseEntity<UploadAssetResponseDto> {
        val assetCategory = AssetCategory.valueOf(category.uppercase())
        val metadata = metadataJson?.let { jacksonObjectMapper().readValue<Map<String, Any>>(it) } ?: emptyMap()

        val response = assetUseCase.handleUpload(file, bucket, assetCategory, metadata)
        return ResponseEntity.ok(response)
    }

    @GetMapping("/{id}")
    suspend fun getAssetById(@PathVariable id: Long): ResponseEntity<AssetDto> {
        val assetDto = assetUseCase.handleFindById(id)
        return assetDto?.let { ResponseEntity.ok(it) } ?: ResponseEntity.notFound().build()
    }
}