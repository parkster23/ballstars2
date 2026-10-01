plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
}

dependencies {
    implementation(project(":shared"))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.splashscreen)
    implementation(libs.compose.uiToolingPreview)
    debugImplementation(libs.compose.uiTooling)

    // Explicitly include compose resources from shared module
    implementation(libs.compose.components.resources)
}

android {
    namespace = "venturewave.one.gridgames"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "venturewave.one.gridgames"
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        versionCode = 1
        versionName = "1.0"

        // Configure manifest merger for TensorFlow Lite GPU library
        manifestPlaceholders["tensorflow_namespace"] = "org.tensorflow.lite"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            // Exclude duplicate files from TensorFlow Lite libraries
            pickFirsts += "META-INF/DEPENDENCIES"
        }
        jniLibs {
            pickFirsts += "**/*.so"
        }
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
        compose = true
    }

    // Configure manifest merger to handle TensorFlow Lite namespace conflict
    androidResources {
        noCompress += "tflite"
        noCompress += "lite"
    }
}