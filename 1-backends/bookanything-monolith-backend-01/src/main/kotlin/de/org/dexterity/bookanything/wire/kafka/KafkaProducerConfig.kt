package de.org.dexterity.bookanything.wire.kafka

import de.org.dexterity.bookanything.dom01geolocation.domain.dtos.GeoJsonUploadedFileDTO
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.kafka.core.ProducerFactory

@Configuration
class KafkaProducerConfig {

    @Bean
    @Primary
    fun kafkaTemplate(producerFactory: ProducerFactory<Any, Any>): KafkaTemplate<String, Any> {
        @Suppress("UNCHECKED_CAST")
        return KafkaTemplate(producerFactory as ProducerFactory<String, Any>)
    }

    @Bean
    fun geoJsonKafkaTemplate(producerFactory: ProducerFactory<Any, Any>): KafkaTemplate<String, GeoJsonUploadedFileDTO> {
        @Suppress("UNCHECKED_CAST")
        return KafkaTemplate(producerFactory as ProducerFactory<String, GeoJsonUploadedFileDTO>)
    }
}
