import com.android.build.api.dsl.ApplicationExtension
import org.gradle.api.JavaVersion
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies

class AndroidApplicationConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        pluginManager.apply("com.android.application")

        extensions.configure<ApplicationExtension> {
            compileSdk = Sdk.COMPILE
            compileSdkMinor = Sdk.COMPILE_MINOR

            defaultConfig {
                minSdk = Sdk.MIN
                targetSdk = Sdk.TARGET
                testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
            }

            buildTypes {
                release {
                    // AGP 9 disallows the non-optimize default ProGuard file — see ADR-0001.
                    proguardFiles(
                        getDefaultProguardFile("proguard-android-optimize.txt"),
                        "proguard-rules.pro",
                    )
                }
            }

            compileOptions {
                sourceCompatibility = JavaVersion.VERSION_17
                targetCompatibility = JavaVersion.VERSION_17
                // java.time back to API 24 — see ADR-0007.
                isCoreLibraryDesugaringEnabled = true
            }

            buildFeatures {
                viewBinding = true
            }
        }

        configureTestTasks()

        dependencies {
            add("coreLibraryDesugaring", libs.findLibrary("desugar-jdk-libs").get())
        }
    }
}
