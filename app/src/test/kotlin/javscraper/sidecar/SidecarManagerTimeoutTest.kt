package javscraper.sidecar

import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue
import kotlin.test.fail
import kotlinx.coroutines.runBlocking
import kotlin.coroutines.cancellation.CancellationException

/** 回归测试：worker 响应超时必须抛出普通异常（而非 CancellationException），
 *  否则调用方（如 SingleScrapeController）会将其视为协程取消而跳过错误处理，
 *  导致单文件刮削对话框停留在 loading 状态。 */
class SidecarManagerTimeoutTest {

    @Test
    fun `request timeout throws plain exception not cancellation`() = runBlocking {
        // 伪 worker：读取请求后永不回复
        val workerScript = File.createTempFile("javscraper-silent-worker-", ".bat")
        workerScript.writeText(
            "@echo off\r\n" +
                "set /p request=\r\n" +
                "ping -n 30 127.0.0.1 > nul\r\n"
        )
        val manager = SidecarManager(workerScript.absolutePath, requestTimeoutMs = 300)

        try {
            assertTrue(manager.start(), "Fake worker should start")
            try {
                manager.searchCandidates("ABC-123")
                fail("Expected SidecarTimeoutException")
            } catch (e: SidecarTimeoutException) {
                assertTrue(e.message?.contains("Timed out waiting for 300 ms") == true, e.message)
            }
        } finally {
            manager.close()
            workerScript.delete()
        }
    }
}
