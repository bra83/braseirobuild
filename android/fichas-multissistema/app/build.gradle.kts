plugins {
    id("com.android.application")
}

android {
    namespace = "com.braseiro.fichas"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.braseiro.fichas"
        minSdk = 23
        targetSdk = 35
        versionCode = 4
        versionName = "1.1.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    buildFeatures {
        buildConfig = false
    }
}