import api.ServerController
import io.ktor.application.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.util.*
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.SetSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer
import kotlin.reflect.KFunction
import kotlin.reflect.full.*
import kotlin.reflect.jvm.jvmErasure

@OptIn(InternalSerializationApi::class)
@Suppress("UNCHECKED_CAST")
suspend fun queryBody(function: KFunction<*>, call: ApplicationCall, args: MutableList<Any?>) {
    val result = function.callSuspend(*args.toTypedArray())!!

    val serializedResult = if (function.returnType.arguments.isNotEmpty()) {
        when {
            function.returnType.isSubtypeOf(List::class.createType(function.returnType.arguments)) -> Json.encodeToString(
                ListSerializer(function.returnType.arguments.first().type?.jvmErasure!!.serializer() as KSerializer<Any>),
                result as List<Any>
            )
            function.returnType.isSubtypeOf(Set::class.createType(function.returnType.arguments)) -> Json.encodeToString(
                SetSerializer(function.returnType.arguments.first().type?.jvmErasure!!.serializer() as KSerializer<Any>),
                result as Set<Any>
            )
            else -> SerializationException("Method must return either List<R> or Set<R>, but it returns ${function.returnType}")
        }
    } else {
        Json.encodeToString(function.returnType.jvmErasure.serializer() as KSerializer<Any>, result)
    }
    call.respond(serializedResult)
}


val json = Json { isLenient = true }

@OptIn(InternalSerializationApi::class)
@Suppress("UNCHECKED_CAST")
inline fun <reified C: ServerController> Route.rpc() {
    val controllerKClass = C::class
    val createControllerInstance = { call: ApplicationCall ->
        controllerKClass.createInstance().also { instance ->
            instance.call = call
        }
    }

    controllerKClass.declaredFunctions.map { function ->
        if (function.name.startsWith("get")) {
            get(function.name) {
                val controller = createControllerInstance(call)
                val args = mutableListOf<Any?>(controller)
                function.valueParameters.mapTo(args) { param ->
                    call.request.queryParameters.getOrFail(param.name.toString())
                        .takeIf { it != null.toString() }
                        ?.let { strValue ->
                            json.decodeFromString(
                                param.type.jvmErasure.serializer(),
                                strValue
                            )
                        }
                }
                queryBody(function, call, args)
            }
        } else {
            post(function.name) {
                val controller = createControllerInstance(call)
                val queryParameters = json.decodeFromString(
                    MapSerializer(String.serializer(), String.serializer()),
                    call.receiveText()
                )
                val args = mutableListOf<Any?>(controller)
                function.valueParameters.mapTo(args) { param ->
                    json.decodeFromString(
                        param.type.jvmErasure.serializer(),
                        queryParameters[param.name!!] ?: error("param is missing")
                    )
                }
                queryBody(function, call, args)
            }
        }
    }
}