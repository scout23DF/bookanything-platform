package br.com.geminiproject.dcl

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class DistributionCenterLocatorApplication

fun main(args: Array<String>) {
	runApplication<DistributionCenterLocatorApplication>(*args)
}
