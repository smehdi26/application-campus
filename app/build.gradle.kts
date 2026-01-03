plugins {
    alias(libs.plugins.android.application)
    id("com.google.gms.google-services")
}

android {
    namespace = "com.example.coursemanagment"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.coursemanagment"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // --- IMPORTANT FIX FOR LANGUAGES ---
        // This forces the app to keep English, French, and Arabic resources.
        resConfigs("en", "fr", "ar")
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
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    viewBinding {
        enable = true
    }
}

dependencies {
    implementation(libs.appcompat)
    implementation("com.google.android.material:material:1.9.0")
    implementation("com.google.firebase:firebase-auth:22.3.0")
    implementation("com.google.firebase:firebase-database:21.0.0")
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    implementation("com.applandeo:material-calendar-view:1.9.2") // Added Material Calendar View
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}
