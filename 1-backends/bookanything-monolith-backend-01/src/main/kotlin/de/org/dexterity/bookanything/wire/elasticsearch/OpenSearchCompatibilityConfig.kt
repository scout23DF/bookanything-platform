package de.org.dexterity.bookanything.wire.elasticsearch

import co.elastic.clients.transport.rest5_client.low_level.Rest5ClientBuilder
import org.apache.hc.client5.http.impl.async.HttpAsyncClientBuilder
import org.apache.hc.core5.http.EntityDetails
import org.apache.hc.core5.http.HttpRequest
import org.apache.hc.core5.http.HttpRequestInterceptor
import org.apache.hc.core5.http.HttpResponse
import org.apache.hc.core5.http.HttpResponseInterceptor
import org.apache.hc.core5.http.message.BasicHeader
import org.apache.hc.core5.http.protocol.HttpContext
import org.springframework.boot.elasticsearch.autoconfigure.Rest5ClientBuilderCustomizer
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class OpenSearchCompatibilityConfig {

    @Bean
    fun openSearchCompatibilityCustomizer(): Rest5ClientBuilderCustomizer {
        return object : Rest5ClientBuilderCustomizer {
            override fun customize(builder: Rest5ClientBuilder) {
                // Low-level rest5 client builder
            }

            override fun customize(httpClientBuilder: HttpAsyncClientBuilder) {
                httpClientBuilder.addRequestInterceptorLast(HttpRequestInterceptor { request: HttpRequest, _: EntityDetails?, _: HttpContext? ->
                    val ctHeaders = request.getHeaders("Content-Type")
                    for (header in ctHeaders) {
                        if (header.value.contains("application/vnd.elasticsearch")) {
                            request.removeHeaders("Content-Type")
                            request.addHeader(BasicHeader("Content-Type", "application/json"))
                        }
                    }

                    val acceptHeaders = request.getHeaders("Accept")
                    for (header in acceptHeaders) {
                        if (header.value.contains("application/vnd.elasticsearch")) {
                            request.removeHeaders("Accept")
                            request.addHeader(BasicHeader("Accept", "application/json"))
                        }
                    }
                })

                httpClientBuilder.addResponseInterceptorLast(HttpResponseInterceptor { response: HttpResponse, _: EntityDetails?, _: HttpContext? ->
                    if (!response.containsHeader("X-Elastic-Product")) {
                        response.addHeader(BasicHeader("X-Elastic-Product", "Elasticsearch"))
                    }
                })
            }
        }
    }
}
