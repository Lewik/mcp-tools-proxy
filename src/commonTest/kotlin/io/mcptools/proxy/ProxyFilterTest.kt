package io.mcptools.proxy

import io.kotest.matchers.shouldBe
import io.mcptools.proxy.config.FilterMode
import io.mcptools.proxy.config.ToolPattern
import io.mcptools.proxy.config.ToolsFilterConfig
import io.modelcontextprotocol.kotlin.sdk.Tool
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.buildJsonObject
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ProxyFilterTest {
    private fun createTestTools(): List<Tool> {
        val emptySchema = Tool.Input(
            properties = buildJsonObject {},
            required = listOf()
        )

        return listOf(
            Tool(
                name = "tool1",
                description = "Test tool 1",
                inputSchema = emptySchema
            ),
            Tool(
                name = "tool2",
                description = "Test tool 2",
                inputSchema = emptySchema
            ),
            Tool(
                name = "tool3",
                description = "Test tool 3",
                inputSchema = emptySchema
            ),
            Tool(
                name = "dangerous_tool",
                description = "Dangerous tool",
                inputSchema = emptySchema
            ),
            Tool(
                name = "prefix_xyz",
                description = "Tool with prefix",
                inputSchema = emptySchema
            )
        )
    }

    @Test
    fun testToolsFilteringAllowlistExactMatch() = runTest {
        val config = ToolsFilterConfig(
            mode = FilterMode.ALLOWLIST,
            tools = listOf(
                ToolPattern.Exact("tool1"),
                ToolPattern.Exact("tool2")
            ),
            upstream = "mock-command"
        )

        val tools = createTestTools()

        val filtered = tools.filter { tool -> config.isToolAllowed(tool.name) }

        assertEquals(2, filtered.size, "There should be 2 tools")
        assertTrue(filtered.any { it.name == "tool1" }, "tool1 should be in the list")
        assertTrue(filtered.any { it.name == "tool2" }, "tool2 should be in the list")
        assertFalse(filtered.any { it.name == "tool3" }, "tool3 should not be in the list")
        assertFalse(filtered.any { it.name == "dangerous_tool" }, "dangerous_tool should not be in the list")
        assertFalse(filtered.any { it.name == "prefix_xyz" }, "prefix_xyz should not be in the list")
    }

    @Test
    fun testToolsFilteringAllowlistRegex() = runTest {
        val config = ToolsFilterConfig(
            mode = FilterMode.ALLOWLIST,
            tools = listOf(
                ToolPattern.RegexPattern("tool\\d+"),
                ToolPattern.RegexPattern("prefix_.*")
            ),
            upstream = "mock-command"
        )

        val tools = createTestTools()

        val filtered = tools.filter { tool -> config.isToolAllowed(tool.name) }

        assertEquals(4, filtered.size, "There should be 4 tools")
        assertTrue(filtered.any { it.name == "tool1" }, "tool1 should be in the list")
        assertTrue(filtered.any { it.name == "tool2" }, "tool2 should be in the list")
        assertTrue(filtered.any { it.name == "tool3" }, "tool3 should be in the list")
        assertTrue(filtered.any { it.name == "prefix_xyz" }, "prefix_xyz should be in the list")
        assertFalse(filtered.any { it.name == "dangerous_tool" }, "dangerous_tool should not be in the list")
    }

    @Test
    fun testToolsFilteringDenylistExactMatch() = runTest {
        val config = ToolsFilterConfig(
            mode = FilterMode.DENYLIST,
            tools = listOf(
                ToolPattern.Exact("tool3"),
                ToolPattern.Exact("dangerous_tool")
            ),
            upstream = "mock-command"
        )

        val tools = createTestTools()

        val filtered = tools.filter { tool -> config.isToolAllowed(tool.name) }

        assertEquals(3, filtered.size, "There should be 3 tools")
        assertTrue(filtered.any { it.name == "tool1" }, "tool1 should be in the list")
        assertTrue(filtered.any { it.name == "tool2" }, "tool2 should be in the list")
        assertTrue(filtered.any { it.name == "prefix_xyz" }, "prefix_xyz should be in the list")
        assertFalse(filtered.any { it.name == "tool3" }, "tool3 should not be in the list")
        assertFalse(filtered.any { it.name == "dangerous_tool" }, "dangerous_tool should not be in the list")
    }

    @Test
    fun testToolsFilteringDenylistRegex() = runTest {
        val config = ToolsFilterConfig(
            mode = FilterMode.DENYLIST,
            tools = listOf(
                ToolPattern.RegexPattern("dangerous_.*"),
                ToolPattern.RegexPattern("prefix_.*")
            ),
            upstream = "mock-command"
        )

        val tools = createTestTools()

        val filtered = tools.filter { tool -> config.isToolAllowed(tool.name) }

        assertEquals(3, filtered.size, "There should be 3 tools")
        assertTrue(filtered.any { it.name == "tool1" }, "tool1 should be in the list")
        assertTrue(filtered.any { it.name == "tool2" }, "tool2 should be in the list")
        assertTrue(filtered.any { it.name == "tool3" }, "tool3 should be in the list")
        assertFalse(filtered.any { it.name == "dangerous_tool" }, "dangerous_tool should not be in the list")
        assertFalse(filtered.any { it.name == "prefix_xyz" }, "prefix_xyz should not be in the list")
    }

    @Test
    fun testEmptyRequest() = runTest {
        val config = ToolsFilterConfig(
            mode = FilterMode.ALLOWLIST,
            tools = listOf(ToolPattern.Exact("tool1")),
            upstream = "mock-command"
        )

        config.mode shouldBe FilterMode.ALLOWLIST
        assertEquals(1, config.tools.size, "There should be 1 rule")
        assertTrue(config.isToolAllowed("tool1"), "tool1 should be allowed")
        assertFalse(config.isToolAllowed("tool2"), "tool2 should be denied")
    }

    @Test
    fun testShouldProxyToUpstream() = runTest {
        val config = ToolsFilterConfig(
            mode = FilterMode.ALLOWLIST,
            tools = listOf(
                ToolPattern.Exact("allowed_tool"),
                ToolPattern.RegexPattern("prefix_.*")
            ),
            upstream = "mock-command"
        )

        assertTrue(config.isToolAllowed("allowed_tool"), "allowed_tool should be allowed")
        assertTrue(config.isToolAllowed("prefix_abc"), "prefix_abc should be allowed")

        assertFalse(config.isToolAllowed("denied_tool"), "denied_tool should be denied")
        assertFalse(config.isToolAllowed("another_tool"), "another_tool should be denied")

        assertFalse(config.isToolAllowed(""), "Empty name should be denied")
    }
} 