package de.org.dexterity.bookanything.dom02assetmanager.infrastructure.config

import de.org.dexterity.bookanything.dom02assetmanager.application.services.AssetCRUDService
import de.org.dexterity.bookanything.dom02assetmanager.domain.ports.AssetEventPublisherPort
import de.org.dexterity.bookanything.dom02assetmanager.domain.ports.AssetPersistRepositoryPort
import de.org.dexterity.bookanything.dom02assetmanager.domain.ports.BucketPersistRepositoryPort
import de.org.dexterity.bookanything.dom02assetmanager.domain.ports.StorageProviderPort
import io.minio.MinioClient
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class AssetsMgmntBeansConfig {

    @Bean
    fun assetCRUDService(
        assetPersistRepositoryPort: AssetPersistRepositoryPort,
        bucketPersistRepositoryPort: BucketPersistRepositoryPort,
        storageProviderPort: StorageProviderPort,
        assetEventPublisherPort: AssetEventPublisherPort
    ): AssetCRUDService {

        return AssetCRUDService(
            assetPersistRepositoryPort,
            bucketPersistRepositoryPort,
            storageProviderPort,
            assetEventPublisherPort
        )
    }
}


@Configuration
// @Profile("storage.provider.minio")
class MinioConfig {

    @Value("\${application.domain-settings.asset-mgmnt.storage-providers.minio.url}")
    private lateinit var url: String

    @Value("\${application.domain-settings.asset-mgmnt.storage-providers.minio.access-key}")
    private lateinit var accessKey: String

    @Value("\${application.domain-settings.asset-mgmnt.storage-providers.minio.secret-key}")
    private lateinit var secretKey: String

    @Bean
    fun minioClient(): MinioClient {
        return MinioClient.builder()
            .endpoint(url)
            .credentials(accessKey, secretKey)
            .build()
    }
}
