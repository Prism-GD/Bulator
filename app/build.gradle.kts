plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.prismgd.bulator"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.prismgd.bulator"
        minSdk = 26
        targetSdk = 35
        versionCode = 3
        versionName = "0.3.0-alpha"

        ndk {
            abiFilters += listOf("arm64-v8a")
        }
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

    packaging {\n        jniLibs {\n            useLegacyPackaging = true\n        }\n    }\n\n    kotlinOptions {
        jvmTarget = "17"
    }
}
\n\ndependencies {\n    implementation("org.apache.commons:commons-compress:1.27.1")\n}\n