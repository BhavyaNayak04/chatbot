plugins {
    alias(libs.plugins.android.application)
    // NO kotlin-kapt plugin needed
}

android {
    namespace = "com.example.chatbot2"
    compileSdk = 36
    // Your specified SDK

    defaultConfig {
        applicationId = "com.example.chatbot2"
        minSdk = 24
        targetSdk = 33 // Your specified SDK
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // This is the correct Kotlin script syntax for the API key
        buildConfigField(
            type = "String",
            name = "GEMINI_API_KEY",
            value = "\"${findProperty("GEMINI_API_KEY") ?: ""}\""
        )
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
        // Use Java 1.8 for Room and Glide compatibility
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
}

dependencies {
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    implementation(libs.espresso.web)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)

    // Gemini
    implementation(libs.generativeai)
    implementation(libs.guava)

    // Room Database (for Java)
    implementation(libs.room.runtime)
    annotationProcessor(libs.room.compiler) // <-- USE 'annotationProcessor' for Java

    // Glide (for Java)
    implementation(libs.glide)
    annotationProcessor(libs.glide.compiler) // <-- USE 'annotationProcessor' for Java
}
android.buildFeatures.buildConfig = true