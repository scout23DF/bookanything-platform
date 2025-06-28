package br.com.geminiproject.dcl.domain

interface CentroDistribuicaoRepositoryPort {
    fun salvar(centroDistribuicao: CentroDistribuicao): CentroDistribuicao
}
