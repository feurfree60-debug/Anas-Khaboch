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

        // إعداد الـ debugConfig بشكل آمن لا يسبب انهيار البناء على سيرفرات GitHub
        create("debugConfig") {
            val keystoreFile = file("${rootDir}/Khaboch/debug.keystore")
            if (keystoreFile.exists()) {
                storeFile = keystoreFile
            } else {
                // إذا لم يجد الملف (على سيرفر GitHub)، سيستخدم ملف الـ debug الافتراضي المدمج بالنظام
                storeFile = file(System.getProperty("user.home") + "/.android/debug.keystore")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = signingConfigs.getByName("release")
        }
        
        debug {
            // فحص ذكي: إذا كان البناء يتم على GitHub Actions، استخدم التوقيع الافتراضي الآمن
            if (System.getenv("GITHUB_ACTIONS") == "true") {
                signingConfig = signingConfigs.getByName("debug")
            } else {
                // إذا كنت تبني محلياً على جهازك، استخدم ملفك الخاص الموجود في مجلد Khaboch
                signingConfig = signingConfigs.getByName("debugConfig")
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    // الطريقة الحديثة والمصححة لإعداد إصدار الـ JVM في كوتلن
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradient.dsl.JvmTarget.JVM_17)
    }

    buildFeatures {
        compose = true
    }
}
