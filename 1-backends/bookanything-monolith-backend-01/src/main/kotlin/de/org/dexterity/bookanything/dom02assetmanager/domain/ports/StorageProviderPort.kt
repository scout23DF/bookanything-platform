package de.org.dexterity.bookanything.dom02assetmanager.domain.ports

import org.springframework.http.ResponseEntity
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody
import java.io.InputStream

interface StorageProviderPort {

    /**
     * Creates a bucket if it does not already exist.
     * @param bucketName The name of the bucket to create.
     */
    suspend fun createBucketIfNotExists(bucketName: String)

    /**
     * Uploads a file to the specified bucket.
     * @param bucketName The name of the target bucket.
     * @param key The unique key (path) for the file within the bucket.
     * @param inputStream The file content as an InputStream.
     * @param size The size of the file in bytes.
     * @param mimeType The MIME type of the file.
     * @return A result object with details of the uploaded file.
     */
    suspend fun upload(bucketName: String, key: String, inputStream: InputStream, size: Long, mimeType: String): UploadResult

    /**
     * Downloads a file from the specified bucket.
     * @param bucketName The name of the bucket.
     * @param key The key of the file to download.
     * @return A ResponseEntity containing a StreamingResponseBody to efficiently stream the file.
     */
    suspend fun download(bucketName: String, key: String): ResponseEntity<StreamingResponseBody>

    /**
     * Deletes a file from the specified bucket.
     * @param bucketName The name of the bucket.
     * @param key The key of the file to delete.
     */
    suspend fun delete(bucketName: String, key: String)
}

data class UploadResult(
    val storageKey: String,
    val eTag: String? = null,
    val versionId: String? = null
)
