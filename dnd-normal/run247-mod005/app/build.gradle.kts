import java.util.Properties
import java.util.Locale
import org.gradle.api.tasks.Copy

plugins {
    id("com.android.application")
    id("com.chaquo.python")
}

val packageRoot = if (rootProject.file("version.properties").exists()) rootProject.projectDir else rootProject.projectDir.parentFile
val releaseProperties = Properties().apply {
    packageRoot.resolve("version.properties").inputStream().use(::load)
}
val appVersionName = releaseProperties.getProperty("VERSION_NAME") ?: error("VERSION_NAME ausente")
val appVersionCode = releaseProperties.getProperty("VERSION_CODE")?.toIntOrNull() ?: error("VERSION_CODE inválido")

val signingProperties = Properties()
val signingPropertiesFile = rootProject.file("signing.properties")
val generatedNpcAssetsDir = layout.buildDirectory.dir("generated/npcAssets")
val syncNpcAssets by tasks.registering(Copy::class) {
    from(packageRoot.resolve("npc"))
    into(generatedNpcAssetsDir.map { it.dir("npc") })
}

val signingKeys = listOf("storeFile", "storePassword", "keyAlias", "keyPassword")
val environmentSigning = listOf("BRASEIRO_STORE_FILE","BRASEIRO_STORE_PASSWORD","BRASEIRO_KEY_ALIAS","BRASEIRO_KEY_PASSWORD").all { !System.getenv(it).isNullOrBlank() }
val hasReleaseKey = if (environmentSigning) {
    signingProperties.setProperty("storeFile", System.getenv("BRASEIRO_STORE_FILE"))
    signingProperties.setProperty("storePassword", System.getenv("BRASEIRO_STORE_PASSWORD"))
    signingProperties.setProperty("keyAlias", System.getenv("BRASEIRO_KEY_ALIAS"))
    signingProperties.setProperty("keyPassword", System.getenv("BRASEIRO_KEY_PASSWORD"))
    true
} else if (signingPropertiesFile.exists()) {
    signingPropertiesFile.inputStream().use(signingProperties::load)
    val missing = signingKeys.filter { signingProperties.getProperty(it).isNullOrBlank() }
    if (missing.isNotEmpty()) throw GradleException("signing.properties incompleto: faltam " + missing.joinToString(", "))
    true
} else false

android {
    namespace = "com.braseiro.dnd5e"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.braseiro.dnd5e"
        minSdk = 24
        targetSdk = 35
        versionCode = appVersionCode
        versionName = appVersionName
        ndk {
            abiFilters += listOf("arm64-v8a", "armeabi-v7a", "x86_64")
        }
    }

    signingConfigs {
        if (hasReleaseKey) {
            create("release") {
                storeFile = rootProject.file(signingProperties.getProperty("storeFile"))
                storePassword = signingProperties.getProperty("storePassword")
                keyAlias = signingProperties.getProperty("keyAlias")
                keyPassword = signingProperties.getProperty("keyPassword")
                enableV1Signing = true
                enableV2Signing = true
                enableV3Signing = true
                enableV4Signing = false
            }
        }
    }

    buildTypes {
        // Debug e release usam o mesmo applicationId. A mesma chave só é usada quando
        // signing.properties ou as variáveis BRASEIRO_* forem fornecidas. O acesso
        // padrão às pastas usa SAF e não depende de identidade OAuth no Google Cloud.
        debug {
            versionNameSuffix = "-debug"
            if (hasReleaseKey) signingConfig = signingConfigs.getByName("release")
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            if (hasReleaseKey) signingConfig = signingConfigs.getByName("release")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    sourceSets.getByName("main").apply {
        assets.srcDir(generatedNpcAssetsDir)
        // Proteção de produção: arquivos de teste copiados por engano para src/main
        // nunca entram no APK nem interrompem a compilação.
        java.exclude("**/*Test.java", "**/*Tests.java")
    }

    buildFeatures {
        buildConfig = true
    }

    testOptions {
        unitTests.isIncludeAndroidResources = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    lint {
        checkReleaseBuilds = true
        abortOnError = true
    }

    androidResources {
        // Evita recompressão de arquivos que já são compactados ou lidos aleatoriamente.
        noCompress += listOf("zip", "pdf", "mp3", "ogg", "wav", "m4a", "flac")
    }

    packaging {
        resources.excludes += setOf(
            "META-INF/DEPENDENCIES",
            "META-INF/LICENSE",
            "META-INF/LICENSE.txt",
            "META-INF/NOTICE",
            "META-INF/NOTICE.txt"
        )
    }
}

chaquopy {
    defaultConfig {
        version = "3.11"
    }
}

dependencies {
    // Sem biblioteca externa de login: a conta é escolhida pelo AccountManager do Android.
    // Extração e OCR são executados integralmente no aparelho. O modelo latino do ML Kit
    // fica empacotado no APK para não depender de download nem enviar páginas à rede.
    implementation("com.tom-roush:pdfbox-android:2.0.27.0")
    implementation("com.google.mlkit:text-recognition:16.0.1")
    implementation("androidx.core:core:1.15.0")
    implementation("androidx.documentfile:documentfile:1.0.1")
    implementation("androidx.work:work-runtime:2.11.2")

    testImplementation("junit:junit:4.13.2")
    testImplementation("org.robolectric:robolectric:4.14.1")

}


tasks.named("preBuild").configure {
    dependsOn(syncNpcAssets)
}


tasks.register("assetFootprintReport") {
    group = "verification"
    description = "Relata o tamanho dos assets empacotados sem alterar o pacote."
    doLast {
        val assets = file("src/main/assets")
        val bytes = assets.walkTopDown().filter { it.isFile }.sumOf { it.length() }
        println("ASSET_BYTES=$bytes")
        println("ASSET_MIB=%.2f".format(Locale.US, bytes / 1048576.0))
    }
}
