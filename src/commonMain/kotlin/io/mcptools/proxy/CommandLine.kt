package io.mcptools.proxy

import io.github.oshai.kotlinlogging.KotlinLogging
import io.mcptools.proxy.config.FilterMode
import io.mcptools.proxy.config.ToolPattern
import io.mcptools.proxy.config.ToolsFilterConfig

class CommandLineException(message: String, val exitCode: Int) : RuntimeException(message)

object CommandLineParser {
    private val logger = KotlinLogging.logger {}

    private val knownFlags = setOf("--upstream", "--allow", "--allowre", "--deny", "--denyre", "--help")

    fun parseCommandLineArgs(args: Array<String>): ToolsFilterConfig {
        logger.debug { "Parsing command line arguments: ${args.joinToString(" ")}" }

        val flagsMap = parseByKnownFlag(args, knownFlags)

        if ("--help" in flagsMap) {
            PlatformUtility.printHelp()
            throw CommandLineException("", 0)
        }

        val upstreamValue = flagsMap["--upstream"]?.singleOrNull()
            ?: throw CommandLineException("Flag --upstream must have exactly one value", 1)

        val allowPatterns = flagsMap.getOrElse("--allow") { emptyList() }.map { ToolPattern.Exact(it) } + 
                             flagsMap.getOrElse("--allowre") { emptyList() }.map { ToolPattern.RegexPattern(it) }
                             
        val denyPatterns = flagsMap.getOrElse("--deny") { emptyList() }.map { ToolPattern.Exact(it) } + 
                            flagsMap.getOrElse("--denyre") { emptyList() }.map { ToolPattern.RegexPattern(it) }

        val hasAllowPatterns = allowPatterns.isNotEmpty()
        val hasDenyPatterns = denyPatterns.isNotEmpty()

        val mode = when {
            hasAllowPatterns && hasDenyPatterns -> throw CommandLineException(
                "Error: Cannot use both --allow/--allowre and --deny/--denyre at the same time",
                1
            )
            hasAllowPatterns -> FilterMode.ALLOWLIST
            hasDenyPatterns -> FilterMode.DENYLIST
            else -> throw CommandLineException(
                "Error: At least one --allow/--allowre or --deny/--denyre argument is required",
                1
            )
        }

        val tools = when (mode) {
            FilterMode.ALLOWLIST -> allowPatterns
            FilterMode.DENYLIST -> denyPatterns
        }

        val config = ToolsFilterConfig(
            mode = mode,
            tools = tools,
            upstream = upstreamValue
        )

        logger.debug { "Parsing results: config=$config" }
        return config
    }

    private fun parseByKnownFlag(args: Array<String>, knownFlags: Set<String>): Map<String, List<String>> {
        val flagsMap = mutableMapOf<String, MutableList<String>>()

        var currentFlag: String? = null
        val valueBuffer = mutableListOf<String>()

        for (arg in args) {
            if (arg in knownFlags) {
                if (currentFlag != null && valueBuffer.isNotEmpty()) {
                    val valueStr = valueBuffer.joinToString(" ")
                    flagsMap.getOrPut(currentFlag) { mutableListOf() }.add(valueStr)
                    valueBuffer.clear()
                }

                currentFlag = arg
            } else if (currentFlag != null) {
                valueBuffer.add(arg)
            } else if (arg.startsWith("--")) {
                logger.error { "Unknown argument: $arg" }
                throw CommandLineException("Unknown argument: $arg", 1)
            }
        }

        if (currentFlag != null && valueBuffer.isNotEmpty()) {
            val valueStr = valueBuffer.joinToString(" ")
            flagsMap.getOrPut(currentFlag) { mutableListOf() }.add(valueStr)
        }

        return flagsMap
    }
}

expect object PlatformUtility {
    fun printError(message: String)

    fun printHelp()
} 