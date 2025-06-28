package br.com.geminiproject.dcl.domain

import java.util.UUID

interface BuscarCentroDistribuicaoQueryPort {
    fun buscarPorId(id: UUID): CentroDistribuicao?
    fun buscarTodos(): List<CentroDistribuicao>
}
