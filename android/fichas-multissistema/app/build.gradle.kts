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
        versionCode = 2
        versionName = "1.0.1"
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