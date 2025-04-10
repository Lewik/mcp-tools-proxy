package dev.lewik.mcptools.proxy.config

import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.serialization.Serializable

private val logger = KotlinLogging.logger {}

enum class FilterMode {
    ALLOWLIST,
    DENYLIST
}

@Serializable
sealed class ToolPattern {
    abstract fun matches(toolName: String): Boolean

    @Serializable
    data class Exact(val name: String) : ToolPattern() {
        override fun matches(toolName: String): Boolean = toolName == name
    }

    @Serializable
    data class RegexPattern(val pattern: String) : ToolPattern() {
        override fun matches(toolName: String): Boolean {
            return try {
                val regex = Regex(pattern)
                regex.matches(toolName)
            } catch (e: Exception) {
                logger.error(e) { "Invalid regex pattern: $pattern" }
                false
            }
        }
    }
}

@Serializable
data class ToolsFilterConfig(
    val mode: FilterMode = FilterMode.ALLOWLIST,
    val tools: List<ToolPattern> = emptyList(),
    val upstream: String,
) {
    fun isToolAllowed(toolName: String): Boolean {
        val matches = tools.any { pattern ->
            val isMatch = pattern.matches(toolName)
            if (isMatch) {
                logger.debug { "Tool '$toolName' matches pattern $pattern" }
            }
            isMatch
        }

        val isAllowed = when (mode) {
            FilterMode.ALLOWLIST -> matches
            FilterMode.DENYLIST -> !matches
        }

        logger.debug { "Tool '$toolName' is ${if (isAllowed) "allowed" else "denied"} (mode=$mode, matches pattern=$matches)" }
        return isAllowed
    }
} 