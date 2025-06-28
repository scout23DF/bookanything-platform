package br.com.geminiproject.dcl.domain

import org.locationtech.jts.geom.Point

interface CadastrarCentroDistribuicaoUseCase {
    fun cadastrar(nome: String, localizacao: Point): CentroDistribuicao
}
