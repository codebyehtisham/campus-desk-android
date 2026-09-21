plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.hilt)
    id("kotlin-kapt")
}

val googleServicesFile = file("google-services.json")
if (googleServicesFile.exists()) {
    apply(plugin = "com.google.gms.google-services")
}

android {
    namespace = "com.derived.campusdesk"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.derived.campusdesk"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        // Archer Console apiKey for package com.derived.campusdesk
        buildConfigField(
            "String",
            "ARCHER_API_KEY",
            "\"arch_live_t2ulr-iuWkcn0F8t2a6YlpWWDB-AbVWgVlgX_hZ3P10\"",
        )
    }

    signingConfigs {
        // Local device installs of release use the debug keystore.
        getByName("debug")
    }

    buildTypes {
        debug {
            isMinifyEnabled = false
            buildConfigField("String", "API_BASE_URL", "\"https://adequate-success-production-39da.up.railway.app\"")
            buildConfigField("String", "APP_ENV", "\"development\"")
            buildConfigField("boolean", "DEV_TOOLS", "true")
            buildConfigField("String", "INSTITUTE_SLUG", "\"explore\"")
            buildConfigField("String", "ARCHER_ENVIRONMENT", "\"development\"")
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            signingConfig = signingConfigs.getByName("debug")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
            buildConfigField("String", "API_BASE_URL", "\"https://campusdesk-production-9ab3.up.railway.app\"")
            buildConfigField("String", "APP_ENV", "\"production\"")
            buildConfigField("boolean", "DEV_TOOLS", "false")
            buildConfigField("String", "INSTITUTE_SLUG", "\"explore\"")
            buildConfigField("String", "ARCHER_ENVIRONMENT", "\"production\"")
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    implementation(project(":networking"))
    implementation(project(":shared-ui"))
    implementation(project(":auth"))
    implementation(project(":home"))
    implementation(project(":courses"))
    implementation(project(":attendance"))
    implementation(project(":profile"))
    implementation(project(":archer-sdk"))

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.material)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.hilt.android)
    kapt(libs.hilt.compiler)
    implementation(libs.hilt.navigation.compose)
    implementation(libs.okhttp)
    implementation(libs.kotlinx.serialization.json)

    // Archer push (FCM). Drop app/google-services.json from the Firebase project
    // configured in Archer Console → Push to enable token registration.
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.messaging)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    debugImplementation(libs.androidx.ui.tooling)
}

kapt {
    correctErrorTypes = true
}
