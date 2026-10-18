package de.org.dexterity.bookanything.shared.coroutines

import kotlinx.coroutines.delay
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.common.serialization.StringDeserializer
import org.apache.kafka.common.serialization.StringSerializer
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.kafka.annotation.EnableKafka
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory
import org.springframework.kafka.core.DefaultKafkaConsumerFactory
import org.springframework.kafka.core.DefaultKafkaProducerFactory
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.kafka.listener.DefaultErrorHandler
import org.springframework.kafka.test.EmbeddedKafkaBroker
import org.springframework.kafka.test.context.EmbeddedKafka
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig
import org.springframework.util.backoff.FixedBackOff
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * What does spring-kafka (Boot 4.1.1 / spring-kafka 4.1.1) do with a `suspend` @KafkaListener?
 * Probes of behaviour, not of our code; the findings decide whether a suspend listener is safe to use.
 */
@SpringJUnitConfig(KafkaSuspendListenerBehaviorTest.Cfg::class)
@EmbeddedKafka(partitions = 1, topics = ["probe-slow", "probe-fail"])
class KafkaSuspendListenerBehaviorTest {

    companion object {
        val slowStarts = ConcurrentLinkedQueue<Long>()
        val slowThreads = ConcurrentLinkedQueue<String>()
        val slowDone = CountDownLatch(5)
        val failAttempts = AtomicInteger()
    }

    open class Listeners {
        @KafkaListener(topics = ["probe-slow"], groupId = "probe-slow")
        suspend fun slow(value: String) {
            slowStarts += System.nanoTime() / 1_000_000
            slowThreads += Thread.currentThread().name
            delay(400)
            slowDone.countDown()
        }

        @KafkaListener(topics = ["probe-fail"], groupId = "probe-fail")
        suspend fun fail(value: String) {
            failAttempts.incrementAndGet()
            delay(10)
            error("boom")
        }
    }

    @Configuration
    @EnableKafka
    open class Cfg {
        @Bean
        open fun listeners() = Listeners()

        @Bean
        open fun consumerFactory(broker: EmbeddedKafkaBroker) = DefaultKafkaConsumerFactory<String, String>(
            mapOf(
                ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG to broker.brokersAsString,
                ConsumerConfig.AUTO_OFFSET_RESET_CONFIG to "earliest",
                ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG to StringDeserializer::class.java,
                ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG to StringDeserializer::class.java
            )
        )

        @Bean
        open fun kafkaListenerContainerFactory(cf: DefaultKafkaConsumerFactory<String, String>) =
            ConcurrentKafkaListenerContainerFactory<String, String>().also {
                it.setConsumerFactory(cf)
                it.setCommonErrorHandler(DefaultErrorHandler(FixedBackOff(50, 2))) // 1 try + 2 retries
            }

        @Bean
        open fun template(broker: EmbeddedKafkaBroker) = KafkaTemplate(
            DefaultKafkaProducerFactory<String, String>(
                mapOf(
                    "bootstrap.servers" to broker.brokersAsString,
                    "key.serializer" to StringSerializer::class.java,
                    "value.serializer" to StringSerializer::class.java
                )
            )
        )
    }

    @Autowired
    lateinit var template: KafkaTemplate<String, String>

    @Test
    fun `suspend listener - does the container wait for the body before the next record`() {
        repeat(5) { template.send("probe-slow", "m$it") }
        template.flush()

        assertTrue(slowDone.await(15, TimeUnit.SECONDS), "all five records must be processed")
        val starts = slowStarts.toList()
        val spreadMs = starts.max() - starts.min()
        println("PROBE slow: starts spread=${spreadMs}ms threads=${slowThreads.toSet()}")
        // sequential would be 4 * 400 = 1600ms of spread; overlapped is close to 0
        println("PROBE slow: ${if (spreadMs >= 1500) "SEQUENTIAL per partition (order preserved)" else "OVERLAPPED (order NOT preserved)"}")
    }

    @Test
    fun `suspend listener - an exception reaches the error handler and is retried`() {
        template.send("probe-fail", "x")
        template.flush()
        Thread.sleep(4000)
        println("PROBE fail: attempts=${failAttempts.get()}")
        assertEquals(3, failAttempts.get(), "1 try + 2 retries if the container sees the failure")
    }
}
