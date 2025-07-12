package de.org.dexterity.bookanything.shared.mediators

interface IGenericRequestHandler<in TObjRequest : IGenericDataRequest<TObjResponse?>, TObjResponse> {

    fun getRequestType() : Class<@UnsafeVariance TObjRequest>

    fun handleRequest(requestHolder: TObjRequest): TObjResponse?

}
