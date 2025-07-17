package de.org.dexterity.bookanything.dom01geolocation.infrastructure.adapters.input.web.mappers

import de.org.dexterity.bookanything.dom01geolocation.domain.models.*
import de.org.dexterity.bookanything.dom01geolocation.infrastructure.adapters.input.web.dtos.*
import org.locationtech.jts.geom.Geometry
import org.locationtech.jts.io.WKTReader
import org.locationtech.jts.io.WKTWriter
import org.springframework.stereotype.Component


@Component
class DeepGeoLocationRestMapper {

    private val wktReader = WKTReader()
    private val wktWriter = WKTWriter()

    private fun String.toGeometry(): Geometry = wktReader.read(this)
    private fun Geometry.toText(): String = wktWriter.write(this)

    fun fromIGeoLocationModelToDeepResponse(sourceModel: IGeoLocationModel): IDeepGeoLocationResponse {
        return when (sourceModel.type) {
            GeoLocationType.CONTINENT -> fromContinentModelToDeepResponse(sourceModel as ContinentModel)
            GeoLocationType.REGION -> fromRegionModelToDeepResponse(sourceModel as RegionModel)
            GeoLocationType.COUNTRY -> fromCountryModelToDeepResponse(sourceModel as CountryModel)
            GeoLocationType.PROVINCE -> fromProvinceModelToDeepResponse(sourceModel as ProvinceModel)
            GeoLocationType.CITY -> fromCityModelToDeepResponse(sourceModel as CityModel)
            GeoLocationType.DISTRICT -> fromDistrictModelToDeepResponse(sourceModel as DistrictModel)
        }
    }

    fun fromContinentModelToDeepResponse(continentModel: ContinentModel): DeepContinentResponse {
        return DeepContinentResponse(
            id = continentModel.id.id,
            name = continentModel.name,
            alias = continentModel.alias,
            type = continentModel.type,
            boundaryRepresentation = continentModel.boundaryRepresentation?.toText(),
            regionsList = continentModel.regionsList?.map {
                fromRegionModelToDeepResponse(it)
            }
        )
    }

    fun fromRegionModelToDeepResponse(regionModel: RegionModel): DeepRegionResponse {
        return DeepRegionResponse(
            id = regionModel.id.id,
            name = regionModel.name,
            alias = regionModel.alias,
            type = regionModel.type,
            boundaryRepresentation = regionModel.boundaryRepresentation?.toText(),
            countriesList = regionModel.countriesList?.map {
                fromCountryModelToDeepResponse(it)
            }
        )
    }

    fun fromCountryModelToDeepResponse(countryModel: CountryModel): DeepCountryResponse {
        return DeepCountryResponse(
            id = countryModel.id.id,
            name = countryModel.name,
            alias = countryModel.alias,
            type = countryModel.type,
            boundaryRepresentation = countryModel.boundaryRepresentation?.toText(),
            provincesList = countryModel.provincesList?.map {
                fromProvinceModelToDeepResponse(it)
            }
        )
    }

    fun fromProvinceModelToDeepResponse(provinceModel: ProvinceModel): DeepProvinceResponse {
        return DeepProvinceResponse(
            id = provinceModel.id.id,
            name = provinceModel.name,
            alias = provinceModel.alias,
            type = provinceModel.type,
            boundaryRepresentation = provinceModel.boundaryRepresentation?.toText(),
            citiesList = provinceModel.citiesList?.map {
                fromCityModelToDeepResponse(it)
            }
        )
    }

    fun fromCityModelToDeepResponse(cityModel: CityModel): DeepCityResponse {
        return DeepCityResponse(
            id = cityModel.id.id,
            name = cityModel.name,
            alias = cityModel.alias,
            type = cityModel.type,
            boundaryRepresentation = cityModel.boundaryRepresentation?.toText(),
            isCountryCapital = cityModel.isCountryCapital,
            isProvinceCapital = cityModel.isProvinceCapital,
            districtsList = cityModel.districtsList?.map {
                fromDistrictModelToDeepResponse(it)
            }
        )
    }

    fun fromDistrictModelToDeepResponse(districtModel: DistrictModel): DeepDistrictResponse {
        return DeepDistrictResponse(
            id = districtModel.id.id,
            name = districtModel.name,
            alias = districtModel.alias,
            type = districtModel.type,
            boundaryRepresentation = districtModel.boundaryRepresentation?.toText(),
            addressesList = districtModel.addressesList?.map {
                fromAddressModelToDeepResponse(it)
            }
        )
    }

    fun fromAddressModelToDeepResponse(addressModel: AddressModel): DeepAddressResponse {
        return DeepAddressResponse(
            id = addressModel.id.id,
            streetName = addressModel.streetName,
            houseNumber = addressModel.houseNumber,
            floorNumber = addressModel.floorNumber,
            doorNumber = addressModel.doorNumber,
            addressLine2 = addressModel.addressLine2,
            postalCode = addressModel.postalCode,
            districtName = addressModel.districtName,
            cityName = addressModel.cityName,
            provinceName = addressModel.provinceName,
            countryName = addressModel.countryName,
            coordinates = addressModel.coordinates?.latitude.toString() + "," + addressModel.coordinates?.longitude.toString()
        )
    }

}