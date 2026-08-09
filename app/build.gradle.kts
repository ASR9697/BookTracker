plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.devtools.ksp")
}

android {
    namespace = "com.example.booktracker.app"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.booktracker"
        minSdk = 26
        targetSdk = 36
        versionCode = 2
        versionName = "2.0.0"
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildTypes {
        release {
            signingConfig = signingConfigs.getByName("debug")
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
    implementation("androidx.compose.material3:material3-window-size-class")
    implementation("androidx.palette:palette-ktx:1.0.0")
    
    implementation("androidx.compose.ui:ui-text-google-fonts")
    implementation("androidx.core:core-splashscreen:1.0.1")

    implementation("io.coil-kt.coil3:coil-compose:3.4.0")
    implementation("io.coil-kt.coil3:coil-network-okhttp:3.4.0")

    val roomVersion = "2.8.4"
    implementation("androidx.room:room-runtime:$roomVersion")
    ksp("androidx.room:room-compiler:$roomVersion")

    val cameraxVersion = "1.4.2"
    implementation("androidx.camera:camera-camera2:$cameraxVersion")
    implementation("androidx.camera:camera-lifecycle:$cameraxVersion")
    implementation("androidx.camera:camera-view:$cameraxVersion")
    implementation("com.google.mlkit:barcode-scanning:17.3.0")
    // Security
    implementation("androidx.biometric:biometric:1.2.0-alpha05")

    // CameraX and ML Kit for Barcode / OCR for the "scan a passage into a margin note"
    // flow. Free + keyless, model bundled in the APK (no download at runtime).
    implementation("com.google.mlkit:text-recognition:16.0.1")
    // On-device handwriting recognition for the stylus scratchpad. Free + keyless;
    // downloads a small per-language model at first use.
    implementation("com.google.mlkit:digital-ink-recognition:18.1.0")

    implementation("androidx.glance:glance-appwidget:1.1.1")
    implementation("androidx.glance:glance-material3:1.1.1")
    implementation("androidx.work:work-runtime-ktx:2.10.0")
    

    implementation("com.google.code.gson:gson:2.10.1")

    testImplementation("junit:junit:4.13.2")
}
