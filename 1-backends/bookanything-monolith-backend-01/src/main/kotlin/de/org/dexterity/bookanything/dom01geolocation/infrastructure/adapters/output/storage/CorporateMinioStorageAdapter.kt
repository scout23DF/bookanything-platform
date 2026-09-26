package de.org.dexterity.bookanything.dom01geolocation.infrastructure.adapters.output.storage

import de.org.dexterity.bookanything.dom01geolocation.domain.ports.CorporateStoragePort
import io.minio.MinioClient
import io.minio.RemoveObjectArgs
import io.minio.StatObjectArgs
import io.minio.errors.ErrorResponseException
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

@Component
class CorporateMinioStorageAdapter(
    @Value("\${corporate.minio.endpoint:http://central-minio.drr-corpshared-plat.svc.cluster.local:9000}")
    private val endpoint: String,
    @Value("\${corporate.minio.access-key:minioadmin}")
    private val accessKey: String,
    @Value("\${corporate.minio.secret-key:Darueira@2026!}")
    private val secretKey: String
) : CorporateStoragePort {

    private val client: MinioClient by lazy {
        MinioClient.builder().endpoint(endpoint).credentials(accessKey, secretKey).build()
    }

    override fun exists(bucket: String, key: String): Boolean =
        try {
            client.statObject(StatObjectArgs.builder().bucket(bucket).`object`(key).build())
            true
        } catch (e: ErrorResponseException) {
            val code = e.errorResponse().code()
            if (code == "NoSuchKey" || code == "NoSuchObject" || code == "NoSuchBucket") false else throw e
        }

    override fun delete(bucket: String, key: String) {
        client.removeObject(RemoveObjectArgs.builder().bucket(bucket).`object`(key).build())
    }
}
