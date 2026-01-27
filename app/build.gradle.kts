plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "fr.ynryo.ouestcefdpdetram"
    compileSdk = 36

    defaultConfig {
        applicationId = "fr.ynryo.ouestcefdpdetram"
        minSdk = 26
        targetSdk = 36
        versionCode = 2
        versionName = "1.0.6"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            applicationIdSuffix = "release"
            versionNameSuffix = "-release"
        }
        getByName("debug") {
            versionNameSuffix = "-debug"
            applicationIdSuffix = "debug"
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
    implementation(libs.play.services.maps)
    implementation("com.google.maps.android:android-maps-utils:3.8.2")
    implementation("com.google.maps.android:android-maps-utils:2.3.0")
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation(libs.play.services.location)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}