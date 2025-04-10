package dev.lewik.mcptools.proxy

import io.github.oshai.kotlinlogging.KotlinLogging
import dev.lewik.mcptools.proxy.config.ToolsFilterConfig
import io.modelcontextprotocol.kotlin.sdk.client.StdioClientTransport
import io.modelcontextprotocol.kotlin.sdk.server.StdioServerTransport
import kotlinx.io.*

private val logger = KotlinLogging.logger {}

object ProxyFactory {

    suspend fun createProcessToStdioProxy(
        config: ToolsFilterConfig,
        stdin: Source,
        stdout: Sink,
    ): McpToolsProxy {
        logger.info { "Creating proxy with upstream process and stdio for clients" }

        val upstreamCommand = config.upstream
        logger.debug { "Upstream command: $upstreamCommand" }

        val command = parseCommandLineWithQuotes(upstreamCommand)
        logger.debug { "Parsed arguments: ${command.joinToString(" | ")}" }

        val process = ProcessBuilder(*command).start()

        Thread {
            try {
                val reader = process.errorStream.bufferedReader()
                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    logger.error { "UPSTREAM-ERROR: $line" }
                }
            } catch (e: Exception) {
                logger.error(e) { "Error reading error stream" }
            }
        }.start()

        val upstreamTransport = StdioClientTransport(
            input = process.inputStream.asSource().buffered(),
            output = process.outputStream.asSink().buffered()
        )

        val proxyTransport = StdioServerTransport(
            inputStream = stdin.buffered(),
            outputStream = stdout.buffered()
        )

        return McpToolsProxy(config, upstreamTransport, proxyTransport).apply {
            setDebugLogger { message ->
                // debugFile.appendText("${System.currentTimeMillis()}: $message\n")
            }
            init()
        }
    }

    private fun parseCommandLineWithQuotes(commandLine: String): Array<String> {
        val result = mutableListOf<String>()
        var current = StringBuilder()
        var inSingleQuotes = false
        var inDoubleQuotes = false
        var escaped = false

        for (c in commandLine) {
            when {
                escaped -> {
                    current.append(c)
                    escaped = false
                }

                c == '\\' -> {
                    escaped = true
                }

                c == '\'' && !inDoubleQuotes -> {
                    inSingleQuotes = !inSingleQuotes
                }

                c == '"' && !inSingleQuotes -> {
                    inDoubleQuotes = !inDoubleQuotes
                }

                c.isWhitespace() && !inSingleQuotes && !inDoubleQuotes -> {
                    if (current.isNotEmpty()) {
                        result.add(current.toString())
                        current = StringBuilder()
                    }
                }

                else -> {
                    current.append(c)
                }
            }
        }

        if (current.isNotEmpty()) {
            result.add(current.toString())
        }

        if (inSingleQuotes || inDoubleQuotes) {
            logger.warn { "Unclosed quotes in command: $commandLine" }
        }

        return result.toTypedArray()
    }
}