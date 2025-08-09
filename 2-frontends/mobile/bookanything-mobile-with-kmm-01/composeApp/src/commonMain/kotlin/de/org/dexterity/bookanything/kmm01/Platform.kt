package de.org.dexterity.bookanything.kmm01

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform