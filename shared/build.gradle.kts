import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompilationTask

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidMultiplatformLibrary)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.realm)
    //alias(libs.plugins.sqldelight)
    kotlin("plugin.serialization") version "2.1.0"
}

// Suppress deprecation warnings as errors for AGP KMP compatibility
tasks.withType<KotlinCompilationTask<*>>().configureEach {
    compilerOptions {
        freeCompilerArgs.add("-Xsuppress-version-warnings")
    }
}

kotlin {
    // TensorFlowLiteTaskVision ships as a single prebuilt universal
    // .framework (static_framework, VALID_ARCHS: x86_64/arm64/armv7) - the
    // same -F path works for both iosArm64 (device) and iosSimulatorArm64
    // (simulator), unlike xcframework-sliced pods. Installed via
    // `pod install` in iosApp/ (see iosApp/Podfile, IOS_HANDOFF.md).
    val tfliteFrameworksDir = "${projectDir}/../iosApp/Pods/TensorFlowLiteTaskVision/Frameworks"
    listOf(
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "Shared"
            isStatic = true
        }
        iosTarget.compilations.getByName("main") {
            cinterops {
                create("TensorFlowLiteTaskVision") {
                    defFile(project.file("src/nativeInterop/cinterop/TensorFlowLiteTaskVision.def"))
                    compilerOpts("-F$tfliteFrameworksDir")
                }
            }
            kotlinOptions.freeCompilerArgs += listOf(
                "-linker-options",
                "-F$tfliteFrameworksDir"
            )
        }
    }

    android {
        namespace = "venturewave.one.gridgames.shared"
        compileSdk = libs.versions.android.compileSdk.get().toInt()
        minSdk = libs.versions.android.minSdk.get().toInt()

        compilerOptions {
            jvmTarget = JvmTarget.JVM_11
        }

        // FIX: Enable Android resources for AGP 9.x Compose Multiplatform
        // This resolves CMP-9547 where resources aren't packaged into APK
        experimentalProperties["android.experimental.kmp.enableAndroidResources"] = true
    }
    
    sourceSets {
        androidMain.dependencies {
            implementation(libs.compose.uiToolingPreview)
            implementation(libs.compose.uiTooling)
            implementation(libs.androidx.activity.compose)

            // TensorFlow Lite Task Library for target detection (DEPRECATED - will be removed)
            implementation("org.tensorflow:tensorflow-lite-task-vision:0.4.0") {
                exclude(group = "org.tensorflow", module = "tensorflow-lite")
                exclude(group = "org.tensorflow", module = "tensorflow-lite-api")
            }
            // Manually add TensorFlow Lite dependencies (using only tensorflow-lite which includes API)
            implementation("org.tensorflow:tensorflow-lite:2.9.0")
            // GPU support for hardware acceleration on Pixel 9 (using 2.9.0 to avoid namespace conflicts)
            implementation("org.tensorflow:tensorflow-lite-gpu-delegate-plugin:0.4.0")
            implementation("org.tensorflow:tensorflow-lite-gpu:2.9.0")

            // CameraX dependencies
            implementation("androidx.camera:camera-core:1.3.1")
            implementation("androidx.camera:camera-camera2:1.3.1")
            implementation("androidx.camera:camera-lifecycle:1.3.1")
            implementation("androidx.camera:camera-view:1.3.1")

            // Kotlinx Serialization JSON
            implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")

            // SQLDelight Android Driver
            implementation(libs.sqldelight.android.driver)
        }
        commonMain.dependencies {
            implementation(libs.compose.runtime)
            implementation(libs.compose.foundation)
            implementation(libs.compose.material3)
            // Multiplatform icons artifact (works on Android AND iOS) -
            // moved here from androidMain, which only had the Android-only
            // androidx.compose.material:material-icons-extended artifact.
            implementation(libs.compose.material.iconsExtended)
            implementation(libs.compose.ui)
            implementation(libs.compose.components.resources)
            implementation(libs.compose.uiToolingPreview)
            implementation(libs.androidx.lifecycle.viewmodelCompose)
            implementation(libs.androidx.lifecycle.runtimeCompose)

            // Kotlinx Serialization Core
            implementation("org.jetbrains.kotlinx:kotlinx-serialization-core:1.7.3")

            // Realm Kotlin
            implementation(libs.realm.base)

            // SQLDelight Common
            implementation(libs.sqldelight.runtime)
            implementation(libs.sqldelight.coroutines.extensions)
        }
        iosMain.dependencies {
            // SQLDelight iOS Driver
            implementation(libs.sqldelight.native.driver)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
    }
}

dependencies {
    androidRuntimeClasspath(libs.compose.uiTooling)
}

// SQLDelight Configuration
/*
sqldelight {
    databases {
        create("BallStarsDatabase") {
            packageName.set("venturewave.one.gridgames.database")
            schemaOutputDirectory.set(file("build/dbs"))
            verifyMigrations.set(true)
        }
    }
}
*/