plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.devtools.ksp")
}

android {
    namespace = "com.makeshio.asciiwidgets"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.makeshio.asciiwidgets"
        minSdk = 26
        targetSdk = 35
        versionCode = 2
        versionName = "1.2.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildFeatures {
        compose = true
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlin {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
        }
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2025.06.00")
    implementation(composeBom)

    implementation("androidx.activity:activity-compose")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx")

    implementation("androidx.room:room-runtime:2.8.5")
    implementation("androidx.room:room-ktx:2.8.5")
    ksp("androidx.room:room-compiler:2.8.5")

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.10.2")

    testImplementation("junit:junit:4.13.2")
}

// После каждой debug-сборки кладём APK в папку «Загрузки» пользователя.
// Обычная задача без объявленных output-каталогов: иначе Gradle ругается на
// пересечение папки назначения с каталогом самого проекта.
val copyDebugApkToDownloads = tasks.register("copyDebugApkToDownloads") {
    doLast {
        val source = layout.buildDirectory.file("outputs/apk/debug/app-debug.apk").get().asFile
        val target = File(System.getProperty("user.home"), "Downloads/ascii-widgets-debug.apk")
        source.copyTo(target, overwrite = true)
        println("APK скопирован: ${target.absolutePath}")
    }
}

tasks.matching { it.name == "assembleDebug" }.configureEach {
    finalizedBy(copyDebugApkToDownloads)
}
