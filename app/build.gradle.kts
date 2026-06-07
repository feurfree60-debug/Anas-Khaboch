plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.google.devtools.ksp)
    alias(libs.plugins.roborazzi)
    alias(libs.plugins.secrets)
}

android {
    namespace = "com.example"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.aisudio.dreamcraft.vjqzwx"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        // إعدادات مفتاح الرفع الخاص بالـ Release
        create("release") {
            val keystorePath = System.getenv("KEYSTORE_PATH") ?: "${rootDir}/my-upload-key.jks"
            storeFile = file(keystorePath)
            storePassword = System.getenv("STORE_PASSWORD")
            keyAlias = "upload"
            keyPassword = System.getenv("KEY_PASSWORD")
        }

        // الحل الجذري: نتحقق أولاً؛ إذا كان الملف موجوداً نستخدمه، وإذا لم يكن موجوداً (مثل سيرفر GitHub) نضع أي ملف افتراضي مؤقت حتى لا يتوقف البناء
        create("debugConfig") {
            val localKeystore = file("${rootDir}/Khaboch/debug.keystore")
            if (localKeystore.exists()) {
                storeFile = localKeystore
            } else {
                // إنشاء ملف وهمي في السيرفر لمنع انهيار وتوقف البناء
                val buildDirKeystore = file("${buildDir}/tmp/debug.keystore")
                buildDirKeystore.parentFile.mkdirs()
                if (!buildDirKeystore.exists()) {
                    buildDirKeystore.createNewFile() 
                }
                storeFile = buildDirKeystore
            }
            storePassword = "android"
            keyAlias = "androiddebugkey"
            keyPassword = "android"
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = signingConfigs.getByName("release")
        }
        
        debug {
            // نستخدم الـ debugConfig المجهز والآمن دائماً
            signingConfig = signingConfigs.getByName("debugConfig")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
    }
}
