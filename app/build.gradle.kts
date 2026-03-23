plugins {
    alias(libs.plugins.android.application)
    id("com.google.gms.google-services")
}

android {
    namespace = "com.example.heami"
    compileSdk {
        version = release(36)
    }

    defaultConfig {
        applicationId = "com.example.heami"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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
}