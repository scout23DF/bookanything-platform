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
    private val geoJsonReader = org.locationtech.jts.io.geojson.GeoJsonReader()

    private fun String.toGeometry(): Geometry = try {
        val trimmed = this.trim()
        val geom = if (trimmed.startsWith("{")) {
            geoJsonReader.read(trimmed)
        } else {
            wktReader.read(trimmed)
        }
        if (geom.srid == 0) {
            geom.srid = 4326
        }
        geom
    } catch (e: Exception) {
        val geom = wktReader.read(this)
        if (geom.srid == 0) {
            geom.srid = 4326
        }
        geom
    }
    private fun Geometry.toText(): String = wktWriter.write(this)

    fun fromIGeoLocationModelToResponse(sourceModel: IGeoLocationModel, includeBoundary: Boolean) : GeoLocationResponse {
        return GeoLocationResponse(
            type = sourceModel.type,
            id = sourceModel.id.id,
            friendlyId = sourceModel.friendlyId,
            name = sourceModel.name,
            alias = sourceModel.alias,
            additionalDetailsMap = sourceModel.additionalDetailsMap,
            boundaryRepresentation = if (includeBoundary) sourceModel.boundaryRepresentation?.toText() else null,
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