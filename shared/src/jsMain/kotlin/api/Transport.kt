package api

import kotlinx.browser.window
import kotlinx.coroutines.await
import kotlinx.coroutines.withContext
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer
import org.w3c.fetch.RequestCredentials
import org.w3c.fetch.RequestInit
import kotlin.coroutines.CoroutineContext
import kotlin.js.json

@JsName("encodeURIComponent")
external fun urlEncode(value: String): String

@JsName("decodeURIComponent")
external fun urlDecode(encoded: String): String

class Transport(private val coroutineContext: CoroutineContext) {

    internal suspend fun <T> get(
        url: String,
        deserializationStrategy: KSerializer<T>,
        vararg args: Pair<String, Any>,
        isJson: Boolean = true
    ): T {
        return parse(deserializationStrategy, fetch("GET", url, isJson, *args), isJson)
    }

    @OptIn(InternalSerializationApi::class)
    internal suspend fun <T> post(
        url: String,
        deserializationStrategy: KSerializer<T>,
        vararg args: Pair<String, Any>,
        isJson: Boolean = true
    ): T {
        val stringArgs = args.map {
            it.first to Json.encodeToString(it.second::class.serializer().unsafeCast<KSerializer<Any>>(), it.second)
        }
        return parse(deserializationStrategy, fetch("POST", url, isJson, *stringArgs.toTypedArray()), isJson)
    }

    internal suspend fun <T> postByTriples(
        url: String,
        deserializationStrategy: KSerializer<T>,
        vararg args: Triple<String, Any, KSerializer<*>>,
        isJson: Boolean = true
    ): T {
        val stringArgs = args.map {
            it.first to Json.encodeToString(it.third.unsafeCast<KSerializer<Any>>(), it.second)
        }
        return parse(deserializationStrategy, fetch("POST", url, isJson, *stringArgs.toTypedArray()), isJson)
    }

    internal suspend fun <T> getList(
        url: String,
        deserializationStrategy: KSerializer<T>,
        vararg args: Pair<String, Any?>,
        isJson: Boolean = true
    ): List<T> {
        return parse(ListSerializer(deserializationStrategy), fetch("GET", url, isJson, *args), isJson)
    }

    private suspend fun fetch(
        method: String,
        shortUrl: String,
        isJson: Boolean,
        vararg args: Pair<String, Any?>,
    ): String {
        var url = if (isJson) "/api/$shortUrl" else shortUrl

        if (method == "GET" && args.isNotEmpty()) {
            url += "?"
            url += args.joinToString("&", transform = { "${it.first}=${urlEncode(it.second.toString())}" })
        }

        return withContext(coroutineContext) {
            val response = window.fetch(
                url, RequestInit(
                    method = method,
                    headers = json(
                        "Accept" to "application/json; charset=UTF-8",
                        "Content-Type" to "application/json; charset=UTF-8"
                    ),
                    credentials = "same-origin".unsafeCast<RequestCredentials>(),
                    body = if (method == "POST") JSON.stringify(json(*args)).also { println(it) } else undefined
                )
            ).await()

            response.text().await()
        }
    }

}

@Suppress("UNCHECKED_CAST")
fun <T> parse(serializationStrategy: DeserializationStrategy<T>, string: String, isJson: Boolean): T {
    if (isJson) {
        return try {
            Json.decodeFromString(serializationStrategy, string)
        } catch (e: Throwable) {
            throw TransportException(e.message ?: "")
        }
    } else {
        return string as? T ?: error("we should want a json or a string")
    }
}

class TransportException(message: String) : Exception(message)