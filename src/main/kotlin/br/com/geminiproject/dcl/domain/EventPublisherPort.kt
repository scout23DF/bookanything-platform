package br.com.geminiproject.dcl.domain

interface EventPublisherPort {
    fun publish(event: Any)
}
