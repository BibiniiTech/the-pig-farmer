plugins {
    alias(libs.plugins.android.app)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.gms.google.services)
    alias(libs.plugins.firebase.crashlytics)
}

android {
    namespace = "com.bibiniitech.smartswine"
    compileSdk = 37
    ndkVersion = "26.1.10909125"

    defaultConfig {
        applicationId = "com.bibiniitech.smartswine"
        minSdk = 24
        targetSdk = 36
        versionCode = 25
        versionName = "1.25"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        debug {
            // Disable Crashlytics symbol upload for debug builds to avoid sync issues
            // when the symbols host is unreachable.
            configure<com.google.firebase.crashlytics.buildtools.gradle.CrashlyticsExtension> {
                mappingFileUploadEnabled = false
                nativeSymbolUploadEnabled = false
            }
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
            ndk {
                debugSymbolLevel = "FULL"
            }
        }
    }

    packaging {
        jniLibs {
            keepDebugSymbols.add("**/*.so")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    lint {
        disable += "CredentialsPlayConsole"
        disable += "OldTargetApi"
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.compose.material.icons.core)
    implementation(libs.androidx.compose.material.icons.extended)

    // Firebase
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.auth)
    implementation(libs.firebase.firestore)
    implementation(libs.firebase.analytics)
    implementation(libs.firebase.crashlytics)
    implementation(libs.firebase.functions)

    // Network & PDF
    implementation(libs.retrofit)
    implementation(libs.converter.gson)
    implementation(libs.itext7.core)
    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.billing.ktx)
    implementation(libs.play.integrity)
    implementation(libs.androidx.credentials)
    implementation(libs.androidx.credentials.play.services.auth)
    implementation(libs.googleid)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
