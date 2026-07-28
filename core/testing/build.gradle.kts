plugins {
    id("idealista.android.library")
}

android {
    namespace = "dev.mkaminski.idealista.testing"
}

dependencies {
    implementation(projects.core.model)
}
