plugins {
    id("com.android.application")
}

android {
    namespace = "tw.hualien.iplayertv"
    compileSdk = 35

    defaultConfig {
        applicationId = "tw.hualien.iplayertv"
        minSdk = 21          // Android TV 5.0+ (all Sony Bravia Android/Google TVs)
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            // Signed with the debug key so the APK installs without extra setup.
            signingConfig = signingConfigs.getByName("debug")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}
