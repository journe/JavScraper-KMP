package javscraper.sidecar

import java.io.BufferedReader
import java.io.IOException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import mu.KLogger
import java.util.concurrent.ConcurrentHashMap

class SidecarRequestException(
    val code: Int,
    message: String?,
    val stage: String? = null,
    val siteId: String? = null,
    val detailUrl: String? = null
) : RuntimeException(message ?: "")

class SidecarTimeoutException(
    message: String,
    val stage: String? = null
) : RuntimeException(message)

internal class SidecarResponseReader(
    private val stdoutReader: BufferedReader?,
    private val pendingRequests: ConcurrentHashMap<String, CompletableDeferred<JsonElement>>,
    private val progressStages: ConcurrentHashMap<String, String>,
    private val json: Json,
    private val prettyJson: Json,
    private val log: KLogger
) {

    suspend fun readResponses() = withContext(Dispatchers.IO) {
        log.info { "Response reader started" }
        try {
            var line: String?
            while (stdoutReader?.readLine().also { line = it } != null) {
                if (line.isNullOrBlank()) continue
                try {
                    handleLine(line.orEmpty())
                } catch (e: Exception) {
                    log.warn(e) { "Response parse error: ${e.message}" }
                }
            }
            log.info { "Response reader reached end of stream" }
        } catch (e: IOException) {
            log.info { "Response reader closed: ${e.message}" }
        }
    }

    private fun handleLine(value: String) {
        val response = json.parseToJsonElement(value).jsonObject
        val method = response["method"]?.jsonPrimitive?.contentOrNull
        if (method == "scrape.progress") {
            handleProgress(response["params"]?.jsonObject ?: return)
            return
        }
        val id = response["id"]?.jsonPrimitive?.contentOrNull ?: return
        log.info {
            "JSON-RPC response body: id=$id, json=${prettyJson.encodeToString(JsonObject.serializer(), response)}"
        }
        val deferred = pendingRequests.remove(id) ?: return
        progressStages.remove(id)
        val error = response["error"]
        if (error != null && error !is JsonNull) {
            deferred.completeExceptionally(requestException(error.jsonObject))
        } else {
            deferred.complete(response["result"] ?: JsonNull)
        }
    }

    private fun handleProgress(params: JsonObject) {
        val requestId = params["request_id"]?.jsonPrimitive?.contentOrNull ?: return
        val stage = params["stage"]?.jsonPrimitive?.contentOrNull ?: return
        progressStages[requestId] = stage
        log.info {
            "Scrape progress: requestId=$requestId, stage=$stage, status=${params["status"]?.jsonPrimitive?.contentOrNull}, siteId=${params["site_id"]?.jsonPrimitive?.contentOrNull}, number=${params["number"]?.jsonPrimitive?.contentOrNull}, detailUrl=${params["detail_url"]?.jsonPrimitive?.contentOrNull}, message=${params["message"]?.jsonPrimitive?.contentOrNull}"
        }
    }

    private fun requestException(error: JsonObject): SidecarRequestException {
        val data = error["data"]?.jsonObject
        return SidecarRequestException(
            code = error["code"]?.jsonPrimitive?.intOrNull ?: -1,
            message = error["message"]?.jsonPrimitive?.contentOrNull,
            stage = data?.get("stage")?.jsonPrimitive?.contentOrNull,
            siteId = data?.get("site_id")?.jsonPrimitive?.contentOrNull,
            detailUrl = data?.get("detail_url")?.jsonPrimitive?.contentOrNull
        )
    }
}
