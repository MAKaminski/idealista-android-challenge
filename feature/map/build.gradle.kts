plugins {
    id("idealista.android.feature")
}

android {
    namespace = "dev.mkaminski.idealista.feature.map"
}

dependencies {
    implementation(libs.osmdroid)
}
