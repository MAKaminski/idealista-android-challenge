plugins {
    id("idealista.android.library")
    id("idealista.android.hilt")
}

android {
    namespace = "dev.mkaminski.idealista.data"
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

dependencies {
    implementation(projects.core.model)

    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)
}
