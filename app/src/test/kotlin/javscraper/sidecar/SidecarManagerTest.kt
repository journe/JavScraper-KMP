package javscraper.sidecar

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonObjectBuilder
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SidecarManagerTest {

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    @Test
    fun `parse listSites JSON response`() {
        val jsonStr = """{"jsonrpc":"2.0","id":"1","result":[{"id":"javbus","name":"JavBus","category":"censored"},{"id":"javdb","name":"JavDB","category":"uncensored"}]}"""
        val obj = json.parseToJsonElement(jsonStr).jsonObject
        assertEquals("2.0", obj["jsonrpc"]?.jsonPrimitive?.content)
        assertEquals("1", obj["id"]?.jsonPrimitive?.content)
        val result = obj["result"]?.jsonArray
        assertTrue(result != null && result.isNotEmpty())
        val first = result!![0].jsonObject
        assertEquals("javbus", first["id"]?.jsonPrimitive?.content)
        assertEquals("JavBus", first["name"]?.jsonPrimitive?.content)
        assertEquals("censored", first["category"]?.jsonPrimitive?.content)
    }

    @Test
    fun `parse scrape success JSON response`() {
        val jsonStr = """{
            "jsonrpc": "2.0",
            "id": "2",
            "result": {
                "success": true,
                "data": {
                    "number": "SONE-205",
                    "title": "完全タイトル",
                    "actresses": ["女優A"],
                    "maker": "SOD Create",
                    "date": "2024-06-11",
                    "duration": 120,
                    "rating": 7.5
                }
            }
        }"""
        val obj = json.parseToJsonElement(jsonStr).jsonObject
        val result = obj["result"]?.jsonObject
        assertTrue(result != null)
        assertEquals(true, result!!["success"]?.jsonPrimitive?.boolean)
        val data = result["data"]?.jsonObject
        assertTrue(data != null)
        assertEquals("SONE-205", data!!["number"]?.jsonPrimitive?.content)
        assertEquals("完全タイトル", data["title"]?.jsonPrimitive?.content)
    }

    @Test
    fun `parse scrape error JSON response`() {
        val jsonStr = """{
            "jsonrpc": "2.0",
            "id": "3",
            "result": {
                "success": false,
                "error": {"code": -1, "message": "No number provided"}
            }
        }"""
        val obj = json.parseToJsonElement(jsonStr).jsonObject
        val result = obj["result"]?.jsonObject
        assertTrue(result != null)
        assertEquals(false, result!!["success"]?.jsonPrimitive?.boolean)
        val error = result["error"]?.jsonObject
        assertTrue(error != null)
        assertEquals(-1, error!!["code"]?.jsonPrimitive?.int)
        assertEquals("No number provided", error["message"]?.jsonPrimitive?.content)
    }

    @Test
    fun `parse get_capabilities response`() {
        val jsonStr = """{
            "jsonrpc": "2.0",
            "id": "4",
            "result": {
                "version": "1.0.0",
                "sites": [{"id": "javbus", "name": "JavBus"}],
                "features": ["scrape", "search", "probe"]
            }
        }"""
        val obj = json.parseToJsonElement(jsonStr).jsonObject
        val result = obj["result"]?.jsonObject
        assertTrue(result != null)
        assertEquals("1.0.0", result!!["version"]?.jsonPrimitive?.content)
        val features = result["features"]?.jsonArray
        assertTrue(features != null)
        assertTrue(features.any { it.jsonPrimitive.content == "scrape" })
    }

    @Test
    fun `parse JSON-RPC error response`() {
        val jsonStr = """{"jsonrpc":"2.0","id":"5","error":{"code":-32601,"message":"Method not found"}}"""
        val obj = json.parseToJsonElement(jsonStr).jsonObject
        val error = obj["error"]?.jsonObject
        assertTrue(error != null)
        assertEquals(-32601, error!!["code"]?.jsonPrimitive?.int)
        assertEquals("Method not found", error["message"]?.jsonPrimitive?.content)
    }

    @Test
    fun `build JSON-RPC request structure`() {
        val request = buildJsonObject {
            put("jsonrpc", "2.0")
            put("id", "1")
            put("method", "list_sites")
            put("params", buildJsonObject { })
        }
        val jsonStr = json.encodeToString(JsonObject.serializer(), request)
        val parsed = json.parseToJsonElement(jsonStr).jsonObject
        assertEquals("2.0", parsed["jsonrpc"]?.jsonPrimitive?.content)
        assertEquals("list_sites", parsed["method"]?.jsonPrimitive?.content)
    }

    @Test
    fun `build scrape request with params`() {
        val request = buildJsonObject {
            put("jsonrpc", "2.0")
            put("id", "2")
            put("method", "scrape")
            put("params", buildJsonObject {
                put("number", "SONE-205")
                put("site", "javbus")
            })
        }
        val jsonStr = json.encodeToString(JsonObject.serializer(), request)
        val parsed = json.parseToJsonElement(jsonStr).jsonObject
        val params = parsed["params"]?.jsonObject
        assertTrue(params != null)
        assertEquals("SONE-205", params!!["number"]?.jsonPrimitive?.content)
        assertEquals("javbus", params["site"]?.jsonPrimitive?.content)
    }
}
