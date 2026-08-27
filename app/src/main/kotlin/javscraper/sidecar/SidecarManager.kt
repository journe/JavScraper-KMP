package javscraper.sidecar

import javscraper.models.ScrapeResult
import javscraper.models.SiteCheckResult
import javscraper.models.SiteInfo
import javscraper.models.Video
import kotlinx.coroutines.*
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.*
import java.io.*
import java.util.concurrent.TimeUnit
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

@OptIn(ExperimentalSerializationApi::class)
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

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        namingStrategy = JsonNamingStrategy.SnakeCase
    }
    private val prettyJson = Json { prettyPrint = true }

    suspend fun start(): Boolean = mutex.withLock {
        val startedAt = System.currentTimeMillis()
        log.info { "Worker start: path=$workerPath" }
        val currentProcess = process
        if (currentProcess != null && currentProcess.isAlive) {
            log.info { "Worker already running: pid=${currentProcess.pid()}" }
            return@withLock true
        }
        try {
            // cmd /c 需要引号包裹含空格的可执行文件路径，否则会按空格拆分
            val command = if (workerPath.contains(' ')) "\"$workerPath\"" else workerPath
            val processBuilder = ProcessBuilder("cmd.exe", "/c", command)
            processBuilder.redirectErrorStream(false)
            processBuilder.environment()["PYTHONIOENCODING"] = "utf-8"
            processBuilder.environment()["PYTHONUNBUFFERED"] = "1"
            val startedProcess = processBuilder.start()
            process = startedProcess
            stdin = startedProcess.outputStream.bufferedWriter(Charsets.UTF_8)
            stdoutReader = startedProcess.inputStream.bufferedReader(Charsets.UTF_8)
            Thread {
                try {
                    startedProcess.errorStream.bufferedReader(Charsets.UTF_8).use { errorReader ->
                        var line: String?
                        while (errorReader.readLine().also { line = it } != null) {
                            log.warn { "[Worker stderr] $line" }
                        }
                    }
                } catch (_: IOException) {
                }
            }.also { it.isDaemon = true; it.start() }
            scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
            responseReader = scope?.launch { readResponses() }
            delay(500)
            if (process == null || process!!.isAlive.not()) {
                log.warn {
                    "Worker process exited shortly after start: path=$workerPath, elapsedMs=${
                        System.currentTimeMillis() - startedAt
                    }"
                }
                cleanup()
                return@withLock false
            }
            log.info {
                "Worker started: pid=${process?.pid()}, elapsedMs=${System.currentTimeMillis() - startedAt}"
            }
            true
        } catch (e: Exception) {
            log.error(e) {
                "Worker start failed: path=$workerPath, elapsedMs=${System.currentTimeMillis() - startedAt}"
            }
            cleanup()
            false
        }
    }

    suspend fun stop() {
        val startedAt = System.currentTimeMillis()
        log.info { "Worker stop: path=$workerPath" }
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
            } catch (e: Exception) {
                log.warn(e) { "Worker shutdown request skipped or failed" }
            }
            // Give the worker a moment to exit on its own after receiving shutdown.
            repeat(15) {
                val currentProcess = process
                if (currentProcess == null || currentProcess.isAlive.not()) return@repeat
                delay(100)
            }
            cleanup()
        }
        log.info { "Worker stopped: elapsedMs=${System.currentTimeMillis() - startedAt}" }
    }

    override fun close() {
        log.info { "Worker close: path=$workerPath" }
        runBlocking { stop() }
        log.info { "Worker closed" }
    }

    suspend fun listSites(): List<SiteInfo> {
        val startedAt = System.currentTimeMillis()
        log.info { "list_sites request" }
        return try {
            val sites = sendRequest("list_sites", JsonObject(emptyMap())).jsonArray.map {
                SiteInfo(
                    it.jsonObject["id"]?.jsonPrimitive?.content ?: "",
                    it.jsonObject["name"]?.jsonPrimitive?.content ?: ""
                )
            }
            log.info {
                "list_sites success: count=${sites.size}, elapsedMs=${System.currentTimeMillis() - startedAt}"
            }
            sites
        } catch (e: Exception) {
            log.error(e) {
                "list_sites failed: elapsedMs=${System.currentTimeMillis() - startedAt}"
            }
            throw e
        }
    }

    suspend fun checkSites(sites: List<String>? = null): List<SiteCheckResult> {
        val startedAt = System.currentTimeMillis()
        log.info { "check_sites request: sites=${sites?.joinToString(",") ?: "all"}" }
        val params = buildJsonObject {
            if (!sites.isNullOrEmpty()) put("sites", JsonArray(sites.map { JsonPrimitive(it) }))
        }
        return try {
            val results = sendRequest("check_sites", params).jsonArray.map {
                json.decodeFromJsonElement<SiteCheckResult>(it)
            }
            log.info {
                "check_sites success: count=${results.size}, elapsedMs=${
                    System.currentTimeMillis() - startedAt
                }"
            }
            results
        } catch (e: Exception) {
            log.error(e) {
                "check_sites failed: elapsedMs=${System.currentTimeMillis() - startedAt}"
            }
            throw e
        }
    }

    suspend fun scrape(
        number: String,
        site: String? = null,
        enabledSites: List<String>? = null
    ): ScrapeResult {
        val startedAt = System.currentTimeMillis()
        log.info {
            "scrape request: number=$number, site=${site ?: "auto"}, enabledSites=${enabledSites?.size ?: "all"}"
        }
        val params = buildJsonObject {
            put("number", number)
            site?.let { put("site", it) }
            enabledSites?.let { put("sites", JsonArray(it.map(::JsonPrimitive))) }
        }
        return try {
            val result = json.decodeFromJsonElement<ScrapeResult>(sendRequest("scrape", params))
            if (result.success) {
                log.info {
                    "scrape success: number=$number, elapsedMs=${System.currentTimeMillis() - startedAt}"
                }
            } else {
                log.warn {
                    "scrape failed: number=$number, error=${result.error?.message}, elapsedMs=${
                        System.currentTimeMillis() - startedAt
                    }"
                }
            }
            result
        } catch (e: Exception) {
            log.error(e) {
                "scrape error: number=$number, elapsedMs=${System.currentTimeMillis() - startedAt}"
            }
            throw e
        }
    }

    suspend fun searchCandidates(
        number: String,
        site: String? = null,
        enabledSites: List<String>? = null
    ): List<Video> {
        val startedAt = System.currentTimeMillis()
        log.info {
            "search request: number=$number, site=${site ?: "auto"}, enabledSites=${enabledSites?.size ?: "all"}"
        }
        val params = buildJsonObject {
            put("number", number)
            site?.let { put("site", it) }
            enabledSites?.let { put("sites", JsonArray(it.map(::JsonPrimitive))) }
        }
        return try {
            val candidates = json.decodeFromJsonElement<List<Video>>(sendRequest("search", params))
            log.info {
                "search success: number=$number, count=${candidates.size}, elapsedMs=${System.currentTimeMillis() - startedAt}"
            }
            candidates
        } catch (e: Exception) {
            log.error(e) {
                "search failed: number=$number, elapsedMs=${System.currentTimeMillis() - startedAt}"
            }
            throw e
        }
    }
    private suspend fun sendRequest(method: String, params: JsonObject): JsonElement {
        val startedAt = System.currentTimeMillis()
        val id = requestId.incrementAndGet().toString()
        log.info { "JSON-RPC request: id=$id, method=$method" }
        ensureRunning()
        val deferred = CompletableDeferred<JsonElement>()
        pendingRequests[id] = deferred
        try {
            val request = buildJsonObject {
                put("jsonrpc", "2.0")
                put("id", id)
                put("method", method)
                put("params", params)
            }
            val requestBody = json.encodeToString(JsonObject.serializer(), request)
            log.info {
                "JSON-RPC request body: ${prettyJson.encodeToString(JsonObject.serializer(), request)}"
            }
            mutex.withLock {
                stdin?.write(requestBody)
                stdin?.newLine()
                stdin?.flush()
            }
            val response = withTimeout(15_000) { deferred.await() }
            log.info {
                "JSON-RPC response: id=$id, elapsedMs=${System.currentTimeMillis() - startedAt}"
            }
            return response
        } catch (e: Exception) {
            log.error(e) {
                "JSON-RPC failed: id=$id, method=$method, elapsedMs=${
                    System.currentTimeMillis() - startedAt
                }"
            }
            pendingRequests.remove(id)
            throw e
        }
    }

    private suspend fun readResponses() {
        log.info { "Response reader started" }
        try {
            var line: String?
            while (stdoutReader?.readLine().also { line = it } != null) {
                if (line.isNullOrBlank()) continue
                try {
                    val response = json.parseToJsonElement(line.orEmpty()).jsonObject
                    val id = response["id"]?.jsonPrimitive?.contentOrNull
                    if (id != null) {
                        log.info {
                            "JSON-RPC response body: id=$id, json=${
                                prettyJson.encodeToString(JsonObject.serializer(), response)
                            }"
                        }
                        val deferred = pendingRequests.remove(id)
                        if (deferred != null) {
                            val error = response["error"]
                            if (error != null && error !is JsonNull) {
                                deferred.completeExceptionally(
                                    RuntimeException(
                                        error.jsonObject["message"]?.jsonPrimitive?.contentOrNull ?: ""
                                    )
                                )
                            } else {
                                deferred.complete(response["result"] ?: JsonNull)
                            }
                        }
                    }
                } catch (e: Exception) {
                    log.warn(e) { "Response parse error: ${e.message}" }
                }
            }
            log.info { "Response reader reached end of stream" }
        } catch (e: IOException) {
            log.info { "Response reader closed: ${e.message}" }
        }
    }

    private fun ensureRunning() {
        val currentProcess = process
        if (currentProcess == null || currentProcess.isAlive.not()) {
            log.warn { "Worker not running: path=$workerPath" }
            throw RuntimeException("Worker not running")
        }
        log.debug { "Worker running check passed: pid=${currentProcess.pid()}" }
    }

    private fun cleanup() {
        log.info { "Worker cleanup: path=$workerPath" }
        try {
            process?.let { currentProcess ->
                if (currentProcess.isAlive) {
                    try {
                        ProcessBuilder(
                            "taskkill",
                            "/PID",
                            currentProcess.pid().toString(),
                            "/T",
                            "/F"
                        )
                            .redirectErrorStream(true)
                            .start()
                            .waitFor(5, TimeUnit.SECONDS)
                    } catch (e: Exception) {
                        log.warn(e) { "Worker taskkill failed: pid=${currentProcess.pid()}" }
                    }
                    if (currentProcess.isAlive) currentProcess.destroyForcibly()
                }
            }
        } catch (e: Exception) {
            log.warn(e) { "Worker cleanup failed" }
        }
        responseReader?.cancel()
        process = null
        stdin = null
        stdoutReader = null
        pendingRequests.clear()
        log.info { "Worker cleanup completed" }
    }
}
