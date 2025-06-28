package br.com.geminiproject.dcl.adapter.input.web

data class CadastrarCentroDistribuicaoRequest(
    val nome: String,
    val latitude: Double,
    val longitude: Double
)
