package de.org.dexterity.bookanything.dom01geolocation.application.services

import de.org.dexterity.bookanything.dom01geolocation.domain.models.IGeoLocationModel
import de.org.dexterity.bookanything.dom01geolocation.domain.ports.SearchEngineInIAProxyPort
import org.springframework.stereotype.Service

@Service
class GetGeoLocationBoundaryViaAIService(
    private val searchEngineInIAProxyPort: SearchEngineInIAProxyPort
) {

    fun generateBoundary(geoLocation: IGeoLocationModel): String? {
        val promptTemplateForAIEngine = """
            Sua tarefa é atuar como um especialista em dados geoespaciais e fornecer a representação geométrica de um local no formato Well-Known Text (WKT).

            O local é: ${geoLocation.humanReadableName()}.

            Pesquise as informações geométricas das localidades na internet. Eis algumas sugestões:

            - https://github.com/giuliano-macedo/geodata-br-states/tree/main/geojson/br_states

            Forneça como resposta APENAS a string WKT que representa o MULTIPOLYGON para este local. Não inclua nenhuma explicação, texto adicional, ou formatação de código como ```wkt ... ```.
            
            Porém, inclua a sentença completa e válida do MULTIPOLYGON, de forma que ele não sê erros de parsing.
        """.trimIndent()

        return searchEngineInIAProxyPort.simpleSearchByPrompt(promptTemplateForAIEngine)

        /*
            Pesquise as informações geométricas das localidades na internet. Eis algumas sugestões:

            - https://github.com/giuliano-macedo/geodata-br-states/tree/main/geojson/br_states

            Pesquise também nestes arquivos GeoJSON que foram baixados e salvos nas seguintes pastas:

            - /home/andre.nascimento/DevEnvALNS/projects/03-PoCs-And-Researches/ALNS-RemoteGitRepos/GitHub-scout23DF/bookanything-platform/1-backends/bookanything-monolith-backend-01/src/main/resources/geojson-data/locations-polygons

        */
    }
}