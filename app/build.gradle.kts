plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.google.services)
}

android {
    namespace = "com.resqmeal.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.resqmeal.app"
        minSdk = 24
        targetSdk = 35
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
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
        viewBinding = true
    }
}

dependencies {
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)

    // Firebase (BOM manages versions for auth and firestore)
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.auth)
    implementation(libs.firebase.firestore)

    // Cloudinary
    implementation(libs.cloudinary.android)

    // Glide
    implementation(libs.glide)
    annotationProcessor(libs.glide.compiler)

    // Maps, Location & Geohash
    implementation(libs.osmdroid.android)
    implementation(libs.play.services.location)
    implementation(libs.geofire.android.common)

    // Shimmer & Lottie
    implementation(libs.shimmer)
    implementation(libs.lottie)

    // Navigation & Lifecycle
    implementation(libs.navigation.fragment)
    implementation(libs.navigation.ui)
    implementation(libs.lifecycle.viewmodel)
    implementation(libs.lifecycle.livedata)

    // Additional UI
    implementation(libs.swiperefreshlayout)
    implementation(libs.viewpager2)

    // Unit & Instrumentation Tests
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}
