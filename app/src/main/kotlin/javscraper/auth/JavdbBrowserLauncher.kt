package javscraper.auth

import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.time.Duration
import java.util.concurrent.TimeUnit
import kotlin.io.path.absolutePathString
import kotlin.io.path.isDirectory
import kotlin.io.path.isRegularFile
import kotlin.io.path.readText

data class DevToolsEndpoint(
    val port: Int,
    val browserWebSocketUrl: String
) {
    val isLocal: Boolean
        get() = port in 1..65_535 && browserWebSocketUrl.startsWith("ws://127.0.0.1:$port/")
}

class JavdbBrowserSession(
    private val process: Process,
    private val profileDir: Path
) {
    fun waitForEndpoint(timeout: Duration = Duration.ofSeconds(20)): DevToolsEndpoint {
        val file = profileDir.resolve("DevToolsActivePort")
        val deadline = System.nanoTime() + timeout.toNanos()
        var lastError: Exception? = null

        while (System.nanoTime() < deadline) {
            if (!process.isAlive) throw IllegalStateException("Browser exited before DevTools was ready")
            try {
                if (Files.isRegularFile(file)) {
                    val endpoint = parseDevToolsActivePort(Files.readString(file))
                    if (endpoint.isLocal) return endpoint
                }
            } catch (error: Exception) {
                lastError = error
            }
            Thread.sleep(200)
        }
        throw IllegalStateException(
            if (lastError != null) "Browser DevTools startup failed" else "Browser DevTools startup timed out",
            lastError
        )
    }

    fun close() {
        terminateProcessTree()
        deleteTemporaryProfile()
    }

    private fun terminateProcessTree() {
        if (!process.isAlive) return
        if (System.getProperty("os.name").lowercase().contains("windows")) {
            try {
                ProcessBuilder(
                    "taskkill", "/PID", process.pid().toString(), "/T", "/F"
                )
                    .redirectOutput(ProcessBuilder.Redirect.DISCARD)
                    .redirectError(ProcessBuilder.Redirect.DISCARD)
                    .start()
                    .waitFor(5, TimeUnit.SECONDS)
            } catch (_: IOException) {
            }
        }
        process.destroyForcibly()
        repeat(20) {
            if (!process.isAlive) return@repeat
            Thread.sleep(100)
        }
    }

    private fun deleteTemporaryProfile() {
        val tempRoot = Path.of(System.getProperty("java.io.tmpdir")).toAbsolutePath().normalize()
        val target = profileDir.toAbsolutePath().normalize()
        if (!target.startsWith(tempRoot) || !target.toString().contains("javdb-login-")) return

        repeat(10) {
            try {
                Files.walk(target).use { paths ->
                    paths.sorted(compareByDescending { it.toString().length }).forEach(Files::deleteIfExists)
                }
                if (!Files.exists(target)) return
            } catch (_: IOException) {
            }
            Thread.sleep(200)
        }
    }
}

object JavdbBrowserLauncher {
    fun resolveExecutable(configured: String? = null): Path {
        val explicit = configured ?: System.getenv("JAVSCRAPER_JAVDB_BROWSER")
        if (!explicit.isNullOrBlank()) {
            val path = Path.of(explicit).toAbsolutePath().normalize()
            require(path.isRegularFile()) { "Configured browser executable does not exist" }
            return path
        }
        return installedCandidates().firstOrNull { it.isRegularFile() }
            ?: throw IllegalArgumentException("No Edge or Chrome executable was found")
    }

    fun buildCommand(
        executable: Path,
        profileDir: Path,
        loginUrl: String,
        headless: Boolean = false
    ): List<String> = buildList {
        add(executable.absolutePathString())
        add("--user-data-dir=${profileDir.absolutePathString()}")
        add("--remote-debugging-port=0")
        if (headless) {
            add("--headless=new")
            add("--disable-gpu")
        }
        add("--no-first-run")
        add("--no-default-browser-check")
        add(loginUrl)
    }

    fun launch(
        executable: Path = resolveExecutable(),
        loginUrl: String,
        profileDir: Path = Files.createTempDirectory("javdb-login-"),
        headless: Boolean = false
    ): JavdbBrowserSession {
        require(profileDir.isDirectory()) { "Browser profile directory does not exist" }
        val process = ProcessBuilder(buildCommand(executable, profileDir, loginUrl, headless))
            .redirectOutput(ProcessBuilder.Redirect.DISCARD)
            .redirectError(ProcessBuilder.Redirect.DISCARD)
            .start()
        return JavdbBrowserSession(process, profileDir)
    }

    private fun installedCandidates(): List<Path> {
        val windows = System.getProperty("os.name").lowercase().contains("windows")
        if (windows) {
            val programFiles = System.getenv("ProgramFiles") ?: "C:\\Program Files"
            val programFilesX86 = System.getenv("ProgramFiles(x86)") ?: "C:\\Program Files (x86)"
            val localAppData = System.getenv("LOCALAPPDATA") ?: ""
            return listOf(
                Path.of(programFilesX86, "Microsoft", "Edge", "Application", "msedge.exe"),
                Path.of(programFiles, "Microsoft", "Edge", "Application", "msedge.exe"),
                Path.of(programFiles, "Google", "Chrome", "Application", "chrome.exe"),
                Path.of(programFilesX86, "Google", "Chrome", "Application", "chrome.exe"),
                Path.of(localAppData, "Google", "Chrome", "Application", "chrome.exe")
            )
        }
        return listOf(
            Path.of("/usr/bin/microsoft-edge"),
            Path.of("/usr/bin/google-chrome"),
            Path.of("/usr/bin/chromium"),
            Path.of("/Applications/Google Chrome.app/Contents/MacOS/Google Chrome")
        )
    }
}

fun parseDevToolsActivePort(content: String): DevToolsEndpoint {
    val lines = content.lineSequence().map { it.trim() }.filter { it.isNotBlank() }.toList()
    require(lines.size >= 2) { "Invalid DevToolsActivePort content" }
    val port = lines[0].toIntOrNull()
    require(port != null && port in 1..65_535) { "Invalid DevTools port" }
    val path = lines[1]
    require(path.startsWith("/devtools/browser/")) { "Invalid DevTools browser path" }
    return DevToolsEndpoint(port, "ws://127.0.0.1:$port$path")
}
