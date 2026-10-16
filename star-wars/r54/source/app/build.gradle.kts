plugins {
    id("com.android.application")
}

android {
    namespace = "com.braseiro.starwarsedge"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.braseiro.starwarsedge"
        minSdk = 26
        targetSdk = 36
        versionCode = 10244
        versionName = "1.0.244"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}


dependencies {
    implementation("com.tom-roush:pdfbox-android:2.0.27.0")
}
