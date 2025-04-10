package io.mcptools.proxy

import io.mcptools.proxy.config.FilterMode
import io.mcptools.proxy.config.ToolPattern
import io.mcptools.proxy.config.ToolsFilterConfig
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ConfigFilterTest {

    @Test
    fun testAllowlistFiltering() {
        val config = ToolsFilterConfig(
            mode = FilterMode.ALLOWLIST,
            tools = listOf(
                ToolPattern.Exact("tool1"),
                ToolPattern.Exact("tool2")
            ),
            upstream = "mock-command"
        )

        assertTrue(config.isToolAllowed("tool1"), "tool1 should be allowed")
        assertTrue(config.isToolAllowed("tool2"), "tool2 should be allowed")
        assertFalse(config.isToolAllowed("tool3"), "tool3 should be denied")
        assertFalse(config.isToolAllowed("unknown_tool"), "unknown_tool should be denied")
    }

    @Test
    fun testDenylistFiltering() {
        val config = ToolsFilterConfig(
            mode = FilterMode.DENYLIST,
            tools = listOf(
                ToolPattern.Exact("tool3"),
                ToolPattern.Exact("denied_tool")
            ),
            upstream = "mock-command"
        )

        assertTrue(config.isToolAllowed("tool1"), "tool1 should be allowed")
        assertTrue(config.isToolAllowed("tool2"), "tool2 should be allowed")
        assertFalse(config.isToolAllowed("tool3"), "tool3 should be denied")
        assertFalse(config.isToolAllowed("denied_tool"), "denied_tool should be denied")
        assertTrue(config.isToolAllowed("unknown_tool"), "unknown_tool should be allowed")
    }

    @Test
    fun testRegexFiltering() {
        val config = ToolsFilterConfig(
            mode = FilterMode.ALLOWLIST,
            tools = listOf(
                ToolPattern.RegexPattern("tool\\d+"),
                ToolPattern.RegexPattern("prefix_.*")
            ),
            upstream = "mock-command"
        )

        assertTrue(config.isToolAllowed("tool1"), "tool1 should match pattern tool\\d+")
        assertTrue(config.isToolAllowed("tool2"), "tool2 should match pattern tool\\d+")
        assertTrue(config.isToolAllowed("tool10"), "tool10 should match pattern tool\\d+")
        assertTrue(config.isToolAllowed("prefix_abc"), "prefix_abc should match pattern prefix_.*")
        assertTrue(config.isToolAllowed("prefix_tool"), "prefix_tool should match pattern prefix_.*")

        assertFalse(config.isToolAllowed("toola"), "toola should not match any pattern")
        assertFalse(config.isToolAllowed("other_tool"), "other_tool should not match any pattern")
    }
} 