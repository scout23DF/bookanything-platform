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
import org.locationtech.jts.io.WKTReader
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

    private val wktReader = WKTReader()

    @KafkaListener(topics = ["geolocation-enrichment-request-topic"], groupId = "geolocation-enricher")
    fun listen(request: GeoLocationEnrichmentEvent) {
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
            val boundaryWkt = aiService.generateBoundary(geoLocation)
            val boundary = wktReader.read(boundaryWkt)
            when (request.type) {
                GeoLocationType.CONTINENT -> continentRepository.updateBoundary(geoLocationId, boundary)
                GeoLocationType.REGION -> regionRepository.updateBoundary(geoLocationId, boundary)
                GeoLocationType.COUNTRY -> countryRepository.updateBoundary(geoLocationId, boundary)
                GeoLocationType.PROVINCE -> provinceRepository.updateBoundary(geoLocationId, boundary)
                GeoLocationType.CITY -> cityRepository.updateBoundary(geoLocationId, boundary)
                GeoLocationType.DISTRICT -> districtRepository.updateBoundary(geoLocationId, boundary)
            }
        }
    }
}