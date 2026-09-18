package de.org.dexterity.bookanything.dom01geolocation.infrastructure.adapters.input.messaging.kafkaconsumers

import de.org.dexterity.bookanything.dom01geolocation.application.services.GetGeoLocationBoundaryViaAIService
import de.org.dexterity.bookanything.dom01geolocation.application.workflow.GeoLocationIngestionActivities
import de.org.dexterity.bookanything.dom01geolocation.domain.events.GeoLocationEnrichmentEvent
import de.org.dexterity.bookanything.dom01geolocation.domain.models.GeoLocationId
import de.org.dexterity.bookanything.dom01geolocation.domain.models.GeoLocationType
import de.org.dexterity.bookanything.dom01geolocation.domain.ports.ICityRepositoryPort
import de.org.dexterity.bookanything.dom01geolocation.domain.ports.IContinentRepositoryPort
import de.org.dexterity.bookanything.dom01geolocation.domain.ports.ICountryRepositoryPort
import de.org.dexterity.bookanything.dom01geolocation.domain.ports.IDistrictRepositoryPort
import de.org.dexterity.bookanything.dom01geolocation.domain.ports.IProvinceRepositoryPort
import de.org.dexterity.bookanything.dom01geolocation.domain.ports.IRegionRepositoryPort
import org.locationtech.jts.geom.Geometry
import org.locationtech.jts.io.WKTReader
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Lazy
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
@Transactional
class GeoLocationEnrichmentKafkaConsumer(
    private val continentRepository: IContinentRepositoryPort,
    private val countryRepository: ICountryRepositoryPort,
    private val regionRepository: IRegionRepositoryPort,
    private val provinceRepository: IProvinceRepositoryPort,
    private val cityRepository: ICityRepositoryPort,
    private val districtRepository: IDistrictRepositoryPort,
    private val aiService: GetGeoLocationBoundaryViaAIService,
    @Lazy private val activities: GeoLocationIngestionActivities,
    @Value("\${application.domain-settings.geolocation.update-boundary-via-ia.feature-enabled:false}")
    private val shouldUpdateBoundaryViaIA: Boolean = false
) {

    private val logger = LoggerFactory.getLogger(javaClass)
    private val wktReader = WKTReader()

    @KafkaListener(topics = ["geolocation-enrichment-request-topic"], groupId = "geolocation-enricher")
    fun listen(request: GeoLocationEnrichmentEvent) {

        var messageToLog : String = "===> "

        val geoLocationId = GeoLocationId(request.id)

        val geoLocation = when (request.type) {
            GeoLocationType.CONTINENT -> continentRepository.findById(geoLocationId).orElse(null)
            GeoLocationType.REGION -> regionRepository.findById(geoLocationId).orElse(null)
            GeoLocationType.COUNTRY -> countryRepository.findById(geoLocationId).orElse(null)
            GeoLocationType.PROVINCE -> provinceRepository.findById(geoLocationId).orElse(null)
            GeoLocationType.CITY -> cityRepository.findById(geoLocationId).orElse(null)
            GeoLocationType.DISTRICT -> districtRepository.findById(geoLocationId).orElse(null)
        }

        if (geoLocation != null) {
            val existingBoundary = geoLocation.boundaryRepresentation

            // Only attempt to find boundary via IA if feature is enabled AND localidade has NO existing boundary
            if (shouldUpdateBoundaryViaIA && (existingBoundary == null || existingBoundary.isEmpty)) {
                try {
                    val boundaryWkt = aiService.generateBoundary(geoLocation)
                    if (!boundaryWkt.isNullOrBlank() && !boundaryWkt.contains("not configured", ignoreCase = true)) {
                        val geometryBoundary = wktReader.read(boundaryWkt)
                        logger.info("Kafka Consumer: Successfully derived boundary via IA for GeoLocation #${request.id} (${geoLocation.name})")

                        when (request.type) {
                            GeoLocationType.CONTINENT -> continentRepository.updateBoundary(geoLocationId, geometryBoundary)
                            GeoLocationType.REGION -> regionRepository.updateBoundary(geoLocationId, geometryBoundary)
                            GeoLocationType.COUNTRY -> countryRepository.updateBoundary(geoLocationId, geometryBoundary)
                            GeoLocationType.PROVINCE -> provinceRepository.updateBoundary(geoLocationId, geometryBoundary)
                            GeoLocationType.CITY -> cityRepository.updateBoundary(geoLocationId, geometryBoundary)
                            GeoLocationType.DISTRICT -> districtRepository.updateBoundary(geoLocationId, geometryBoundary)
                        }
                    } else {
                        logger.info("Kafka Consumer: IA returned no valid boundary for GeoLocation #${request.id} (${geoLocation.name}). Leaving boundary as-is.")
                    }
                } catch (ex: Exception) {
                    logger.warn("Kafka Consumer: Could not generate boundary via IA for GeoLocation #${request.id} (${geoLocation.name}): ${ex.message}. Preserving entity state without dummy overwrite.")
                }
            } else {
                logger.info("Kafka Consumer: Preserving rich geographic boundary for GeoLocation #${request.id} (${geoLocation.name}) [Points: ${existingBoundary?.numPoints ?: 0}].")
            }

            try {
                logger.info("Kafka Consumer: Triggering SVG artifacts and JSReport generation for GeoLocation #${request.id}...")
                activities.generateGeoLocationArtifactsAndReport(request.id)
            } catch (e: Exception) {
                logger.warn("Kafka Consumer: Automatic artifact/report generation for GeoLocation #${request.id} finished with notice: ${e.message}")
            }
        }
    }
}