import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction

/**
 * Writes `SDK_VERSION`, the version in gradle.properties, into a module's common code, so that `version()` and
 * the published artifacts can never disagree.
 */
@CacheableTask
abstract class GenerateSdkVersionTask : DefaultTask() {
    @get:Input
    abstract val sdkVersion: Property<String>

    @get:Input
    abstract val packageName: Property<String>

    @get:OutputDirectory
    abstract val outputDir: DirectoryProperty

    @TaskAction
    fun generate() {
        val root = outputDir.get().asFile
        root.deleteRecursively()
        val dir = root.resolve(packageName.get().replace('.', '/'))
        dir.mkdirs()
        dir.resolve("SdkVersion.kt").writeText(
            """
            |package ${packageName.get()}
            |
            |/** The SDK version: `version` in gradle.properties, written here by the build. */
            |internal const val SDK_VERSION: String = "${sdkVersion.get()}"
            |
            """.trimMargin(),
        )
    }
}
