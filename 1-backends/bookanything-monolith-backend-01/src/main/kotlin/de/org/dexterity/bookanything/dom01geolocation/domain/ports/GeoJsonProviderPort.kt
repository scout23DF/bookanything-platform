package de.org.dexterity.bookanything.dom01geolocation.domain.ports

import java.nio.file.Path

/**
 * Port for a GeoJSON data provider, responsible for fetching GeoJSON data from an external source.
 */
interface GeoJsonProviderPort {

    /**
     * Checks if a specific GeoJSON file exists at the provider.
     *
     * @param countryIso3Code The 3-letter ISO code of the country.
     * @param level The administrative level.
     * @return True if the file exists, false otherwise.
     */
    suspend fun fileExists(countryIso3Code: String, level: Int): Boolean

    /**
     * Downloads a GeoJSON file for a specific country and level and saves it to a temporary location.
     *
     * @param countryIso3Code The 3-letter ISO code of the country.
     * @param level The administrative level.
     * @return The Path to the temporarily saved file.
     * @throws java.io.FileNotFoundException if the file does not exist at the provider.
     */
    suspend fun downloadFile(countryIso3Code: String, level: Int): Path

}
