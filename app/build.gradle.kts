import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.devtools.ksp")
}

// Release signing comes from keystore.properties (never committed). See RELEASE.md.
val keystorePropertiesFile = rootProject.file("keystore.properties")
val keystoreProperties = Properties().apply {
    if (keystorePropertiesFile.exists()) keystorePropertiesFile.inputStream().use { load(it) }
}

android {
    namespace = "com.example.booktracker.app"
    compileSdk = 36

    defaultConfig {
        // Set once in gradle.properties; the Wear app must share it for watch sync.
        applicationId = providers.gradleProperty("APP_ID").get()
        minSdk = 26
        targetSdk = 36
        versionCode = providers.gradleProperty("APP_VERSION_CODE").get().toInt()
        versionName = providers.gradleProperty("APP_VERSION_NAME").get()
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    signingConfigs {
        create("release") {
            if (keystorePropertiesFile.exists()) {
                storeFile = rootProject.file(keystoreProperties.getProperty("storeFile"))
                storePassword = keystoreProperties.getProperty("storePassword")
                keyAlias = keystoreProperties.getProperty("keyAlias")
                keyPassword = keystoreProperties.getProperty("keyPassword")
            }
        }
    }
    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            // Without keystore.properties the release build is left unsigned
            // (never debug-signed — Play rejects debug certificates).
            signingConfig = if (keystorePropertiesFile.exists()) signingConfigs.getByName("release") else null
        }
    }
    buildFeatures {
        compose = true
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

// Google Play rejects com.example.* package names; fail release packaging early
// with a clear message instead of at upload time.
tasks.matching { it.name == "bundleRelease" || it.name == "assembleRelease" }.configureEach {
    doFirst {
        val appId = providers.gradleProperty("APP_ID").get()
        check(!appId.startsWith("com.example")) {
            "APP_ID in gradle.properties is still '$appId'. Google Play does not accept com.example.* — " +
                "set your own permanent application ID before building a release."
        }
    }
}

dependencies {
    implementation(project(":shared"))
    implementation("com.google.android.gms:play-services-wearable:19.0.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.10.2")

    implementation(platform("androidx.compose:compose-bom:2025.09.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.activity:activity-compose:1.10.1")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.9.4")
    implementation("androidx.navigation:navigation-compose:2.8.8")
    implementation("androidx.datastore:datastore-preferences:1.1.7")
    // Foldable postures: the reading timer splits at the hinge when half-open (tabletop).
    implementation("androidx.window:window:1.3.0")
    implementation("androidx.palette:palette-ktx:1.0.0")

    implementation("androidx.core:core-splashscreen:1.0.1")

    implementation("io.coil-kt.coil3:coil-compose:3.4.0")
    implementation("io.coil-kt.coil3:coil-network-okhttp:3.4.0")

    val roomVersion = "2.8.4"
    implementation("androidx.room:room-runtime:$roomVersion")
    ksp("androidx.room:room-compiler:$roomVersion")

    // CameraX and ML Kit for ISBN barcode scanning and the "scan a passage into a
    // margin note" flow. Free + keyless, models bundled in the APK.
    val cameraxVersion = "1.4.2"
    implementation("androidx.camera:camera-camera2:$cameraxVersion")
    implementation("androidx.camera:camera-lifecycle:$cameraxVersion")
    implementation("androidx.camera:camera-view:$cameraxVersion")
    implementation("com.google.mlkit:barcode-scanning:17.3.0")
    implementation("com.google.mlkit:text-recognition:16.0.1")
    // On-device handwriting recognition for the stylus scratchpad. Free + keyless;
    // downloads a small per-language model at first use. 19.x ships native code
    // aligned for 16 KB page sizes, which Google Play requires.
    implementation("com.google.mlkit:digital-ink-recognition:19.0.0")
    // "Clean page" capture: corner detection, perspective fix and shadow/finger
    // removal. The scanner UI and model live in Play services, not the APK.
    implementation("com.google.android.gms:play-services-mlkit-document-scanner:16.0.0")
    // On-device translation and language detection for foreign passages. Free +
    // keyless; each language pair downloads a ~30 MB model on first use.
    implementation("com.google.mlkit:translate:17.0.3")
    implementation("com.google.mlkit:language-id:17.0.6")
    // Drive sync sign-in: Google's AuthorizationClient for the drive.appdata scope.
    // Drive itself is called over plain HTTPS (sync/DriveClient.kt), no Drive SDK.
    implementation("com.google.android.gms:play-services-auth:21.3.0")

    // App lock (fingerprint / face / screen lock)
    implementation("androidx.biometric:biometric:1.2.0-alpha05")

    implementation("androidx.glance:glance-appwidget:1.1.1")
    implementation("androidx.glance:glance-material3:1.1.1")
    implementation("androidx.work:work-runtime-ktx:2.10.0")

    implementation("com.google.code.gson:gson:2.10.1")

    testImplementation("junit:junit:4.13.2")
}
