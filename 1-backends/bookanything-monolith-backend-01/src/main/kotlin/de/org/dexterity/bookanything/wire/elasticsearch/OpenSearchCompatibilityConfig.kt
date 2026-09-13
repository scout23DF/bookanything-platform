package de.org.dexterity.bookanything.wire.elasticsearch

import org.apache.http.Header
import org.apache.http.HttpEntityEnclosingRequest
import org.apache.http.HttpRequest
import org.apache.http.HttpRequestInterceptor
import org.apache.http.HttpResponseInterceptor
import org.apache.http.entity.HttpEntityWrapper
import org.apache.http.message.BasicHeader
import org.apache.http.protocol.HTTP
import org.apache.http.protocol.HttpContext
import org.elasticsearch.client.RestClientBuilder
import org.springframework.boot.autoconfigure.elasticsearch.RestClientBuilderCustomizer
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class OpenSearchCompatibilityConfig {

    @Bean
    fun openSearchCompatibilityCustomizer(): RestClientBuilderCustomizer {
        return RestClientBuilderCustomizer { builder: RestClientBuilder ->
            builder.setHttpClientConfigCallback { httpClientBuilder ->
                httpClientBuilder.addInterceptorLast(HttpRequestInterceptor { request: HttpRequest, _: HttpContext ->
                    if (request is HttpEntityEnclosingRequest && request.entity != null) {
                        val originalEntity = request.entity
                        val ct = originalEntity.contentType?.value
                        if (ct != null && ct.contains("application/vnd.elasticsearch")) {
                            request.entity = object : HttpEntityWrapper(originalEntity) {
                                override fun getContentType(): Header {
                                    return BasicHeader(HTTP.CONTENT_TYPE, "application/json")
                                }
                            }
                        }
                    }

                    val ctHeaders = request.getHeaders("Content-Type")
                    for (header in ctHeaders) {
                        if (header.value.contains("application/vnd.elasticsearch")) {
                            request.removeHeader(header)
                            request.addHeader(BasicHeader("Content-Type", "application/json"))
                        }
                    }

                    val acceptHeaders = request.getHeaders("Accept")
                    for (header in acceptHeaders) {
                        if (header.value.contains("application/vnd.elasticsearch")) {
                            request.removeHeader(header)
                            request.addHeader(BasicHeader("Accept", "application/json"))
                        }
                    }
                })

                httpClientBuilder.addInterceptorLast(HttpResponseInterceptor { response, _ ->
                    if (!response.containsHeader("X-Elastic-Product")) {
                        response.addHeader(BasicHeader("X-Elastic-Product", "Elasticsearch"))
                    }
                })

                httpClientBuilder
            }
        }
    }
}
