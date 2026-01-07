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

    packaging {
        resources.excludes.add("META-INF/DEPENDENCIES")
    }
}

configurations.all {
    exclude(group = "com.google.guava", module = "listenablefuture")
}


dependencies {
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.firebase.auth)
    implementation(libs.firebase.database)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    // MPAndroidChart temporarily removed - can be added back later if needed
    // implementation("com.github.PhilJay:MPAndroidChart:v3.1.0")
    implementation(libs.applandeo.calendar)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)

    implementation ("com.github.bumptech.glide:glide:4.16.0")
    annotationProcessor ("com.github.bumptech.glide:compiler:4.16.0")

    // --- UI helpers for Map + Details ---
    implementation("androidx.recyclerview:recyclerview:1.4.0")
    implementation("androidx.viewpager2:viewpager2:1.1.0")

    // Image loading
    implementation("com.github.bumptech.glide:glide:4.16.0")
    annotationProcessor("com.github.bumptech.glide:compiler:4.16.0")

    // QR scanning (in-app)
    implementation("com.journeyapps:zxing-android-embedded:4.3.0")
    implementation("com.google.zxing:core:3.5.3")

    implementation("org.osmdroid:osmdroid-android:6.1.18")
    // Google API
    implementation("com.google.android.gms:play-services-auth:20.7.0") {
        exclude(group = "com.google.guava", module = "listenablefuture")
    }
    implementation("com.google.api-client:google-api-client-android:1.23.0")
    implementation("com.google.api-client:google-api-client-gson:1.23.0")
    implementation("com.google.apis:google-api-services-calendar:v3-rev411-1.25.0")
}
