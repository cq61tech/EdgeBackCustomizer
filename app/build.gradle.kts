plugins {
    id("com.android.application")
}

android {
    namespace = "com.edgeback.customizer"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.edgeback.customizer"
        minSdk = 31
        targetSdk = 35
        versionCode = 1
        versionName = "0.0.1-discovery"
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
    compileOnly("io.github.libxposed:api:102.0.0")
}
