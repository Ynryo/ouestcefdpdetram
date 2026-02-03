import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "fr.ynryo.ouestcefdpdetram"
    compileSdk = 36

    defaultConfig {
        manifestPlaceholders += mapOf()
        applicationId = "fr.ynryo.ouestcefdpdetram"
        minSdk = 33
        targetSdk = 36
        versionName = "1.1.1"
        versionCode = 111

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        var properties = Properties()
        properties.load(project.rootProject.file("local.properties").inputStream())
        manifestPlaceholders.putAll(mapOf("MAPS_API_KEY" to properties.getProperty("MAPS_API_KEY")))

    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            applicationIdSuffix = ".release"
            versionNameSuffix = "-release"
        }
        getByName("debug") {
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
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
    implementation("com.github.bumptech.glide:glide:4.16.0")
    annotationProcessor("com.github.bumptech.glide:compiler:4.16.0")
    implementation("com.caverock:androidsvg-aar:1.4")
    implementation(libs.play.services.location)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}