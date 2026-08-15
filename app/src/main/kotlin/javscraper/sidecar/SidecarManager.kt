package javscraper.sidecar

import javscraper.models.ScrapeResult
import javscraper.models.SiteInfo
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.*
import java.io.*
import java.util.concurrent.TimeUnit
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

class SidecarManager(private val workerPath: String) : AutoCloseable {
    private val log = mu.KotlinLogging.logger {}

    private var process: Process? = null
    private var stdin: BufferedWriter? = null
    private var stdoutReader: BufferedReader? = null
    private val requestId = AtomicInteger(0)
    private val pendingRequests = ConcurrentHashMap<String, CompletableDeferred<JsonElement>>()
    private val mutex = Mutex()
    private var responseReader: Job? = null
    private var scope: CoroutineScope? = null

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    suspend fun start(): Boolean = mutex.withLock {
        if (process != null && process!!.isAlive) return@withLock true
        try {
            val pb = ProcessBuilder("cmd.exe", "/c", workerPath)
            pb.redirectErrorStream(false)
            pb.environment()["PYTHONIOENCODING"] = "utf-8"
            pb.environment()["PYTHONUNBUFFERED"] = "1"
            process = pb.start()
            stdin = process!!.outputStream.bufferedWriter(Charsets.UTF_8)
            stdoutReader = process!!.inputStream.bufferedReader(Charsets.UTF_8)
            Thread {
                try {
                    process!!.errorStream.bufferedReader(Charsets.UTF_8).use { err ->
                        var l: String?; while (err.readLine().also { l = it } != null) {
                        log.warn { "[Worker stderr] $l" }
                    }
                    }
                } catch (_: IOException) {
                }
            }.also { it.isDaemon = true; it.start() }
            scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
            responseReader = scope?.launch { readResponses() }
            delay(500)
            log.info { "Worker started" }
            true
        } catch (e: Exception) {
            log.error(e) { "Worker start failed" }; cleanup(); false
        }
    }

    suspend fun stop() {
        mutex.withLock {
            try {
                ensureRunning()
                stdin?.write(
                    json.encodeToString(
                        JsonObject.serializer(),
                        buildJsonObject {
                            put("jsonrpc", "2.0")
                            put("id", "shutdown")
                            put("method", "shutdown")
                            put("params", JsonObject(emptyMap()))
                        }
                    )
                )
                stdin?.newLine()
                stdin?.flush()
            } catch (_: Exception) {
            }
            // Give the worker a moment to exit on its own after receiving shutdown
            repeat(15) {
                val p = process
                if (p == null || !p.isAlive) return@withLock
                delay(100)
            }
            cleanup()
        }
    }

    override fun close() = runBlocking { stop() }

    suspend fun listSites(): List<SiteInfo> {
        val resp = sendRequest("list_sites", JsonObject(emptyMap()))
        return resp.jsonArray.map {
            SiteInfo(
                it.jsonObject["id"]?.jsonPrimitive?.content ?: "",
                it.jsonObject["name"]?.jsonPrimitive?.content ?: ""
            )
        }
    }

    suspend fun scrape(number: String, site: String? = null): ScrapeResult {
        val params = buildJsonObject { put("number", number); site?.let { put("site", it) } }
        return json.decodeFromJsonElement(sendRequest("scrape", params))
    }

    private suspend fun sendRequest(method: String, params: JsonObject): JsonElement {
        ensureRunning()
        val id = requestId.incrementAndGet().toString()
        val deferred = CompletableDeferred<JsonElement>()
        pendingRequests[id] = deferred
        try {
            mutex.withLock {
                stdin?.write(
                    json.encodeToString(
                        JsonObject.serializer(),
                        buildJsonObject {
                            put("jsonrpc", "2.0"); put("id", id); put("method", method); put(
                            "params",
                            params
                        )
                        })
                )
                stdin?.newLine(); stdin?.flush()
            }
            return withTimeout(60_000) { deferred.await() }
        } catch (e: Exception) {
            pendingRequests.remove(id); throw e
        }
    }

    private suspend fun readResponses() {
        try {
            var line: String?; while (stdoutReader?.readLine().also { line = it } != null) {
                if (line.isNullOrBlank()) continue; try {
                    val resp = json.parseToJsonElement(line).jsonObject;
                    val id = resp["id"]?.jsonPrimitive?.contentOrNull; if (id != null) {
                        val d = pendingRequests.remove(id); if (d != null) {
                            val err = resp["error"]; if (err != null && err !is JsonNull) d.completeExceptionally(
                                RuntimeException(err.jsonObject["message"]?.jsonPrimitive?.contentOrNull ?: "")
                            ) else d.complete(resp["result"] ?: JsonNull)
                        }
                    }
                } catch (e: Exception) {
                    log.warn { "Parse error: ${e.message}" }
                }
            }
        } catch (_: IOException) {
        }
    }

    private fun ensureRunning() {
        if (process == null || !process!!.isAlive) throw RuntimeException("Worker not running")
    }

    private fun cleanup() {
        try {
            process?.let { p ->
                if (p.isAlive) {
                    try {
                        ProcessBuilder("taskkill", "/PID", p.pid().toString(), "/T", "/F")
                            .redirectErrorStream(true)
                            .start()
                            .waitFor(5, TimeUnit.SECONDS)
                    } catch (_: Exception) {
                    }
                    if (p.isAlive) p.destroyForcibly()
                }
            }
        } catch (_: Exception) {
        }
        responseReader?.cancel()
        process = null
        stdin = null
        stdoutReader = null
        pendingRequests.clear()
    }
}
