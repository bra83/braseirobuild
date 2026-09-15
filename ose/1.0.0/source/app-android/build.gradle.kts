plugins {
    id("com.android.application")
    kotlin("android")
}

android {
    namespace = "braseiro.ose.app"
    compileSdk = 35
    defaultConfig {
        applicationId = "braseiro.ose"
        minSdk = 26
        targetSdk = 35
        versionCode = 5
        versionName = "1.0.4"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    sourceSets["main"].assets.srcDirs("../web-ui-p1", "../assets")
    testOptions { unitTests.isIncludeAndroidResources = true }
}

kotlin { jvmToolchain(17) }

dependencies {
    implementation(project(":core-model"))
    implementation(project(":core-rng"))
    implementation(project(":persistence-api"))
    implementation(project(":persistence-room"))
    implementation(project(":rules-api"))
    implementation(project(":rules-shared-confirmed"))
    implementation(project(":rules-classic"))
    implementation(project(":rules-advanced"))
    implementation(project(":referee-boundary"))
    implementation(project(":session-engine"))
    implementation(project(":campaign-domain"))
    implementation(project(":character-domain"))
    implementation(project(":map-core"))
    implementation(project(":map-dungeon"))
    implementation(project(":map-hex"))
    implementation(project(":map-settlement"))
    implementation(project(":procedural-dungeon"))
    implementation(project(":procedural-hex"))
    implementation(project(":world-domain"))
    implementation(project(":npc-domain"))
    implementation(project(":backup-restore"))
    implementation(project(":rag-source-registry"))
    implementation(project(":asset-resolver"))
    implementation(project(":barbara-adapter"))
    implementation(project(":tts-api"))
    implementation(project(":tts-android"))
    implementation("androidx.webkit:webkit:1.12.1")
    implementation("androidx.room:room-runtime:2.6.1")

    testImplementation("junit:junit:4.13.2")
    testImplementation("org.json:json:20240303")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test:core:1.6.1")
    androidTestImplementation("androidx.test:runner:1.6.2")
}
