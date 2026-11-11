import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.w3c.dom.Element
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

/**
 * Writes the string resources (the `strings.xml` of every `values` folder) as Kotlin tables, one per language, which
 * both UIs read: the SwiftUI one on iOS directly, the Compose one on Android through `Res.string` accessors written
 * next to them when [resourcesPackage] is set. The XML files stay the only source of the texts.
 */
@CacheableTask
abstract class GeneratePaymentStringsTask : DefaultTask() {
    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val stringFiles: ConfigurableFileCollection

    @get:Input
    abstract val packageName: Property<String>

    @get:OutputDirectory
    abstract val outputDir: DirectoryProperty

    /** Package of the `Res.string` accessors, the one Compose resources used, so the Compose screens keep their imports. */
    @get:Input
    @get:Optional
    abstract val resourcesPackage: Property<String>

    @get:OutputDirectory
    @get:Optional
    abstract val resourcesOutputDir: DirectoryProperty

    @TaskAction
    fun generate() {
        val tables =
            stringFiles.files
                .associate { languageOf(it) to readStrings(it) }
                .toSortedMap()
        check(DEFAULT_LANGUAGE in tables) { "No values/strings.xml among ${stringFiles.files}" }
        val root = outputDir.get().asFile
        root.deleteRecursively()
        val dir = root.resolve(packageName.get().replace('.', '/'))
        dir.mkdirs()
        dir.resolve("PaymentStringTables.kt").writeText(render(tables))
        if (resourcesPackage.isPresent) {
            val resourcesRoot = resourcesOutputDir.get().asFile
            resourcesRoot.deleteRecursively()
            val resourcesDir = resourcesRoot.resolve(resourcesPackage.get().replace('.', '/'))
            resourcesDir.mkdirs()
            resourcesDir.resolve("Res.kt").writeText(renderAccessors(tables.getValue(DEFAULT_LANGUAGE).keys))
        }
    }

    /** `values` holds the default language, `values-de` German. */
    private fun languageOf(file: File): String {
        val qualifier = file.parentFile.name.removePrefix("values").removePrefix("-")
        return qualifier.ifEmpty { DEFAULT_LANGUAGE }
    }

    private fun readStrings(file: File): Map<String, String> {
        val nodes = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(file).getElementsByTagName("string")
        val strings = sortedMapOf<String, String>()
        for (i in 0 until nodes.length) {
            val element = nodes.item(i) as Element
            strings[element.getAttribute("name")] = unescape(element.textContent)
        }
        return strings
    }

    private fun render(tables: Map<String, Map<String, String>>): String =
        buildString {
            // The texts are as long as they are; ktlint checks every source directory of the module.
            appendLine("@file:Suppress(\"ktlint\")")
            appendLine()
            appendLine("package ${packageName.get()}")
            appendLine()
            appendLine("/** The texts per language; written by the build from the string resources. */")
            appendLine("internal object PaymentStringTables {")
            appendLine("    fun table(language: String): Map<String, String>? =")
            appendLine("        when (language) {")
            tables.keys.forEach { appendLine("            \"$it\" -> $it") }
            appendLine("            else -> null")
            appendLine("        }")
            tables.forEach { (language, strings) ->
                appendLine()
                appendLine("    private val $language: Map<String, String> by lazy {")
                appendLine("        mapOf(")
                strings.forEach { (key, text) -> appendLine("            \"$key\" to \"${literal(text)}\",") }
                appendLine("        )")
                appendLine("    }")
            }
            appendLine("}")
        }

    private fun renderAccessors(keys: Set<String>): String =
        buildString {
            appendLine("@file:Suppress(\"ktlint\")")
            appendLine()
            appendLine("package ${resourcesPackage.get()}")
            appendLine()
            appendLine("/** A text of the string resources, by its key; written by the build from them. */")
            appendLine("internal data class StringResource(val key: String)")
            appendLine()
            appendLine("/** The string resources, as `Res.string.<key>`. */")
            appendLine("internal object Res {")
            appendLine("    object string")
            appendLine("}")
            keys.forEach { key ->
                appendLine()
                appendLine("internal val Res.string.$key: StringResource")
                appendLine("    get() = StringResource(\"$key\")")
            }
        }

    private companion object {
        const val DEFAULT_LANGUAGE = "en"
        const val HEX = 16
        const val UNICODE_DIGITS = 4
        val Escape = Regex("""\\(u[0-9a-fA-F]{4}|.)""", RegexOption.DOT_MATCHES_ALL)

        /** Resolves the escapes of a string resource the way Compose resources do: `\n`, `\t`, `\uXXXX`, an escaped character. */
        fun unescape(text: String): String =
            Escape.replace(text) { match ->
                val escaped = match.groupValues[1]
                val resolved =
                    when {
                        escaped == "n" -> "\n"
                        escaped == "t" -> "\t"
                        escaped.length > 1 -> escaped.drop(1).toInt(HEX).toChar().toString()
                        else -> escaped
                    }
                Regex.escapeReplacement(resolved)
            }

        /** [text] as the inside of a Kotlin string literal; invisible characters go in as escapes, to be seen in the source. */
        fun literal(text: String): String =
            buildString {
                text.forEach { c ->
                    when {
                        c == '\\' -> append("\\\\")
                        c == '"' -> append("\\\"")
                        c == '$' -> append("\\$")
                        c == '\n' -> append("\\n")
                        c == '\t' -> append("\\t")
                        c.isISOControl() || Character.getType(c) == Character.FORMAT.toInt() ->
                            append("\\u").append(c.code.toString(HEX).padStart(UNICODE_DIGITS, '0'))
                        else -> append(c)
                    }
                }
            }
    }
}
