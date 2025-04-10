package dev.lewik.mcptools.proxy

import io.github.oshai.kotlinlogging.KotlinLogging

private val logger = KotlinLogging.logger {}

fun main(args: Array<String>) {
    try {
        mainInternal(args)
    } catch (e: CommandLineException) {
        if (e.message?.isNotEmpty() == true) {
            logger.error { e.message }
        }
        System.exit(e.exitCode)
    }
}

fun mainInternal(args: Array<String>) {
    logger.info { "Starting proxy with arguments: ${args.joinToString(" ")}" }
    Main.runWithConfig(args)
} 