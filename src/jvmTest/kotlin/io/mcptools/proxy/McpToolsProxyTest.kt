package io.mcptools.proxy

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.mcptools.proxy.config.FilterMode
import io.mcptools.proxy.config.ToolPattern
import io.mcptools.proxy.config.ToolsFilterConfig
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import kotlin.test.assertTrue

/**
 * Tests for MCP Tools Proxy functionality.
 * Focuses on testing filtering logic and command line arguments processing
 */
class McpToolsProxyTest {

    /**
     * Test verifies filter configuration in allowlist mode
     */
    @Test
    @DisplayName("Test tool filtering in allowlist mode")
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
        assertTrue(!config.isToolAllowed("tool3"), "tool3 should be denied")
        assertTrue(!config.isToolAllowed("unknown_tool"), "unknown_tool should be denied")
    }

    /**
     * Test verifies filter configuration in denylist mode
     */
    @Test
    @DisplayName("Test tool filtering in denylist mode")
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
        assertTrue(!config.isToolAllowed("tool3"), "tool3 should be denied")
        assertTrue(!config.isToolAllowed("denied_tool"), "denied_tool should be denied")
        assertTrue(config.isToolAllowed("unknown_tool"), "unknown_tool should be allowed")
    }

    /**
     * Test verifies regular expression patterns in filters
     */
    @Test
    @DisplayName("Test filtering by regular expressions")
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

        assertTrue(!config.isToolAllowed("toola"), "toola should not match any pattern")
        assertTrue(!config.isToolAllowed("other_tool"), "other_tool should not match any pattern")
    }

    /**
     * Test for parsing command line arguments in allowlist mode
     */
    @Test
    @DisplayName("Test command line arguments parsing in allowlist mode")
    fun testCommandLineArgsParsingAllowlist() {
        val args = arrayOf(
            "--upstream", "npx -y @anthropic-ai/mcp-server",
            "--allow", "tool1",
            "--allow", "tool2",
            "--allowre", "prefix_.*"
        )

        val config = CommandLineParser.parseCommandLineArgs(args)

        config.upstream shouldBe "npx -y @anthropic-ai/mcp-server"
        config.mode shouldBe FilterMode.ALLOWLIST
        config.tools.size shouldBe 3
        assertTrue(config.isToolAllowed("tool1"), "tool1 should be allowed")
        assertTrue(config.isToolAllowed("tool2"), "tool2 should be allowed")
        assertTrue(config.isToolAllowed("prefix_test"), "prefix_test should be allowed")
        assertTrue(!config.isToolAllowed("another_tool"), "another_tool should be denied")
    }

    /**
     * Test for parsing command line arguments in denylist mode
     */
    @Test
    @DisplayName("Test command line arguments parsing in denylist mode")
    fun testCommandLineArgsParsingDenylist() {
        val args = arrayOf(
            "--upstream", "npx -y @anthropic-ai/mcp-server",
            "--deny", "tool3",
            "--denyre", "dangerous_.*"
        )

        val config = CommandLineParser.parseCommandLineArgs(args)

        config.upstream shouldBe "npx -y @anthropic-ai/mcp-server"
        config.mode shouldBe FilterMode.DENYLIST
        config.tools.size shouldBe 2
        assertTrue(!config.isToolAllowed("tool3"), "tool3 should be denied")
        assertTrue(!config.isToolAllowed("dangerous_tool"), "dangerous_tool should be denied")
        assertTrue(config.isToolAllowed("tool1"), "tool1 should be allowed")
        assertTrue(config.isToolAllowed("tool2"), "tool2 should be allowed")
    }

    /**
     * Test for parsing command line arguments with spaces between arguments
     */
    @Test
    @DisplayName("Test command line arguments parsing with spaces between arguments")
    fun testCommandLineArgsParsingWithSpaces() {
        val args = arrayOf(
            "--upstream", "npx -y @anthropic-ai/mcp-server",
            "--allow", "tool1",
            "--allowre", "prefix_.*"
        )

        val config = CommandLineParser.parseCommandLineArgs(args)

        config.upstream shouldBe "npx -y @anthropic-ai/mcp-server"
        config.mode shouldBe FilterMode.ALLOWLIST
        config.tools.size shouldBe 2
        assertTrue(config.isToolAllowed("tool1"), "tool1 should be allowed")
        assertTrue(config.isToolAllowed("prefix_abc"), "prefix_abc should be allowed")
        assertTrue(!config.isToolAllowed("other_tool"), "other_tool should be denied")
    }

    /**
     * Test for error when using --allow and --deny simultaneously
     */
    @Test
    @DisplayName("Test error when using --allow and --deny simultaneously")
    fun testErrorOnAllowAndDenyMix() {
        val args = arrayOf(
            "--upstream", "npx -y @anthropic-ai/mcp-server",
            "--allow", "tool1",
            "--deny", "tool3"
        )

        val exception = shouldThrow<CommandLineException> {
            CommandLineParser.parseCommandLineArgs(args)
        }

        exception.exitCode shouldBe 1
        assertTrue(exception.message!!.contains("Cannot use both --allow/--allowre and --deny/--denyre at the same time"))
    }

    /**
     * Test for error when missing required --upstream argument
     */
    @Test
    @DisplayName("Test error when missing --upstream argument")
    fun testErrorOnMissingUpstream() {
        val args = arrayOf(
            "--allow", "tool1",
            "--allow", "tool2"
        )

        val exception = shouldThrow<CommandLineException> {
            CommandLineParser.parseCommandLineArgs(args)
        }

        exception.exitCode shouldBe 1
        assertTrue(exception.message!!.contains("--upstream must have exactly one value"))
    }

    /**
     * Test for error when missing filter arguments
     */
    @Test
    @DisplayName("Test error when missing filter arguments")
    fun testErrorOnMissingFilterArgs() {
        val args = arrayOf(
            "--upstream", "npx -y @anthropic-ai/mcp-server"
        )

        val exception = shouldThrow<CommandLineException> {
            CommandLineParser.parseCommandLineArgs(args)
        }

        exception.exitCode shouldBe 1
        assertTrue(exception.message!!.contains("At least one --allow/--allowre or --deny/--denyre argument is required"))
    }
} 