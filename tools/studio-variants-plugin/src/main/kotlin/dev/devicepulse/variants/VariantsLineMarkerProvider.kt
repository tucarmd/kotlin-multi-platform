package dev.devicepulse.variants

import com.intellij.codeInsight.daemon.LineMarkerInfo
import com.intellij.codeInsight.daemon.LineMarkerProviderDescriptor
import com.intellij.openapi.editor.markup.GutterIconRenderer
import com.intellij.openapi.util.IconLoader
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtNamedFunction
import javax.swing.Icon

object VariantsIcons {
    val Gutter: Icon = IconLoader.getIcon("/icons/variants.svg", VariantsIcons::class.java)
}

// Puts the "3V" icon on the name of every named Kotlin function.
// Users can switch it off under Settings → Editor → General → Gutter Icons.
class VariantsLineMarkerProvider : LineMarkerProviderDescriptor() {

    override fun getName(): String = "Kotlin variants (Claude)"

    override fun getIcon(): Icon = VariantsIcons.Gutter

    override fun getLineMarkerInfo(element: PsiElement): LineMarkerInfo<*>? {
        // Line markers must sit on leaf elements, so anchor on the name identifier
        if (element.node?.elementType != KtTokens.IDENTIFIER) return null
        val function = element.parent as? KtNamedFunction ?: return null
        if (function.nameIdentifier != element) return null

        return LineMarkerInfo(
            element,
            element.textRange,
            VariantsIcons.Gutter,
            { _: PsiElement -> "Show 3 variants of ${function.name}() (Claude)" },
            { _, anchor -> (anchor.parent as? KtNamedFunction)?.let(VariantsGenerator::show) },
            GutterIconRenderer.Alignment.RIGHT,
            { "Show variants" },
        )
    }
}
