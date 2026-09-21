package javscraper.auth

import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class JavdbBrowserLauncherTest {
    @Test
    fun `configured browser wins over installed candidates`() {
        val directory = Files.createTempDirectory("javscraper-browser-test-")
        val executable = Files.createFile(directory.resolve("custom-browser.exe"))

        val resolved = JavdbBrowserLauncher.resolveExecutable(executable.toString())

        assertEquals(executable.toAbsolutePath().normalize(), resolved)
    }

    @Test
    fun `configured browser must exist`() {
        assertFailsWith<IllegalArgumentException> {
            JavdbBrowserLauncher.resolveExecutable("F:/does-not-exist/browser.exe")
        }
    }

    @Test
    fun `browser uses isolated profile and random debug port`() {
        val executable = java.nio.file.Path.of("F:/tools/browser.exe")
        val profile = java.nio.file.Path.of("F:/temp/profile")
        val command = JavdbBrowserLauncher.buildCommand(executable, profile, "https://javdb580.com/login")

        assertEquals(
            listOf(
                executable.toString(),
                "--user-data-dir=$profile",
                "--remote-debugging-port=0",
                "--no-first-run",
                "--no-default-browser-check",
                "https://javdb580.com/login"
            ),
            command
        )
    }

    @Test
    fun `headless browser adds rendering flags`() {
        val command = JavdbBrowserLauncher.buildCommand(
            java.nio.file.Path.of("F:/tools/browser.exe"),
            java.nio.file.Path.of("F:/temp/profile"),
            "https://javdb580.com/login",
            headless = true
        )

        assertTrue("--headless=new" in command)
        assertTrue("--disable-gpu" in command)
    }

    @Test
    fun `parses devtools active port file`() {
        val endpoint = parseDevToolsActivePort(
            "52341\n/devtools/browser/7f7f7f-1111\n"
        )

        assertEquals(52341, endpoint.port)
        assertEquals(
            "ws://127.0.0.1:52341/devtools/browser/7f7f7f-1111",
            endpoint.browserWebSocketUrl
        )
        assertTrue(endpoint.isLocal)
    }
}
