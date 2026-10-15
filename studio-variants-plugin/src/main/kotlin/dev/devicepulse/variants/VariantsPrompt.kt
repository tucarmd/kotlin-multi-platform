package dev.devicepulse.variants

// The instructions Claude gets. Explanations are in Vietnamese because the learner reads them.
object VariantsPrompt {

    private const val MAX_CONTEXT_CHARS = 20_000

    fun build(functionName: String, functionCode: String, fileName: String, fileCode: String): String = """
You are a Kotlin mentor. The learner is studying Kotlin and just wrote the function `$functionName` in `$fileName`.
Write 3 alternative implementations of it so they can compare styles and learn Kotlin features.

Reply with ONLY the content of one Kotlin source file. No markdown fences, no text outside Kotlin comments.
Do not edit any project file.

File layout:
1. Header block comment: the original signature and, in 1-2 Vietnamese lines, what the function does.
2. Three sections, each starting with `// ===== Variant N: <short name> =====`:
   - Variant 1 "Cơ bản": plain and explicit, easy for a beginner to follow.
   - Variant 2 "Idiomatic Kotlin": stdlib collection ops, expression bodies, null-safety operators, scope functions, `when`.
   - Variant 3 "Nâng cao": a more advanced idea that really fits (extension function, sequence, generics,
     sealed/value class, inline/reified, coroutine/Flow, ...). Do not force it; if nothing fits, show a different design.
   Name them `${functionName}V1`, `${functionName}V2`, `${functionName}V3` so they never clash with the original.
   Above each variant, a Vietnamese comment with:
     // Ý tưởng: ...
     // Học được: <Kotlin keywords/features, each with a one-line explanation>
     // Ưu: ...  Nhược: ...
     // Dùng khi: ...
3. If the function is pure enough, a `fun main()` that calls all 3 with the same sample input and prints the results.
   If it depends on Android/platform APIs, skip main and say why in one comment line.
   If it uses project types, refer to them as they are; only add small stubs when that is needed to make main run.
4. At the end a Vietnamese comment block "So sánh nhanh" (one line per variant), then exactly these lines for the learner:
// TÔI CHỌN:
// VÌ:

--- Function `$functionName` ---
$functionCode

--- Whole file `$fileName` (context only) ---
${fileCode.take(MAX_CONTEXT_CHARS)}
""".trimIndent()

    // Models sometimes wrap the file in a fence anyway
    fun stripFences(text: String): String {
        val lines = text.trim().lines()
        if (lines.firstOrNull()?.startsWith("```") != true) return text.trim() + "\n"
        val body = lines.drop(1).let { if (it.lastOrNull()?.startsWith("```") == true) it.dropLast(1) else it }
        return body.joinToString("\n").trim() + "\n"
    }
}
