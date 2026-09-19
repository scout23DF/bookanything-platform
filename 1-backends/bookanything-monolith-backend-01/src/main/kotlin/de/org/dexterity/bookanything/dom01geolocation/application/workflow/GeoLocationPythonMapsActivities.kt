package de.org.dexterity.bookanything.dom01geolocation.application.workflow

import de.org.dexterity.bookanything.dom01geolocation.domain.dtos.GeoLocationMapsRequestDto
import de.org.dexterity.bookanything.dom01geolocation.domain.dtos.GeoLocationMapsResponseDto
import io.temporal.activity.ActivityInterface
import io.temporal.activity.ActivityMethod

@ActivityInterface
interface GeoLocationPythonMapsActivities {

    @ActivityMethod(name = "GenerateGeoLocationMaps")
    fun generateGeoLocationMaps(request: GeoLocationMapsRequestDto): GeoLocationMapsResponseDto
}
