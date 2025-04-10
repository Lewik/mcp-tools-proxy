package dev.lewik.mcptools.proxy

/**
 * Entry point for Native
 */
fun main(args: Array<String>) {
    try {
        Main.runWithConfig(args)
    } catch (e: CommandLineException) {
        println(e.message)
        kotlin.system.exitProcess(e.exitCode)
    } catch (e: Exception) {
        println("Error: ${e.message}")
        kotlin.system.exitProcess(1)
    }
} 