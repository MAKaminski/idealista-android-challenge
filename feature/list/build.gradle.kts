plugins {
    id("idealista.android.feature")
}

android {
    namespace = "dev.mkaminski.idealista.feature.list"
}

/** The real payloads, so image alignment is asserted against production data, not a hand-written stub. */
val syncApiFixtures = tasks.register<Sync>("syncApiFixtures") {
    from(rootProject.layout.projectDirectory) {
        include("list.json")
    }
    into(layout.projectDirectory.dir("src/test/resources/fixtures"))
}

tasks.matching { it.name.contains("UnitTestJavaRes") }.configureEach {
    dependsOn(syncApiFixtures)
}

dependencies {
    testImplementation(libs.kotlinx.serialization.json)
    // ComponentActivity: the screenshot renderer needs a ViewTree lifecycle owner to compose into.
    testImplementation(libs.androidx.activity.compose)
}
