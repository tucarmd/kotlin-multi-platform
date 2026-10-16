package dev.devicepulse.variants

import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.process.CapturingProcessHandler
import com.intellij.ide.util.PropertiesComponent
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.util.EnvironmentUtil
import java.io.File

// Runs `claude -p` with the prompt on stdin and returns what it prints.
object ClaudeCli {

    private const val PATH_KEY = "dev.devicepulse.variants.claudePath"
    private const val TIMEOUT_MS = 5 * 60 * 1000

    class Failure(message: String) : Exception(message)

    var savedPath: String?
        get() = PropertiesComponent.getInstance().getValue(PATH_KEY)
        set(value) = PropertiesComponent.getInstance().setValue(PATH_KEY, value)

    // Apps opened from the Dock lack the shell PATH, but EnvironmentUtil reads it from the login shell
    fun findClaude(): String? {
        savedPath?.takeIf { File(it).canExecute() }?.let { return it }
        val path = EnvironmentUtil.getValue("PATH") ?: return null
        return path.split(File.pathSeparator)
            .map { File(it, "claude") }
            .firstOrNull { it.canExecute() }
            ?.absolutePath
    }

    fun run(claudePath: String, prompt: String, workDir: String, indicator: ProgressIndicator): String {
        val shellPath = EnvironmentUtil.getValue("PATH").orEmpty()
        val command = GeneralCommandLine(claudePath, "-p", "--output-format", "text")
            .withWorkDirectory(workDir)
            .withParentEnvironmentType(GeneralCommandLine.ParentEnvironmentType.CONSOLE)
            // claude is a node script; put its folder first so the matching node is found
            .withEnvironment("PATH", File(claudePath).parent + File.pathSeparator + shellPath)
            .withCharset(Charsets.UTF_8)

        val handler = CapturingProcessHandler(command)
        handler.processInput.use { it.write(prompt.toByteArray(Charsets.UTF_8)) }
        val output = handler.runProcessWithProgressIndicator(indicator, TIMEOUT_MS)

        when {
            output.isCancelled -> throw Failure("Cancelled")
            output.isTimeout -> throw Failure("Claude did not answer within 5 minutes")
            output.exitCode != 0 -> throw Failure(
                "claude exited with code ${output.exitCode}\n${output.stderr.ifBlank { output.stdout }.take(2000)}"
            )
        }
        return output.stdout
    }
}
