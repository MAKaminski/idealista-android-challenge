import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalog
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.api.tasks.testing.Test
import org.gradle.kotlin.dsl.getByType
import org.gradle.kotlin.dsl.withType

/** The `libs` version catalog, reachable from convention plugins where `libs` is not generated. */
internal val Project.libs: VersionCatalog
    get() = extensions.getByType<VersionCatalogsExtension>().named("libs")

/**
 * SDK levels applied to every module. Kept here rather than in each module script so a platform
 * bump is a one-line change. See docs/DECISIONS/ADR-0001-toolchain.md and ADR-0007.
 */
/**
 * Gradle 9 fails a test task that finds no tests. Modules gain their tests as features land
 * (docs/PLAN.md), so an empty suite is expected during scaffolding rather than a misconfiguration.
 * Remove this once every module has tests — it would then be hiding a real problem.
 */
internal fun Project.allowEmptyTestSuites() {
    tasks.withType<Test>().configureEach {
        failOnNoDiscoveredTests.set(false)
    }
}

internal object Sdk {
    const val COMPILE = 37
    const val COMPILE_MINOR = 1
    const val MIN = 24
    const val TARGET = 37
}
