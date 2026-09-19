package de.org.dexterity.bookanything.dom01geolocation.application.services

import com.fasterxml.jackson.databind.ObjectMapper
import de.org.dexterity.bookanything.dom01geolocation.domain.models.GeoLocationType
import de.org.dexterity.bookanything.dom01geolocation.domain.models.IGeoLocationModel
import de.org.dexterity.bookanything.dom01geolocation.domain.ports.SearchEngineInIAProxyPort
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.util.Base64

data class GeoLocationFlagResult(
    val flagName: String,
    val flagDescription: String,
    val flagImageContent: String, // inline SVG or "data:image/png;base64,..."
    val rawBytes: ByteArray,
    val isSvg: Boolean,
    val mimeType: String,
    val sourceUrl: String? = null
)

@Service
class GeoLocationFlagService(
    private val searchEngineInIAProxyPort: SearchEngineInIAProxyPort,
    private val webClient: WebClient,
    private val objectMapper: ObjectMapper
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    /**
     * Resolves the official flag for a GeoLocation using a multi-tier strategy:
     * 1. Consult Spring AI (Gemini) for canonical flag metadata, country ISO, Wikipedia article and SVG fallback.
     * 2. Download authoritative vector SVG or high-resolution raster image from CDN / Wikimedia.
     * 3. Fallback to AI-generated vector SVG markup if remote image download fails.
     * 4. Deterministic vexillological SVG generation if AI is completely unavailable.
     */
    fun obtainFlag(geoLocation: IGeoLocationModel, parentName: String? = null): GeoLocationFlagResult {
        val name = geoLocation.humanReadableName()
        val type = geoLocation.type
        val code = geoLocation.alias ?: geoLocation.friendlyId
        val parent = parentName ?: "Global / Continente"

        logger.info("Flag Service: Requesting flag resolution for '$name' ($type) [Parent: $parent]...")

        var aiFlagName = "Bandeira de $name"
        var aiDescription = "Bandeira oficial representativa de $name ($type)."
        var countryIso2: String? = null
        var wikipediaTitle: String? = null
        var aiSvgFallback: String? = null

        // 1. Invoke Spring AI Gemini for flag vexillology metadata
        try {
            val prompt = """
                Atue como um especialista em vexilologia (estudo de bandeiras) e geografia política.
                Para a localidade abaixo, identifique a bandeira oficial:
                - Nome: $name
                - Tipo de Localidade: ${type.name}
                - Região / Pertencimento: $parent
                - Código / Alias: $code

                Responda ESTRITAMENTE em formato JSON válido (sem texto fora do bloco json):
                {
                  "flagName": "Nome oficial da bandeira (ex: Bandeira do Brasil, Bandeira do Estado de São Paulo)",
                  "countryIso2": "Código ISO de 2 letras do país se for COUNTRY ou se tiver país associado (ex: BR, AR, US), ou null",
                  "wikipediaArticleTitle": "Título exato do artigo em inglês na Wikipédia sobre a bandeira (ex: Flag_of_Brazil, Flag_of_São_Paulo_(state), Flag_of_Acre), ou null",
                  "flagDescription": "Síntese descritiva de 1 a 2 frases em português explicando as cores e simbolismo da bandeira",
                  "svgFallback": "Código SVG válido, autocontido, com viewBox='0 0 300 200' desenhando a bandeira fielmente"
                }
            """.trimIndent()

            val aiResponse = searchEngineInIAProxyPort.simpleSearchByPrompt(prompt)
            if (!aiResponse.isNullOrBlank() && !aiResponse.contains("not configured in this environment", ignoreCase = true)) {
                val cleanJson = cleanJsonBlock(aiResponse)
                val node = objectMapper.readTree(cleanJson)

                if (node.hasNonNull("flagName")) aiFlagName = node.get("flagName").asText()
                if (node.hasNonNull("flagDescription")) aiDescription = node.get("flagDescription").asText()
                if (node.hasNonNull("countryIso2")) countryIso2 = node.get("countryIso2").asText().trim().lowercase()
                if (node.hasNonNull("wikipediaArticleTitle")) wikipediaTitle = node.get("wikipediaArticleTitle").asText().trim().replace(" ", "_")
                if (node.hasNonNull("svgFallback")) aiSvgFallback = node.get("svgFallback").asText().trim()

                logger.info("Flag Service: AI identified flag '$aiFlagName' (ISO: $countryIso2, Wiki: $wikipediaTitle)")
            }
        } catch (e: Exception) {
            logger.warn("Flag Service: Spring AI flag metadata lookup encountered error: ${e.message}")
        }

        // 2. Strategy A: For COUNTRY, attempt FlagCDN vector SVG (ultra-reliable, pure vector)
        if (type == GeoLocationType.COUNTRY) {
            val iso = countryIso2 ?: code.take(2).lowercase()
            if (iso.length == 2 && iso.all { it.isLetter() }) {
                val flagCdnSvgUrl = "https://flagcdn.com/$iso.svg"
                val svgBytes = downloadBytes(flagCdnSvgUrl)
                if (svgBytes != null && isValidSvg(svgBytes)) {
                    val svgContent = String(svgBytes, StandardCharsets.UTF_8)
                    logger.info("Flag Service: Successfully retrieved authoritative SVG from FlagCDN ($flagCdnSvgUrl)")
                    return GeoLocationFlagResult(
                        flagName = aiFlagName,
                        flagDescription = aiDescription,
                        flagImageContent = svgContent,
                        rawBytes = svgBytes,
                        isSvg = true,
                        mimeType = "image/svg+xml",
                        sourceUrl = flagCdnSvgUrl
                    )
                }

                val flagCdnPngUrl = "https://flagcdn.com/w640/$iso.png"
                val pngBytes = downloadBytes(flagCdnPngUrl)
                if (pngBytes != null && pngBytes.isNotEmpty()) {
                    val base64 = Base64.getEncoder().encodeToString(pngBytes)
                    logger.info("Flag Service: Successfully retrieved authoritative PNG from FlagCDN ($flagCdnPngUrl)")
                    return GeoLocationFlagResult(
                        flagName = aiFlagName,
                        flagDescription = aiDescription,
                        flagImageContent = "data:image/png;base64,$base64",
                        rawBytes = pngBytes,
                        isSvg = false,
                        mimeType = "image/png",
                        sourceUrl = flagCdnPngUrl
                    )
                }
            }
        }

        // 3. Strategy B: Query Wikipedia REST API for official article image (Provinces, States, Cities)
        if (!wikipediaTitle.isNullOrBlank()) {
            val wikiImageUrl = resolveWikipediaImageUrl(wikipediaTitle)
            if (wikiImageUrl != null) {
                val imgBytes = downloadBytes(wikiImageUrl)
                if (imgBytes != null && imgBytes.isNotEmpty()) {
                    val isSvg = isValidSvg(imgBytes) || wikiImageUrl.endsWith(".svg", ignoreCase = true)
                    val mime = if (isSvg) "image/svg+xml" else "image/png"
                    val content = if (isSvg) {
                        String(imgBytes, StandardCharsets.UTF_8)
                    } else {
                        "data:$mime;base64," + Base64.getEncoder().encodeToString(imgBytes)
                    }
                    logger.info("Flag Service: Successfully retrieved flag from Wikipedia ($wikiImageUrl, SVG=$isSvg, size=${imgBytes.size})")
                    return GeoLocationFlagResult(
                        flagName = aiFlagName,
                        flagDescription = aiDescription,
                        flagImageContent = content,
                        rawBytes = imgBytes,
                        isSvg = isSvg,
                        mimeType = mime,
                        sourceUrl = wikiImageUrl
                    )
                }
            }
        }

        // 4. Strategy C: Use AI-generated SVG Fallback
        if (!aiSvgFallback.isNullOrBlank() && aiSvgFallback.contains("<svg") && aiSvgFallback.contains("</svg>")) {
            val cleanSvg = extractSvgOnly(aiSvgFallback)
            if (cleanSvg.isNotBlank()) {
                val bytes = cleanSvg.toByteArray(StandardCharsets.UTF_8)
                logger.info("Flag Service: Using Spring AI Gemini generated vector SVG fallback (${bytes.size} bytes)")
                return GeoLocationFlagResult(
                    flagName = aiFlagName,
                    flagDescription = aiDescription,
                    flagImageContent = cleanSvg,
                    rawBytes = bytes,
                    isSvg = true,
                    mimeType = "image/svg+xml",
                    sourceUrl = "ai-generated://gemini/svg-fallback"
                )
            }
        }

        // 5. Strategy D: Deterministic Vexillological Vector Badge Fallback
        logger.info("Flag Service: Generating deterministic vector vexillology badge for '$name' ($code)")
        val fallbackSvg = generateDeterministicFlagSvg(name, code, type.name)
        val fallbackBytes = fallbackSvg.toByteArray(StandardCharsets.UTF_8)
        return GeoLocationFlagResult(
            flagName = aiFlagName,
            flagDescription = aiDescription,
            flagImageContent = fallbackSvg,
            rawBytes = fallbackBytes,
            isSvg = true,
            mimeType = "image/svg+xml",
            sourceUrl = "system-generated://vexillology-badge"
        )
    }

    private fun cleanJsonBlock(raw: String): String {
        var text = raw.trim()
        if (text.startsWith("```json")) {
            text = text.removePrefix("```json")
        } else if (text.startsWith("```")) {
            text = text.removePrefix("```")
        }
        if (text.endsWith("```")) {
            text = text.removeSuffix("```")
        }
        return text.trim()
    }

    private fun extractSvgOnly(rawSvg: String): String {
        val start = rawSvg.indexOf("<svg")
        val end = rawSvg.lastIndexOf("</svg>")
        return if (start >= 0 && end > start) {
            rawSvg.substring(start, end + 6)
        } else {
            rawSvg
        }
    }

    private fun isValidSvg(bytes: ByteArray): Boolean {
        val sample = String(bytes.take(200).toByteArray(), StandardCharsets.UTF_8).lowercase()
        return sample.contains("<svg") || sample.contains("<?xml")
    }

    private fun downloadBytes(url: String): ByteArray? {
        return try {
            webClient.get()
                .uri(url)
                .header("User-Agent", "BookAnythingBot/1.0 (contact: admin@dexterity.org.de)")
                .retrieve()
                .bodyToMono(ByteArray::class.java)
                .block()
        } catch (e: Exception) {
            logger.warn("Flag Service: Download failed for $url: ${e.message}")
            null
        }
    }

    private fun resolveWikipediaImageUrl(articleTitle: String): String? {
        return try {
            val encodedTitle = URLEncoder.encode(articleTitle, StandardCharsets.UTF_8.name())
            val apiUrl = "https://en.wikipedia.org/api/rest_v1/page/summary/$encodedTitle"

            val json = webClient.get()
                .uri(apiUrl)
                .header("User-Agent", "BookAnythingBot/1.0 (contact: admin@dexterity.org.de)")
                .retrieve()
                .bodyToMono(String::class.java)
                .block() ?: return null

            val root = objectMapper.readTree(json)
            when {
                root.hasNonNull("originalimage") && root.get("originalimage").hasNonNull("source") -> {
                    root.get("originalimage").get("source").asText()
                }
                root.hasNonNull("thumbnail") && root.get("thumbnail").hasNonNull("source") -> {
                    root.get("thumbnail").get("source").asText()
                }
                else -> null
            }
        } catch (e: Exception) {
            logger.warn("Flag Service: Wikipedia API lookup failed for '$articleTitle': ${e.message}")
            null
        }
    }

    private fun generateDeterministicFlagSvg(name: String, code: String, type: String): String {
        val safeName = name.replace("<", "&lt;").replace(">", "&gt;")
        val safeCode = code.replace("<", "&lt;").replace(">", "&gt;")
        return """
            <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 300 200" width="100%" height="100%">
              <defs>
                <linearGradient id="flagBg" x1="0%" y1="0%" x2="100%" y2="100%">
                  <stop offset="0%" stop-color="#0f172a" />
                  <stop offset="50%" stop-color="#1e293b" />
                  <stop offset="100%" stop-color="#334155" />
                </linearGradient>
                <linearGradient id="accentGrad" x1="0%" y1="0%" x2="100%" y2="0%">
                  <stop offset="0%" stop-color="#38bdf8" />
                  <stop offset="100%" stop-color="#818cf8" />
                </linearGradient>
              </defs>
              <rect width="300" height="200" rx="6" fill="url(#flagBg)" />
              <rect x="0" y="85" width="300" height="30" fill="url(#accentGrad)" opacity="0.85" />
              <circle cx="150" cy="100" r="38" fill="#0f172a" stroke="#38bdf8" stroke-width="2.5" />
              <text x="150" y="96" font-family="-apple-system, BlinkMacSystemFont, Segoe UI, Roboto, sans-serif" font-size="16" font-weight="bold" fill="#f8fafc" text-anchor="middle">$safeCode</text>
              <text x="150" y="112" font-family="-apple-system, BlinkMacSystemFont, Segoe UI, Roboto, sans-serif" font-size="9" font-weight="600" fill="#94a3b8" text-anchor="middle">$type</text>
              <text x="150" y="160" font-family="-apple-system, BlinkMacSystemFont, Segoe UI, Roboto, sans-serif" font-size="11" font-weight="600" fill="#e2e8f0" text-anchor="middle">$safeName</text>
            </svg>
        """.trimIndent()
    }
}
