plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    id("com.chaquo.python")
}

android {
    namespace = "com.kemsinin.dubber"
    compileSdk {
        version = release(37)
    }

    buildFeatures {
        compose = true
    }

    defaultConfig {
        applicationId = "com.kemsinin.dubber"
        minSdk = 26
        targetSdk = 37
        versionCode = 2
        versionName = "1.1"

        ndk {
            abiFilters += listOf("arm64-v8a", "x86_64")
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            // Signed with the debug key so the CI-produced APK can be sideloaded
            // directly. Swap in a real keystore before publishing to a store.
            signingConfig = signingConfigs.getByName("debug")
        }
    }

    splits {
        abi {
            isEnable = true
            reset()
            include("arm64-v8a", "x86_64")
            // The universal APK is the one to install when you are unsure
            // which ABI your phone uses.
            isUniversalApk = true
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.coil.compose)
    debugImplementation(libs.androidx.compose.ui.tooling)
}

chaquopy {
    defaultConfig {
        version = "3.12"

        // Optional: set PYTHON_BUILD_PATH when the 3.12 interpreter is not on PATH.
        System.getenv("PYTHON_BUILD_PATH")
            ?.takeIf { it.isNotBlank() }
            ?.let { buildPython(it) }

        pip {
            install("google-generativeai")
            install("edge-tts")
            install("requests")
        }
    }
}
