plugins {
  alias(libs.plugins.android.application)
}

android {
  namespace = "com.roy.autoclicker"
  compileSdk {
    version = release(37)
  }

  defaultConfig {
    applicationId = "com.roy.autoclicker"
    minSdk = 24
    targetSdk = 37
    versionCode = 1
    versionName = "1.0"
  }

  signingConfigs {
    create("public") {
      storeFile = file("../signing/roy_pub.jks")
      storePassword = "Aa123456."
      keyAlias = "Roy"
      keyPassword = "Aa123456."
    }
  }

  buildTypes {
    debug {
      signingConfig = signingConfigs.getByName("public")
    }
    release {
      signingConfig = signingConfigs.getByName("public")
      optimization {
        enable = false
      }
    }
  }
  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
  }
}
