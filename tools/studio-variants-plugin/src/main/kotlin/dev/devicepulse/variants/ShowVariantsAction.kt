package dev.devicepulse.variants

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.kotlin.psi.KtNamedFunction

// Same as clicking the gutter icon, for the function around the caret (shortcut or right-click menu).
class ShowVariantsAction : AnAction() {

    override fun getActionUpdateThread() = ActionUpdateThread.BGT

    override fun update(e: AnActionEvent) {
        e.presentation.isEnabledAndVisible = functionAtCaret(e) != null
    }

    override fun actionPerformed(e: AnActionEvent) {
        functionAtCaret(e)?.let(VariantsGenerator::show)
    }

    private fun functionAtCaret(e: AnActionEvent): KtNamedFunction? {
        val editor = e.getData(CommonDataKeys.EDITOR) ?: return null
        val file = e.getData(CommonDataKeys.PSI_FILE) ?: return null
        val element = file.findElementAt(editor.caretModel.offset) ?: return null
        return PsiTreeUtil.getParentOfType(element, KtNamedFunction::class.java, false)
    }
}
