package de.org.dexterity.bookanything.kmm01

class Greeting {
    private val platform = getPlatform()

    fun greet(): String {
        return "Hello, Buddy -> ${platform.name}!"
    }
}