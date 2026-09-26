package de.org.dexterity.bookanything.dom01geolocation.infrastructure.adapters.input.workflow

import com.fasterxml.jackson.databind.ObjectMapper
import de.org.dexterity.bookanything.dom01geolocation.application.services.GeoLocationAIEnrichmentService
import de.org.dexterity.bookanything.dom01geolocation.application.services.GeoLocationAssetIntegrationService
import de.org.dexterity.bookanything.dom01geolocation.application.services.GeoLocationCRUDService
import de.org.dexterity.bookanything.dom01geolocation.application.services.GeoLocationFlagService
import de.org.dexterity.bookanything.dom01geolocation.application.services.GeoLocationSvgGeneratorService
import de.org.dexterity.bookanything.dom01geolocation.application.services.MapSvgGeneratorMechanismHolder
import de.org.dexterity.bookanything.dom01geolocation.application.workflow.GeoLocationArtifactsActivities
import de.org.dexterity.bookanything.dom01geolocation.domain.dtos.FlagStepResult
import de.org.dexterity.bookanything.dom01geolocation.domain.dtos.GeoLocationMapsRequestDto
import de.org.dexterity.bookanything.dom01geolocation.domain.dtos.MapsStepResult
import de.org.dexterity.bookanything.dom01geolocation.domain.dtos.ReportStepResult
import de.org.dexterity.bookanything.dom01geolocation.domain.models.IGeoLocationModel
import de.org.dexterity.bookanything.dom01geolocation.infrastructure.adapters.output.rest.GeoLocationPythonMapsClient
import io.minio.BucketExistsArgs
import io.minio.MakeBucketArgs
import io.minio.MinioClient
import io.minio.PutObjectArgs
import io.temporal.failure.ApplicationFailure
import kotlinx.coroutines.runBlocking
import org.locationtech.jts.geom.Geometry
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.io.ByteArrayInputStream
import java.net.HttpURLConnection
import java.net.URI
import java.time.Instant
import java.time.format.DateTimeFormatter
import java.util.Base64
import java.util.Locale

/**
 * Step activities of the per-GeoLocation artifacts workflow (ADR-0014). The logic comes from the former
 * monolithic `generateGeoLocationArtifactsAndReport`; the differences are that each step persists its output
 * and returns references (the report step re-reads SVGs/flag from the storage), and that failures are thrown so
 * Temporal retries only the failed step.
 */
@Component
class GeoLocationArtifactsActivitiesImpl(
    private val objectMapper: ObjectMapper,
    private val geoLocationCRUDService: GeoLocationCRUDService,
    private val svgGeneratorService: GeoLocationSvgGeneratorService,
    private val assetIntegrationService: GeoLocationAssetIntegrationService,
    private val aiEnrichmentService: GeoLocationAIEnrichmentService,
    private val flagService: GeoLocationFlagService,
    private val pythonMapsClient: GeoLocationPythonMapsClient,
    private val mechanismHolder: MapSvgGeneratorMechanismHolder,
    @Value("\${jsreport.base-url:http://10.152.183.35:5488}")
    private val jsreportBaseUrl: String,
    @Value("\${jsreport.username:admin}")
    private val jsreportUsername: String,
    @Value("\${jsreport.password:change-me-in-openbao}")
    private val jsreportPassword: String,
    @Value("\${jsreport.detail-template-name:geolocation-detail-report}")
    private val jsreportDetailTemplateName: String = "geolocation-detail-report",
    @Value("\${corporate.minio.endpoint:http://central-minio.drr-corpshared-plat.svc.cluster.local:9000}")
    private val corporateMinioEndpoint: String,
    @Value("\${corporate.minio.access-key:minioadmin}")
    private val corporateMinioAccessKey: String,
    @Value("\${corporate.minio.secret-key:Darueira@2026!}")
    private val corporateMinioSecretKey: String,
    @Value("\${corporate.minio.reports-bucket:darueira-reports}")
    private val corporateMinioReportsBucket: String
) : GeoLocationArtifactsActivities {

    private val logger = LoggerFactory.getLogger(javaClass)

    private fun load(geoLocationId: Long): IGeoLocationModel =
        geoLocationCRUDService.findAnyById(geoLocationId)
            ?: throw ApplicationFailure.newNonRetryableFailure(
                "GeoLocation not found for ID: $geoLocationId", "GEOLOCATION_NOT_FOUND"
            )

    private fun s3Url(bucket: String, key: String) = "s3://$bucket/$key"

    override fun generateAndStoreMaps(geoLocationId: Long): MapsStepResult {
        val geoLocation = load(geoLocationId)
        val name = geoLocation.name
        val type = geoLocation.type.name
        val friendlyId = geoLocation.friendlyId
        val alias = geoLocation.alias

        val boundary = geoLocation.boundaryRepresentation
        if (boundary == null || boundary.isEmpty) {
            logger.warn("Activity: GeoLocation #$geoLocationId ($name) has no boundary geometry. Skipping artifacts and report.")
            return MapsStepResult(
                geoLocationId = geoLocationId, name = name, type = type, friendlyId = friendlyId,
                status = "SKIPPED_NO_GEOMETRY",
                errorMessage = "No geographic boundary geometry found (ge_geographic_boundary is null or empty)."
            )
        }

        val mechanism = mechanismHolder.current
        logger.info("Activity: Map SVG generation mechanism: '$mechanism' for #$geoLocationId ($name)...")
        val (localMapSvg, worldHighlightSvg) = generateSvgs(geoLocationId, mechanism, name, type, friendlyId, alias, boundary)

        logger.info("Activity: Uploading SVG maps to Tenant MinIO for #$geoLocationId...")
        val (localAsset, worldAsset) = runBlocking {
            assetIntegrationService.saveGeoLocationMapSvgs(geoLocation, localMapSvg, worldHighlightSvg)
        }
        logger.info("Activity: Both SVGs successfully persisted in Tenant MinIO (Local: Asset #${localAsset.id}, World: Asset #${worldAsset.id})")

        return MapsStepResult(
            geoLocationId = geoLocationId, name = name, type = type, friendlyId = friendlyId,
            localMapAssetId = localAsset.id,
            localMapAssetUrl = s3Url(localAsset.bucket.name, localAsset.storageKey),
            worldHighlightAssetId = worldAsset.id,
            worldHighlightAssetUrl = s3Url(worldAsset.bucket.name, worldAsset.storageKey)
        )
    }

    /** GeoPandas (Python service) when configured, falling back to the Kotlin generator on any problem. */
    private fun generateSvgs(
        geoLocationId: Long, mechanism: String, name: String, type: String, friendlyId: String,
        alias: String?, boundary: Geometry
    ): Pair<String, String> {
        val label = alias ?: friendlyId
        fun kotlinSvgs() = Pair(
            svgGeneratorService.generateLocalMapSvg(boundary, name, label),
            svgGeneratorService.generateWorldHighlightMapSvg(boundary, name, label)
        )
        if (!mechanism.equals("GEOPANDAS", ignoreCase = true)) return kotlinSvgs()
        return try {
            logger.info("Activity: Invoking Python Maps Generator (GeoPandas) for #$geoLocationId...")
            val pyResponse = pythonMapsClient.generateMaps(
                GeoLocationMapsRequestDto(
                    geoLocationId = geoLocationId, name = name, type = type, friendlyId = friendlyId,
                    alias = alias, boundaryWkt = boundary.toText(), saveToMinio = true,
                    minioBucket = "bookanything-images"
                )
            )
            if (pyResponse.status == "SUCCESS" && pyResponse.localMapSvg.isNotBlank()) {
                logger.info("Activity: Successfully received high-fidelity GeoPandas SVGs for #$geoLocationId")
                Pair(pyResponse.localMapSvg, pyResponse.worldHighlightSvg)
            } else {
                logger.warn("Activity: Python Maps Generator returned non-success (${pyResponse.errorMessage}). Falling back to Kotlin SVG generator.")
                kotlinSvgs()
            }
        } catch (e: Exception) {
            logger.error("Activity: Error calling Python Maps Generator (${e.message}). Falling back to Kotlin SVG generator.")
            kotlinSvgs()
        }
    }

    override fun resolveAndStoreFlag(geoLocationId: Long): FlagStepResult {
        val geoLocation = load(geoLocationId)
        val parentName = geoLocationCRUDService.findParentModel(geoLocation)?.name
        logger.info("Activity: Resolving official flag for #$geoLocationId (${geoLocation.name})...")
        val flagResult = flagService.obtainFlag(geoLocation, parentName)
        val flagAsset = runBlocking {
            assetIntegrationService.saveGeoLocationFlag(geoLocation, flagResult.rawBytes, flagResult.isSvg)
        }
        logger.info("Activity: Flag successfully persisted in Tenant MinIO as Asset #${flagAsset.id} (key: ${flagAsset.storageKey})")
        return FlagStepResult(
            flagAssetId = flagAsset.id!!,
            flagAssetUrl = s3Url(flagAsset.bucket.name, flagAsset.storageKey),
            flagName = flagResult.flagName,
            flagDescription = flagResult.flagDescription,
            isSvg = flagResult.isSvg
        )
    }

    override fun enrichWithAi(geoLocationId: Long): String {
        val geoLocation = load(geoLocationId)
        val parentName = geoLocationCRUDService.findParentModel(geoLocation)?.name
        logger.info("Activity: Requesting Spring AI synthesis for #$geoLocationId (${geoLocation.name}) [Parent: $parentName]...")
        val aiSummary = aiEnrichmentService.enrichGeoLocation(geoLocation, parentName)
        try {
            val updatedDetails = (geoLocation.additionalDetailsMap ?: emptyMap()).toMutableMap()
            updatedDetails["ai_enrichment_summary"] = aiSummary
            geoLocationCRUDService.updateAdditionalDetails(geoLocation.type, geoLocationId, updatedDetails)
            logger.info("Activity: Persisted AI summary in additional details for GeoLocation #$geoLocationId")
        } catch (e: Exception) {
            logger.warn("Activity: Could not persist AI summary in entity details: ${e.message}")
        }
        return aiSummary
    }

    override fun renderAndStoreReport(
        geoLocationId: Long,
        maps: MapsStepResult,
        flag: FlagStepResult,
        aiSummary: String
    ): ReportStepResult {
        val geoLocation = load(geoLocationId)
        val name = geoLocation.name
        val boundary = geoLocation.boundaryRepresentation
            ?: throw ApplicationFailure.newNonRetryableFailure(
                "GeoLocation #$geoLocationId lost its boundary geometry", "GEOLOCATION_NO_GEOMETRY"
            )
        val parentModel = geoLocationCRUDService.findParentModel(geoLocation)

        // The previous steps stored the content; load it here instead of receiving it through the history.
        val localMapSvg = runBlocking { assetIntegrationService.readAssetText(maps.localMapAssetId!!) }
        val worldHighlightSvg = runBlocking { assetIntegrationService.readAssetText(maps.worldHighlightAssetId!!) }
        val flagBytes = runBlocking { assetIntegrationService.readAssetBytes(flag.flagAssetId) }
        val flagImageContent =
            if (flag.isSvg) String(flagBytes, Charsets.UTF_8)
            else "data:image/png;base64," + Base64.getEncoder().encodeToString(flagBytes)

        logger.info("Activity: Calling JSReport '$jsreportDetailTemplateName' for #$geoLocationId ($name)...")
        val env = boundary.envelopeInternal
        val envelopeStr = String.format(Locale.US, "[%.3f, %.3f] to [%.3f, %.3f]", env.minX, env.minY, env.maxX, env.maxY)

        val renderData = mapOf(
            "geoLocation" to mapOf(
                "id" to geoLocation.id.id,
                "name" to name,
                "type" to geoLocation.type.name,
                "alias" to (geoLocation.alias ?: "N/A"),
                "friendlyId" to geoLocation.friendlyId,
                "parentName" to (parentModel?.name ?: ""),
                "parentId" to (geoLocation.parentId ?: ""),
                "geometryType" to boundary.geometryType,
                "pointsCount" to boundary.numPoints,
                "envelope" to envelopeStr
            ),
            "localMapSvg" to localMapSvg,
            "localMapAssetId" to maps.localMapAssetId,
            "localMapAssetUrl" to maps.localMapAssetUrl,
            "worldHighlightSvg" to worldHighlightSvg,
            "worldHighlightAssetId" to maps.worldHighlightAssetId,
            "worldHighlightAssetUrl" to maps.worldHighlightAssetUrl,
            "flagName" to flag.flagName,
            "flagDescription" to flag.flagDescription,
            "flagImage" to flagImageContent,
            "isFlagSvg" to flag.isSvg,
            "flagAssetId" to flag.flagAssetId,
            "flagAssetUrl" to flag.flagAssetUrl,
            "aiEnrichment" to aiSummary,
            "generatedAt" to DateTimeFormatter.ISO_INSTANT.format(Instant.now())
        )
        val pdfBytes = renderPdf(mapOf("template" to mapOf("name" to jsreportDetailTemplateName), "data" to renderData))
        logger.info("Activity: JSReport rendered detail PDF successfully (Size: ${pdfBytes.size} bytes)")

        val reportAsset = runBlocking { assetIntegrationService.saveGeoLocationReportPdf(geoLocation, pdfBytes) }
        return ReportStepResult(
            reportAssetId = reportAsset.id!!,
            reportUrl = s3Url(reportAsset.bucket.name, reportAsset.storageKey)
        )
    }

    /** One attempt: Temporal retries the whole step (with backoff) if JSReport answers with an error. */
    private fun renderPdf(payload: Map<String, Any?>): ByteArray {
        val conn = URI.create("$jsreportBaseUrl/api/report").toURL().openConnection() as HttpURLConnection
        try {
            val basicAuth = "Basic " + Base64.getEncoder().encodeToString("$jsreportUsername:$jsreportPassword".toByteArray())
            conn.requestMethod = "POST"
            conn.setRequestProperty("Content-Type", "application/json")
            conn.setRequestProperty("Authorization", basicAuth)
            conn.doOutput = true
            conn.connectTimeout = 30000
            conn.readTimeout = 60000
            conn.outputStream.use { it.write(objectMapper.writeValueAsBytes(payload)) }
            val code = conn.responseCode
            if (code !in 200..299) {
                val err = conn.errorStream?.bufferedReader()?.readText() ?: "HTTP $code"
                throw RuntimeException("JSReport detail rendering failed with HTTP $code: $err")
            }
            return conn.inputStream.use { it.readBytes() }
        } finally {
            conn.disconnect()
        }
    }

    override fun copyReportToCorporateStorage(geoLocationId: Long, reportAssetId: Long): String {
        val geoLocation = load(geoLocationId)
        val pdfBytes = runBlocking { assetIntegrationService.readAssetBytes(reportAssetId) }
        val minioClient = MinioClient.builder()
            .endpoint(corporateMinioEndpoint)
            .credentials(corporateMinioAccessKey, corporateMinioSecretKey)
            .build()
        if (!minioClient.bucketExists(BucketExistsArgs.builder().bucket(corporateMinioReportsBucket).build())) {
            minioClient.makeBucket(MakeBucketArgs.builder().bucket(corporateMinioReportsBucket).build())
        }
        val idOrAlias = (geoLocation.alias ?: geoLocation.friendlyId).replace("/", "-")
        val corpObjectKey = "geolocations/geolocation-detail-report-$idOrAlias.pdf"
        ByteArrayInputStream(pdfBytes).use { bais ->
            minioClient.putObject(
                PutObjectArgs.builder()
                    .bucket(corporateMinioReportsBucket)
                    .`object`(corpObjectKey)
                    .stream(bais, pdfBytes.size.toLong(), -1)
                    .contentType("application/pdf")
                    .build()
            )
        }
        logger.info("Activity: Also stored detail report copy in Corporate MinIO at s3://$corporateMinioReportsBucket/$corpObjectKey")
        return s3Url(corporateMinioReportsBucket, corpObjectKey)
    }
}
