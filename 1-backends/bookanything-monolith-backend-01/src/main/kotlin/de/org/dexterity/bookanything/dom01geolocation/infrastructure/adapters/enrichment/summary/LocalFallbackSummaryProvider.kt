package de.org.dexterity.bookanything.dom01geolocation.infrastructure.adapters.enrichment.summary

import de.org.dexterity.bookanything.dom01geolocation.domain.models.IGeoLocationModel
import de.org.dexterity.bookanything.dom01geolocation.domain.ports.enrichment.IGeoLocationSummaryProvider
import org.slf4j.LoggerFactory
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component

@Component
@Order(999)
class LocalFallbackSummaryProvider : IGeoLocationSummaryProvider {

    private val logger = LoggerFactory.getLogger(javaClass)

    override val providerId: String = "LOCAL_FALLBACK"
    override val order: Int = 999

    override fun getSummary(geoLocation: IGeoLocationModel, parentName: String?): String {
        val name = geoLocation.name
        val type = geoLocation.type.name
        val code = geoLocation.alias ?: geoLocation.friendlyId
        val parent = parentName ?: "Global / Continente"

        logger.info("LocalFallbackSummaryProvider: Generating deterministic fallback synthesis for '$name' ($type)...")

        return """
        1. Contexto Geopolítico e Histórico:
        $name é uma unidade territorial de categoria $type integrada a $parent (código de referência: $code). Possui relevância geoestratégica em sua malha administrativa regional, desempenhando papel chave na articulação federativa, soberania territorial e conectividade logística dentro de sua esfera de influência.

        2. Economia e Principais Atividades:
        A matriz econômica de $name compreende atividades produtivas diversificadas, com destaque para a prestação de serviços, cadeias comerciais, agronegócio regional e polos de desenvolvimento infraestrutural. Sua localização viabiliza corredores de transporte de cargas e integração aos mercados consumidores circundantes.

        3. Turismo e Atrativos Culturais:
        Apresenta patrimônio cultural, histórico e paisagístico representativo de suas tradições regionais. Entre seus principais atrativos destacam-se rotas ecoturísticas, festividades culturais populares, gastronomia típica e monumentos que preservam a memória e a identidade de seus habitantes.

        4. Perfil Populacional e Demográfico:
        Caracteriza-se por uma população dinâmica com índice de urbanização consolidado. Apresenta distribuição etária equilibrada, expansão gradual do índice de desenvolvimento humano e demandas estruturantes nas áreas de mobilidade urbana, saúde, saneamento e educação tecnológica.
        """.trimIndent().take(2000)
    }
}
