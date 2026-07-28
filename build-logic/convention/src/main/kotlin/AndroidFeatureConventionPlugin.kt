import com.android.build.api.dsl.LibraryExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.project

/**
 * A feature module: an Android library with ViewBinding, Hilt and the dependencies every screen
 * needs. Keeps `:feature:*` scripts to a plugins block plus whatever is genuinely specific.
 */
class AndroidFeatureConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        pluginManager.apply("idealista.android.library")
        pluginManager.apply("idealista.android.hilt")

        extensions.configure<LibraryExtension> {
            buildFeatures {
                viewBinding = true
            }
        }

        dependencies {
            add("implementation", project(":core:model"))
            add("implementation", project(":core:data"))
            add("implementation", project(":core:designsystem"))
            add("testImplementation", project(":core:testing"))

            add("implementation", libs.findLibrary("androidx-fragment-ktx").get())
            add("implementation", libs.findLibrary("androidx-lifecycle-runtime-ktx").get())
            add("implementation", libs.findLibrary("androidx-lifecycle-viewmodel-ktx").get())
            add("implementation", libs.findLibrary("androidx-recyclerview").get())
            add("implementation", libs.findLibrary("androidx-swiperefreshlayout").get())
            add("implementation", libs.findLibrary("coil").get())
            add("implementation", libs.findLibrary("coil-network-okhttp").get())
            add("implementation", libs.findLibrary("androidx-browser").get())
            add("implementation", libs.findLibrary("hilt-navigation-fragment").get())

            add("testImplementation", libs.findLibrary("junit").get())
            add("testImplementation", libs.findLibrary("kotlinx-coroutines-test").get())
            add("testImplementation", libs.findLibrary("turbine").get())
            add("testImplementation", libs.findLibrary("robolectric").get())
            add("testImplementation", libs.findLibrary("androidx-test-core").get())
        }
    }
}
