package de.org.dexterity.bookanything.dom02assetmanager.infrastructure.adapters.input.web

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import de.org.dexterity.bookanything.dom02assetmanager.application.usecases.AssetUseCase
import de.org.dexterity.bookanything.dom02assetmanager.application.services.dtos.AssetDto
import de.org.dexterity.bookanything.dom02assetmanager.application.services.dtos.UpdateAssetDto
import de.org.dexterity.bookanything.dom02assetmanager.application.services.dtos.UploadAssetResponseDto
import de.org.dexterity.bookanything.dom02assetmanager.domain.models.AssetCategory
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile

@RestController
@RequestMapping("/api/v1/assets")
class AssetController(private val assetUseCase: AssetUseCase) {

    @PostMapping("/upload", consumes = ["multipart/form-data"])
    suspend fun uploadAsset(
        @RequestPart("file") uploadedFile: MultipartFile,
        @RequestPart("category") category: String,
        @RequestPart("bucket", required = false) bucket: String?,
        @RequestPart("parentAliasToAttach", required = false) parentAliasToAttach: String?,
        @RequestPart("forceReimportIfExists", required = false) forceReimportIfExists: Boolean?,
        @RequestPart("metadata", required = false) metadataJson: String?
    ): ResponseEntity<UploadAssetResponseDto> {
        val assetCategory = AssetCategory.valueOf(category.uppercase())
        val metadata = metadataJson?.let { jacksonObjectMapper().readValue<Map<String, Any>>(it) } ?: emptyMap()

        val response = assetUseCase.handleUpload(
            uploadedFile,
            bucket,
            assetCategory,
            parentAliasToAttach,
            forceReimportIfExists,
            metadata)

        return ResponseEntity.ok(response)
    }

    @PutMapping("/{id}")
    fun updateAsset(
        @PathVariable id: Long,
        @RequestBody dto: UpdateAssetDto
    ): ResponseEntity<AssetDto> {
        val updatedAsset = assetUseCase.handleUpdate(id, dto)
        return updatedAsset?.let { ResponseEntity.ok(it) } ?: ResponseEntity.notFound().build()
    }

    @GetMapping("/{id}")
    fun getAssetById(@PathVariable id: Long): ResponseEntity<AssetDto> {
        val assetDto = assetUseCase.handleFindById(id)
        return assetDto?.let { ResponseEntity.ok(it) } ?: ResponseEntity.notFound().build()
    }

    @DeleteMapping("/{id}")
    suspend fun deleteAsset(@PathVariable id: Long): ResponseEntity<Void> {
        val success = assetUseCase.handleDelete(id)
        return if (success) ResponseEntity.noContent().build() else ResponseEntity.notFound().build()
    }

    @GetMapping
    fun getAllAssets(pageable: Pageable): ResponseEntity<Page<AssetDto>> {
        val assets = assetUseCase.handleFindAll(pageable)
        return ResponseEntity.ok(assets)
    }

    @GetMapping("/search/by-filename")
    fun searchAssetsByFileName(@RequestParam fileName: String, pageable: Pageable): ResponseEntity<Page<AssetDto>> {
        val assets = assetUseCase.handleFindByFileNameStartingWith(fileName, pageable)
        return ResponseEntity.ok(assets)
    }

    @GetMapping("/search/by-bucket")
    fun searchAssetsByBucketName(@RequestParam bucketName: String, pageable: Pageable): ResponseEntity<Page<AssetDto>> {
        val assets = assetUseCase.handleFindByBucketName(bucketName, pageable)
        return ResponseEntity.ok(assets)
    }

    @GetMapping("/search/by-storage-key")
    fun searchAssetsByStorageKey(@RequestParam storageKey: String, pageable: Pageable): ResponseEntity<Page<AssetDto>> {
        val assets = assetUseCase.handleFindByStorageKeyStartingWith(storageKey, pageable)
        return ResponseEntity.ok(assets)
    }

    @GetMapping("/search/by-category")
    fun searchAssetsByCategory(@RequestParam category: String, pageable: Pageable): ResponseEntity<Page<AssetDto>> {
        val assetCategory = AssetCategory.valueOf(category.uppercase())
        val assets = assetUseCase.handleFindByCategory(assetCategory, pageable)
        return ResponseEntity.ok(assets)
    }

    @GetMapping("/search/by-metadata")
    fun searchAssetsByMetadata(
        @RequestParam key: String,
        @RequestParam value: String,
        pageable: Pageable
    ): ResponseEntity<Page<AssetDto>> {
        val assets = assetUseCase.handleFindByMetadataContains(key, value, pageable)
        return ResponseEntity.ok(assets)
    }

}