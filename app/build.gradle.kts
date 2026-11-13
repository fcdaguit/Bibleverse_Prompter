plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.dlbc.verseprompter"
    compileSdk = libs.versions.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "com.dlbc.verseprompter"
        minSdk = libs.versions.minSdk.get().toInt()
        targetSdk = libs.versions.targetSdk.get().toInt()
        versionCode = 1
        versionName = "1.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        ndk {
            // Vosk ships native .so files for 4 ABIs. Keeping only the two that
            // matter on real phones cuts roughly 15-20 MB off the APK.
            abiFilters += listOf("arm64-v8a", "armeabi-v7a")
        }
    }

    buildTypes {
        debug {
            isMinifyEnabled = false
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            // Unsigned by default. GitHub Actions builds assembleDebug, which is
            // signed with the auto-generated debug keystore and installs fine.
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            excludes += "/META-INF/DEPENDENCIES"
        }
        // Vosk / JNA native libraries must stay uncompressed and page-aligned.
        jniLibs {
            useLegacyPackaging = false
        }
    }

    androidResources {
        // Bible JSON in assets is already gzipped by us (.json.gz); telling aapt2
        // not to re-compress saves build time and keeps streaming reads fast.
        noCompress += listOf("gz")
    }
}

// Kotlin JVM target. Declared at the top level (Kotlin Gradle Plugin 2.x style)
// rather than inside android {} so it does not depend on AGP DSL changes.
kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

dependencies {
    // --- Core / lifecycle ----------------------------------------------------
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.kotlinx.coroutines.android)

    // --- Compose (Material 3) ------------------------------------------------
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.navigation.compose)
    debugImplementation(libs.androidx.compose.ui.tooling)

    // --- Room (offline Bible database) --------------------------------------
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    // --- DataStore (persistent user settings) -------------------------------
    implementation(libs.androidx.datastore.preferences)

    // --- Offline speech recognition -----------------------------------------
    // Used by the VoskSpeechEngine implementation in Step 3. The app still runs
    // without a Vosk model present: the engine abstraction falls back to the
    // platform SpeechRecognizer with EXTRA_PREFER_OFFLINE.
    implementation(libs.vosk.android)

    // --- Optional Bible downloader (only touched when online) ---------------
    implementation(libs.okhttp)
}

// Room schema export directory - keeps migration history in version control.
ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
    arg("room.incremental", "true")
}
