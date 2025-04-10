package dev.lewik.mcptools.proxy

import io.github.oshai.kotlinlogging.KotlinLogging
import dev.lewik.mcptools.proxy.config.ToolsFilterConfig
import io.modelcontextprotocol.kotlin.sdk.*
import io.modelcontextprotocol.kotlin.sdk.client.Client
import io.modelcontextprotocol.kotlin.sdk.client.ClientOptions
import io.modelcontextprotocol.kotlin.sdk.server.Server
import io.modelcontextprotocol.kotlin.sdk.server.ServerOptions
import io.modelcontextprotocol.kotlin.sdk.shared.Transport
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

private val logger = KotlinLogging.logger {}

class McpToolsProxy(
    private val config: ToolsFilterConfig,
    private val upstreamTransport: Transport,
    private val proxyTransport: Transport,
) {
    companion object {
        val PROXY_INFO = Implementation("mcp-tools-proxy", "0.1.0")
    }

    private val proxyScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val upstreamClient = Client(
        clientInfo = PROXY_INFO,
        options = ClientOptions()
    )

    private val proxyServer = Server(
        serverInfo = PROXY_INFO,
        options = ServerOptions(
            capabilities = ServerCapabilities(
                tools = ServerCapabilities.Tools(listChanged = true)
            )
        )
    )

    private var cachedUpstreamTools: List<Tool> = emptyList()

    private var debugLogger: ((String) -> Unit)? = null

    fun setDebugLogger(logger: (String) -> Unit) {
        debugLogger = logger
    }

    suspend fun init() {
        logger.info { "Initializing MCP Tools Proxy with configuration: $config" }

        debugLogger?.invoke("Connecting to upstream with transport: $upstreamTransport")
        upstreamClient.connect(upstreamTransport)

        fetchAndCacheTools()

        setupRequestHandlers()

        debugLogger?.invoke("Connecting to proxy with transport: $proxyTransport")
        proxyServer.connect(proxyTransport)

        logger.info { "MCP Tools Proxy successfully started" }
    }

    private suspend fun fetchAndCacheTools() {
        try {
            debugLogger?.invoke("Fetching tools from upstream server")
            val toolsResult = upstreamClient.listTools()
            if (toolsResult != null) {
                cachedUpstreamTools = toolsResult.tools
                logger.info { "Received list of tools from upstream server: ${cachedUpstreamTools.size} tools" }

                cachedUpstreamTools.forEach { tool ->
                    logger.debug { "Upstream tool: ${tool.name}" }
                }

                logger.debug { "Filter mode: ${config.mode}" }
                config.tools.forEach { pattern ->
                    logger.debug { "Filter pattern: $pattern" }
                }
            } else {
                logger.warn { "Failed to get list of tools from upstream server" }
                debugLogger?.invoke("Failed to get list of tools from upstream server (null result)")
            }
        } catch (e: Exception) {
            logger.error(e) { "Error getting list of tools from upstream server" }
            debugLogger?.invoke("Error getting list of tools: ${e.message}\n${e.stackTraceToString()}")
        }
    }

    private fun setupRequestHandlers() {
        debugLogger?.invoke("Setting up request handlers")

        proxyServer.setRequestHandler<ListToolsRequest>(Method.Defined.ToolsList) { _, _ ->
            handleListTools()
        }

        proxyServer.setRequestHandler<CallToolRequest>(Method.Defined.ToolsCall) { request, _ ->
            handleCallTool(request)
        }
    }

    private fun handleListTools(): ListToolsResult {
        logger.debug { "Starting filtering of ${cachedUpstreamTools.size} tools" }
        debugLogger?.invoke("Handling list tools request, filtering ${cachedUpstreamTools.size} tools")

        val filteredTools = cachedUpstreamTools.filter { tool ->
            val isAllowed = config.isToolAllowed(tool.name)
            logger.debug { if (isAllowed) "✅ Allowed: ${tool.name}" else "❌ Denied: ${tool.name}" }
            isAllowed
        }

        logger.info { "Returning filtered list of tools: ${filteredTools.size} out of ${cachedUpstreamTools.size}" }

        filteredTools.forEach { tool ->
            logger.debug { "Allowed tool in result: ${tool.name}" }
        }

        return ListToolsResult(tools = filteredTools, nextCursor = null)
    }

    private suspend fun handleCallTool(request: CallToolRequest): CallToolResult {
        val toolName = request.name

        if (!config.isToolAllowed(toolName)) {
            logger.warn { "Attempt to call a forbidden tool: $toolName" }
            debugLogger?.invoke("Attempt to call a forbidden tool: $toolName")
            return CallToolResult(
                content = listOf(TextContent("Tool $toolName is forbidden in proxy configuration")),
                isError = true
            )
        }

        logger.info { "Forwarding tool call $toolName to upstream server" }
        debugLogger?.invoke("Forwarding tool call $toolName to upstream server with arguments: ${request.arguments}")

        return try {
            val result = upstreamClient.callTool(name = toolName, arguments = request.arguments)
                ?: throw Exception("Upstream server returned null")

            when (result) {
                is CallToolResult -> {
                    debugLogger?.invoke("Got result for $toolName, isError=${result.isError}, content=${result.content}")
                    result
                }

                else -> {
                    debugLogger?.invoke("Got result for $toolName (different type), converting: $result")
                    CallToolResult(
                        content = result.content,
                        isError = result.isError,
                        _meta = result._meta
                    )
                }
            }
        } catch (e: Exception) {
            logger.error(e) { "Error calling tool $toolName on upstream server" }
            debugLogger?.invoke("Error calling tool $toolName: ${e.message}\n${e.stackTraceToString()}")

            CallToolResult(
                content = listOf(TextContent("Error calling tool $toolName: ${e.message}")),
                isError = true
            )
        }
    }

    suspend fun close() {
        logger.info { "Shutting down MCP Tools Proxy" }
        debugLogger?.invoke("Shutting down MCP Tools Proxy")
        try {
            proxyServer.close()
            upstreamClient.close()
        } catch (e: Exception) {
            logger.error(e) { "Error closing connections" }
            debugLogger?.invoke("Error closing connections: ${e.message}")
        }
    }
} 