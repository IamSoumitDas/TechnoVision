import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.google.devtools.ksp)
    alias(libs.plugins.hilt.android)
    alias(libs.plugins.firebase.crashlytics)
    // Add the Google Services plugin for Firebase
    id("com.google.gms.google-services")
}

val now: LocalDateTime = LocalDateTime.now()
// Format: yMMddHHmm (e.g. 611272214 -> 611,272,214, fits comfortably inside 32-bit Int max 2,147,483,647)
val autoVersionCode = (now.year % 10 * 100000000) +
        (now.monthValue * 1000000) +
        (now.dayOfMonth * 10000) +
        (now.hour * 100) +
        now.minute
val autoVersionName = "1.0.0"

android {
    namespace = "com.soumit.technovision"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.soumit.technovision"
        minSdk = 29
        targetSdk = 37
        versionCode = autoVersionCode
        versionName = autoVersionName

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    testOptions {
        unitTests.all {
            it.testLogging {
                events("passed", "skipped", "failed", "standardError")
                showStandardStreams = false
            }
        }
    }

    signingConfigs {
        create("release") {
            val keystoreFile = project.rootProject.file("app/release.jks")
            if (keystoreFile.exists()) {
                storeFile = keystoreFile
                storePassword = System.getenv("KEYSTORE_PASSWORD") ?: ""
                keyAlias = System.getenv("KEY_ALIAS") ?: ""
                keyPassword = System.getenv("KEY_PASSWORD") ?: ""
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            val keystoreFile = project.rootProject.file("app/release.jks")
            if (keystoreFile.exists()) {
                signingConfig = signingConfigs.getByName("release")
            }
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
    }
}
androidComponents {
    onVariants(selector().all()) { variant ->
        val appName = "TechnoVision"
        //val buildType = variant.buildType ?: "debug"

        variant.outputs.forEach { output ->
            val versionName = output.versionName.orNull ?: "1.0"

            output.outputFileName.set(
                //"${appName}_${buildType}_v${versionName}.apk"
                "${appName}_v${versionName}.apk"
            )
        }
    }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.kotlinx.coroutines.play.services)
    implementation(libs.kotlinx.serialization.json)

    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.auth)
    implementation(libs.firebase.firestore)
    implementation(libs.firebase.crashlytics)
    implementation(libs.androidx.navigation.compose)
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    androidTestImplementation(libs.androidx.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
    implementation(libs.firebase.firestore)
    implementation(libs.androidx.compose.material.icons.extended)

    // Room
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    // Hilt
    implementation(libs.hilt.android)
    implementation(libs.androidx.hilt.navigation.compose)
    ksp(libs.hilt.compiler)

    // WorkManager
    implementation(libs.androidx.work.runtime.ktx)

    // ProfileInstaller for Ahead-Of-Time Baseline Profiles
    implementation(libs.androidx.profileinstaller)
}