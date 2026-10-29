plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.kapt)
    alias(libs.plugins.hilt)
    alias(libs.plugins.google.services)
    alias(libs.plugins.firebase.crashlytics)
}

android {
//    namespace = "com.hairstyle.generator.aimakeover"
    namespace = "com.hairstyle.changer.aitryon"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.hairstyle.changer.aitryon"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        // Old ailabapi key — unused; generate uses Firebase URL via LocalHairstyleRepository.
//        buildConfigField(
//            "String",
//            "API_KEY",
//            "\"XDEwSQCH1YzTuHtLZni0VMw1funqNTiRQ27LvEaTzOROZVhgOCF043u8SkeLjKt5\""
//        )
        // Old ailabapi base — unused (Retrofit NetworkModule commented).
//        buildConfigField(
//            "String",
//            "BASE_URL",
//            "\"https://www.ailabapi.com/\""
//        )
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        viewBinding = true
        buildConfig = true
    }

    packaging {
        resources {
            excludes += setOf(
                "META-INF/*.kotlin_module",
                "META-INF/*.version",
                "META-INF/proguard/**",
                "META-INF/com.android.tools/**",
                "kotlin/**",
                "DebugProbesKt.bin"
            )
        }
    }
}

configurations.configureEach {
    resolutionStrategy {
        force("androidx.core:core-ktx:${libs.versions.coreKtx.get()}")
        force("androidx.core:core:${libs.versions.coreKtx.get()}")
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.constraintlayout)
    implementation(libs.lifecycle.viewmodel.ktx)
    implementation(libs.lifecycle.runtime.ktx)
    implementation(libs.lifecycle.livedata.ktx)
    implementation(libs.navigation.fragment.ktx)
    implementation(libs.navigation.ui.ktx)
    implementation(libs.activity.ktx)
    implementation(libs.fragment.ktx)
    implementation(libs.recyclerview)
    implementation("androidx.viewpager2:viewpager2:1.1.0")
    implementation(libs.hilt.android)
    kapt(libs.hilt.compiler)
    implementation(libs.retrofit)
    implementation(libs.retrofit.converter.gson)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging)
    implementation(libs.coil)
    implementation(libs.coil.gif)
    implementation(libs.androidx.media3.exoplayer)
    implementation(libs.lottie)
    implementation(libs.datastore.preferences)
    implementation(libs.coroutines.core)
    implementation(libs.coroutines.android)
    implementation(libs.ucrop)
    implementation(libs.splashscreen)

    // Firebase
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.analytics)
    implementation(libs.firebase.crashlytics)
    implementation(libs.firebase.config)
    implementation(libs.firebase.messaging)

    implementation(libs.shimmer)
    implementation("com.intuit.sdp:sdp-android:1.0.3")
    implementation("com.intuit.ssp:ssp-android:1.1.1")
    implementation("com.google.mlkit:face-detection:16.1.7")


    implementation("com.google.android.gms:play-services-ads:24.7.0")
//    implementation("com.google.android.ump:user-messaging-platform:3.1.0")
    implementation("com.google.ads.mediation:facebook:6.21.0.0")
    implementation("com.facebook.android:audience-network-sdk:6.21.0")

    // Play In-App Updates
    implementation("com.google.android.play:app-update:2.1.0")
    implementation("com.google.android.play:app-update-ktx:2.1.0")

    // Play In-App Review (no API to check if user already reviewed on Play Store)
    implementation("com.google.android.play:review:2.0.2")
    implementation("com.google.android.play:review-ktx:2.0.2")
//// Install Refer
    implementation("com.android.installreferrer:installreferrer:2.2")


}
