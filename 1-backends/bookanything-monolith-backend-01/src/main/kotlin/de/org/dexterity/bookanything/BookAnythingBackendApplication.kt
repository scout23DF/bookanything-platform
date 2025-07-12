package de.org.dexterity.bookanything

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.cache.annotation.EnableCaching

@SpringBootApplication
@EnableCaching
class BookAnythingBackendApplication

fun main(args: Array<String>) {
	runApplication<BookAnythingBackendApplication>(*args)
}
