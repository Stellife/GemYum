plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.jetbrains.kotlin.android)
    alias(libs.plugins.google.ksp)
    alias(libs.plugins.jetbrains.kotlin.compose)
    alias(libs.plugins.jetbrains.kotlin.serialization)



}

android {
    namespace = "com.stel.gemmunch"
    compileSdk = 36 // Adjusted to match stable dependencies

    defaultConfig {
        applicationId = "com.stel.gemmunch"
        minSdk = 35
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        buildConfigField(
            "String",
            "HF_TOKEN",
            "\"${project.findProperty("HF_TOKEN") ?: ""}\""       // reads value from gradle.properties or CI env
        )

        buildConfigField(
            "String",
            "USDA_API_KEY",
            "\"${project.findProperty("USDA_API_KEY") ?: "uJiMUGGjocfI5P4F5j3g4oT6DCqGl0f0i7kwT1R4"}\""  // TODO: Move to gradle.properties before production
        )


        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
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
    composeOptions {
        // Updated to use the newer Kotlin compiler extension version
        kotlinCompilerExtensionVersion = "2.2.0"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    // ---- Core Android & Jetpack Compose ----
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.extended)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation("androidx.navigation:navigation-compose:2.9.2")
    implementation(libs.kotlinx.serialization.json)

    implementation("androidx.appcompat:appcompat:1.7.1")


    implementation("androidx.core:core-splashscreen:1.1.0-rc01")

    // ---- Google AI Edge SDKs (Phase 0) - FINAL CORRECTED ----
    implementation(libs.google.ai.edge.aicore)          // Gemini Nano
    implementation(libs.google.mediapipe.tasks.genai)   // LLM Inference
    implementation(libs.google.ai.edge.localagents.fc)  // Function‑Calling SDK
    implementation(libs.google.ai.edge.localagents.rag) // RAG AI-EDGE SDK
    implementation("com.google.mediapipe:tasks-vision:latest.release")
    
    // ---- TensorFlow Lite via Google Play Services ----
    // Using Play Services TensorFlow Lite (see dependencies below at lines 134-138)
    // This provides better integration and automatic updates
    
    // Utils
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.gson)
    
    // Health Connect
    implementation(libs.androidx.health.connect)
    implementation(libs.accompanist.permissions)

    // ---- Testing ----
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)


    // Health Connect
    implementation("androidx.health.connect:connect-client:1.1.0-rc02")

    // --- NEW: Room Database Dependencies ---
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx) // Provides Coroutine support
    ksp(libs.androidx.room.compiler)

    // Google Play Services TensorFlow Lite
    implementation("com.google.android.gms:play-services-base:18.7.2")
    implementation("com.google.android.gms:play-services-tflite-java:16.4.0")
    implementation("com.google.android.gms:play-services-tflite-gpu:16.4.0")
    implementation("com.google.android.gms:play-services-tflite-support:16.4.0")
    implementation("com.google.android.gms:play-services-tflite-acceleration-service:16.0.0-beta01")





    implementation("com.squareup.okhttp3:okhttp:5.1.0")

    // Networking for API calls
    implementation("com.squareup.retrofit2:retrofit:3.0.0")
    implementation("com.squareup.retrofit2:converter-gson:3.0.0")
    implementation("com.squareup.okhttp3:logging-interceptor:5.1.0")

    implementation("androidx.concurrent:concurrent-futures-ktx:1.3.0")

    // Android WorkManager for background processing
    implementation("androidx.work:work-runtime-ktx:2.10.2")

    // CameraX for camera functionality
    implementation("androidx.camera:camera-core:1.4.2")
    implementation("androidx.camera:camera-camera2:1.4.2")
    implementation("androidx.camera:camera-lifecycle:1.4.2")
    implementation("androidx.camera:camera-view:1.4.2")

    // Coil for image loading
    implementation("io.coil-kt:coil-compose:2.7.0")

    // Accompanist for permissions
    implementation("com.google.accompanist:accompanist-permissions:0.37.3")


}