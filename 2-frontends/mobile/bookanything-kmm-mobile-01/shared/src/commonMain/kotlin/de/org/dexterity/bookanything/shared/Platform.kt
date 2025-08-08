
package de.org.dexterity.bookanything.shared

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform
