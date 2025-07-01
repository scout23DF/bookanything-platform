package br.com.geminiproject.dcl

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.cache.annotation.EnableCaching

@SpringBootApplication
@EnableCaching
class DistributionCenterLocatorApplication

fun main(args: Array<String>) {
	runApplication<DistributionCenterLocatorApplication>(*args)
}
