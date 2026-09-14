pluginManagement {
    repositories { google(); mavenCentral(); gradlePluginPortal() }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories { google(); mavenCentral() }
}
rootProject.name = "BraseiroOSE"
include(
    ":core-model",
    ":core-rng",
    ":campaign-domain",
    ":persistence-api",
    ":migration-registry",
    ":persistence-room",
    ":test-support",
    ":asset-resolver",
    ":rules-api",
    ":rules-shared-confirmed",
    ":rules-classic",
    ":rules-advanced",
    ":character-domain",
    ":referee-boundary",
    ":session-engine",
    ":barbara-adapter",
    ":rag-source-registry",
    ":map-core",
    ":map-dungeon",
    ":map-hex",
    ":map-settlement",
    ":procedural-dungeon",
    ":procedural-hex",
    ":world-domain",
    ":npc-domain",
    ":backup-restore",
    ":tts-api",
    ":tts-android",
    ":integration-tests",
    ":app-android"
)
