package de.org.dexterity.bookanything.dom01geolocation.infrastructure.adapters.input.messaging.kafkaconsumers

import de.org.dexterity.bookanything.dom01geolocation.domain.events.CountryDataToMakeGeoLocationsEvent
import de.org.dexterity.bookanything.dom01geolocation.domain.ports.*
import org.locationtech.jts.io.WKTReader
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
@Transactional
class GeoLocationsMakerFromGeoJsonImportedFileKafkaConsumer(
    private val continentRepository: IContinentRepositoryPort,
    private val countryRepository: ICountryRepositoryPort,
    private val regionRepository: IRegionRepositoryPort,
    private val provinceRepository: IProvinceRepositoryPort,
    private val cityRepository: ICityRepositoryPort,
    private val districtRepository: IDistrictRepositoryPort,
    @Value("\${topics.geolocation.geojson-imported-file.ready-to-make-geo-locations}") private val geoJsonImportedFileReadyToMakeGeoLocationsTopic: String,
    private val geoJsonImportedFileRepositoryPort: GeoJsonImportedFileRepositoryPort
) {

    private val logger = LoggerFactory.getLogger(javaClass)
    private val wktReader = WKTReader()

    @KafkaListener(
        topics = ["geolocation.geojson-imported-file.ready-to-make-geo-locations"],
        groupId = "geolocation-maker-from-geojson-imported-processor"
    )
    fun listen(countryDataToMakeGeoLocationsEvent: CountryDataToMakeGeoLocationsEvent) {

        var messageToLog : String = "===> Event consumed: $countryDataToMakeGeoLocationsEvent"

        val geoJsonImportedFileId = countryDataToMakeGeoLocationsEvent.geoJsonImportedFileId

        val geoJsonImportedFileModel = geoJsonImportedFileRepositoryPort.findById(geoJsonImportedFileId)

        messageToLog += " :: GeoJsonImportedFile found: $geoJsonImportedFileModel"

        logger.info(messageToLog)

        /*
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
        */

    }
}