package dev.lewik.mcptools.proxy

/**
 * Entry point for Node.js
 */
@JsExport
fun main() {
    try {
        
        val argsArray = js("process.argv.slice(2)")
        
        
        val args = Array(argsArray.length) { i -> argsArray[i] as String }
        Main.runWithConfig(args)
    } catch (e: CommandLineException) {
        console.error(e.message)
        js("process.exit(e.exitCode)")
    } catch (e: Exception) {
        console.error("Error: " + e.message)
        js("process.exit(1)")
    }
} 