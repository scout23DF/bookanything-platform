package de.org.dexterity.bookanything.shared.mediators

import org.springframework.stereotype.Component
import java.lang.reflect.ParameterizedType

class CommandNotRegisteredError(dataRequestClazz: Class<out IGenericDataRequest<*>?>?) : Exception(
    "The command $dataRequestClazz hasn't a command handler associated"
)

class CommandHandlerExecutionError(override val cause: Throwable?) : RuntimeException()


@Suppress("UNCHECKED_CAST")
@Component
class HandlersMediatorManager(handlerList: MutableList<IGenericRequestHandler<*, *>?>) {

    private var handlersMap: MutableMap<Class<out IGenericDataRequest<*>>?, IGenericRequestHandler<*, *>?> = hashMapOf()

    init {

        handlersMap = handlerList.associateBy {
            val paramType = it?.javaClass?.genericInterfaces[0] as ParameterizedType
            val dataRequestClazz = paramType.actualTypeArguments[0] as Class<out IGenericDataRequest<*>?>
            return@associateBy dataRequestClazz
        }.toMutableMap()

    }

    @Throws(CommandNotRegisteredError::class)
    fun <TObjResponse, TObjRequest : IGenericDataRequest<TObjResponse>> dispatch(request: TObjRequest): TObjResponse {

        if (!handlersMap.containsKey(request.javaClass)) {
            throw CommandNotRegisteredError(request.javaClass)
        } else {
            val handler = (handlersMap[request.javaClass] as IGenericRequestHandler<TObjRequest, TObjResponse>)

            return handler.handleRequest(request) as TObjResponse

        }
    }
}
