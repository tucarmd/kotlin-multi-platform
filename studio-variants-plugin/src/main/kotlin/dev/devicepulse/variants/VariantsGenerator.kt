package dev.devicepulse.variants

import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.application.WriteAction
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.ex.FileEditorManagerEx
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import org.jetbrains.kotlin.psi.KtNamedFunction
import java.io.File
import javax.swing.SwingConstants

// Asks Claude for the variants in the background, saves them under <project>/learn-variants/
// and opens the file in a split to the right of the code.
object VariantsGenerator {

    const val OUTPUT_DIR = "learn-variants"

    fun show(function: KtNamedFunction) {
        val project = function.project
        val functionName = function.name ?: return
        val ktFile = function.containingKtFile
        val basePath = project.basePath ?: return
        val outFile = File(basePath, "$OUTPUT_DIR/${ktFile.virtualFile.nameWithoutExtension}.$functionName.variants.kt")

        if (outFile.exists()) {
            val choice = Messages.showYesNoCancelDialog(
                project,
                "Variants of $functionName() already exist. Open them, or ask Claude again?",
                "Kotlin Variants",
                "Open",
                "Ask Again",
                "Cancel",
                Messages.getQuestionIcon(),
            )
            when (choice) {
                Messages.YES -> return open(project, outFile)
                Messages.NO -> Unit
                else -> return
            }
        }

        val claude = ClaudeCli.findClaude() ?: askForClaudePath(project) ?: return
        val prompt = VariantsPrompt.build(functionName, function.text, ktFile.name, ktFile.text)

        ProgressManager.getInstance().run(object : Task.Backgroundable(project, "Claude: 3 variants of $functionName()", true) {
            private var result: String? = null

            override fun run(indicator: ProgressIndicator) {
                indicator.isIndeterminate = true
                indicator.text = "Waiting for Claude…"
                result = VariantsPrompt.stripFences(ClaudeCli.run(claude, prompt, basePath, indicator))
            }

            override fun onSuccess() {
                val text = result ?: return
                if (text.isBlank()) return notify(project, "Claude returned nothing for $functionName()", NotificationType.WARNING)
                WriteAction.run<Exception> {
                    val dir = VfsUtil.createDirectoryIfMissing(outFile.parent) ?: error("Cannot create ${outFile.parent}")
                    val file = dir.findChild(outFile.name) ?: dir.createChildData(this, outFile.name)
                    VfsUtil.saveText(file, text)
                }
                open(project, outFile)
            }

            override fun onThrowable(error: Throwable) {
                notify(project, error.message ?: error.toString(), NotificationType.ERROR)
            }
        })
    }

    private fun open(project: Project, file: File) {
        val virtualFile: VirtualFile = LocalFileSystem.getInstance().refreshAndFindFileByIoFile(file) ?: return
        // Side by side with the original makes comparing easy; fall back to a plain tab
        val manager = FileEditorManagerEx.getInstanceEx(project)
        val alreadyOpen = manager.isFileOpen(virtualFile)
        val window = manager.currentWindow
        if (alreadyOpen || window == null || window.split(SwingConstants.VERTICAL, true, virtualFile, true) == null) {
            FileEditorManager.getInstance(project).openFile(virtualFile, true)
        }
    }

    private fun askForClaudePath(project: Project): String? {
        val path = Messages.showInputDialog(
            project,
            "Could not find the claude command. Paste its full path (run `which claude` in a terminal):",
            "Kotlin Variants",
            null,
        )?.trim()?.takeIf { it.isNotEmpty() } ?: return null
        if (!File(path).canExecute()) {
            notify(project, "$path is not an executable file", NotificationType.ERROR)
            return null
        }
        ClaudeCli.savedPath = path
        return path
    }

    private fun notify(project: Project, message: String, type: NotificationType) {
        NotificationGroupManager.getInstance()
            .getNotificationGroup("Kotlin Variants")
            .createNotification("Kotlin Variants", message, type)
            .notify(project)
    }
}
