package de.org.dexterity.bookanything.dom01geolocation.infrastructure.adapters.input.web.mappers

import de.org.dexterity.bookanything.dom01geolocation.domain.models.*
import de.org.dexterity.bookanything.dom01geolocation.infrastructure.adapters.input.web.dtos.CreateGeoLocationRequest
import de.org.dexterity.bookanything.dom01geolocation.infrastructure.adapters.input.web.dtos.GeoLocationResponse
import org.locationtech.jts.geom.Geometry
import org.locationtech.jts.io.WKTReader
import org.locationtech.jts.io.WKTWriter
import org.springframework.stereotype.Component


@Component
class GeoLocationRestMapper {

    private val wktReader = WKTReader()
    private val wktWriter = WKTWriter()

    private fun String.toGeometry(): Geometry = wktReader.read(this)
    private fun Geometry.toText(): String = wktWriter.write(this)

    fun fromIGeoLocationModelToResponse(sourceModel: IGeoLocationModel) : GeoLocationResponse {
        return GeoLocationResponse(
            type = sourceModel.type,
            id = sourceModel.id.id,
            friendlyId = sourceModel.friendlyId,
            name = sourceModel.name,
            alias = sourceModel.alias,
            additionalDetailsMap = sourceModel.additionalDetailsMap,
            boundaryRepresentation = sourceModel.boundaryRepresentation?.toText(),
            parentId = sourceModel.parentId
        )
    }

    fun fromCreateGeoLocationRequestToModel(
        type: GeoLocationType,
        createRequest: CreateGeoLocationRequest,
        parent: IGeoLocationModel? = null
    ): IGeoLocationModel {

        val boundary = createRequest.boundaryRepresentation?.toGeometry()

        return when (type) {
            GeoLocationType.CONTINENT -> ContinentModel(id = GeoLocationId(0), friendlyId = createRequest.friendlyId, name = createRequest.name, alias = createRequest.alias, additionalDetailsMap = createRequest.additionalDetailsMap, boundaryRepresentation = boundary)
            GeoLocationType.REGION -> RegionModel(id = GeoLocationId(0), friendlyId = createRequest.friendlyId, name = createRequest.name, alias = createRequest.alias, additionalDetailsMap = createRequest.additionalDetailsMap, boundaryRepresentation = boundary, parentId = parent?.id?.id, continent = parent as ContinentModel)
            GeoLocationType.COUNTRY -> CountryModel(id = GeoLocationId(0), friendlyId = createRequest.friendlyId, name = createRequest.name, alias = createRequest.alias, additionalDetailsMap = createRequest.additionalDetailsMap, boundaryRepresentation = boundary, parentId = parent?.id?.id, region = parent as RegionModel)
            GeoLocationType.PROVINCE -> ProvinceModel(id = GeoLocationId(0), friendlyId = createRequest.friendlyId, name = createRequest.name, alias = createRequest.alias, additionalDetailsMap = createRequest.additionalDetailsMap, boundaryRepresentation = boundary, parentId = parent?.id?.id, country = parent as CountryModel)
            GeoLocationType.CITY -> CityModel(id = GeoLocationId(0), friendlyId = createRequest.friendlyId, name = createRequest.name, alias = createRequest.alias, additionalDetailsMap = createRequest.additionalDetailsMap, boundaryRepresentation = boundary, parentId = parent?.id?.id, province = parent as ProvinceModel)
            GeoLocationType.DISTRICT -> DistrictModel(id = GeoLocationId(0), friendlyId = createRequest.friendlyId, name = createRequest.name, alias = createRequest.alias, additionalDetailsMap = createRequest.additionalDetailsMap, boundaryRepresentation = boundary, parentId = parent?.id?.id, city = parent as CityModel)
        }
    }

}