plugins { kotlin("jvm") }
kotlin { jvmToolchain(17) }
dependencies {
    testImplementation(kotlin("test"))
    testImplementation(project(":core-model"))
    testImplementation(project(":test-support"))
    testImplementation(project(":persistence-api"))
    testImplementation(project(":rules-api"))
    testImplementation(project(":rules-classic"))
    testImplementation(project(":rules-advanced"))
    testImplementation(project(":character-domain"))
    testImplementation(project(":referee-boundary"))
    testImplementation(project(":session-engine"))
    testImplementation(project(":procedural-dungeon"))
    testImplementation(project(":procedural-hex"))
    testImplementation(project(":map-settlement"))
    testImplementation(project(":npc-domain"))
    testImplementation(project(":world-domain"))
    testImplementation(project(":backup-restore"))
    testImplementation(project(":barbara-adapter"))
}
tasks.test { useJUnitPlatform() }
