import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.jvm.toolchain.JavaLanguageVersion
import org.gradle.kotlin.dsl.configure

/**
 * A pure-Kotlin/JVM module — used by `:core:model` so domain types stay free of Android
 * dependencies and test on the JVM. Unlike Android modules, this one applies KGP explicitly:
 * AGP's built-in Kotlin only covers Android modules.
 */
class JvmLibraryConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        pluginManager.apply("org.jetbrains.kotlin.jvm")

        configureTestTasks()

        // A JVM module has no build variants, so `testDebugUnitTest` means nothing here — but it is
        // the command the docs and CI run. Without this alias those tests silently do not run, and a
        // green suite would be under-reporting itself.
        tasks.register("testDebugUnitTest") {
            group = "verification"
            description = "Alias for `test`, so the project-wide testDebugUnitTest covers this module."
            dependsOn("test")
        }

        extensions.configure<JavaPluginExtension> {
            toolchain {
                languageVersion.set(JavaLanguageVersion.of(17))
            }
        }
    }
}
