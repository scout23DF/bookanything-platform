package de.org.dexterity.bookanything.dom01geolocation.infrastructure.adapters.input.messaging.kafkaconsumers

import de.org.dexterity.bookanything.dom01geolocation.application.services.GetGeoLocationBoundaryViaAIService
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
    private val aiService: GetGeoLocationBoundaryViaAIService
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

            var geometryBoundary: Geometry? = null

            try {

                val boundaryWkt = aiService.generateBoundary(geoLocation)
                messageToLog += "For the GeoLocation: [$geoLocation] the IA Proxy returned the boundaryWkt: [$boundaryWkt]."
                geometryBoundary = wktReader.read(boundaryWkt)

            } catch (ex: Exception) {
                geometryBoundary = wktReader.read("POLYGON ((30 10, 40 40, 20 40, 10 20, 30 10))")
                messageToLog += " ::> Error occurred: '${ex.message}' --> Adopting the default boundaryGeometry: [POLYGON ((30 10, 40 40, 20 40, 10 20, 30 10))]."
            }

            logger.info(messageToLog)

            when (request.type) {
                GeoLocationType.CONTINENT -> continentRepository.updateBoundary(geoLocationId, geometryBoundary)
                GeoLocationType.REGION -> regionRepository.updateBoundary(geoLocationId, geometryBoundary)
                GeoLocationType.COUNTRY -> countryRepository.updateBoundary(geoLocationId, geometryBoundary)
                GeoLocationType.PROVINCE -> provinceRepository.updateBoundary(geoLocationId, geometryBoundary)
                GeoLocationType.CITY -> cityRepository.updateBoundary(geoLocationId, geometryBoundary)
                GeoLocationType.DISTRICT -> districtRepository.updateBoundary(geoLocationId, geometryBoundary)
            }
        }
    }
}