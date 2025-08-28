import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.gradle.api.artifacts.DependencyResolveDetails

/*
configurations.all {
    resolutionStrategy.eachDependency {
        if (requested.group == "com.google.protobuf" &&
            (requested.name == "protobuf-javalite" ||
                    requested.name == "protobuf-kotlin-lite")
        ) {
            useVersion("3.21.12") // versão compatível com signal-protocol-android 2.8.1
        }
    }
}*/

configurations.all {
    resolutionStrategy {
        // Remove a força de versões conflitantes
        force(
            "com.google.protobuf:protobuf-javalite:3.21.12"
        )

        eachDependency {
            when (requested.group) {
                "com.google.protobuf" -> {
                    if (requested.name == "protobuf-java" || requested.name == "protobuf-javalite") {
                        useVersion("3.21.12")
                        because("Force protobuf version for signal-protocol compatibility")
                    }
                }
            }
        }
    }
}

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.baselineprofile)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
    alias(libs.plugins.kotlin.serialization)
    id("com.google.gms.google-services")
}

android {
    namespace = "com.pdm.vczap_o"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.pdm.vczap_o"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            val boolean = false
            isMinifyEnabled = true //boolean
            isShrinkResources = true //boolean
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {
            isMinifyEnabled = false // Ativa mesmo no debug para testar
            isShrinkResources = false
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
    kotlin {
        compilerOptions {
            jvmTarget = JvmTarget.JVM_11
        }
    }
    buildFeatures {
        compose = true
    }



    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            excludes += "/META-INF/*.version"
            excludes += "**/*.proto"
        }
    }
}


dependencies {
    // CameraX
    implementation(libs.androidx.camera.core)
    implementation(libs.androidx.camera.camera2)
    implementation(libs.androidx.camera.lifecycle)
    implementation(libs.androidx.camera.view)

    // Room Database
    implementation(libs.androidx.room.runtime)
    ksp(libs.room.compiler)
    implementation(libs.androidx.room.ktx)

    // Status Bar
    implementation(libs.accompanist.systemuicontroller)

    //Datastore
    implementation(libs.androidx.datastore.preferences)

    // Lottie animations
    implementation(libs.lottie.compose)

    // Google maps
    implementation(libs.play.services.location)

    // Image Cropping
    implementation(libs.ucrop)

    // Media player
    implementation(libs.androidx.media3.exoplayer)
    implementation(libs.androidx.media3.ui)

    // Gson
    implementation(libs.gson)

    // Material Icons
    implementation(libs.androidx.material.icons.extended)

    // Baseline profile
    implementation(libs.androidx.profileinstaller)
    "baselineProfile"(project(":baselineprofile"))
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.ui.test.junit4)
    androidTestImplementation(libs.androidx.benchmark.macro.junit4)

    // Async Image
    implementation(libs.coil.compose)

    // Firebase libs
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.auth.ktx)
    implementation(libs.firebase.firestore.ktx){
        exclude(group = "com.google.protobuf", module = "protobuf-javalite")
    }
    implementation(libs.firebase.storage.ktx)
    implementation(libs.firebase.messaging.ktx)

    // Navigation lib
    implementation(libs.androidx.navigation.compose)

    //Retrofit API
    implementation(libs.retrofit)
    implementation(libs.converter.gson)

    // Dependency Injection
    implementation(libs.hilt.android)
    implementation(libs.androidx.hilt.navigation.compose)
    ksp(libs.hilt.android.compiler)

    implementation(libs.material3)

    implementation(libs.kotlinx.serialization.json)

    //Signal Protocol - Comentado devido a problemas de compatibilidade
    //implementation("org.whispersystems:signal-protocol-android:2.16.1") {
    //    exclude(group = "com.google.protobuf", module = "protobuf-java")
    //}

    // Versões específicas do protobuf
    implementation("com.google.protobuf:protobuf-javalite:3.21.12")
    //implementation("com.google.protobuf:protobuf-java:3.21.12")

    //Security
    implementation("androidx.security:security-crypto:1.1.0")

    //Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.10.2")

    //autenticação do Google Play Services
    implementation("com.google.android.gms:play-services-auth:21.1.0")

    // Biblioteca Biometria
    implementation("androidx.biometric:biometric:1.2.0-alpha05")

    // Default libs
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}