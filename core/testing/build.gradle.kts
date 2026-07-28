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
}
