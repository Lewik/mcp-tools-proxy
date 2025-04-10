package io.mcptools.proxy

import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.runBlocking
import kotlinx.io.asSink
import kotlinx.io.asSource
import kotlinx.io.buffered

private val logger = KotlinLogging.logger {}

object Main {
    fun runWithConfig(args: Array<String>) {
        runBlocking {
            try {
                logger.info { "Starting MCP Tools Proxy" }

                val config = CommandLineParser.parseCommandLineArgs(args)

                logger.info { "Starting proxy with configuration: $config" }

                val proxy = ProxyFactory.createProcessToStdioProxy(
                    config = config,
                    stdin = getStandardInput().buffered(),
                    stdout = getStandardOutput().buffered()
                )

                setupShutdownHook(proxy)

                waitForShutdown()

            } catch (e: Exception) {
                logger.error(e) { "Error starting proxy" }
                System.err.println("Error: ${e.message}")
                System.exit(1)
            }
        }
    }

    private fun getStandardInput() = System.`in`.asSource()

    private fun getStandardOutput() = System.out.asSink()

    private fun setupShutdownHook(proxy: McpToolsProxy) {
        Runtime.getRuntime().addShutdownHook(Thread {
            runBlocking {
                logger.info { "Shutting down on stop signal" }
                proxy.close()
            }
        })
    }

    private fun waitForShutdown() {
        Thread.currentThread().join()
    }
} 