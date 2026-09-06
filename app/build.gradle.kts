plugins {
  id("com.android.application")
  id("org.jetbrains.kotlin.android")
}

val appId = System.getenv("BUILD_APP_ID") ?: "demo"
val appName = System.getenv("BUILD_APP_NAME") ?: "Mi App"
val appUrl = System.getenv("BUILD_APP_URL") ?: "https://example.com"
val apiUrl = System.getenv("BUILD_API_URL") ?: "https://apilatam.workers.dev"

android {
  namespace = "com.apilatam.wrap"
  compileSdk = 34

  defaultConfig {
    applicationId = "com.apilatam.wrap"
    minSdk = 23
    targetSdk = 34
    versionCode = 1
    versionName = "1.0.0"
    buildConfigField("String", "APP_ID", "\"${appId.replace("\"", "\\\"")}\"")
    buildConfigField("String", "APP_NAME", "\"${appName.replace("\"", "\\\"")}\"")
    buildConfigField("String", "APP_URL", "\"$appUrl\"")
    buildConfigField("String", "API_URL", "\"$apiUrl\"")
  }

  signingConfigs {
    create("release") {
      val f = File(System.getenv("KEYSTORE_FILE") ?: "")
      if (f.exists() && System.getenv("KEYSTORE_B64_PRESENT") == "1") {
        storeFile = f
        storePassword = System.getenv("KEYSTORE_PASS") ?: ""
        keyAlias = System.getenv("KEY_ALIAS") ?: ""
        keyPassword = System.getenv("KEY_PASS") ?: ""
      }
    }
  }

  buildTypes {
    release {
      isMinifyEnabled = false
      if (System.getenv("KEYSTORE_B64_PRESENT") == "1") {
        signingConfig = signingConfigs.getByName("release")
      }
      proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
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
    buildConfig = true
    viewBinding = true
  }
}

dependencies {
  implementation("androidx.appcompat:appcompat:1.6.1")
  implementation("androidx.core:core-ktx:1.12.0")
  implementation(platform("com.google.firebase:firebase-bom:33.1.2"))
  implementation("com.google.firebase:firebase-messaging-ktx")
}