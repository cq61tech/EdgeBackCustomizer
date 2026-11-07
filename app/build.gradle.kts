plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.edgeback.customizer"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.edgeback.customizer"
        minSdk = 31
        targetSdk = 36
        versionCode = 2
        versionName = "0.1.0-ui"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    // Xposed API: only needed at compile time, the framework provides it.
    compileOnly("io.github.libxposed:api:102.0.0")

    val composeBom = platform("androidx.compose:compose-bom:2026.09.00")
    implementation(composeBom)
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-core")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.animation:animation")

    implementation("androidx.activity:activity-compose:1.13.0")
    implementation("androidx.appcompat:appcompat:1.7.1")
}
