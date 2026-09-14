plugins { kotlin("jvm"); kotlin("plugin.serialization") }
kotlin { jvmToolchain(17) }
dependencies {
    implementation(project(":core-model"))
    implementation(project(":core-rng"))
    implementation(project(":rules-api"))
    implementation(project(":rules-shared-confirmed"))
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")
    testImplementation(kotlin("test"))
}
tasks.test { useJUnitPlatform() }
