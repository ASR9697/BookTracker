plugins {
    id("com.android.test")
    id("org.jetbrains.kotlin.android")
}

// Records the app's startup profile on a connected device (Android 13+, no root):
//   ANDROID_SERIAL=<device> ./gradlew :baselineprofile:connectedBenchmarkAndroidTest \
//     -Pandroid.testInstrumentationRunnerArguments.androidx.benchmark.enabledRules=BaselineProfile
// then copy the pulled *-baseline-prof.txt to app/src/main/baseline-prof.txt
// (scripts/update-baseline-profile.sh does both). It drives the app's "benchmark"
// build, a separate package, so the reader's own install is never touched.
android {
    namespace = "com.example.booktracker.baselineprofile"
    compileSdk = 36

    defaultConfig {
        minSdk = 28
        targetSdk = 36
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        val targetAppId = providers.gradleProperty("APP_ID").get() + ".benchmark"
        testInstrumentationRunnerArguments["targetAppId"] = targetAppId
        manifestPlaceholders["targetAppId"] = targetAppId
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildTypes {
        create("benchmark") {
            isDebuggable = true
            signingConfig = signingConfigs.getByName("debug")
            matchingFallbacks += listOf("release")
        }
    }

    targetProjectPath = ":app"
    experimentalProperties["android.experimental.self-instrumenting"] = true
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

// Only the benchmark variant has an app build to drive.
androidComponents {
    beforeVariants(selector().all()) { it.enable = it.buildType == "benchmark" }
}

dependencies {
    implementation("androidx.test.ext:junit:1.3.0")
    implementation("androidx.test.uiautomator:uiautomator:2.4.0")
    implementation("androidx.benchmark:benchmark-macro-junit4:1.5.0")
}
