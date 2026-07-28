plugins {
    id("idealista.android.library")
}

android {
    namespace = "dev.mkaminski.idealista.testing"
}

dependencies {
    api(projects.core.model)
    api(projects.core.data)
    api(libs.kotlinx.coroutines.test)

    // Screenshots renders real views offscreen, so this module carries the rendering seam: JUnit for
    // the opt-in assumption and Coil for the URL-keyed photo loader.
    api(libs.junit)
    api(libs.coil)
    api(libs.robolectric)
}
