> **Note:** I believe **all MCP clients should support tool filtering as a built-in feature** — it’s a fundamental part of safety and control.  
> Until that’s the case, **this proxy can help**.

---


# MCP Tools Proxy - MCP Tools Filter

A configurable proxy for Model Context Protocol (MCP) tool filtering.
This proxy acts as a filter that allows you to control which MCP tools are available to AI assistants.

## Features

- **Allowlist Mode**: Only explicitly allowed tools are accessible
- **Denylist Mode**: All tools are accessible except explicitly denied ones
- **Regular Expression Support**: Use regex patterns to match tool names
- **Simple CLI**: Easy to configure with command-line arguments


## Integration with MCP Clients

To use this proxy with an MCP client (e.g., Cursor), configure the client to use this proxy as the MCP server:

> **Note:** JBang is like npx but for Java - it allows running Java applications without explicit prior installation. JBang installation instructions are available on the [official website](https://www.jbang.dev/download/).

### Using JBang

```json
{
  "mcpServers": {
    "default": {
      "command": "jbang",
      "args": [
        "mcpproxy@lewik/mcp-tools-proxy", 
        "--upstream", "npx -y @anthropic-ai/mcp-server", 
        "--allow", "list_files_in_folder", 
        "--allowre", "get_.*"
      ]
    }
  }
}
```

### Using Java JAR

```json
{
  "mcpServers": {
    "default": {
      "command": "java",
      "args": [
        "-jar", 
        "/path/to/mcp-tools-proxy-0.1.0.jar", 
        "--upstream", "npx -y @anthropic-ai/mcp-server", 
        "--allow", "list_files_in_folder", 
        "--allowre", "get_.*"
      ]
    }
  }
}
```

### Options

```
Options:
  --upstream <command>  Command to start the upstream MCP server (required)
  --allow <tool>        Add a tool to the allowlist (exact match)
  --allowre <regex>     Add a regex pattern to the allowlist
  --deny <tool>         Add a tool to the denylist (exact match)
  --denyre <regex>      Add a regex pattern to the denylist
  --help                Show this help message
```

**Note**: You must use either allow/allowre OR deny/denyre arguments, not both.

## Building from Source

```bash
./gradlew clean shadowJar
```

The output JAR will be in `build/libs/mcp-tools-proxy-0.1.0.jar`

## License

MIT 
