package dev.lewik.mcptools.proxy

import io.github.oshai.kotlinlogging.KotlinLogging

actual object PlatformUtility {
    private val logger = KotlinLogging.logger {}

    actual fun printError(message: String) {
        logger.error { message }
    }

    actual fun printHelp() {
        val helpText = """
            Usage: java -jar mcp-tools-proxy.jar [options]
            
            Options:
              --upstream <command>  Command to start the upstream MCP server (required)
              --allow <tool>        Add a tool to the allowlist (exact match)
              --allowre <regex>     Add a regex pattern to the allowlist
              --deny <tool>         Add a tool to the denylist (exact match)
              --denyre <regex>      Add a regex pattern to the denylist
              --help                Show this help message
              
            Note: 
              - You must use either allow/allowre OR deny/denyre arguments, not both.
              - Arguments can be provided in two formats: '--flag value' or '--flag=value'
            
            Examples:
              java -jar mcp-tools-proxy.jar --upstream "npx -y @anthropic-ai/mcp-server" --allow list_files_in_folder --allowre "get_.*"
              java -jar mcp-tools-proxy.jar --upstream="npx -y @anthropic-ai/mcp-server" --deny=execute_terminal_command --deny=replace_current_file_text
        """.trimIndent()

        logger.info { "Help requested:\n$helpText" }
    }
} 