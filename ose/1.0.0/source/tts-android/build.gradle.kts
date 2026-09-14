plugins { id("com.android.library"); kotlin("android") }
android { namespace="braseiro.ose.tts.android"; compileSdk=35; defaultConfig { minSdk=26 } }
kotlin { jvmToolchain(17) }
dependencies { implementation(project(":tts-api")) }
