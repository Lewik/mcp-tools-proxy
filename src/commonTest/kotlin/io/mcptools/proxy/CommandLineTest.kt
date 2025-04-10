package io.mcptools.proxy

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.mcptools.proxy.config.FilterMode
import kotlin.test.Test
import kotlin.test.assertTrue

class CommandLineTest {

    @Test
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
        assertTrue(config.tools.any { it.matches("tool1") })
        assertTrue(config.tools.any { it.matches("tool2") })
        assertTrue(config.tools.any { it.matches("prefix_test") })
        assertTrue(!config.tools.any { it.matches("another_tool") })
    }

    @Test
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
        assertTrue(config.tools.any { it.matches("tool3") })
        assertTrue(config.tools.any { it.matches("dangerous_tool") })
        assertTrue(!config.tools.any { it.matches("tool1") })
        assertTrue(!config.tools.any { it.matches("tool2") })
    }

    @Test
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
        assertTrue(config.tools.any { it.matches("tool1") })
        assertTrue(config.tools.any { it.matches("prefix_abc") })
        assertTrue(!config.tools.any { it.matches("other_tool") })
    }

    @Test
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

    @Test
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

    @Test
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