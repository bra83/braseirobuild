plugins { kotlin("jvm"); kotlin("plugin.serialization") }
kotlin { jvmToolchain(17) }
dependencies {
    implementation(project(":core-model")); implementation(project(":core-rng")); implementation(project(":persistence-api"))
    implementation(project(":rules-api")); implementation(project(":rules-shared-confirmed")); implementation(project(":rules-classic")); implementation(project(":rules-advanced")); implementation(project(":character-domain"))
    implementation(project(":referee-boundary")); implementation(project(":session-engine")); implementation(project(":barbara-adapter")); implementation(project(":rag-source-registry"))
    implementation(project(":map-core")); implementation(project(":map-dungeon")); implementation(project(":map-hex")); implementation(project(":map-settlement")); implementation(project(":procedural-dungeon")); implementation(project(":procedural-hex")); implementation(project(":world-domain")); implementation(project(":npc-domain")); implementation(project(":backup-restore")); implementation(project(":tts-api"))
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")
    testImplementation(kotlin("test"))
}
tasks.test { useJUnitPlatform() }
