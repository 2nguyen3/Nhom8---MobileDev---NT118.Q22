import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    id("com.google.gms.google-services")
}

val localProperties = Properties()
val localPropertiesFile = rootProject.file("local.properties")
if (localPropertiesFile.exists()) {
    localProperties.load(localPropertiesFile.inputStream())
}

android {
    namespace = "com.example.heami"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.heami"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        buildConfigField("String", "VNP_TMN_CODE", "\"${localProperties.getProperty("vnp_TmnCode") ?: ""}\"")
        buildConfigField("String", "VNP_HASH_SECRET", "\"${localProperties.getProperty("vnp_HashSecret") ?: ""}\"")
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
    buildFeatures {
        buildConfig = true
    }
}

dependencies {
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    implementation(libs.firebase.auth)
    implementation(libs.cardview)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)

    // Firebase BoM
    implementation(platform(libs.firebase.bom))
    implementation("com.google.firebase:firebase-firestore:24.10.0")
    implementation(libs.firebase.analytics)

    // Thư viện Credential Manager
    implementation("androidx.credentials:credentials:1.3.0")
    implementation("androidx.credentials:credentials-play-services-auth:1.3.0")
    implementation("com.google.android.libraries.identity.googleid:googleid:1.1.1")


    // MVVM Lifecycle (ViewModel & LiveData)
    implementation(libs.lifecycle.viewmodel)
    implementation(libs.lifecycle.livedata)

    implementation("com.github.bumptech.glide:glide:4.16.0")

    implementation("com.google.android.flexbox:flexbox:3.0.0")

    // CameraX
    implementation("androidx.camera:camera-core:1.6.0")
    implementation("androidx.camera:camera-camera2:1.6.0")
    implementation("androidx.camera:camera-lifecycle:1.6.0")
    implementation("androidx.camera:camera-view:1.6.0")

    // MediaPipe Face Landmarker
    implementation("com.google.mediapipe:tasks-vision:latest.release")
    implementation("androidx.media:media:1.7.0")

    // Lifecycle Process
    implementation("androidx.lifecycle:lifecycle-process:2.8.4")

    // Realtime Database
    implementation("com.google.firebase:firebase-database")

    implementation("com.google.firebase:firebase-messaging")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
}
