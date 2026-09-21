package javscraper.auth

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.put
import java.net.URI
import java.net.http.HttpClient
import java.net.http.WebSocket
import java.time.Duration
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CompletionStage
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

private val json = Json { ignoreUnknownKeys = true }

@Serializable
private data class DevToolsTarget(
    val type: String = "",
    val url: String = "",
    val webSocketDebuggerUrl: String = ""
)

fun runtimeEvaluateRequest(id: Int, expression: String): String = json.encodeToString(
    buildJsonObject {
        put("id", id)
        put("method", "Runtime.evaluate")
        put("params", buildJsonObject {
            put("expression", expression)
            put("returnByValue", true)
            put("awaitPromise", true)
        })
    }
)

fun networkCookiesRequest(id: Int, url: String): String = json.encodeToString(
    buildJsonObject {
        put("id", id)
        put("method", "Network.getCookies")
        put("params", buildJsonObject {
            put("urls", JsonArray(listOf(kotlinx.serialization.json.JsonPrimitive(url))))
        })
    }
)

fun cdpEvaluatedValue(payload: String): String {
    val result = json.parseToJsonElement(payload).jsonObject["result"]?.jsonObject
        ?: error("CDP response has no result")
    val value = result["result"]?.jsonObject ?: error("CDP evaluation has no value")
    return value["value"]?.jsonPrimitive?.content ?: error("CDP evaluation value is not a string")
}

class JavdbCdpClient private constructor(
    private val httpClient: HttpClient,
    private val webSocket: WebSocket,
    private val listener: Listener
) : AutoCloseable {
    private val nextId = AtomicInteger(1)

    fun evaluate(expression: String): String {
        val id = nextId.getAndIncrement()
        val result = request(id, runtimeEvaluateRequest(id, expression))
        val evaluated = result["result"]?.jsonObject ?: error("CDP evaluation has no value")
        return evaluated["value"]?.jsonPrimitive?.content ?: error("CDP evaluation value is not a string")
    }

    fun cookies(url: String): String {
        val id = nextId.getAndIncrement()
        val result = request(id, networkCookiesRequest(id, url))
        return result.toString()
    }

    override fun close() {
        webSocket.sendClose(WebSocket.NORMAL_CLOSURE, "javdb-login-complete")
    }

    private fun request(id: Int, payload: String): JsonObject {
        val future = CompletableFuture<JsonObject>()
        listener.pending[id] = future
        webSocket.sendText(payload, true).join()
        try {
            val response = future.get(15, TimeUnit.SECONDS)
            if (response.containsKey("error")) {
                error("CDP command failed")
            }
            return response["result"]?.jsonObject ?: error("CDP response has no result")
        } finally {
            listener.pending.remove(id)
        }
    }

    private class Listener : WebSocket.Listener {
        val pending = ConcurrentHashMap<Int, CompletableFuture<JsonObject>>()
        private val incoming = StringBuilder()

        override fun onOpen(webSocket: WebSocket) {
            webSocket.request(1)
        }

        override fun onText(webSocket: WebSocket, data: CharSequence, last: Boolean): CompletionStage<*>? {
            val completeText = synchronized(incoming) {
                incoming.append(data)
                if (last) {
                    val text = incoming.toString()
                    incoming.setLength(0)
                    text
                } else {
                    null
                }
            }
            completeText?.let(::handleCompleteText)
            webSocket.request(1)
            return null
        }

        override fun onError(webSocket: WebSocket, error: Throwable) {
            pending.values.forEach { it.completeExceptionally(error) }
            pending.clear()
        }

        private fun handleCompleteText(text: String) {
            val response = try {
                json.parseToJsonElement(text).jsonObject
            } catch (_: IllegalArgumentException) {
                return
            }
            val id = response["id"]?.jsonPrimitive?.content?.toIntOrNull() ?: return
            pending.remove(id)?.complete(response)
        }
    }

    companion object {
        fun connect(endpoint: DevToolsEndpoint, loginUrl: String): JavdbCdpClient {
            val client = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .connectTimeout(Duration.ofSeconds(5))
                .build()
            val webSocketUrl = waitForPageTarget(client, endpoint, loginUrl)
            val listener = Listener()
            val webSocket = client.newWebSocketBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .buildAsync(URI.create(webSocketUrl), listener)
                .get(5, TimeUnit.SECONDS)
            return JavdbCdpClient(client, webSocket, listener)
        }

        private fun waitForPageTarget(
            client: HttpClient,
            endpoint: DevToolsEndpoint,
            loginUrl: String
        ): String {
            val deadline = System.nanoTime() + Duration.ofSeconds(15).toNanos()
            while (System.nanoTime() < deadline) {
                val targetUrl = findPageTarget(client, endpoint, loginUrl)
                if (targetUrl != null) return targetUrl
                Thread.sleep(200)
            }
            error("JavDB page target was not found")
        }

        private fun findPageTarget(
            client: HttpClient,
            endpoint: DevToolsEndpoint,
            loginUrl: String
        ): String? {
            val request = java.net.http.HttpRequest.newBuilder(
                URI.create("http://127.0.0.1:${endpoint.port}/json/list")
            )
                .header("Origin", "http://127.0.0.1")
                .timeout(Duration.ofSeconds(3))
                .GET()
                .build()
            val response = client.send(request, java.net.http.HttpResponse.BodyHandlers.ofString())
            if (response.statusCode() != 200) return null
            val targets = json.decodeFromString(
                ListSerializer(DevToolsTarget.serializer()),
                response.body()
            )
            return targets
                .firstOrNull { target ->
                    target.type == "page" &&
                            target.webSocketDebuggerUrl.isNotBlank() &&
                            sameHost(target.url, loginUrl)
                }
                ?.webSocketDebuggerUrl
        }

        private fun sameHost(left: String, right: String): Boolean {
            val leftHost = runCatching { URI(left).host?.lowercase() }.getOrNull()
            val rightHost = runCatching { URI(right).host?.lowercase() }.getOrNull()
            return !leftHost.isNullOrBlank() && leftHost == rightHost
        }
    }
}
