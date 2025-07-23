package de.org.dexterity.bookanything.dom02assetmanager.infrastructure.adapters.output.storage

import de.org.dexterity.bookanything.dom02assetmanager.domain.ports.StorageProviderPort
import de.org.dexterity.bookanything.dom02assetmanager.domain.ports.UploadResult
import io.minio.*
import io.minio.errors.ErrorResponseException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Profile
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Component
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody
import java.io.InputStream

@Component
// @Profile("storage.provider.minio")
class MinioStorageProviderAdapter(private val minioClient: MinioClient) : StorageProviderPort {

    private val log = LoggerFactory.getLogger(MinioStorageProviderAdapter::class.java)

    // ... (métodos createBucketIfNotExists e upload permanecem os mesmos)
    override suspend fun createBucketIfNotExists(bucketName: String) {
        withContext(Dispatchers.IO) {
            val bucketExists = minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucketName).build())
            if (!bucketExists) {
                minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucketName).build())
                log.info("Bucket '$bucketName' created successfully.")
            }
        }
    }

    override suspend fun upload(
        bucketName: String,
        key: String,
        inputStream: InputStream,
        size: Long,
        mimeType: String
    ): UploadResult {
        return withContext(Dispatchers.IO) {
            val args = PutObjectArgs.builder()
                .bucket(bucketName)
                .`object`(key)
                .stream(inputStream, size, -1)
                .contentType(mimeType)
                .build()
            val response = minioClient.putObject(args)
            UploadResult(
                storageKey = response.`object`(),
                eTag = response.etag(),
                versionId = response.versionId()
            )
        }
    }


    /**
     * CORRIGIDO: Downloads an object from a Minio bucket.
     * This implementation is robust against resource leaks by ensuring the InputStream
     * from Minio is closed even if an error occurs during the response entity creation.
     */
    override suspend fun download(bucketName: String, key: String): ResponseEntity<StreamingResponseBody> {
        return withContext(Dispatchers.IO) {
            try {
                val stat = minioClient.statObject(
                    StatObjectArgs.builder()
                        .bucket(bucketName)
                        .`object`(key)
                        .build()
                )

                // A linha #51 agora está dentro de um fluxo seguro
                val objectStream: InputStream = minioClient.getObject(
                    GetObjectArgs.builder()
                        .bucket(bucketName)
                        .`object`(key)
                        .build()
                )

                // Usamos try-finally para garantir que o stream seja fechado
                try {
                    val responseBody = StreamingResponseBody { outputStream ->
                        objectStream.use { stream -> // .use garante o fechamento após o streaming
                            stream.copyTo(outputStream)
                        }
                    }

                    val contentType = stat.contentType() ?: "application/octet-stream"
                    ResponseEntity.ok()
                        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"${stat.`object`()}\"")
                        .contentType(MediaType.parseMediaType(contentType))
                        .contentLength(stat.size())
                        .body(responseBody)
                } catch (e: Exception) {
                    // Se a criação do ResponseEntity falhar, fechamos o stream e relançamos a exceção
                    objectStream.close()
                    throw e
                }

            } catch (e: ErrorResponseException) {
                if (e.errorResponse().code() == "NoSuchKey") {
                    log.warn("Requested object not found: bucket='{}', key='{}'", bucketName, key)
                    ResponseEntity.notFound().build<StreamingResponseBody>()
                } else {
                    log.error("Minio error downloading object: bucket='{}', key='{}'", bucketName, key, e)
                    ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build<StreamingResponseBody>()
                }
            } catch (e: Exception) {
                log.error("Unexpected error during download: bucket='{}', key='{}'", bucketName, key, e)
                ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build<StreamingResponseBody>()
            }
        }
    }

    /**
     * CORRIGIDO: Deletes an object, handling exceptions gracefully.
     */
    override suspend fun delete(bucketName: String, key: String) {
        withContext(Dispatchers.IO) {
            try {
                // A linha #93 agora está protegida por um try-catch
                minioClient.removeObject(
                    RemoveObjectArgs.builder()
                        .bucket(bucketName)
                        .`object`(key)
                        .build()
                )
                log.info("Object deleted successfully: bucket='{}', key='{}'", bucketName, key)
            } catch (e: ErrorResponseException) {
                // Se o objeto não existe, consideramos a operação um sucesso (idempotência)
                if (e.errorResponse().code() == "NoSuchKey") {
                    log.warn("Attempted to delete a non-existent object (ignoring): bucket='{}', key='{}'", bucketName, key)
                } else {
                    // Para outros erros do Minio, registramos e propagamos a exceção
                    log.error("Minio error deleting object: bucket='{}', key='{}'", bucketName, key, e)
                    throw e
                }
            } catch (e: Exception) {
                log.error("Unexpected error during object deletion: bucket='{}', key='{}'", bucketName, key, e)
                throw e
            }
        }
    }
}