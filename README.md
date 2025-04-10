# MCP Tools Proxy

A configurable proxy for Model Context Protocol (MCP) tool filtering. This proxy allows you to control which MCP tools are available to AI assistants.

## Features

- **Allowlist Mode**: Only explicitly allowed tools are accessible
- **Denylist Mode**: All tools are accessible except explicitly denied ones
- **Regular Expression Support**: Use regex patterns to match tool names
- **Simple CLI**: Easy to configure with command-line arguments

## Usage

```bash
java -jar mcp-tools-proxy-0.1.0.jar [options]
```

### Command Line Options

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

### Examples

#### Allow only specific tools

```bash
java -jar mcp-tools-proxy-0.1.0.jar --upstream "npx -y @anthropic-ai/mcp-server" \
  --allow list_files_in_folder \
  --allowre "get_.*"
```

This will allow only:
- The exact tool named "list_files_in_folder"
- Any tool whose name starts with "get_"

#### Block dangerous tools

```bash
java -jar mcp-tools-proxy-0.1.0.jar --upstream "npx -y @anthropic-ai/mcp-server" \
  --deny execute_terminal_command \
  --deny replace_current_file_text \
  --deny replace_file_text_by_path \
  --deny replace_selected_text
```

This will block these specific tools while allowing all others.

## Integration with MCP Clients

To use this proxy with an MCP client (e.g., Cursor), configure the client to use this proxy as the MCP server:

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

## Building from Source

```bash
./gradlew clean shadowJar
```

The output JAR will be in `build/libs/mcp-tools-proxy-0.1.0.jar`

## License

MIT 